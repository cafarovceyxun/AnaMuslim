package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Astronomiya nüvəsi (Meeus, `adhan`-dan portlanıb).
 *
 * Referans dəyərlər dərsliklərdən gələn **sabitlərdir** — deklinasiya ekstremumları, Julian gün
 * epoxası və Meeus-un öz interpolyasiya nümunəsi. Şəhər cədvəlindən köçürülmüş rəqəm yoxdur, ona
 * görə mənbənin yuvarlaqlaşdırması testə sızmır.
 */
class PrayerMathTest {

    private fun declinationOn(dateIso: String): Double = PrayerMath
        .solarCoordinates(PrayerMath.julianDay(IsoDate.toEpochDay(dateIso)!!))
        .declinationDeg

    @Test
    fun julianDayMatchesTheUnixEpoch() {
        // 1970-01-01 00:00 UT = JD 2440587.5 (təriflə).
        assertTrue(abs(PrayerMath.julianDay(0L) - 2440587.5) < 1e-9)
        assertTrue(abs(PrayerMath.julianDay(1L) - PrayerMath.julianDay(0L) - 1.0) < 1e-9)
    }

    @Test
    fun declinationIsNearZeroAtEquinox() {
        // Ekvinoks anı gün ərzində dəyişir, ona görə tolerantlıq bir günün sürüşməsidir.
        assertTrue(abs(declinationOn("2026-03-20")) < 0.6, "${declinationOn("2026-03-20")}")
        assertTrue(abs(declinationOn("2026-09-23")) < 0.6, "${declinationOn("2026-09-23")}")
    }

    @Test
    fun declinationIsNearExtremeAtSolstice() {
        val june = declinationOn("2026-06-21")
        val december = declinationOn("2026-12-21")

        assertTrue(june in 23.2..23.45, "yay gündönümü: $june")
        assertTrue(december in -23.45..-23.2, "qış gündönümü: $december")
    }

    @Test
    fun interpolationMatchesTheTextbookExample() {
        // Meeus, Astronomical Algorithms, 3-cü fəsil — kitabın öz nümunəsi.
        val value = PrayerMath.interpolate(
            Triple(0.884226, 0.877366, 0.870531),
            factor = 0.18125,
        )

        assertTrue(abs(value - 0.876125) < 1e-6, "alındı $value")
    }

    @Test
    fun angleInterpolationWrapsAcrossZero() {
        // 359° → 1° keçidi 2° irəliləmədir, −358° yox.
        val value = PrayerMath.interpolateAngles(Triple(359.0, 0.0, 1.0), factor = 0.5)

        assertTrue(abs(value - 0.5) < 1e-9, "alındı $value")
    }

    @Test
    fun unwindAngleNormalisesBothDirections() {
        assertTrue(abs(PrayerMath.unwindAngle(370.0) - 10.0) < 1e-9)
        assertTrue(abs(PrayerMath.unwindAngle(-10.0) - 350.0) < 1e-9)
    }

    @Test
    fun altitudeIsInverseOfTheHourAngle() {
        val decl = declinationOn("2026-09-01")
        // Transitdə (H = 0) hündürlük 90 − |φ − δ| olmalıdır.
        val altitude = PrayerMath.altitudeOfCelestialBody(40.0, decl, 0.0)

        assertTrue(abs(altitude - (90.0 - abs(40.0 - decl))) < 1e-9, "alındı $altitude")
    }

    @Test
    fun asrAltitudeHanafiIsLowerThanStandard() {
        val decl = declinationOn("2026-09-01")
        val shafii = PrayerMath.asrAltitudeDeg(40.0, decl, shadowFactor = 1)
        val hanafi = PrayerMath.asrAltitudeDeg(40.0, decl, shadowFactor = 2)

        assertTrue(hanafi < shafii, "Hənəfidə kölgə uzundur, günəş alçaqdır: $hanafi vs $shafii")
    }

    @Test
    fun horizonDipLowersTheSunriseAltitude() {
        assertTrue(
            abs(PrayerMath.horizonAltitudeDeg(0.0) - PrayerMath.SUNRISE_ALTITUDE_DEG) < 1e-12,
            "dəniz səviyyəsində düzəliş olmamalıdır",
        )
        // 462 m → ~0.75° enmə (0.0347·√462).
        val dip = PrayerMath.SUNRISE_ALTITUDE_DEG - PrayerMath.horizonAltitudeDeg(462.0)
        assertTrue(abs(dip - 0.746) < 0.01, "alındı $dip")
    }

    @Test
    fun twelveDegreeBoundarySitsAtFiftyFourPointFiveSix() {
        // φ + δ − 90 = −12  →  φ = 78 − δ.  δ = 23.44 üçün 54.56°N.
        val decl = declinationOn("2026-06-21")

        assertTrue(PrayerMath.lowerCulminationAltitudeDeg(54.4, decl) < -12.0, "54.4°N həll olunur")
        assertTrue(PrayerMath.lowerCulminationAltitudeDeg(54.7, decl) > -12.0, "54.7°N olunmur")
    }

    @Test
    fun lowerCulminationWorksInSouthernHemisphere() {
        val decl = declinationOn("2026-12-21")

        assertTrue(PrayerMath.lowerCulminationAltitudeDeg(-54.4, decl) < -12.0)
        assertTrue(PrayerMath.lowerCulminationAltitudeDeg(-54.7, decl) > -12.0)
    }
}
