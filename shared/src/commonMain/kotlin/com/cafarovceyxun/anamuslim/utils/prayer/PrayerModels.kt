package com.cafarovceyxun.anamuslim.utils.prayer

/**
 * Namaz vaxtı qatının modelləri — saf data, platforma və preference bilgisi yoxdur.
 *
 * Bütün anlar **UTC epoxa millisaniyəsidir**, «yerli dəqiqə» deyil. Səbəb qəsdlidir:
 * astronomiya təbii olaraq UTC anı verir (`transit = 12 − lng/15 − EqT`), yerli dəqiqəyə çevirib
 * geri qaytarmaq isə (a) saniyələri itirir, (b) DST keçid günündə «mövcud olmayan yerli saat»
 * probleminə düşür — `java.time` və `NSCalendar` onu fərqli sürüşdürür, yəni ildə iki dəfə Avropa
 * istifadəçisində Fəcr pozulardı. Ona görə bu qat
 * [com.cafarovceyxun.anamuslim.utils.epochMillisAtLocalTime] seam-inə **toxunmur**;
 * göstərmə mərhələsində `formatLocalDateTime` kifayət edir.
 */
enum class Prayer {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA,
    ;

    /** Günəş ibadət vaxtı deyil — cədvəldə göstərilir, geri sayımda və bildirişdə default olaraq yox. */
    val isPrayer: Boolean get() = this != SUNRISE
}

/**
 * Vaxtın necə alındığı. UI bunu `≈` işarəsi və izah sətri üçün oxuyur — istifadəçi yuxarı enlikdə
 * rəqəmin təxmini olduğunu bilməlidir.
 */
enum class TimeSource {
    /** Bucaq həqiqətən həll olunub. */
    ASTRONOMICAL,

    /** Bucaq çatmır, amma günəş doğur: gecə `bucaq/60` nisbətində bölünüb. */
    NIGHT_FRACTION,

    /** Günəş ümumiyyətlə doğmur/batmır: ən yaxın normal günün transit-fərqləri köçürülüb. */
    NEAREST_DAY,
}

/**
 * Onluq dərəcə + dəniz səviyyəsindən hündürlük (metr).
 *
 * ⚠️ Əvvəl burada «hündürlük saxlanmır — refraksiya sabiti onsuz da onu üstələyir» yazılmışdı.
 * **Bu, dağlıq şəhərlər üçün doğru deyil:** hündürlükdə üfüq aşağı düşür, ona görə günəş gec batır.
 * Sürüşmə Bakıda ~1 dəqiqə, Ankarada (850 m) ~5, Tehranda (1178 m) ~6 dəqiqədir — dəniz səviyyəsi
 * ilə hesablanmış **Axşam bu qədər erkən** çıxır, yəni Ramazanda iftar vaxtına birbaşa təsir edir.
 *
 * Yalnız günəş doğuşu və Axşama təsir edir: Fəcr/İşa astronomik üfüqdən ölçülən enmə bucaqlarıdır,
 * Zöhr isə transitdir.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double = 0.0,
) {
    val isValid: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)
}

/**
 * Hesablama parametrləri. Default **Fransa/UOIF 12°** — tətbiqin tək metodu budur, istifadəçi
 * bucaqları və hər vaxta ± dəqiqə düzəlişini ayarlardan özü dəyişir.
 *
 * [asrShadowFactor] **istifadəçiyə açılmır** — tətbiqin qərarı «tək metod»dur. Sahə yalnız
 * riyaziyyatın tərifini ifadə etmək və testlərdə hər iki nisbəti yoxlamaq üçün qalır; dəyəri
 * həmişə 1-dir (kölgə = obyektin uzunluğu + günorta kölgəsi).
 *
 * [useElevation]: default **SÖNÜLÜ**.
 *
 * ⚠️ Əvvəl açıq idi — «fiziki olaraq doğrudur» arqumenti ilə. Ölçmə bunu təkzib etdi: `adhan`
 * (MIT, bu sahənin de-fakto kitabxanası — Mihrab və bir çox tətbiq onu işlədir) `Coordinates(lat,
 * lng)`-dən başqa **heç nə qəbul etmir**, yəni hündürlüyü modelləşdirmir. AlAdhan, Diyanet və
 * çap təqvimləri də dəniz səviyyəsindədir. Bizim dəniz-səviyyəsi çıxışımız `adhan` ilə **saniyə
 * dəqiqliyində** üst-üstə düşür (yoxlanılıb: üç tarix, altı vaxt, fərq < 2 saniyə; yalnız Əsrdə
 * 33 saniyə).
 *
 * Yəni «düz» olmaq burada icmanın işlətdiyi cədvəllə üst-üstə düşmək deməkdir. Hündürlük düzəlişi
 * ayar olaraq qalır (462 m-də Axşamı 4 dəqiqə gecikdirir), amma **istifadəçi onu özü seçməlidir**.
 */
data class PrayerParams(
    val fajrAngle: Double = DEFAULT_ANGLE,
    val ishaAngle: Double = DEFAULT_ANGLE,
    val asrShadowFactor: Int = 1,
    val offsetMinutes: Map<Prayer, Int> = emptyMap(),
    val useElevation: Boolean = false,
) {
    fun offsetOf(prayer: Prayer): Int = offsetMinutes[prayer] ?: 0

    companion object {
        /** Fransa/UOIF: Fəcr və İşa üçün eyni 12°. */
        const val DEFAULT_ANGLE = 12.0

        /** Ayarlarda sürüşdürücünün hüdudları — bundan kənarda vaxtlar mənasızlaşır. */
        val ANGLE_RANGE = 8.0..20.0
        val OFFSET_RANGE = -30..30
    }
}

/**
 * İstifadəçinin işlətdiyi yer.
 *
 * [spotKey] koordinatı **iki onluğa** yuvarlaqlaşdırır (≈1.1 km): eyni şəhərdə alınan iki GPS
 * mövqeyi siyahını doldurmamalıdır, amma qonşu şəhərlər ayrı qalmalıdır.
 */
data class SavedPlace(
    val name: String,
    val point: GeoPoint,
) {
    val spotKey: String
        get() = "${round2(point.latitude)},${round2(point.longitude)}"

    fun isSameSpot(other: SavedPlace): Boolean = spotKey == other.spotKey

    private fun round2(value: Double): Long = kotlin.math.round(value * 100.0).toLong()
}

data class PrayerTime(
    val prayer: Prayer,
    val atMillis: Long,
    val source: TimeSource,
)

/** Bir günün cədvəli. [times] həmişə [Prayer] sırasındadır və altı elementdən ibarətdir. */
data class PrayerDayTimes(
    val dateIso: String,
    val times: List<PrayerTime>,
) {
    operator fun get(prayer: Prayer): PrayerTime? = times.firstOrNull { it.prayer == prayer }

    /** Ən azı bir vaxt təxminidir → UI izah sətrini göstərir. */
    val hasFallback: Boolean get() = times.any { it.source != TimeSource.ASTRONOMICAL }
}

/**
 * Planlaşdırma və UI üçün tam vəziyyət. [point] null = yer hələ təyin edilməyib; bu halda nə cədvəl,
 * nə bildiriş olur (UI istifadəçini yer seçməyə dəvət edir).
 */
data class PrayerSettings(
    val enabled: Boolean = false,
    val point: GeoPoint? = null,
    val placeName: String = "",
    val params: PrayerParams = PrayerParams(),
    val notify: Set<Prayer> = emptySet(),
    /**
     * Qəməri tarixə tətbiq olunan **gün** düzəlişi (−2…+2).
     *
     * [params]-a qoyulmur: ora astronomik hesablamanın girişidir, qəməri günün ona aidiyyatı
     * yoxdur — düzəliş yalnız tarixin göstərilməsinə (ekran, paylaşılan şəkil) təsir edir.
     */
    val lunarOffsetDays: Int = 0,
    /**
     * Hər namaz üçün seçilmiş bildiriş səsi. Sadalanmayan vaxt [AdhanSound.DEFAULT] alır —
     * ona görə xəritə boş ola bilər və yeni namaz/səs əlavə olunanda köhnə seçim pozulmur.
     */
    val sounds: Map<Prayer, AdhanSound> = emptyMap(),
) {
    /** Bildiriş planlaşdırmaq mümkündürmü — hər üç şərt lazımdır. */
    val canSchedule: Boolean
        get() = enabled && point?.isValid == true && notify.isNotEmpty()

    fun soundOf(prayer: Prayer): AdhanSound = sounds[prayer] ?: AdhanSound.DEFAULT
}
