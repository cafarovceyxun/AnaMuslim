package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.compose.components.qibla.QiblaMapLayer
import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import com.cafarovceyxun.anamuslim.utils.qibla.QiblaMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Qiblə ekranının ayarları.
 *
 * ### Niyə ayrıca sancaq saxlanılır
 * Namaz vaxtları üçün **kobud** mövqe kifayətdir (bir dəqiqə vaxt ≈ 25 km), ona görə tətbiq yalnız
 * `ACCESS_COARSE_LOCATION` istəyir. Qiblə **bucağı** üçün də kobud mövqe tam yetərlidir: 3 km
 * sürüşmə bucağı 0.1°-dən az dəyişir, sensorun öz xətası isə ±2–5°-dir.
 *
 * Amma xəritə rejimində istifadəçi **öz damını** görmək istəyir, kobud mövqe isə onu qonşu
 * məhəlləyə qoya bilər. Həlli dəqiq icazə istəmək **deyil** (bax aşağıda), sancağı əl ilə
 * dəqiqləşdirmək imkanıdır — bir dəfə qoyulur və burada saxlanılır.
 *
 * ⚠️ `ACCESS_FINE_LOCATION` qəsdən **əlavə edilmir**: o, GPS deməkdir və qapalı yerdə — yəni məhz
 * namaz qılınan yerdə — ya heç gəlmir, ya da 30–60 saniyə çəkir; üstəlik mağaza bəyannamələrini və
 * `PRIVACY.md`-ni açır. Əl ilə sancaq eyni dəqiqliyi verir və otaqda etibarlı işləyir.
 *
 * ### Daşınma
 * Hər üç açar adi ayardır — ehtiyat nüsxə onları özü daşıyır
 * ([com.cafarovceyxun.anamuslim.utils.univ.PreferenceBackup]), `DEVICE_LOCAL_KEYS`-ə **əlavə
 * edilmir**. Sancaq yer qrupuna oxşasa da `prayer.lat`-dan asılıdır: ondan uzağa düşəndə onsuz da
 * atılır ([pinFor]).
 */
object QiblaPreferences {

    /**
     * Sancağın namaz koordinatından uzaqlaşa biləcəyi hədd, metr.
     *
     * Bundan uzaqda saxlanmış sancaq **köhnədir** (istifadəçi şəhər dəyişib) və atılır — yoxsa
     * Bakıda qoyulmuş sancaq Berlində açılan xəritədə qalardı.
     */
    private const val PIN_VALID_RADIUS_METERS = 5_000.0

    private val KEY_LAYER = PrefKey(
        stringPreferencesKey("qibla.layer"),
        QiblaMapLayer.SATELLITE.id,
    )

    private val KEY_PIN_SET = PrefKey(booleanPreferencesKey("qibla.pin_set"), false)
    private val KEY_PIN_LAT = PrefKey(doublePreferencesKey("qibla.pin_lat"), 0.0)
    private val KEY_PIN_LNG = PrefKey(doublePreferencesKey("qibla.pin_lng"), 0.0)

    fun getLayer(): QiblaMapLayer = layerFromId(DataStoreManager.read(KEY_LAYER))

    @Composable
    fun observeLayer(): QiblaMapLayer = layerFromId(DataStoreManager.observe(KEY_LAYER))

    suspend fun setLayer(layer: QiblaMapLayer) {
        DataStoreManager.write(KEY_LAYER, layer.id)
    }

    /**
     * Saxlanmış sancaq, və ya yoxdursa (yaxud [base]-dən çox uzaqdırsa) **null**.
     *
     * Tanınmayan qat adı da eyni məntiqlə defolta düşür: qat kataloqdan çıxarılsa köhnə istifadəçi
     * boş xəritə yox, açıq peyk qatını görür.
     */
    fun pinFor(base: GeoPoint): GeoPoint? {
        if (!DataStoreManager.read(KEY_PIN_SET)) return null

        val pin = GeoPoint(
            latitude = DataStoreManager.read(KEY_PIN_LAT),
            longitude = DataStoreManager.read(KEY_PIN_LNG),
            elevationMeters = base.elevationMeters,
        )

        if (!pin.isValid) return null

        return pin.takeIf {
            QiblaMath.distanceMeters(base, it) <= PIN_VALID_RADIUS_METERS
        }
    }

    /**
     * Əl ilə qoyulmuş sancağı atır.
     *
     * «Yerimi dəqiqləşdir» düyməsi bunu çağırır: istifadəçi cihazın **həqiqi** mövqeyini istəyir,
     * köhnə sancaq isə xəritəni ondan uzaqda saxlayardı.
     */
    suspend fun clearPin() {
        // Jestdən qalan, hələ yazılmamış sancaq təmizlənmədən sonra onu geri qaytarmasın.
        pendingPin.value = null
        DataStoreManager.write(KEY_PIN_SET, false)
    }

    /**
     * Xəritədə sürüşdürülən sancağı saxlayır — **jest sakitləşəndən sonra, bir dəfə**.
     *
     * ⚠️ Hər jest kadrında yazmaq olmaz (2026-10-18): bu, saniyədə onlarla `DataStore` yazısı
     * deməkdir, hər yazı isə bütün ayarlar faylını (məzmun keşləri ilə ~1 MB) yenidən serializasiya
     * edir. Yazılar növbədə yığılır, növbədəki hər mesaj ayarların tam nüsxəsini saxlayır və xəritəni
     * yarım dəqiqə sürüşdürmək 256 MB-lıq heap-i doldururdu (`OutOfMemoryError`). Kompilyator, testlər
     * və qısa sınaq susur — yalnız uzun sürüşdürmədə görünür.
     *
     * Yazı ekranın scope-unda deyil, burada gedir: ekran bağlansa da son mövqe itmir.
     */
    fun savePin(point: GeoPoint) {
        pendingPin.value = point
    }

    private const val PIN_SAVE_DELAY_MS = 500L

    /** Son sürüşdürülən sancaq; `null` — gözləyən yazı yoxdur (və ya [clearPin] onu ləğv edib). */
    private val pendingPin = MutableStateFlow<GeoPoint?>(null)

    init {
        startPinWriter()
    }

    @OptIn(FlowPreview::class)
    private fun startPinWriter() {
        // `debounce` `filterNotNull`-dan **əvvəldir**: [clearPin]-in `null`-u gözləyən yazını ləğv edir.
        pendingPin
            .debounce(PIN_SAVE_DELAY_MS)
            .filterNotNull()
            .onEach { writePin(it) }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }

    /** Üç açar bir redaktədə — ayrı-ayrı yazılsa hər biri faylı yenidən yazardı. */
    private suspend fun writePin(point: GeoPoint) {
        DataStoreManager.edit {
            this[KEY_PIN_LAT.key] = point.latitude
            this[KEY_PIN_LNG.key] = point.longitude
            this[KEY_PIN_SET.key] = true
        }
    }

    private fun layerFromId(id: String): QiblaMapLayer =
        QiblaMapLayer.entries.firstOrNull { it.id == id } ?: QiblaMapLayer.SATELLITE
}
