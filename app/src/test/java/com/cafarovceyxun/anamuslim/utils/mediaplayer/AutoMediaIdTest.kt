package com.cafarovceyxun.anamuslim.utils.mediaplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The browse tree hands Android Auto an id and gets that same id back when the item is played.
 * When the two sides drifted apart (items built as `chapter_5`, play handler matching
 * `auto_chapter_`) nothing broke loudly: the compiler was happy, playback in the app was fine, and
 * only tapping an item in a car did nothing at all — which is what got the app rejected from Play.
 * These tests pin the round-trip.
 */
class AutoMediaIdTest {

    @Test
    fun `chapter id round-trips`() {
        val request = AutoMediaId.parseChapterRequest(AutoMediaId.chapter(5))

        assertEquals(AutoChapterRequest(chapterNo = 5, reciterId = null), request)
    }

    @Test
    fun `every chapter id round-trips`() {
        for (chapterNo in 1..114) {
            assertEquals(
                AutoChapterRequest(chapterNo, null),
                AutoMediaId.parseChapterRequest(AutoMediaId.chapter(chapterNo)),
            )
        }
    }

    @Test
    fun `reciter-scoped id round-trips with an underscored reciter id`() {
        val mediaId = AutoMediaId.chapterOfReciter(36, "ad_dussary")

        assertEquals(
            AutoChapterRequest(chapterNo = 36, reciterId = "ad_dussary"),
            AutoMediaId.parseChapterRequest(mediaId),
        )
    }

    @Test
    fun `reciter folder id round-trips`() {
        assertEquals("ad_dussary", AutoMediaId.reciterIdOf(AutoMediaId.reciter("ad_dussary")))
    }

    @Test
    fun `legacy auto-prefixed ids are still accepted`() {
        assertEquals(
            AutoChapterRequest(chapterNo = 2, reciterId = "ad_dussary"),
            AutoMediaId.parseChapterRequest("auto_chapter_2_reciter_ad_dussary"),
        )
    }

    @Test
    fun `browsable and unknown ids are not playable`() {
        assertNull(AutoMediaId.parseChapterRequest(AutoMediaId.ROOT))
        assertNull(AutoMediaId.parseChapterRequest(AutoMediaId.CHAPTERS))
        assertNull(AutoMediaId.parseChapterRequest(AutoMediaId.RECITERS))
        assertNull(AutoMediaId.parseChapterRequest(AutoMediaId.reciter("ad_dussary")))
        assertNull(AutoMediaId.parseChapterRequest("chapter_"))
        assertNull(AutoMediaId.parseChapterRequest("chapter_0"))
        assertNull(AutoMediaId.parseChapterRequest("chapter_115"))
        assertNull(AutoMediaId.parseChapterRequest("chapter_x"))
        assertNull(AutoMediaId.parseChapterRequest("something_else"))
    }

    @Test
    fun `reciter folder id is only read from a reciter id`() {
        assertNull(AutoMediaId.reciterIdOf(AutoMediaId.chapter(5)))
        assertNull(AutoMediaId.reciterIdOf(AutoMediaId.RECITERS))
        assertNull(AutoMediaId.reciterIdOf("auto_reciter_"))
    }
}
