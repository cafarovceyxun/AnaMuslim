package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotificationPlan
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerParams
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerSettings
import com.cafarovceyxun.anamuslim.utils.prayer.SavedPlace

/**
 * Namaz vaxtlarının ayarları.
 *
 * ### Daşınma qaydası
 * **Bütün hesablama ayarları portativdir, bütün yer qrupu cihaza bağlıdır**
 * ([com.cafarovceyxun.anamuslim.utils.univ.PreferenceBackup.DEVICE_LOCAL_KEYS]). Qrupu yarıya
 * bölmək səssiz səhv verir: `place_name` köçüb koordinat köçməsə yeni cihaz «Bakı» yazır amma
 * hesablaya bilmir; koordinat da köçsə Bakıda alınmış ehtiyat nüsxə Berlində quraşdırılanda
 * **səhv cədvəl** verir və heç nə xəbərdarlıq etmir.
 *
 * ### Niyə tək sətir
 * [KEY_OFFSETS] və [KEY_NOTIFY] hər vaxt üçün ayrıca açar yox, **bir sətir** saxlayır — `home.layout`
 * ilə eyni səbəb: paralel açar dəstləri gec-tez bir-birindən sürüşür, biri yazılıb digəri yazılmır.
 */
object PrayerPreferences {

    /** Neçə yer yadda saxlanılır. Siyahı seçim vərəqinə sığmalıdır, tarixçə deyil. */
    const val SAVED_PLACES_LIMIT = 8

    private const val FIELD_SEPARATOR = '\u001F'
    private const val RECORD_SEPARATOR = '\u001E'

    /** Yer rejimi: cihazın mövqeyindən, yoxsa əl ilə seçilmiş nöqtədən. */
    const val MODE_GPS = "gps"
    const val MODE_MANUAL = "manual"

    /** Günəş ibadət vaxtı deyil — default olaraq nə xatırladılır, nə də geri sayımda görünür. */
    val DEFAULT_NOTIFY: Set<Prayer> = Prayer.entries.filter { it.isPrayer }.toSet()

    // region — portativ (hesablama)

    val KEY_ENABLED = PrefKey(booleanPreferencesKey("prayer.enabled"), false)
    val KEY_FAJR_ANGLE = PrefKey(doublePreferencesKey("prayer.angle.fajr"), PrayerParams.DEFAULT_ANGLE)
    val KEY_ISHA_ANGLE = PrefKey(doublePreferencesKey("prayer.angle.isha"), PrayerParams.DEFAULT_ANGLE)

    /**
     * Şəhərin hündürlüyü hesaba alınsınmı. Default **sönülü** — səbəb
     * [com.cafarovceyxun.anamuslim.utils.prayer.PrayerParams] KDoc-undadır: `adhan` və onunla
     * qurulmuş bütün ekosistem dəniz səviyyəsindədir, bizim çıxışımız isə ona saniyə dəqiqliyində
     * uyğundur. Açanda 462 m-də Axşam 4 dəqiqə gecikir.
     */
    val KEY_USE_ELEVATION = PrefKey(booleanPreferencesKey("prayer.use_elevation"), false)

    /** `"0,0,0,0,0,0"` — [Prayer.entries] sırasında dəqiqə düzəlişləri. */
    val KEY_OFFSETS = PrefKey(stringPreferencesKey("prayer.offsets"), "")

    /** `"fajr,dhuhr,!asr,…"` — `!` = söndürülmüş. Siyahıda olmayan vaxt **default** dəyərini alır. */
    val KEY_NOTIFY = PrefKey(stringPreferencesKey("prayer.notify"), "")

    // endregion

    // region — cihaza bağlı (yer)

    val KEY_LATITUDE = PrefKey(doublePreferencesKey("prayer.lat"), 0.0)
    val KEY_LONGITUDE = PrefKey(doublePreferencesKey("prayer.lng"), 0.0)

    /** Açıq bayraq — `0.0` sentinel kimi işlədilmir, çünki Qvineya körfəzi real koordinatdır. */
    val KEY_LOCATION_SET = PrefKey(booleanPreferencesKey("prayer.location_set"), false)
    val KEY_PLACE_NAME = PrefKey(stringPreferencesKey("prayer.place_name"), "")
    val KEY_LOCATION_MODE = PrefKey(stringPreferencesKey("prayer.location_mode"), MODE_GPS)

    /** Seçilmiş şəhərin (və ya GPS-in) hündürlüyü, metr. Yer qrupuna aiddir — cihaza bağlıdır. */
    val KEY_ELEVATION = PrefKey(doublePreferencesKey("prayer.elevation"), 0.0)

    /** Son yenilənmə anı — GPS rejimində 24 saatdan köhnə mövqe sakitcə təzələnir. */
    val KEY_LOCATION_AT = PrefKey(longPreferencesKey("prayer.location_at"), 0L)

    /**
     * Yadda saxlanan yerlər — istifadəçinin işlətdiyi son [SAVED_PLACES_LIMIT] nöqtə.
     *
     * Ayrıca «yadda saxla» düyməsi yoxdur: hər təyin edilən yer (GPS və ya siyahı) buraya düşür,
     * ona görə səyahətdən sonra köhnə şəhərə qayıtmaq bir toxunuşdur. Siyahı ən son işlədilən
     * başda olmaqla saxlanılır və koordinata görə təkrarlar birləşdirilir.
     */
    val KEY_SAVED_PLACES = PrefKey(stringPreferencesKey("prayer.saved_places"), "")

    /** Çalınmış bildirişlərin açarları (`"tarix#NAMAZ"`), vergüllə. */
    private val KEY_DELIVERED = PrefKey(stringPreferencesKey("prayer.delivered"), "")

    // endregion

    // region — oxu

    fun getEnabled(): Boolean = DataStoreManager.read(KEY_ENABLED)

    @Composable
    fun observeEnabled(): Boolean = DataStoreManager.observe(KEY_ENABLED)

    fun getPoint(): GeoPoint? {
        if (!DataStoreManager.read(KEY_LOCATION_SET)) return null

        val point = GeoPoint(
            latitude = DataStoreManager.read(KEY_LATITUDE),
            longitude = DataStoreManager.read(KEY_LONGITUDE),
            elevationMeters = DataStoreManager.read(KEY_ELEVATION),
        )

        return point.takeIf { it.isValid }
    }

    fun getParams(): PrayerParams = PrayerParams(
        fajrAngle = DataStoreManager.read(KEY_FAJR_ANGLE),
        ishaAngle = DataStoreManager.read(KEY_ISHA_ANGLE),
        offsetMinutes = parseOffsets(DataStoreManager.read(KEY_OFFSETS)),
        useElevation = DataStoreManager.read(KEY_USE_ELEVATION),
    )

    fun getNotify(): Set<Prayer> = parseNotify(DataStoreManager.read(KEY_NOTIFY))

    /** Planlaşdırıcıların və UI-nin oxuduğu tam vəziyyət. */
    fun getSettings(): PrayerSettings = PrayerSettings(
        enabled = getEnabled(),
        point = getPoint(),
        placeName = DataStoreManager.read(KEY_PLACE_NAME),
        params = getParams(),
        notify = getNotify(),
    )

    /**
     * UI üçün canlı vəziyyət. Ayar dəyişən kimi ekran yenilənir — cədvəl `remember(settings)` ilə
     * keşləndiyi üçün bucaq sürüşdürüləndə vaxtlar dərhal hərəkət edir.
     */
    @Composable
    fun observeSettings(): PrayerSettings {
        val locationSet = DataStoreManager.observe(KEY_LOCATION_SET)
        val latitude = DataStoreManager.observe(KEY_LATITUDE)
        val longitude = DataStoreManager.observe(KEY_LONGITUDE)
        val elevation = DataStoreManager.observe(KEY_ELEVATION)

        val point = if (locationSet) {
            GeoPoint(latitude, longitude, elevation).takeIf { it.isValid }
        } else {
            null
        }

        return PrayerSettings(
            enabled = DataStoreManager.observe(KEY_ENABLED),
            point = point,
            placeName = DataStoreManager.observe(KEY_PLACE_NAME),
            params = PrayerParams(
                fajrAngle = DataStoreManager.observe(KEY_FAJR_ANGLE),
                ishaAngle = DataStoreManager.observe(KEY_ISHA_ANGLE),
                offsetMinutes = parseOffsets(DataStoreManager.observe(KEY_OFFSETS)),
                useElevation = DataStoreManager.observe(KEY_USE_ELEVATION),
            ),
            notify = parseNotify(DataStoreManager.observe(KEY_NOTIFY)),
        )
    }

    fun getLocationMode(): String = DataStoreManager.read(KEY_LOCATION_MODE)

    fun getLocationUpdatedAt(): Long = DataStoreManager.read(KEY_LOCATION_AT)

    fun getDelivered(): Set<String> = DataStoreManager.read(KEY_DELIVERED)
        .split(',')
        .filter { it.isNotBlank() }
        .toSet()

    // endregion

    // region — yazı

    suspend fun setEnabled(enabled: Boolean) = DataStoreManager.write(KEY_ENABLED, enabled)

    suspend fun setAngles(fajrAngle: Double, ishaAngle: Double) = DataStoreManager.edit {
        this[KEY_FAJR_ANGLE.key] = fajrAngle.coerceIn(PrayerParams.ANGLE_RANGE)
        this[KEY_ISHA_ANGLE.key] = ishaAngle.coerceIn(PrayerParams.ANGLE_RANGE)
    }

    suspend fun setUseElevation(enabled: Boolean) =
        DataStoreManager.write(KEY_USE_ELEVATION, enabled)

    suspend fun setOffsets(offsets: Map<Prayer, Int>) =
        DataStoreManager.write(KEY_OFFSETS, serializeOffsets(offsets))

    suspend fun setNotify(prayers: Set<Prayer>) =
        DataStoreManager.write(KEY_NOTIFY, serializeNotify(prayers))

    /** Yeri **atomik** yazır: yarımçıq vəziyyət (koordinat var, bayraq yox) yaranmamalıdır. */
    suspend fun setLocation(
        point: GeoPoint,
        placeName: String,
        mode: String,
        atMillis: Long,
    ) {
        // Siyahı EYNİ yazıda yenilənir: ayrı `write` çağırışı yarımçıq vəziyyət yarada bilər
        // (yer dəyişib, siyahı köhnə qalıb) və istifadəçi səbəbini heç vaxt görməz.
        val places = serializePlaces(
            listOf(SavedPlace(placeName, point)) + getSavedPlaces()
        )

        DataStoreManager.edit {
            this[KEY_LATITUDE.key] = point.latitude
            this[KEY_LONGITUDE.key] = point.longitude
            this[KEY_ELEVATION.key] = point.elevationMeters
            this[KEY_PLACE_NAME.key] = placeName
            this[KEY_LOCATION_MODE.key] = mode
            this[KEY_LOCATION_AT.key] = atMillis
            this[KEY_LOCATION_SET.key] = true
            this[KEY_SAVED_PLACES.key] = places
        }
    }

    suspend fun clearLocation() = DataStoreManager.edit {
        this[KEY_LOCATION_SET.key] = false
        this[KEY_PLACE_NAME.key] = ""
        this[KEY_LOCATION_AT.key] = 0L
    }

    /**
     * Açarı çatdırılmış kimi qeyd edir və **eyni yazıda** köhnələri təmizləyir.
     *
     * Təmizləmə burada olmasa dəst sonsuz böyüyür — VOTD tərəfdə `daily_content_delivered` məhz
     * belə yığılmışdı.
     */
    suspend fun markDelivered(key: String, nowMillis: Long) {
        val pruned = PrayerNotificationPlan.pruneDelivered(getDelivered() + key, nowMillis)
        DataStoreManager.write(KEY_DELIVERED, pruned.sorted().joinToString(","))
    }

    fun getSavedPlaces(): List<SavedPlace> = parsePlaces(DataStoreManager.read(KEY_SAVED_PLACES))

    @Composable
    fun observeSavedPlaces(): List<SavedPlace> =
        parsePlaces(DataStoreManager.observe(KEY_SAVED_PLACES))

    suspend fun removeSavedPlace(place: SavedPlace) {
        val remaining = getSavedPlaces().filterNot { it.isSameSpot(place) }
        DataStoreManager.write(KEY_SAVED_PLACES, serializePlaces(remaining))
    }

    // endregion

    // region — serializasiya

    internal fun parseOffsets(raw: String): Map<Prayer, Int> {
        if (raw.isBlank()) return emptyMap()

        val pieces = raw.split(',')

        return Prayer.entries
            .mapIndexedNotNull { index, prayer ->
                val minutes = pieces.getOrNull(index)?.trim()?.toIntOrNull() ?: return@mapIndexedNotNull null
                if (minutes == 0) null else prayer to minutes.coerceIn(PrayerParams.OFFSET_RANGE)
            }
            .toMap()
    }

    internal fun serializeOffsets(offsets: Map<Prayer, Int>): String =
        Prayer.entries.joinToString(",") { prayer ->
            (offsets[prayer] ?: 0).coerceIn(PrayerParams.OFFSET_RANGE).toString()
        }

    /**
     * `home.layout` naxışı: sadalanmayan vaxt **default** dəyərini alır, ona görə gələcəkdə yeni
     * vaxt əlavə olunsa köhnə istifadəçilərin seçimi pozulmur.
     */
    internal fun parseNotify(raw: String): Set<Prayer> {
        if (raw.isBlank()) return DEFAULT_NOTIFY

        val explicit = HashMap<Prayer, Boolean>(Prayer.entries.size)

        for (token in raw.split(',')) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue

            val enabled = !trimmed.startsWith('!')
            val name = trimmed.removePrefix("!")
            val prayer = Prayer.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: continue

            explicit[prayer] = enabled
        }

        return Prayer.entries
            .filterTo(HashSet()) { explicit[it] ?: (it in DEFAULT_NOTIFY) }
    }

    /**
     * `ad␟enlik␟uzunluq␟hündürlük` sətirləri `␞` ilə ayrılır.
     *
     * Ayırıcılar qəsdən idarəedici simvollardır (U+001F, U+001E): şəhər adında vergül, tire və
     * hətta `|` ola bilər (`Gasteiz / Vitoria`, `Halle (Saale)`), bunlar isə ola bilməz.
     */
    internal fun parsePlaces(raw: String): List<SavedPlace> {
        if (raw.isBlank()) return emptyList()

        return raw.split(RECORD_SEPARATOR).mapNotNull { record ->
            val parts = record.split(FIELD_SEPARATOR)
            if (parts.size < 3) return@mapNotNull null

            val name = parts[0]
            val latitude = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val longitude = parts[2].toDoubleOrNull() ?: return@mapNotNull null
            val elevation = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0

            val point = GeoPoint(latitude, longitude, elevation)
            if (!point.isValid || name.isBlank()) return@mapNotNull null

            SavedPlace(name, point)
        }
    }

    internal fun serializePlaces(places: List<SavedPlace>): String = places
        .distinctBy { it.spotKey }
        .take(SAVED_PLACES_LIMIT)
        .joinToString(RECORD_SEPARATOR.toString()) { place ->
            listOf(
                place.name,
                place.point.latitude.toString(),
                place.point.longitude.toString(),
                place.point.elevationMeters.toString(),
            ).joinToString(FIELD_SEPARATOR.toString())
        }

    internal fun serializeNotify(prayers: Set<Prayer>): String =
        Prayer.entries.joinToString(",") { prayer ->
            val prefix = if (prayer in prayers) "" else "!"
            prefix + prayer.name.lowercase()
        }

    // endregion
}
