package com.cafarovceyxun.anamuslim.compose.navigation

import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.utils.reader.ReadType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers "continue reading" on iOS: a stored history row → the reader route that resumes it. On
 * Android the same job is done by `ReaderFactory.prepareHistoryIntent`; here it has to survive the
 * flat, primitive-only route DTO that type-safe navigation allows.
 *
 * Two things are deliberately dropped in that translation (reader mode and mushaf-page resume), and
 * the validation guards matter: a row pointing at a chapter/juz/hizb that no longer exists must
 * produce no route rather than a reader that opens on nothing.
 */
class ReadHistoryRouteTest {

    @Test
    fun mapsAChapterRowToAChapterRouteThatScrollsToTheStoredVerse() {
        val route = history(ReadType.Chapter, chapterNo = 2, fromVerseNo = 255).toReaderRoute()

        assertEquals(2, route?.chapterNo)
        assertEquals(2, route?.initialChapterNo)
        assertEquals(255, route?.initialVerseNo)
        assertNull(route?.juzNo)
        assertNull(route?.hizbNo)
    }

    /**
     * Juz and hizb rows carry the division in `divisionNo` and the position in `chapterNo` — the
     * two must not be swapped, or the reader opens the right verse in the wrong division.
     */
    @Test
    fun mapsJuzAndHizbRowsWithTheDivisionAndPositionKeptApart() {
        val juz = history(ReadType.Juz, divisionNo = 30, chapterNo = 78, fromVerseNo = 1).toReaderRoute()

        assertEquals(30, juz?.juzNo)
        assertEquals(78, juz?.initialChapterNo)
        assertEquals(1, juz?.initialVerseNo)
        assertNull(juz?.chapterNo)

        val hizb = history(ReadType.Hizb, divisionNo = 60, chapterNo = 114, fromVerseNo = 3).toReaderRoute()

        assertEquals(60, hizb?.hizbNo)
        assertEquals(114, hizb?.initialChapterNo)
        assertNull(hizb?.chapterNo)
    }

    @Test
    fun rejectsRowsPointingOutsideTheQuran() {
        assertNull(history(ReadType.Chapter, chapterNo = 0).toReaderRoute())
        assertNull(history(ReadType.Chapter, chapterNo = 115).toReaderRoute())
        assertNull(history(ReadType.Juz, divisionNo = 31, chapterNo = 1).toReaderRoute())
        assertNull(history(ReadType.Juz, divisionNo = 0, chapterNo = 1).toReaderRoute())
        assertNull(history(ReadType.Hizb, divisionNo = 61, chapterNo = 1).toReaderRoute())
    }

    @Test
    fun acceptsTheBoundaryDivisions() {
        assertEquals(1, history(ReadType.Chapter, chapterNo = 1).toReaderRoute()?.chapterNo)
        assertEquals(114, history(ReadType.Chapter, chapterNo = 114).toReaderRoute()?.chapterNo)
        assertEquals(1, history(ReadType.Juz, divisionNo = 1, chapterNo = 1).toReaderRoute()?.juzNo)
        assertEquals(60, history(ReadType.Hizb, divisionNo = 60, chapterNo = 1).toReaderRoute()?.hizbNo)
    }

    /**
     * An unreadable `read_type` (an older build's value, or a corrupted row) falls back to chapter
     * rather than dropping the row — `ReadType.fromValue` decides that, and the route follows.
     */
    @Test
    fun fallsBackToChapterForAnUnknownReadType() {
        val row = ReadHistoryEntity(
            readType = "not-a-read-type",
            readerMode = ReaderMode.VerseByVerse.value,
            chapterNo = 18,
            fromVerseNo = 10,
        )

        val route = row.toReaderRoute()

        assertEquals(18, route?.chapterNo)
        assertEquals(10, route?.initialVerseNo)
    }

    /**
     * Documented loss: the flat route carries neither the reader mode nor the mushaf page, so a
     * mushaf row resumes as the same verse in whatever mode the reader is set to. Pinned here so
     * the omission stays a decision rather than becoming a surprise.
     */
    @Test
    fun dropsReaderModeAndMushafPage() {
        val row = ReadHistoryEntity(
            readType = ReadType.Chapter.value,
            readerMode = ReaderMode.Translation.value,
            chapterNo = 3,
            fromVerseNo = 5,
            mushafCode = "kfqpc",
            mushafVariant = "v2",
            pageNo = 50,
        )

        val route = row.toReaderRoute()

        assertEquals(3, route?.chapterNo)
        assertEquals(5, route?.initialVerseNo)
        assertNull(route?.pageNo, "the stored mushaf page is intentionally not carried over")
    }

    private fun history(
        readType: ReadType,
        divisionNo: Int = 0,
        chapterNo: Int = 1,
        fromVerseNo: Int = 1,
    ) = ReadHistoryEntity(
        readType = readType.value,
        readerMode = ReaderMode.VerseByVerse.value,
        divisionNo = divisionNo,
        chapterNo = chapterNo,
        fromVerseNo = fromVerseNo,
    )
}
