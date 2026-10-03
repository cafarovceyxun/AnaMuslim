package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Oxucudan açılan ayə vərəqində sağa-sola sürüşdürmənin hədəfi ([neighbourVerse]). */
class NeighbourVerseTest {

    private fun single(chapter: Int, verse: Int) = QuickReferenceVerses.Range(chapter, verse..verse)

    @Test
    fun `forward goes to the next verse`() {
        assertEquals(single(1, 2), neighbourVerse(single(1, 1), 1, verseCount = 7))
    }

    @Test
    fun `backward goes to the previous verse`() {
        assertEquals(single(1, 1), neighbourVerse(single(1, 2), -1, verseCount = 7))
    }

    @Test
    fun `stops at the first verse`() {
        assertNull(neighbourVerse(single(1, 1), -1, verseCount = 7))
    }

    @Test
    fun `stops at the last verse`() {
        assertNull(neighbourVerse(single(1, 7), 1, verseCount = 7))
    }

    @Test
    fun `range steps past its ends`() {
        val range = QuickReferenceVerses.Range(2, 255..257)
        assertEquals(single(2, 258), neighbourVerse(range, 1, verseCount = 286))
        assertEquals(single(2, 254), neighbourVerse(range, -1, verseCount = 286))
    }

    @Test
    fun `discrete list steps past its ends`() {
        val discrete = QuickReferenceVerses.Discrete(2, listOf(3, 5, 9))
        assertEquals(single(2, 10), neighbourVerse(discrete, 1, verseCount = 286))
        assertEquals(single(2, 2), neighbourVerse(discrete, -1, verseCount = 286))
    }

    @Test
    fun `no forward step before the verse count is known`() {
        assertNull(neighbourVerse(single(1, 1), 1, verseCount = null))
    }

    @Test
    fun `chapter only has no neighbour`() {
        assertNull(neighbourVerse(QuickReferenceVerses.ChapterOnly(1), 1, verseCount = 7))
    }
}
