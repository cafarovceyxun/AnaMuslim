package com.cafarovceyxun.anamuslim.utils.prayer

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NextPrayerTest {

    private val allPrayers = Prayer.entries.filter { it.isPrayer }.toSet()

    private fun days(count: Int = 2) =
        PrayerDay.forLocalDates("2026-09-01", count, Fx.BAKU, Fx.DEFAULT, Fx.fakeLocalDate(4))

    @Test
    fun returnsTheFirstTimeStrictlyAfterNow() {
        val days = days()
        val fajr = days[0][Prayer.FAJR]!!
        val upcoming = NextPrayer.after(fajr.atMillis - 1L, days, allPrayers)

        assertNotNull(upcoming)
        assertEquals(Prayer.FAJR, upcoming.prayer)
        assertEquals(fajr.atMillis, upcoming.atMillis)
    }

    @Test
    fun aTimeExactlyNowIsAlreadyPast() {
        val days = days()
        val fajr = days[0][Prayer.FAJR]!!
        val upcoming = NextPrayer.after(fajr.atMillis, days, allPrayers)

        assertNotNull(upcoming)
        assertTrue(upcoming.prayer != Prayer.FAJR, "tam vaxtında olan artıq keçmişdir")
        assertEquals(Prayer.FAJR, upcoming.previousPrayer)
    }

    @Test
    fun crossesIntoTheNextDayAfterIsha() {
        val days = days()
        val isha = days[0][Prayer.ISHA]!!
        val upcoming = NextPrayer.after(isha.atMillis + Fx.ONE_MINUTE, days, allPrayers)

        assertNotNull(upcoming)
        assertEquals(Prayer.FAJR, upcoming.prayer, "İşadan sonra sabahkı Fəcr gəlir")
        assertEquals(days[1][Prayer.FAJR]!!.atMillis, upcoming.atMillis)
        assertEquals(Prayer.ISHA, upcoming.previousPrayer)
    }

    @Test
    fun skipsExcludedPrayers() {
        val days = days()
        val sunrise = days[0][Prayer.SUNRISE]!!

        val withSunrise = NextPrayer.after(sunrise.atMillis - 1L, days, allPrayers + Prayer.SUNRISE)
        val withoutSunrise = NextPrayer.after(sunrise.atMillis - 1L, days, allPrayers)

        assertEquals(Prayer.SUNRISE, withSunrise?.prayer)
        assertEquals(Prayer.DHUHR, withoutSunrise?.prayer, "Günəş default olaraq sayılmır")
    }

    @Test
    fun returnsNullWhenNothingIsAhead() {
        val days = days(count = 1)
        val last = days[0].times.maxOf { it.atMillis }

        assertNull(NextPrayer.after(last + 1L, days, allPrayers))
        assertNull(NextPrayer.after(0L, days, include = emptySet()), "boş dəst → null")
    }

    @Test
    fun progressFractionIsZeroAtPreviousAndOneAtNext() {
        val days = days()
        val upcoming = NextPrayer.after(days[0][Prayer.DHUHR]!!.atMillis + 1L, days, allPrayers)
        assertNotNull(upcoming)

        val start = upcoming.previousAtMillis!!
        val end = upcoming.atMillis

        assertTrue(abs(NextPrayer.progressFraction(start, upcoming)) < 1e-6)
        assertTrue(abs(NextPrayer.progressFraction(end, upcoming) - 1f) < 1e-6)
        assertTrue(abs(NextPrayer.progressFraction((start + end) / 2, upcoming) - 0.5f) < 1e-3)
    }

    @Test
    fun progressFractionIsZeroWithoutAPreviousTime() {
        val upcoming = Upcoming(Prayer.FAJR, atMillis = 1_000L, previousPrayer = null, previousAtMillis = null)

        assertEquals(0f, NextPrayer.progressFraction(500L, upcoming))
    }

    @Test
    fun progressFractionIsClampedOutsideTheInterval() {
        val upcoming = Upcoming(Prayer.DHUHR, 2_000L, Prayer.FAJR, 1_000L)

        assertEquals(0f, NextPrayer.progressFraction(0L, upcoming))
        assertEquals(1f, NextPrayer.progressFraction(9_000L, upcoming))
    }
}
