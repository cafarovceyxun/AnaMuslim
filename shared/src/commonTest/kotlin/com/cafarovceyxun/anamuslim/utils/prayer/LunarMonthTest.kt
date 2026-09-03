package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.hijriDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Qəməri ay sərhədləri platformanın öz təqvimindən gəlir, ona görə testlər **konkret tarixə** deyil,
 * ayın strukturuna baxır: uzunluq, ardıcıllıq və qonşu aylara keçid. Belə olanda test həm Android
 * `HijrahDate`, həm də iOS `NSCalendar` altında eyni cür mənalıdır.
 */
class LunarMonthTest {

    private val now = currentEpochMillis()

    /** Platforma çevirməni dəstəkləmirsə (Android API < 26) bütün testlər mənasızdır. */
    private val supported = hijriDate(now) != null

    @Test
    fun monthHasTwentyNineOrThirtyConsecutiveDays() {
        if (!supported) return

        val span = assertNotNull(LunarMonth.spanContaining(now, offsetDays = 0), "cari ay tapılmalıdır")
        assertTrue(span.days.size in 29..30, "qəməri ay 29 və ya 30 gündür: ${span.days.size}")

        span.days.zipWithNext().forEach { (a, b) ->
            val gap = b - a
            // Yay vaxtı keçidində fərq 23 və ya 25 saat ola bilər — günorta lövbəri buna görədir.
            assertTrue(gap in 82_800_000L..90_000_000L, "günlər ardıcıl olmalıdır: $gap")
        }
    }

    @Test
    fun everyDayBelongsToTheSameLunarMonth() {
        if (!supported) return

        val span = LunarMonth.spanContaining(now, offsetDays = 0) ?: return

        span.days.forEach { atMillis ->
            val (_, month, year) = hijriDate(atMillis) ?: return@forEach
            assertEquals(span.month, month)
            assertEquals(span.year, year)
        }
    }

    @Test
    fun steppingBackAndForwardReturnsToTheSameMonth() {
        if (!supported) return

        val span = LunarMonth.spanContaining(now, offsetDays = 0) ?: return
        val next = LunarMonth.spanContaining(span.nextAnchor, offsetDays = 0) ?: return
        val back = LunarMonth.spanContaining(next.previousAnchor, offsetDays = 0) ?: return

        assertTrue(next.month != span.month || next.year != span.year, "növbəti ay fərqli olmalıdır")
        assertEquals(span.month, back.month)
        assertEquals(span.year, back.year)
    }

    /** Düzəliş ayı bütövlükdə sürüşdürür — bir günün içində qalıb ay sərhədini pozmur. */
    @Test
    fun offsetShiftsTheWholeMonthWindow() {
        if (!supported) return

        val plain = LunarMonth.spanContaining(now, offsetDays = 0) ?: return
        val shifted = LunarMonth.spanContaining(now, offsetDays = -2) ?: return

        assertTrue(shifted.days.size in 29..30)
        assertTrue(
            shifted.days.first() != plain.days.first() || shifted.month != plain.month,
            "iki günlük düzəliş pəncərəni tərpətməlidir",
        )
    }
}
