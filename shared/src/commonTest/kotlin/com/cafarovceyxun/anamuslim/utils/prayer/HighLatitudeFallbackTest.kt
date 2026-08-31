package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Yuxarı enlik — funksiyanın ən kövrək hissəsi.
 *
 * 12° ilə riyazi sərhəd 54.56°N-dir; tətbiqin `values-ru` dili var, yəni Moskva (55.75°N) və
 * S.Peterburq (59.94°N) **real istifadəçidir**, nəzəri hal deyil.
 */
class HighLatitudeFallbackTest {

    private val summer = "2026-06-21"
    private val winter = "2026-12-21"

    @Test
    fun parisSummerStaysAstronomical() {
        // 48.86°N < 54.56°N — sərhəddən aşağı, fallback lazım deyil.
        val day = Fx.times(summer, Fx.PARIS)

        assertTrue(day.times.all { it.source == TimeSource.ASTRONOMICAL }, "${day.times}")
        assertTrue(!day.hasFallback)
    }

    @Test
    fun moscowSummerUsesNightFraction() {
        val day = Fx.times(summer, Fx.MOSCOW)

        assertEquals(TimeSource.NIGHT_FRACTION, day[Prayer.FAJR]!!.source)
        assertEquals(TimeSource.NIGHT_FRACTION, day[Prayer.ISHA]!!.source)
        assertEquals(TimeSource.ASTRONOMICAL, day[Prayer.MAGHRIB]!!.source, "günəş hələ batır")
        assertTrue(day.hasFallback)
    }

    @Test
    fun moscowNightFractionUsesOneFifthOfTheNight() {
        // 12° / 60 = gecənin beşdə biri.
        val day = Fx.times(summer, Fx.MOSCOW)
        val maghrib = day[Prayer.MAGHRIB]!!.atMillis
        val nextDay = Fx.times("2026-06-22", Fx.MOSCOW)
        val night = nextDay[Prayer.SUNRISE]!!.atMillis - maghrib

        val ishaGap = day[Prayer.ISHA]!!.atMillis - maghrib
        val fajrGap = day[Prayer.SUNRISE]!!.atMillis - day[Prayer.FAJR]!!.atMillis

        assertTrue(abs(ishaGap - night / 5) < Fx.ONE_MINUTE, "İşa: $ishaGap, gecə/5: ${night / 5}")
        assertTrue(abs(fajrGap - night / 5) < 2 * Fx.ONE_MINUTE, "Fəcr: $fajrGap, gecə/5: ${night / 5}")
    }

    @Test
    fun stPetersburgSummerKeepsFajrBeforeSunriseAndIshaAfterMaghrib() {
        val day = Fx.times(summer, Fx.ST_PETERSBURG)

        assertTrue(day[Prayer.FAJR]!!.atMillis < day[Prayer.SUNRISE]!!.atMillis)
        assertTrue(day[Prayer.ISHA]!!.atMillis > day[Prayer.MAGHRIB]!!.atMillis)
        assertEquals(6, day.times.size)
    }

    @Test
    fun murmanskPolarDayUsesNearestDay() {
        val day = Fx.times(summer, Fx.MURMANSK)

        assertTrue(day.times.all { it.source == TimeSource.NEAREST_DAY }, "${day.times}")
        assertEquals(6, day.times.distinctBy { it.atMillis }.size, "altı fərqli an olmalıdır")
    }

    @Test
    fun murmanskPolarNightUsesNearestDay() {
        val day = Fx.times(winter, Fx.MURMANSK)

        assertTrue(day.times.all { it.source == TimeSource.NEAREST_DAY }, "${day.times}")
        assertEquals(6, day.times.distinctBy { it.atMillis }.size)
    }

    @Test
    fun murmanskDhuhrStaysAtSolarTransitEvenInPolarNight() {
        // Transit riyazi olaraq həmişə mövcuddur — Zöhr heç vaxt köçürülmür.
        val day = Fx.times(winter, Fx.MURMANSK)
        val hourAngle = Fx.hourAngleOf(winter, Fx.MURMANSK, day[Prayer.DHUHR]!!.atMillis)

        assertTrue(abs(hourAngle) < 0.01, "qütb gecəsində də Zöhr transitdədir: $hourAngle")
    }

    @Test
    fun theBoundaryTransitionIsSharpAndDocumented() {
        // Ölçülmüş davranış — kəsilməzlik İDDİA EDİLMİR.
        //
        // Sərhəd günündə astronomik İşa günəşin alt kulminasiyasına, yəni gecənin ORTASINA
        // yaxınlaşır; bucaq/60 isə 12° üçün gecənin beşdə biridir. 55°N-də sıçrayış ~109 dəqiqədir.
        // Bu, `PrayerTimes` KDoc-unda əsaslandırılmış kompromisdir: alternativlər (gecə/2 və limit
        // forması) ya cədvəli yararsız edir, ya da UOIF metodundan kənara çıxır.
        //
        // Test sıçrayışı ÖLÇÜR ki, kimsə qaydanı səssizcə dəyişəndə bu qərar yenidən müzakirə olunsun.
        val at = GeoPoint(55.0, 30.0)
        var lastAstronomical: PrayerDayTimes? = null
        var firstFallback: PrayerDayTimes? = null

        for (index in 0 until 200) {
            val day = Fx.times(IsoDate.plusDays("2026-04-01", index)!!, at)

            if (day[Prayer.ISHA]!!.source == TimeSource.ASTRONOMICAL) {
                lastAstronomical = day
            } else if (lastAstronomical != null) {
                firstFallback = day
                break
            }
        }

        val before = assertNotNull(lastAstronomical, "astronomik gün tapılmalı idi")
        val after = assertNotNull(firstFallback, "fallback gününə keçid tapılmalı idi")

        val beforeGap = before[Prayer.ISHA]!!.atMillis - before[Prayer.MAGHRIB]!!.atMillis
        val afterGap = after[Prayer.ISHA]!!.atMillis - after[Prayer.MAGHRIB]!!.atMillis
        val jumpMinutes = abs(afterGap - beforeGap) / Fx.ONE_MINUTE

        assertTrue(afterGap < beforeGap, "fallback İşanı erkənləşdirir")
        assertTrue(
            jumpMinutes in 60..150,
            "sıçrayış gözlənilən aralıqdan çıxdı ($jumpMinutes dəq) — qayda dəyişibsə KDoc da yenilənməlidir",
        )
    }

    @Test
    fun fallbackTimesStayInsideTheNight() {
        // Sıçrayış qəbul edilir, amma nəticə mənasız ola bilməz: Fəcr gecənin içində qalmalı,
        // İşa Axşamdan sonra və Fəcrdən əvvəl olmalıdır.
        val day = Fx.times(summer, Fx.MOSCOW)
        val nextSunrise = Fx.times("2026-06-22", Fx.MOSCOW)[Prayer.SUNRISE]!!.atMillis

        assertTrue(day[Prayer.ISHA]!!.atMillis > day[Prayer.MAGHRIB]!!.atMillis)
        assertTrue(day[Prayer.ISHA]!!.atMillis < nextSunrise, "İşa səhərə keçməməlidir")
        assertTrue(day[Prayer.FAJR]!!.atMillis < day[Prayer.SUNRISE]!!.atMillis)
        assertTrue(day[Prayer.FAJR]!!.atMillis > day[Prayer.DHUHR]!!.atMillis - 24 * 3_600_000L)
    }

    @Test
    fun everyLatitudeProducesSixTimesOnTheSolstices() {
        for (latitude in listOf(50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 89.0)) {
            for (dateIso in listOf(summer, winter)) {
                for (sign in listOf(1.0, -1.0)) {
                    val at = GeoPoint(latitude * sign, 20.0)
                    val day = PrayerTimes.calculate(dateIso, at, Fx.DEFAULT)

                    assertNotNull(day, "$dateIso @ $at")
                    assertEquals(6, day.times.size, "$dateIso @ $at → ${day.times}")
                }
            }
        }
    }

    @Test
    fun degenerateOrderIsClampedNotBroken() {
        // Qütb astanasında ofsetsiz nəticə də bir-birinə çox yaxın düşə bilər; sıralama pozulmamalıdır.
        val at = GeoPoint(66.5, 25.0)

        for (index in 0 until 365) {
            val dateIso = IsoDate.plusDays("2026-01-01", index)!!
            val day = Fx.times(dateIso, at)
            val sorted = day.times.sortedBy { it.prayer.ordinal }

            for (i in 1 until sorted.size) {
                assertTrue(
                    sorted[i].atMillis > sorted[i - 1].atMillis,
                    "$dateIso: ${sorted[i].prayer} sıradan çıxdı",
                )
            }
        }
    }
}
