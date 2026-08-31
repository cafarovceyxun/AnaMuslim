package com.cafarovceyxun.anamuslim.utils.prayer

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Günəş astronomiyası — **saf**, yalnız `kotlin.math`. Nə vaxt seam-i, nə preference, nə resurs.
 *
 * Düsturlar USNO-nun «Approximate Solar Coordinates» aşağı dəqiqlikli modelidir (Meeus-un standart
 * yaxınlaşmaları, ictimai sahə). Dəqiqlik: deklinasiya ~0.01°, zaman tənliyi ~0.5 dəqiqə —
 * namaz vaxtı üçün bu, saniyələrlə ölçülən səhv deməkdir və cədvəllərin öz yuvarlaqlaşdırmasından
 * kiçikdir.
 *
 * ⚠️ Kitabxana **əlavə edilmir**: `kotlinx-datetime` burada yalnız transitivdir, birbaşa versiya
 * yazmaq CLAUDE.md-dəki «asılılıq versiya sürüşməsi» tələsini açır (yalnız iOS-da runtime-da
 * partlayır). PrayTimes.org isə LGPL-dir — ideya oxunub, kod köçürülməyib.
 */
object PrayerMath {

    /** Günəş mərkəzinin doğuş/batış hündürlüyü: refraksiya (~34′) + günəş radiusu (~16′). */
    const val SUNRISE_ALTITUDE_DEG = -0.833

    /**
     * Üfüq enməsi: hündürlükdə üfüq `0.0347·√metr` dərəcə aşağı düşür, ona görə günəş gec batır.
     * Standart geodeziya sabitidir (refraksiya nəzərə alınmış Yer radiusundan).
     */
    private const val HORIZON_DIP_PER_SQRT_METRE = 0.0347

    private const val DEG = 180.0 / kotlin.math.PI
    private const val RAD = kotlin.math.PI / 180.0

    /** Unix epoxa günü → JD. 1970-01-01 = JD 2440587.5 (0h UT). */
    private const val JD_UNIX_EPOCH = 2440587.5

    /**
     * [epochDay] gününün **yerli günorta**sına (təxminən) uyğun Julian günü.
     *
     * Günəşin mövqeyi gün ərzində dəyişdiyi üçün onu günün ortasında qiymətləndiririk: 0h UT-yə
     * yarım gün əlavə edib boylamın gətirdiyi sürüşməni çıxırıq. Bu, tək iterasiya ilə bütün
     * vaxtlar üçün kifayət qədər dəqiqdir (səhv < 1 saniyə).
     */
    fun julianDayFromEpochDay(epochDay: Long, longitudeDeg: Double): Double =
        epochDay.toDouble() + JD_UNIX_EPOCH + 0.5 - longitudeDeg / 360.0

    /** Günəşin deklinasiyası (dərəcə) və zaman tənliyi (dəqiqə). */
    data class SunPosition(
        val declinationDeg: Double,
        val eqTimeMinutes: Double,
    )

    fun sunPosition(julianDay: Double): SunPosition {
        val d = julianDay - 2451545.0

        // Orta anomaliya və orta boylam (dərəcə).
        val g = normalizeDegrees(357.529 + 0.98560028 * d)
        val q = normalizeDegrees(280.459 + 0.98564736 * d)

        // Görünən ekliptik boylam — mərkəz tənliyinin ilk iki həddi.
        val lambda = normalizeDegrees(q + 1.915 * sin(g * RAD) + 0.020 * sin(2.0 * g * RAD))

        // Ekliptikanın maili.
        val epsilon = 23.439 - 0.00000036 * d

        val declination = asin(sin(epsilon * RAD) * sin(lambda * RAD)) * DEG

        // Düz qalxma, saatla. atan2 kvadrantı özü seçir — ayrıca düzəliş lazım deyil.
        val ra = normalizeDegrees(
            atan2(cos(epsilon * RAD) * sin(lambda * RAD), cos(lambda * RAD)) * DEG
        ) / 15.0

        // Zaman tənliyi = orta günəş − həqiqi günəş. ±12 saat ətrafına gətirilir.
        var eqTime = q / 15.0 - ra
        if (eqTime > 12.0) eqTime -= 24.0
        if (eqTime < -12.0) eqTime += 24.0

        return SunPosition(declinationDeg = declination, eqTimeMinutes = eqTime * 60.0)
    }

    /**
     * [elevationMeters] hündürlüyündən görünən üfüqün hündürlüyü (dərəcə).
     *
     * Dəniz səviyyəsində (və ya mənfi hündürlükdə) [SUNRISE_ALTITUDE_DEG] qaytarır. Tehranda
     * (1178 m) ~1.19° verir, bu da Axşamı ~6 dəqiqə gecikdirir.
     */
    fun horizonAltitudeDeg(elevationMeters: Double): Double =
        if (elevationMeters <= 0.0) {
            SUNRISE_ALTITUDE_DEG
        } else {
            SUNRISE_ALTITUDE_DEG - HORIZON_DIP_PER_SQRT_METRE * sqrt(elevationMeters)
        }

    /** Günəşin meridiandan keçmə anı, UT saatı ilə. Riyazi olaraq **həmişə** mövcuddur. */
    fun transitUtcHours(longitudeDeg: Double, eqTimeMinutes: Double): Double =
        12.0 - longitudeDeg / 15.0 - eqTimeMinutes / 60.0

    /**
     * Günəşin [altitudeDeg] hündürlüyünə uyğun saat bucağı (dərəcə), və ya bu hündürlük həmin gün
     * heç vaxt baş vermirsə **null**.
     *
     * Null hal süni deyil, real coğrafiyadır: 12° ilə lat > 54.56°N-də yay gündönümündə günəş 12°-ə
     * enmir (Moskva 55.75°N). Ayrıca «yuxarı enlik» yoxlaması yazmağa ehtiyac yoxdur — bu funksiya
     * onu öz-özünə bildirir.
     */
    fun hourAngleDeg(latDeg: Double, declDeg: Double, altitudeDeg: Double): Double? {
        val numerator = sin(altitudeDeg * RAD) - sin(latDeg * RAD) * sin(declDeg * RAD)
        val denominator = cos(latDeg * RAD) * cos(declDeg * RAD)

        if (denominator == 0.0) return null

        val cosH = numerator / denominator
        if (cosH > 1.0 || cosH < -1.0) return null

        return acos(cosH) * DEG
    }

    /**
     * Əsrin başladığı günəş hündürlüyü.
     *
     * Tərif: cismin kölgəsi öz uzunluğunun [shadowFactor] mislinə **plus** günortadakı kölgəsinə
     * çatanda. Günorta kölgəsi `tan|φ − δ|` olduğu üçün hündürlük `atan(1 / (n + tan|φ − δ|))`.
     */
    fun asrAltitudeDeg(latDeg: Double, declDeg: Double, shadowFactor: Int): Double =
        atan(1.0 / (shadowFactor + tan(abs(latDeg - declDeg) * RAD))) * DEG

    /**
     * [hourAngleDeg]-in tərsi: verilmiş saat bucağında günəşin hündürlüyü (dərəcə).
     *
     * Hesablamada işlənmir — **invariant testləri** üçündür. «Fəcr anında günəş həqiqətənmi
     * −12°-dədir?» sualı cədvəldən köçürülmüş qızıl dəyərdən qat-qat güclü testdir: xarici mənbədən
     * asılı deyil və düsturdakı istənilən işarə/vahid səhvini tutur.
     */
    fun altitudeDeg(latDeg: Double, declDeg: Double, hourAngleDeg: Double): Double =
        asin(
            sin(latDeg * RAD) * sin(declDeg * RAD) +
                cos(latDeg * RAD) * cos(declDeg * RAD) * cos(hourAngleDeg * RAD)
        ) * DEG

    /**
     * Günəşin alt kulminasiyadakı (gecə yarısı) hündürlüyü — `asin(−cos(φ + δ))`.
     *
     * Hər iki yarımkürədə işləyir. Birbaşa hesablamada istifadə olunmur; testlər üçün 54.56°N
     * sərhədini müstəqil təsdiqləyən **ikinci mənbədir** (eyni düsturla yoxlamaq təsdiq deyil).
     */
    fun lowerCulminationAltitudeDeg(latDeg: Double, declDeg: Double): Double =
        asin(-cos((latDeg + declDeg) * RAD)) * DEG

    private fun normalizeDegrees(value: Double): Double {
        val wrapped = value % 360.0
        return if (wrapped < 0.0) wrapped + 360.0 else wrapped
    }
}
