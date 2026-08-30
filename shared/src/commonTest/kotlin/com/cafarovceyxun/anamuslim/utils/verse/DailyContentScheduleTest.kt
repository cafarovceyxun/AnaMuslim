package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.utils.supabase.DailyContentSlots
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Növbənin yuvalara bölünməsi — «gündə 5 bildiriş, 10 element seçilibsə səhərə davam etsin» tələbi
 * burada yoxlanılır.
 */
class DailyContentScheduleTest {

    @Test
    fun `there are five slots`() {
        assertEquals(DailyContentSlots.COUNT, DailyContentSchedule.SLOT_TIMES.size)
        assertEquals(5, DailyContentSlots.COUNT)
    }

    @Test
    fun `slot times are strictly increasing through the day`() {
        val minutes = DailyContentSchedule.SLOT_TIMES.map { it.hour * 60 + it.minute }

        assertEquals(minutes.sorted(), minutes)
        assertEquals(minutes.distinct().size, minutes.size)
    }

    @Test
    fun `ten items spill onto the following day`() {
        val slots = DailyContentSchedule.consecutiveSlots(
            count = 10,
            startDate = "2026-08-30",
            startSlot = 0,
        )

        assertEquals(10, slots.size)
        assertEquals(List(5) { "2026-08-30" to it }, slots.take(5))
        assertEquals(List(5) { "2026-08-31" to it }, slots.drop(5))
    }

    @Test
    fun `consecutive slots continue from a mid-day start`() {
        val slots = DailyContentSchedule.consecutiveSlots(
            count = 4,
            startDate = "2026-08-30",
            startSlot = 3,
        )

        assertEquals(
            listOf(
                "2026-08-30" to 3,
                "2026-08-30" to 4,
                "2026-08-31" to 0,
                "2026-08-31" to 1,
            ),
            slots,
        )
    }

    @Test
    fun `consecutive slots cross a month boundary`() {
        val slots = DailyContentSchedule.consecutiveSlots(
            count = 6,
            startDate = "2026-08-31",
            startSlot = 0,
        )

        assertEquals("2026-09-01" to 0, slots.last())
    }

    @Test
    fun `first free slot skips taken ones`() {
        // Vaxtı hələ gəlməmiş bir an: bütün yuvalar gələcəkdədir.
        val now = 0L
        val today = "2026-08-30"

        val free = DailyContentSchedule.firstFreeSlot(
            taken = setOf("$today#0", "$today#1"),
            today = today,
            nowMillis = now,
        )

        assertEquals(today to 2, free)
    }

    @Test
    fun `first free slot rolls to the next day when today is full`() {
        val today = "2026-08-30"
        val taken = (0 until DailyContentSlots.COUNT).mapTo(HashSet()) { "$today#$it" }

        val free = DailyContentSchedule.firstFreeSlot(
            taken = taken,
            today = today,
            nowMillis = 0L,
        )

        assertEquals("2026-08-31" to 0, free)
    }

    @Test
    fun `first free slot returns null when the horizon is full`() {
        val today = "2026-08-30"

        val taken = (0..1).flatMap { dayOffset ->
            val date = com.cafarovceyxun.anamuslim.utils.IsoDate.plusDays(today, dayOffset)!!
            (0 until DailyContentSlots.COUNT).map { "$date#$it" }
        }.toSet()

        assertNull(
            DailyContentSchedule.firstFreeSlot(
                taken = taken,
                today = today,
                nowMillis = 0L,
                maxDaysAhead = 1,
            )
        )
    }

    @Test
    fun `slot label is zero padded`() {
        assertEquals("08:00", DailyContentSchedule.label(0))
        assertTrue(DailyContentSchedule.label(DailyContentSlots.COUNT).isEmpty())
    }
}
