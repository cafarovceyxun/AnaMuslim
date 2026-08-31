package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Astronomiya nüvəsi.
 *
 * Referans dəyərlər dərsliklərdən gələn **sabitlərdir** (deklinasiya ekstremumları, zaman
 * tənliyinin sıfır və pik günləri) — şəhər cədvəlindən köçürülmüş rəqəm deyil, ona görə mənbənin
 * öz yuvarlaqlaşdırması testə sızmır.
 */
class PrayerMathTest {

    private fun declinationOn(dateIso: String): Double =
        Fx.sun(dateIso, Fx.GREENWICH).declinationDeg

    private fun eqTimeOn(dateIso: String): Double =
        Fx.sun(dateIso, Fx.GREENWICH).eqTimeMinutes

    @Test
    fun declinationIsNearZeroAtEquinox() {
        // Ekvinoks anı gün ərzində dəyişir, ona görə tolerantlıq bir günün deklinasiya sürüşməsidir.
        assertTrue(abs(declinationOn("2026-03-20")) < 0.6, "mart ekvinoksu: ${declinationOn("2026-03-20")}")
        assertTrue(abs(declinationOn("2026-09-23")) < 0.6, "sentyabr ekvinoksu: ${declinationOn("2026-09-23")}")
    }

    @Test
    fun declinationIsNearExtremeAtSolstice() {
        val june = declinationOn("2026-06-21")
        val december = declinationOn("2026-12-21")

        assertTrue(june in 23.2..23.45, "yay gündönümü: $june")
        assertTrue(december in -23.45..-23.2, "qış gündönümü: $december")
    }

    @Test
    fun equationOfTimeMatchesKnownExtremes() {
        // Noyabrın əvvəli ≈ +16.4 dəq, fevralın ortası ≈ −14.2 dəq (Astronomical Almanac).
        val november = eqTimeOn("2026-11-03")
        val february = eqTimeOn("2026-02-11")

        assertTrue(abs(november - 16.4) < 0.8, "noyabr piki: $november")
        assertTrue(abs(february + 14.2) < 0.8, "fevral minimumu: $february")
    }

    @Test
    fun equationOfTimeCrossesZeroInEarlySeptember() {
        val september = eqTimeOn("2026-09-01")
        assertTrue(abs(september) < 1.5, "sentyabrın əvvəli sıfıra yaxın olmalıdır: $september")
    }

    @Test
    fun hourAngleReturnsNullWhenAngleUnreachable() {
        val summerDecl = declinationOn("2026-06-21")

        assertNull(
            PrayerMath.hourAngleDeg(latDeg = 60.0, declDeg = summerDecl, altitudeDeg = -12.0),
            "60°N-də yay gündönümündə günəş 12°-ə enmir",
        )
        assertNotNull(
            PrayerMath.hourAngleDeg(latDeg = 40.0, declDeg = summerDecl, altitudeDeg = -12.0),
            "40°N-də enir",
        )
    }

    @Test
    fun twelveDegreeBoundarySitsAtFiftyFourPointFiveSix() {
        // φ + δ − 90 = −12  →  φ = 78 − δ.  δ = 23.44 üçün 54.56°N.
        val decl = declinationOn("2026-06-21")

        assertNotNull(PrayerMath.hourAngleDeg(54.4, decl, -12.0), "54.4°N hələ həll olunur")
        assertNull(PrayerMath.hourAngleDeg(54.7, decl, -12.0), "54.7°N artıq həll olunmur")
    }

    @Test
    fun lowerCulminationConfirmsTheSameBoundary() {
        // Sərhədi ikinci, müstəqil düsturla təsdiqləyir — eyni düsturla yoxlamaq təsdiq deyil.
        val decl = declinationOn("2026-06-21")

        assertTrue(PrayerMath.lowerCulminationAltitudeDeg(54.4, decl) < -12.0)
        assertTrue(PrayerMath.lowerCulminationAltitudeDeg(54.7, decl) > -12.0)
    }

    @Test
    fun lowerCulminationWorksInSouthernHemisphere() {
        val decl = declinationOn("2026-12-21")

        assertTrue(PrayerMath.lowerCulminationAltitudeDeg(-54.4, decl) < -12.0)
        assertTrue(PrayerMath.lowerCulminationAltitudeDeg(-54.7, decl) > -12.0)
    }

    @Test
    fun asrAltitudeHanafiIsLowerThanStandard() {
        val decl = declinationOn("2026-09-01")
        val shafii = PrayerMath.asrAltitudeDeg(40.0, decl, shadowFactor = 1)
        val hanafi = PrayerMath.asrAltitudeDeg(40.0, decl, shadowFactor = 2)

        assertTrue(hanafi < shafii, "Hənəfidə kölgə uzundur, günəş alçaqdır: $hanafi vs $shafii")
    }

    @Test
    fun altitudeIsInverseOfHourAngle() {
        val decl = declinationOn("2026-09-01")
        val hourAngle = PrayerMath.hourAngleDeg(40.0, decl, altitudeDeg = -12.0)
        assertNotNull(hourAngle)

        val roundTrip = PrayerMath.altitudeDeg(40.0, decl, hourAngle)
        assertTrue(abs(roundTrip + 12.0) < 1e-6, "geri çevirmə: $roundTrip")
    }

    @Test
    fun julianDayAdvancesOneUnitPerDay() {
        val first = PrayerMath.julianDayFromEpochDay(IsoDate.toEpochDay("2026-09-01")!!, 0.0)
        val second = PrayerMath.julianDayFromEpochDay(IsoDate.toEpochDay("2026-09-02")!!, 0.0)

        assertTrue(abs((second - first) - 1.0) < 1e-9)
    }
}
