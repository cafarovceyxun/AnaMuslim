package com.cafarovceyxun.anamuslim.utils.supabase

import com.cafarovceyxun.anamuslim.viewModels.containsHadith
import com.cafarovceyxun.anamuslim.viewModels.containsVerse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Çoxayəli element və növbədəki yer açarı — kart, story və oxucu nişanı bunlara söykənir. */
class DailyContentTest {

    private fun verse(chapter: Int, from: Int, to: Int? = null) = DailyContent(
        content_type = DailyContent.CONTENT_TYPE_VERSE,
        chapter_no = chapter,
        verse_no = from,
        verse_end = to,
        text_ar = "",
        text_az = "",
        date = "2026-08-30",
    )

    @Test
    fun `single verse yields one number`() {
        assertEquals(listOf(7), verse(1, 7).verseNumbers)
    }

    @Test
    fun `range yields every verse inclusive`() {
        assertEquals(listOf(1, 2, 3, 4, 5), verse(2, 1, 5).verseNumbers)
    }

    @Test
    fun `inverted range degrades to the first verse instead of throwing`() {
        // Baza CHECK-i bunu qadağan edir, amma köhnə və ya pozulmuş sətir ekranı yıxmamalıdır.
        assertEquals(listOf(5), verse(2, 5, 3).verseNumbers)
    }

    @Test
    fun `hadith has no verse numbers`() {
        val hadith = DailyContent(
            content_type = DailyContent.CONTENT_TYPE_HADITH,
            hadith_id = 12L,
            text_ar = "",
            text_az = "",
        )

        assertTrue(hadith.isHadith)
        assertTrue(hadith.verseNumbers.isEmpty())
    }

    @Test
    fun `slot key combines date and slot`() {
        assertEquals("2026-08-30#3", verse(1, 1).copy(slot_index = 3).slotKey)
    }

    @Test
    fun `badge matches every verse of a range`() {
        val items = listOf(verse(2, 1, 5))

        assertTrue(items.containsVerse(2, 1))
        assertTrue(items.containsVerse(2, 3))
        assertTrue(items.containsVerse(2, 5))
        assertFalse(items.containsVerse(2, 6))
        assertFalse(items.containsVerse(3, 1))
    }

    @Test
    fun `badge finds a hadith among the day's items`() {
        val items = listOf(
            verse(2, 1),
            DailyContent(
                content_type = DailyContent.CONTENT_TYPE_HADITH,
                hadith_id = 42L,
                text_ar = "",
                text_az = "",
            ),
        )

        assertTrue(items.containsHadith(42L))
        assertFalse(items.containsHadith(43L))
        assertFalse(items.containsHadith(null))
    }
}
