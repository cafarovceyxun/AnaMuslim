package com.cafarovceyxun.anamuslim.utils.prayer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Günəş astronomiyası — **saf**, yalnız `kotlin.math`.
 *
 * ### Mənbə
 * Alqoritm [adhan-js](https://github.com/batoulapps/adhan-js)-dən (Batoul Apps, **MIT**)
 * portlanıb; düsturlar Jean Meeus, *Astronomical Algorithms* (səh. 88, 93, 102, 163–165).
 * MIT → GPLv3 tək istiqamətdə uyğundur; atribusiya `CREDITS.md`-dədir.
 *
 * ⚠️ Əvvəl burada USNO-nun **aşağı dəqiqlikli** modeli vardı (tək sinus həddi, nutasiyasız).
 * Ölçüldü: günəş doğuşu open-meteo-dan bir dəqiqə gec çıxırdı. Bu model isə nutasiya, görünən
 * ulduz vaxtı və **üç günlük interpolyasiya** işlədir — nəticə `adhan` ilə saniyə dəqiqliyində
 * eynidir, yəni AlAdhan/Mihrab/çap təqvimləri ilə eyni ailədədir.
 *
 * ### Vahidlər
 * Bütün bucaqlar dərəcə, bütün vaxtlar həmin mülci günün **0h UT-dən saatıdır** (24-dən böyük və
 * ya mənfi ola bilər — qonşu günə düşən hadisə normaldır).
 */
object PrayerMath {

    /** Günəş mərkəzinin doğuş/batış hündürlüyü: refraksiya + radius = 50′. `adhan` ilə eyni. */
    const val SUNRISE_ALTITUDE_DEG = -50.0 / 60.0

    /**
     * Üfüq enməsi: hündürlükdə üfüq `0.0347·√metr` dərəcə aşağı düşür.
     *
     * ⚠️ `adhan`-da bu **yoxdur** — o, yalnız enlik/uzunluq qəbul edir. Bizdə könüllü ayardır və
     * default sönülüdür, məhz həmin ekosistemlə eyni qalmaq üçün.
     */
    private const val HORIZON_DIP_PER_SQRT_METRE = 0.0347

    private const val DEG = 180.0 / PI
    private const val RAD = PI / 180.0

    /** Unix epoxa günü → həmin günün 0h UT-si üçün Julian günü. */
    fun julianDay(epochDay: Long): Double = epochDay.toDouble() + 2440587.5

    fun julianCentury(julianDay: Double): Double = (julianDay - 2451545.0) / 36525.0

    /** Günəşin ekvatorial koordinatları və görünən ulduz vaxtı — bir gün üçün. */
    data class SolarCoordinates(
        val declinationDeg: Double,
        val rightAscensionDeg: Double,
        val apparentSiderealTimeDeg: Double,
    )

    fun solarCoordinates(julianDay: Double): SolarCoordinates {
        val t = julianCentury(julianDay)
        val l0 = meanSolarLongitude(t)
        val lp = meanLunarLongitude(t)
        val omega = ascendingLunarNodeLongitude(t)
        val lambda = apparentSolarLongitude(t, l0) * RAD
        val theta0 = meanSiderealTime(t)
        val dPsi = nutationInLongitude(l0, lp, omega)
        val dEpsilon = nutationInObliquity(l0, lp, omega)
        val epsilon0 = meanObliquityOfTheEcliptic(t)
        val epsilonApparent = apparentObliquityOfTheEcliptic(t, epsilon0) * RAD

        return SolarCoordinates(
            declinationDeg = asin(sin(epsilonApparent) * sin(lambda)) * DEG,
            rightAscensionDeg = unwindAngle(
                atan2(cos(epsilonApparent) * sin(lambda), cos(lambda)) * DEG
            ),
            apparentSiderealTimeDeg = theta0 + dPsi * cos((epsilon0 + dEpsilon) * RAD),
        )
    }

    // region — Meeus primitivləri

    private fun meanSolarLongitude(t: Double): Double =
        unwindAngle(280.4664567 + 36000.76983 * t + 0.0003032 * t.pow(2))

    private fun meanLunarLongitude(t: Double): Double =
        unwindAngle(218.3165 + 481267.8813 * t)

    private fun ascendingLunarNodeLongitude(t: Double): Double =
        unwindAngle(125.04452 - 1934.136261 * t + 0.0020708 * t.pow(2) + t.pow(3) / 450000.0)

    private fun meanSolarAnomaly(t: Double): Double =
        unwindAngle(357.52911 + 35999.05029 * t - 0.0001537 * t.pow(2))

    private fun solarEquationOfTheCenter(t: Double, meanAnomaly: Double): Double {
        val m = meanAnomaly * RAD

        return (1.914602 - 0.004817 * t - 0.000014 * t.pow(2)) * sin(m) +
            (0.019993 - 0.000101 * t) * sin(2 * m) +
            0.000289 * sin(3 * m)
    }

    private fun apparentSolarLongitude(t: Double, meanLongitude: Double): Double {
        val longitude = meanLongitude + solarEquationOfTheCenter(t, meanSolarAnomaly(t))
        val omega = 125.04 - 1934.136 * t

        return unwindAngle(longitude - 0.00569 - 0.00478 * sin(omega * RAD))
    }

    private fun meanObliquityOfTheEcliptic(t: Double): Double =
        23.439291 - 0.013004167 * t - 0.0000001639 * t.pow(2) + 0.0000005036 * t.pow(3)

    private fun apparentObliquityOfTheEcliptic(t: Double, meanObliquity: Double): Double =
        meanObliquity + 0.00256 * cos((125.04 - 1934.136 * t) * RAD)

    private fun meanSiderealTime(t: Double): Double {
        val jd = t * 36525 + 2451545.0

        return unwindAngle(
            280.46061837 + 360.98564736629 * (jd - 2451545.0) +
                0.000387933 * t.pow(2) - t.pow(3) / 38710000.0
        )
    }

    private fun nutationInLongitude(l0: Double, lp: Double, omega: Double): Double =
        (-17.2 / 3600) * sin(omega * RAD) -
            (1.32 / 3600) * sin(2 * l0 * RAD) -
            (0.23 / 3600) * sin(2 * lp * RAD) +
            (0.21 / 3600) * sin(2 * omega * RAD)

    private fun nutationInObliquity(l0: Double, lp: Double, omega: Double): Double =
        (9.2 / 3600) * cos(omega * RAD) +
            (0.57 / 3600) * cos(2 * l0 * RAD) +
            (0.1 / 3600) * cos(2 * lp * RAD) -
            (0.09 / 3600) * cos(2 * omega * RAD)

    // endregion

    // region — hadisə vaxtları (Meeus, səh. 102)

    /** Günün təxmini transiti, gün kəsri (0..1). */
    fun approximateTransit(
        longitudeDeg: Double,
        siderealTimeDeg: Double,
        rightAscensionDeg: Double,
    ): Double = normalizeToScale((rightAscensionDeg - longitudeDeg - siderealTimeDeg) / 360.0, 1.0)

    /** Dəqiqləşdirilmiş transit, 0h UT-dən saat. */
    fun correctedTransit(
        approximateTransit: Double,
        longitudeDeg: Double,
        siderealTimeDeg: Double,
        rightAscension: Triple<Double, Double, Double>,
    ): Double {
        val theta = unwindAngle(siderealTimeDeg + 360.985647 * approximateTransit)
        val alpha = unwindAngle(interpolateAngles(rightAscension, approximateTransit))
        val hourAngle = quadrantShiftAngle(theta + longitudeDeg - alpha)

        return (approximateTransit + hourAngle / -360.0) * 24.0
    }

    /**
     * Günəşin [altitudeDeg] hündürlüyünə çatdığı an (0h UT-dən saat), və ya bu hündürlük həmin gün
     * baş vermirsə **null**.
     *
     * Null hal süni deyil, real coğrafiyadır: 12° ilə lat > 54.56°N-də yay gündönümündə günəş
     * 12°-ə enmir (Moskva 55.75°N).
     */
    fun correctedHourAngle(
        approximateTransit: Double,
        altitudeDeg: Double,
        latitudeDeg: Double,
        longitudeDeg: Double,
        afterTransit: Boolean,
        siderealTimeDeg: Double,
        rightAscension: Triple<Double, Double, Double>,
        declination: Triple<Double, Double, Double>,
    ): Double? {
        val d2 = declination.second
        val numerator = sin(altitudeDeg * RAD) - sin(latitudeDeg * RAD) * sin(d2 * RAD)
        val denominator = cos(latitudeDeg * RAD) * cos(d2 * RAD)
        if (denominator == 0.0) return null

        val cosH0 = numerator / denominator
        if (cosH0 > 1.0 || cosH0 < -1.0) return null

        val h0 = acos(cosH0) * DEG
        val m = if (afterTransit) approximateTransit + h0 / 360.0 else approximateTransit - h0 / 360.0

        val theta = unwindAngle(siderealTimeDeg + 360.985647 * m)
        val alpha = unwindAngle(interpolateAngles(rightAscension, m))
        val delta = interpolate(declination, m)
        val hourAngle = theta + longitudeDeg - alpha
        val altitude = altitudeOfCelestialBody(latitudeDeg, delta, hourAngle)

        val denom = 360.0 * cos(delta * RAD) * cos(latitudeDeg * RAD) * sin(hourAngle * RAD)
        if (denom == 0.0) return null

        return (m + (altitude - altitudeDeg) / denom) * 24.0
    }

    fun altitudeOfCelestialBody(latitudeDeg: Double, declDeg: Double, hourAngleDeg: Double): Double =
        asin(
            sin(latitudeDeg * RAD) * sin(declDeg * RAD) +
                cos(latitudeDeg * RAD) * cos(declDeg * RAD) * cos(hourAngleDeg * RAD)
        ) * DEG

    /**
     * Əsrin başladığı günəş hündürlüyü.
     *
     * Tərif: cismin kölgəsi öz uzunluğunun [shadowFactor] mislinə **plus** günortadakı kölgəsinə
     * çatanda. Günorta kölgəsi `tan|φ − δ|` olduğu üçün hündürlük `atan(1 / (n + tan|φ − δ|))`.
     */
    fun asrAltitudeDeg(latDeg: Double, declDeg: Double, shadowFactor: Int): Double =
        atan(1.0 / (shadowFactor + tan(abs(latDeg - declDeg) * RAD))) * DEG

    /** [elevationMeters] hündürlüyündən görünən üfüqün hündürlüyü (dərəcə). */
    fun horizonAltitudeDeg(elevationMeters: Double): Double =
        if (elevationMeters <= 0.0) {
            SUNRISE_ALTITUDE_DEG
        } else {
            SUNRISE_ALTITUDE_DEG - HORIZON_DIP_PER_SQRT_METRE * sqrt(elevationMeters)
        }

    /**
     * Günəşin alt kulminasiyadakı (gecə yarısı) hündürlüyü — `asin(−cos(φ + δ))`.
     *
     * Hesablamada işlədilmir; testlər üçün 54.56°N sərhədini **ikinci, müstəqil** düsturla
     * təsdiqləyir (eyni düsturla yoxlamaq təsdiq deyil).
     */
    fun lowerCulminationAltitudeDeg(latDeg: Double, declDeg: Double): Double =
        asin(-cos((latDeg + declDeg) * RAD)) * DEG

    // endregion

    // region — köməkçilər

    /** Üç ardıcıl günün dəyərləri arasında interpolyasiya (Meeus, səh. 24). */
    fun interpolate(values: Triple<Double, Double, Double>, factor: Double): Double {
        val (y1, y2, y3) = values
        val a = y2 - y1
        val b = y3 - y2

        return y2 + (factor / 2.0) * (a + b + factor * (b - a))
    }

    /** Bucaqlar üçün eyni interpolyasiya — fərqlər 0–360 aralığına sarınır. */
    fun interpolateAngles(values: Triple<Double, Double, Double>, factor: Double): Double {
        val (y1, y2, y3) = values
        val a = unwindAngle(y2 - y1)
        val b = unwindAngle(y3 - y2)

        return y2 + (factor / 2.0) * (a + b + factor * (b - a))
    }

    fun unwindAngle(value: Double): Double = normalizeToScale(value, 360.0)

    private fun normalizeToScale(value: Double, max: Double): Double =
        value - max * floor(value / max)

    private fun quadrantShiftAngle(angle: Double): Double =
        if (angle in -180.0..180.0) angle else angle - 360.0 * round(angle / 360.0)

    // endregion
}
