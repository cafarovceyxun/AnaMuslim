package com.cafarovceyxun.anamuslim.utils.prayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * UTC günündən cihazın yerli gününə yığım.
 *
 * Qurşaq inyeksiya olunur ([Fx.fakeLocalDate]), ona görə testlər cihazın öz qurşağından asılı deyil —
 * eyni nəticə həm CI-də, həm Bakıda alınır.
 */
class PrayerDayTest {

    private val date = "2026-09-01"

    @Test
    fun assemblesSixTimesForDeviceLocalDate() {
        val day = PrayerDay.forLocalDate(date, Fx.BAKU, Fx.DEFAULT, Fx.fakeLocalDate(offsetHours = 4))

        assertEquals(6, day.times.size)
        assertEquals(date, day.dateIso)
        assertTrue(day.times.map { it.prayer } == Prayer.entries.toList(), "sıralı olmalıdır")
    }

    @Test
    fun allAssembledTimesBelongToTheRequestedLocalDate() {
        val localDateOf = Fx.fakeLocalDate(offsetHours = 4)
        val day = PrayerDay.forLocalDate(date, Fx.BAKU, Fx.DEFAULT, localDateOf)

        for (time in day.times) {
            assertEquals(date, localDateOf(time.atMillis), "${time.prayer}")
        }
    }

    @Test
    fun distantTimezoneStillYieldsAFullDay() {
        // Cihaz Bakıda (UTC+4), seçilmiş yer London — anlar doğrudur, sadəcə «yad» görünür.
        val london = GeoPoint(51.5074, -0.1278)
        val day = PrayerDay.forLocalDate(date, london, Fx.DEFAULT, Fx.fakeLocalDate(offsetHours = 4))

        assertEquals(6, day.times.size, "${day.times}")
    }

    @Test
    fun extremeTimezoneOffsetsDoNotDuplicateAPrayer() {
        for (offset in listOf(-11, -5, 0, 5, 13)) {
            val day = PrayerDay.forLocalDate(date, Fx.BAKU, Fx.DEFAULT, Fx.fakeLocalDate(offset))

            assertEquals(
                day.times.size,
                day.times.distinctBy { it.prayer }.size,
                "ofset $offset: eyni namaz iki dəfə düşməməlidir",
            )
        }
    }

    @Test
    fun dstTransitionKeepsExactlySixTimes() {
        // Saxta «DST»: 2026-03-29 gecə yarısından sonra ofset 1 → 2 saat.
        val springForward: (Long) -> String = { millis ->
            val offsetHours = if (millis >= 1_774_828_800_000L) 2 else 1
            Fx.fakeLocalDate(offsetHours)(millis)
        }

        val day = PrayerDay.forLocalDate("2026-03-29", Fx.PARIS, Fx.DEFAULT, springForward)

        assertEquals(6, day.times.size, "DST keçid günündə də altı vaxt: ${day.times}")
        assertEquals(day.times.size, day.times.distinctBy { it.prayer }.size)
    }

    @Test
    fun forLocalDatesReturnsConsecutiveDays() {
        val days = PrayerDay.forLocalDates(date, count = 3, Fx.BAKU, Fx.DEFAULT, Fx.fakeLocalDate(4))

        assertEquals(3, days.size)
        assertEquals(listOf("2026-09-01", "2026-09-02", "2026-09-03"), days.map { it.dateIso })
        assertTrue(days.all { it.times.size == 6 })
    }

    @Test
    fun deviceUtcOffsetSecondsMatchesTheFormatter() {
        val now = 1_756_700_000_000L

        assertEquals(4 * 3600, PrayerDay.deviceUtcOffsetSeconds(now, Fx.fakeFormatter(4)))
        assertEquals(0, PrayerDay.deviceUtcOffsetSeconds(now, Fx.fakeFormatter(0)))
        assertEquals(-5 * 3600, PrayerDay.deviceUtcOffsetSeconds(now, Fx.fakeFormatter(-5)))
    }

    @Test
    fun deviceUtcOffsetSecondsFallsBackToZeroOnMalformedText() {
        assertEquals(0, PrayerDay.deviceUtcOffsetSeconds(1_756_700_000_000L) { "zibil" })
    }

    @Test
    fun utcEpochDayHandlesPreEpochInstants() {
        assertEquals(0L, PrayerDay.utcEpochDay(0L))
        assertEquals(-1L, PrayerDay.utcEpochDay(-1L), "mənfi anlar aşağı yuvarlaqlaşmalıdır")
        assertEquals(-1L, PrayerDay.utcEpochDay(-86_400_000L))
        assertEquals(-2L, PrayerDay.utcEpochDay(-86_400_001L))
    }

    @Test
    fun highLatitudeLocalDayStillAssembles() {
        val day = PrayerDay.forLocalDate("2026-06-21", Fx.MURMANSK, Fx.DEFAULT, Fx.fakeLocalDate(3))

        assertNotNull(day[Prayer.DHUHR])
        assertTrue(day.times.isNotEmpty())
    }
}
