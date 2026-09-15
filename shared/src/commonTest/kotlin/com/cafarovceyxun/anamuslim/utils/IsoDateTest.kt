package com.cafarovceyxun.anamuslim.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `IsoDate` növbənin gün hesabını daşıyır — «10 ayə seçilibsə səhərə davam etsin» tələbi ay və il
 * sərhədlərindən keçəndə də düzgün işləməlidir.
 */
class IsoDateTest {

    @Test
    fun `plusDays crosses month boundary`() {
        assertEquals("2026-09-01", IsoDate.plusDays("2026-08-31", 1))
        assertEquals("2026-08-31", IsoDate.plusDays("2026-09-01", -1))
    }

    @Test
    fun `plusDays crosses year boundary`() {
        assertEquals("2027-01-01", IsoDate.plusDays("2026-12-31", 1))
    }

    @Test
    fun `plusDays handles leap day`() {
        assertEquals("2028-02-29", IsoDate.plusDays("2028-02-28", 1))
        assertEquals("2028-03-01", IsoDate.plusDays("2028-02-29", 1))

        // 2026 sıçrayış ili deyil.
        assertEquals("2026-03-01", IsoDate.plusDays("2026-02-28", 1))
    }

    @Test
    fun `epoch day round trips`() {
        val dates = listOf("1970-01-01", "2026-08-30", "1999-12-31", "2100-03-01")

        dates.forEach { date ->
            val epochDay = IsoDate.toEpochDay(date)
            assertEquals(date, epochDay?.let { IsoDate.fromEpochDay(it) })
        }
    }

    @Test
    fun `epoch day zero is unix epoch`() {
        assertEquals(0L, IsoDate.toEpochDay("1970-01-01"))
        assertEquals("1970-01-01", IsoDate.fromEpochDay(0L))
    }

    @Test
    fun `daysBetween counts forward and backward`() {
        assertEquals(1L, IsoDate.daysBetween("2026-08-30", "2026-08-31"))
        assertEquals(-1L, IsoDate.daysBetween("2026-08-31", "2026-08-30"))
        assertEquals(0L, IsoDate.daysBetween("2026-08-30", "2026-08-30"))
        assertEquals(365L, IsoDate.daysBetween("2026-01-01", "2027-01-01"))
    }

    @Test
    fun `dayOfWeek follows ISO numbering`() {
        // Sabit nöqtə: 1970-01-01 cümə axşamıdır.
        assertEquals(4, IsoDate.dayOfWeek("1970-01-01"))
        assertEquals(1, IsoDate.dayOfWeek("2026-09-07"))
        assertEquals(IsoDate.FRIDAY, IsoDate.dayOfWeek("2026-09-11"))
        assertEquals(6, IsoDate.dayOfWeek("2026-02-28"))
    }

    @Test
    fun `dayOfWeek stays correct before the epoch`() {
        // Mənfi epoxa günü — `%` mənfi qalıq versəydi cümə şənbəyə sürüşərdi.
        assertEquals(IsoDate.FRIDAY, IsoDate.dayOfWeek("1969-12-26"))
        assertEquals(IsoDate.FRIDAY, IsoDate.dayOfWeek(-6L))
    }

    @Test
    fun `display turns a date or a timestamp into dd MM yyyy`() {
        assertEquals("14.09.2026", IsoDate.display("2026-09-14"))
        // Supabase `created_at` — saat hissəsi atılır, qurşaq göstəricisi ilə birlikdə.
        assertEquals("14.09.2026", IsoDate.display("2026-09-14T10:33:21.481+00:00"))
        assertEquals("01.01.2026", IsoDate.display("2026-01-01T00:00:00Z"))
    }

    @Test
    fun `display leaves an unrecognised value alone`() {
        // Ekranda boşluq yox, heç olmasa xam dəyər görünsün.
        assertEquals("", IsoDate.display(""))
        assertEquals("2026-09", IsoDate.display("2026-09"))
        assertEquals("bugün", IsoDate.display("bugün"))
    }

    @Test
    fun `malformed input returns null instead of throwing`() {
        assertNull(IsoDate.toEpochDay(""))
        assertNull(IsoDate.toEpochDay("2026-08"))
        assertNull(IsoDate.toEpochDay("bugün"))
        assertNull(IsoDate.toEpochDay("2026-13-01"))
        assertNull(IsoDate.plusDays("pozuq", 1))
        assertNull(IsoDate.daysBetween("2026-08-30", "pozuq"))
        assertNull(IsoDate.dayOfWeek("pozuq"))
    }
}
