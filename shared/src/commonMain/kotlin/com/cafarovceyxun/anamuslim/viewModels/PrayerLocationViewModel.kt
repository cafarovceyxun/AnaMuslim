package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import com.cafarovceyxun.anamuslim.utils.prayer.SavedPlace
import com.cafarovceyxun.anamuslim.utils.prayer.location.City
import com.cafarovceyxun.anamuslim.utils.prayer.location.CityCatalog
import com.cafarovceyxun.anamuslim.utils.prayer.location.CityCatalogStore
import com.cafarovceyxun.anamuslim.utils.prayer.location.CoordinateLabel
import com.cafarovceyxun.anamuslim.utils.prayer.location.currentDeviceLocation
import com.cafarovceyxun.anamuslim.utils.prayer.location.reverseGeocode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Yer seçimi: oflayn şəhər kataloqu, cihaz mövqeyi və əl ilə koordinat.
 *
 * Namaz **cədvəli** burada hesablanmır — o, saf funksiyadır və ekranda `remember(...)` ilə alınır.
 * ViewModel yalnız iki şeyi daşıyır: asinxron kataloq yüklənməsi və GPS sorğusu.
 */
class PrayerLocationViewModel : ViewModel() {

    private val _catalog = MutableStateFlow<CityCatalog?>(null)
    val catalog: StateFlow<CityCatalog?> = _catalog.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<City>>(emptyList())
    val results: StateFlow<List<City>> = _results.asStateFlow()

    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating.asStateFlow()

    /** GPS uğursuz oldu — ekran istifadəçini siyahıya yönləndirir. */
    private val _locationFailed = MutableStateFlow(false)
    val locationFailed: StateFlow<Boolean> = _locationFailed.asStateFlow()

    /** Cari axtarış işi — yeni hərf gələndə ləğv olunur. */
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { loadCatalog() }
    }

    /**
     * Kataloq iki pillədədir: **endirilmiş** nüsxə varsa o, yoxsa APK/IPA-nın içindəki siyahı.
     *
     * Endirmə açılışda arxa fonda gedir ([CityCatalogStore.refreshIfNeeded]), ona görə ilk açılışın
     * ilk saniyələrində burada hələ paketdəki siyahı görünə bilər. ViewModel ekranla birlikdə
     * qurulub-dağıldığı üçün (`viewModel { PrayerLocationViewModel() }`) vərəq növbəti dəfə
     * açılanda zəngin siyahı özü gəlir — ayrıca yenilənmə siqnalı lazım deyil.
     */
    private suspend fun loadCatalog() {
        val loaded = runCatching {
            withContext(Dispatchers.Default) {
                downloadedCatalog() ?: bundledCatalog()
            }
        }.onFailure { AppLogger.saveError(it, "prayer.cities") }.getOrNull() ?: return

        _catalog.value = loaded
        if (_query.value.isNotBlank()) search(_query.value)
    }

    /**
     * Endirilmiş kataloq, və ya **null** — fayl yoxdursa, oxunmursa, yaxud parse boş çıxırsa.
     *
     * Boş nəticə ayrıca yoxlanılır: [CityCatalog.parse] pozuq sətirləri **atır**, bütün faylı
     * yıxmır, yəni tamamilə zibil fayl istisna yox, sıfır şəhərlik kataloq qaytarardı və şəhər
     * seçimi səssizcə boş görünərdi.
     */
    private fun downloadedCatalog(): CityCatalog? =
        CityCatalogStore.downloadedText()
            ?.let { text -> runCatching { CityCatalog.parse(text) }.getOrNull() }
            ?.takeIf { it.size > 0 }

    private suspend fun bundledCatalog(): CityCatalog =
        CityCatalog.parse(Res.readBytes(CityCatalog.RESOURCE_PATH).decodeToString())

    /**
     * Axtarış **fon telində** işləyir və hər hərfdə əvvəlkini ləğv edir.
     *
     * ⚠️ Sinxron olsaydı yazarkən donardı. Ölçdüm (JVM, `CityCatalog.search`): paketdəki 3 500
     * şəhərdə sorğu 0,2–1,2 ms, endirilən 69 691 şəhərdə **3,3–8,2 ms** — 7–14 dəfə çox, çünki
     * axtarış hər pillədə bütün siyahını gəzir. Kotlin/Native JVM-dən bir neçə dəfə yavaşdır, yəni
     * iOS-da hər toxunuş 16,7 ms-lik kadr büdcəsini aşa bilər. Kompilyator da, testlər də bunu
     * tutmur — yalnız cihazda «klaviatura ilişir» kimi görünür.
     */
    fun search(text: String) {
        _query.value = text

        searchJob?.cancel()
        val catalog = _catalog.value
        if (catalog == null) {
            _results.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            val hits = withContext(Dispatchers.Default) { catalog.search(text) }
            // Ləğv `withContext`-dən SONRA gəlmiş ola bilər; yoxlamasaq köhnə sorğunun nəticəsi
            // yenisinin üstünə düşər.
            ensureActive()
            _results.value = hits
        }
    }

    fun dismissLocationError() {
        _locationFailed.value = false
    }

    /**
     * Cihazın mövqeyini alıb yazır.
     *
     * Koordinat **olduğu kimi** saxlanılır; ad platformanın geocoder-indən gəlir.
     *
     * ⚠️ Əvvəl ad daxildəki şəhər kataloqundan «ən yaxın şəhər» kimi seçilirdi və səhv idi:
     * kataloqda yalnız iri şəhərlər var, ona görə Gədəbəydə 30 km uzaqdakı Şəmkir yazılırdı.
     * Hündürlük də oradan tamamlanırdı — indi yalnız GPS-in özündən gəlir (hündürlük düzəlişi
     * onsuz da default sönülüdür).
     */
    fun useDeviceLocation(onSaved: () -> Unit = {}) {
        if (_locating.value) return

        _locating.value = true
        _locationFailed.value = false

        viewModelScope.launch {
            val point = runCatching { currentDeviceLocation() }
                .onFailure { AppLogger.saveError(it, "prayer.location") }
                .getOrNull()

            if (point == null || !point.isValid) {
                _locating.value = false
                _locationFailed.value = true
                return@launch
            }

            // Geocoding uğursuz olsa da (şəbəkə yox, xidmət əlçatmaz) funksiya işləyir —
            // etiket koordinat kimi göstərilir, hesablama onsuz da koordinatla gedir.
            val name = runCatching { reverseGeocode(point) }
                .onFailure { AppLogger.saveError(it, "prayer.geocode") }
                .getOrNull()

            PrayerPreferences.setLocation(
                point = point,
                placeName = name ?: CoordinateLabel.of(point),
                mode = PrayerPreferences.MODE_GPS,
                atMillis = currentEpochMillis(),
            )

            _locating.value = false
            onSaved()
        }
    }

    /**
     * Yadda saxlanmış yerə geri keçir.
     *
     * Rejim [PrayerPreferences.MODE_MANUAL] olur, hətta nöqtə əvvəl GPS-dən gəlsə də: istifadəçi
     * onu **özü seçdi**, yəni avtomatik təzələmə həmin seçimi əzməməlidir.
     */
    fun setSavedPlace(place: SavedPlace, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            PrayerPreferences.setLocation(
                point = place.point,
                placeName = place.name,
                mode = PrayerPreferences.MODE_MANUAL,
                atMillis = currentEpochMillis(),
            )
            onSaved()
        }
    }

    fun selectCity(city: City, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            PrayerPreferences.setLocation(
                point = city.point,
                placeName = city.name,
                mode = PrayerPreferences.MODE_MANUAL,
                atMillis = currentEpochMillis(),
            )
            onSaved()
        }
    }

    /**
     * Əl ilə koordinat — siyahıda olmayan yerlər üçün.
     *
     * Ad geocoder-dən gəlir, alınmasa koordinatın özü göstərilir. Hündürlük dəniz səviyyəsində
     * qalır (düzəliş default sönülüdür).
     */
    fun setManualPoint(latitude: Double, longitude: Double, onSaved: () -> Unit = {}) {
        val base = GeoPoint(latitude, longitude)
        if (!base.isValid) return

        viewModelScope.launch {
            val name = runCatching { reverseGeocode(base) }
                .onFailure { AppLogger.saveError(it, "prayer.geocode") }
                .getOrNull()

            PrayerPreferences.setLocation(
                point = base,
                placeName = name ?: CoordinateLabel.of(base),
                mode = PrayerPreferences.MODE_MANUAL,
                atMillis = currentEpochMillis(),
            )
            onSaved()
        }
    }

}
