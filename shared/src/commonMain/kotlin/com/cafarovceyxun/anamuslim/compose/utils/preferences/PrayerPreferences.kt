package com.cafarovceyxun.anamuslim.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotificationPlan
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerParams
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerSettings

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

    /** Yer rejimi: cihazın mövqeyindən, yoxsa əl ilə seçilmiş nöqtədən. */
    const val MODE_GPS = "gps"
    const val MODE_MANUAL = "manual"

    /** Günəş ibadət vaxtı deyil — default olaraq nə xatırladılır, nə də geri sayımda görünür. */
    val DEFAULT_NOTIFY: Set<Prayer> = Prayer.entries.filter { it.isPrayer }.toSet()

    // region — portativ (hesablama)

    val KEY_ENABLED = PrefKey(booleanPreferencesKey("prayer.enabled"), false)
    val KEY_FAJR_ANGLE = PrefKey(doublePreferencesKey("prayer.angle.fajr"), PrayerParams.DEFAULT_ANGLE)
    val KEY_ISHA_ANGLE = PrefKey(doublePreferencesKey("prayer.angle.isha"), PrayerParams.DEFAULT_ANGLE)
    val KEY_ASR_SHADOW = PrefKey(intPreferencesKey("prayer.asr_shadow"), 1)

    /**
     * Şəhərin hündürlüyü hesaba alınsınmı. Default **açıq**.
     *
     * Fiziki olaraq doğrudur və səhvin istiqaməti təhlükəlidir: dəniz səviyyəsi Axşamı
     * erkənləşdirir (Tehranda ~6 dəq), yəni iftar vaxtından əvvəl. Rəsmi UOIF cədvəlləri dəniz
     * səviyyəsində hesablandığı üçün söndürmək mümkündür.
     */
    val KEY_USE_ELEVATION = PrefKey(booleanPreferencesKey("prayer.use_elevation"), true)

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
        asrShadowFactor = DataStoreManager.read(KEY_ASR_SHADOW),
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

    suspend fun setAsrShadowFactor(factor: Int) =
        DataStoreManager.write(KEY_ASR_SHADOW, if (factor == 2) 2 else 1)

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
    ) = DataStoreManager.edit {
        this[KEY_LATITUDE.key] = point.latitude
        this[KEY_LONGITUDE.key] = point.longitude
        this[KEY_ELEVATION.key] = point.elevationMeters
        this[KEY_PLACE_NAME.key] = placeName
        this[KEY_LOCATION_MODE.key] = mode
        this[KEY_LOCATION_AT.key] = atMillis
        this[KEY_LOCATION_SET.key] = true
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

    internal fun serializeNotify(prayers: Set<Prayer>): String =
        Prayer.entries.joinToString(",") { prayer ->
            val prefix = if (prayer in prayers) "" else "!"
            prefix + prayer.name.lowercase()
        }

    // endregion
}
