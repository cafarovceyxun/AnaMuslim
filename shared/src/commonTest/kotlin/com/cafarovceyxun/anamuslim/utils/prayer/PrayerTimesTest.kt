package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import kotlin.math.abs
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cədvəlin özü.
 *
 * Əsas vasitə **invariant testləridir**: «Fəcr anında günəş həqiqətənmi −12°-dədir?» sualı
 * cədvəldən köçürülmüş qızıl dəyərdən qat-qat güclüdür — xarici mənbədən asılı deyil və düsturdakı
 * istənilən işarə/vahid səhvini tutur. Qızıl dəyərlər yalnız **lövbər** kimi var.
 */
class PrayerTimesTest {

    private val date = "2026-09-01"

    // region — invariantlar

    @Test
    fun sunriseAltitudeMatchesRefractionConstant() {
        val sunrise = Fx.times(date, Fx.BAKU)[Prayer.SUNRISE]!!
        val altitude = Fx.altitudeAt(Fx.BAKU, sunrise.atMillis)

        assertTrue(
            abs(altitude - PrayerMath.SUNRISE_ALTITUDE_DEG) < Fx.ROUNDING_DEGREES,
            "günəş doğuşunda hündürlük −0.833° olmalıdır, alındı $altitude",
        )
    }

    @Test
    fun fajrAndIshaAltitudesEqualConfiguredAngles() {
        val params = PrayerParams(fajrAngle = 12.0, ishaAngle = 15.0)
        val day = Fx.times(date, Fx.BAKU, params)

        val fajrAltitude = Fx.altitudeAt(Fx.BAKU, day[Prayer.FAJR]!!.atMillis)
        val ishaAltitude = Fx.altitudeAt(Fx.BAKU, day[Prayer.ISHA]!!.atMillis)

        assertTrue(abs(fajrAltitude + 12.0) < Fx.ROUNDING_DEGREES, "Fəcr: $fajrAltitude")
        assertTrue(abs(ishaAltitude + 15.0) < Fx.ROUNDING_DEGREES, "İşa: $ishaAltitude")
    }

    @Test
    fun asrSatisfiesTheShadowRatio() {
        // ⚠️ Kölgə bucağı **həmin günün 0h UT deklinasiyasından** qurulur — `adhan` belə edir və
        // biz onunla eyni qalırıq. Hadisə anının deklinasiyası ilə yoxlamaq sınandı və testi
        // yıxdı: fərq cəmi ~33 saniyədir, amma tərif baxımından yanlış müqayisədir.
        val declination = PrayerMath
            .solarCoordinates(PrayerMath.julianDay(IsoDate.toEpochDay(date)!!))
            .declinationDeg

        for (shadowFactor in listOf(1, 2)) {
            val day = Fx.times(date, Fx.BAKU, PrayerParams(asrShadowFactor = shadowFactor))
            val altitude = Fx.altitudeAt(Fx.BAKU, day[Prayer.ASR]!!.atMillis)

            // Tərif: kölgə = n + günorta kölgəsi  →  tan(zenit) = n + tan|φ − δ|
            val actual = tan((90.0 - altitude) * (kotlin.math.PI / 180.0))
            val expected = shadowFactor + tan(abs(Fx.BAKU.latitude - declination) * (kotlin.math.PI / 180.0))

            // tan(zenit) yuvarlaqlaşmaya nisbətən daha həssasdır — 30 saniyə burada ~0.02 verir.
            assertTrue(abs(actual - expected) < 0.05, "n=$shadowFactor: $actual vs $expected")
        }
    }

    @Test
    fun dhuhrIsTheSolarTransit() {
        val day = Fx.times(date, Fx.BAKU)
        val hourAngle = Fx.hourAngleOf(Fx.BAKU, day[Prayer.DHUHR]!!.atMillis)

        assertTrue(
            abs(hourAngle) < Fx.ROUNDING_DEGREES,
            "Zöhr saat bucağı sıfıra yaxın olmalıdır: $hourAngle",
        )
    }

    @Test
    fun sunriseAndMaghribAreNearlySymmetricAboutDhuhr() {
        // ⚠️ TAM simmetriya YOXDUR və olmamalıdır. Əvvəl bu test bərabərliyi 1 saniyə dəqiqliklə
        // tələb edirdi — o, günəşin mövqeyini yalnız günorta üçün hesablayan (daha kobud) yanaşmanın
        // artefaktı idi. Deklinasiya gün ərzində dəyişdiyi üçün doğuş və batış transitdən fərqli
        // məsafələrdədir; bu fərq real və müstəqil mənbələrlə (open-meteo) təsdiqlənən davranışdır.
        val day = Fx.times(date, Fx.BAKU)
        val dhuhr = day[Prayer.DHUHR]!!.atMillis
        val beforeSunrise = dhuhr - day[Prayer.SUNRISE]!!.atMillis
        val afterMaghrib = day[Prayer.MAGHRIB]!!.atMillis - dhuhr
        val gap = abs(beforeSunrise - afterMaghrib)

        assertTrue(gap < 3 * Fx.ONE_MINUTE, "asimmetriya çox böyükdür: ${gap / 1000} saniyə")
        assertTrue(gap > 0L, "tam simmetriya iterasiyanın işləmədiyini göstərərdi")
    }

    // endregion

    // region — lövbərlər

    @Test
    fun greenwichDhuhrIsNearNoonWhenEquationOfTimeIsZero() {
        // Sentyabrın əvvəlində zaman tənliyi sıfırdan keçir → 0° boylamda transit ≈ 12:00 UTC.
        val day = Fx.times(date, GeoPoint(51.4779, 0.0))
        val epochDay = IsoDate.toEpochDay(date)!!
        val hours = (day[Prayer.DHUHR]!!.atMillis - epochDay * Fx.MILLIS_PER_DAY) / Fx.MILLIS_PER_HOUR

        assertTrue(abs(hours - 12.0) < 0.05, "Qrinviç transit saatı: $hours")
    }

    @Test
    fun equatorAtEquinoxHasSixHourSymmetry() {
        // Ekvatorda ekvinoksda günəş təxminən 06:00-da doğur, 18:00-da batır (yerli günəş saatı).
        val day = Fx.times("2026-03-20", Fx.EQUATOR)
        val dhuhr = day[Prayer.DHUHR]!!.atMillis
        val halfDay = dhuhr - day[Prayer.SUNRISE]!!.atMillis

        assertTrue(
            abs(halfDay - 6 * 3_600_000L) < 5 * Fx.ONE_MINUTE,
            "yarım gün 6 saata yaxın olmalıdır: ${halfDay / 60000.0} dəq",
        )
    }

    @Test
    fun southernHemisphereProducesAnOrderedDay() {
        for (at in listOf(Fx.JAKARTA, Fx.SYDNEY)) {
            val day = Fx.times(date, at)
            assertEquals(6, day.times.size, "$at")
            assertOrdered(day)
        }
    }

    // endregion

    // region — struktur

    @Test
    fun prayersAreStrictlyOrderedAcrossFullYear() {
        // Ən dəyərli tək test: 365 gün × 16 nöqtə, hər üç fallback pilləsini əhatə edir.
        val latitudes = listOf(0.0, 20.0, 40.0, 51.0, 55.0, 60.0, 66.0, 70.0)
        var checked = 0

        for (latitude in latitudes) {
            for (sign in listOf(1.0, -1.0)) {
                val at = GeoPoint(latitude * sign, 30.0)

                for (dayIndex in 0 until 365) {
                    val dateIso = IsoDate.plusDays("2026-01-01", dayIndex)!!
                    val day = PrayerTimes.calculate(dateIso, at, Fx.DEFAULT)

                    assertNotNull(day, "$dateIso @ $at cədvəl verməlidir")
                    assertEquals(6, day.times.size, "$dateIso @ $at")
                    assertOrdered(day)
                    checked++
                }
            }
        }

        assertEquals(365 * 16, checked)
    }

    @Test
    fun offsetsShiftEachPrayerIndependently() {
        val base = Fx.times(date, Fx.BAKU)
        val shifted = Fx.times(
            date,
            Fx.BAKU,
            PrayerParams(offsetMinutes = mapOf(Prayer.FAJR to -5, Prayer.MAGHRIB to 17)),
        )

        assertEquals(-5 * Fx.ONE_MINUTE, shifted[Prayer.FAJR]!!.atMillis - base[Prayer.FAJR]!!.atMillis)
        assertEquals(17 * Fx.ONE_MINUTE, shifted[Prayer.MAGHRIB]!!.atMillis - base[Prayer.MAGHRIB]!!.atMillis)
        assertEquals(base[Prayer.ISHA]!!.atMillis, shifted[Prayer.ISHA]!!.atMillis)
        assertEquals(base[Prayer.DHUHR]!!.atMillis, shifted[Prayer.DHUHR]!!.atMillis)
    }

    @Test
    fun asrShadowFactorTwoIsLaterThanOne() {
        val shafii = Fx.times(date, Fx.BAKU, PrayerParams(asrShadowFactor = 1))[Prayer.ASR]!!
        val hanafi = Fx.times(date, Fx.BAKU, PrayerParams(asrShadowFactor = 2))[Prayer.ASR]!!

        assertTrue(hanafi.atMillis > shafii.atMillis)
        // Bakıda sentyabrda fərq təxminən bir saatdır — ofsetlə əvəz edilə bilməyəcək qədər böyük.
        assertTrue(hanafi.atMillis - shafii.atMillis > 30 * Fx.ONE_MINUTE)
    }

    @Test
    fun largerAnglesMoveFajrEarlierAndIshaLater() {
        val twelve = Fx.times(date, Fx.BAKU, PrayerParams(fajrAngle = 12.0, ishaAngle = 12.0))
        val eighteen = Fx.times(date, Fx.BAKU, PrayerParams(fajrAngle = 18.0, ishaAngle = 18.0))

        assertTrue(eighteen[Prayer.FAJR]!!.atMillis < twelve[Prayer.FAJR]!!.atMillis)
        assertTrue(eighteen[Prayer.ISHA]!!.atMillis > twelve[Prayer.ISHA]!!.atMillis)
    }

    @Test
    fun elevationIsIgnoredEntirely() {
        // ⚠️ Hündürlük QƏSDƏN modelləşdirilmir (ayar 2026-09-07-də silindi): `adhan` (MIT, bu
        // sahənin de-fakto kitabxanası) onu qəbul etmir, çap təqvimləri və Diyanet də dəniz
        // səviyyəsindədir. Tehran 1178 m-dədir — düzəliş sağ qalsaydı Axşam 4+ dəqiqə sürüşərdi,
        // yəni bu test onun geri qayıtmasını tutur.
        val tehran = GeoPoint(35.694, 51.422, elevationMeters = 1178.0)
        val seaLevel = tehran.copy(elevationMeters = 0.0)

        val high = Fx.times(date, tehran)
        val flat = Fx.times(date, seaLevel)

        Prayer.entries.forEach { prayer ->
            assertEquals(
                flat[prayer]!!.atMillis,
                high[prayer]!!.atMillis,
                "${prayer.name} hündürlükdən asılı olmamalıdır",
            )
        }
    }

    @Test
    fun timesAreRoundedToTheNearestMinute() {
        // `adhan`-ın default davranışı. Mənbədə edilir ki, ekran/bildiriş/vidcet eyni dəqiqəni
        // göstərsin — göstərmə qatında kəsmək bildirişi 40 saniyəyə qədər tez çaldırardı.
        val day = Fx.times(date, Fx.BAKU)

        for (time in day.times) {
            assertEquals(0L, time.atMillis % 60_000L, "${time.prayer} dəqiqəyə oturmalıdır")
        }
    }

    @Test
    fun malformedInputReturnsNull() {
        assertNull(PrayerTimes.calculate("2026-13-99", Fx.BAKU, Fx.DEFAULT))
        assertNull(PrayerTimes.calculate("belə tarix yoxdur", Fx.BAKU, Fx.DEFAULT))
        assertNull(PrayerTimes.calculate(date, GeoPoint(0.0, 0.0), Fx.DEFAULT), "təyin edilməmiş nöqtə")
        assertNull(PrayerTimes.calculate(date, GeoPoint(95.0, 10.0), Fx.DEFAULT), "diapazondan kənar enlik")
    }

    // endregion

    private fun assertOrdered(day: PrayerDayTimes) {
        val sorted = day.times.sortedBy { it.prayer.ordinal }

        for (index in 1 until sorted.size) {
            assertTrue(
                sorted[index].atMillis > sorted[index - 1].atMillis,
                "${day.dateIso}: ${sorted[index].prayer} ${sorted[index].atMillis} " +
                    "${sorted[index - 1].prayer} ${sorted[index - 1].atMillis}-dən sonra olmalıdır",
            )
        }
    }
}
