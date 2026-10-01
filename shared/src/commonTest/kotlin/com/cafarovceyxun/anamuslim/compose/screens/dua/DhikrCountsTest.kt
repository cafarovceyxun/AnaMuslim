package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Zikr sayğacı — 33 + 33 + 33 + 1 təsbihi.
 *
 * Səhv sayğac ekranda sınmır: hədəfi keçən say, ya da bitmiş hissədə qalan vurğu istifadəçini
 * neçə dəfə dediyi barədə çaşdırır — ona görə hər qayda ayrıca yoxlanılır.
 */
class DhikrCountsTest {

    private fun part(id: Long, count: Int?) = Dua(
        id = id,
        category_slug = "namaz",
        source_type = "hadith",
        text_ar = "ع$id",
        text_az = "",
        repeat_count = count,
        part_of_id = if (id == 1L) null else 1L,
    )

    private val tasbih = listOf(part(1, 33), part(2, 33), part(3, 33), part(4, 1))

    @Test
    fun activeMovesToNextPartWhenOneIsDone() {
        val counts = DhikrCounts()
        assertEquals(0, counts.activeIndex(tasbih))

        repeat(32) { assertFalse(counts.increment(tasbih[0])) }
        // 33-cü toxunuş hissəni bitirir — titrəmə məhz bu anda fərqlidir.
        assertTrue(counts.increment(tasbih[0]))
        assertEquals(1, counts.activeIndex(tasbih))
    }

    @Test
    fun countNeverPassesTarget() {
        val counts = DhikrCounts()
        repeat(5) { counts.increment(tasbih[3]) }

        assertEquals(1, counts.countOf(tasbih[3]))
        assertEquals(1, counts.total(tasbih))
    }

    @Test
    fun allDone_hasNoActivePart() {
        val counts = DhikrCounts()
        tasbih.forEach { part -> repeat(part.repeat_count!!) { counts.increment(part) } }

        assertEquals(-1, counts.activeIndex(tasbih))
        assertEquals(100, counts.total(tasbih))
    }

    @Test
    fun partsWithoutCountAreSkipped() {
        val counts = DhikrCounts()
        val mixed = listOf(part(1, null), part(2, 3))

        assertFalse(counts.increment(mixed[0]))
        assertEquals(1, counts.activeIndex(mixed))
    }

    @Test
    fun resetClearsOnlyThisDua() {
        val counts = DhikrCounts()
        val other = part(9, 10)
        counts.increment(tasbih[0])
        counts.increment(other)

        counts.reset(tasbih)

        assertEquals(0, counts.total(tasbih))
        assertEquals(1, counts.countOf(other))
    }
}
