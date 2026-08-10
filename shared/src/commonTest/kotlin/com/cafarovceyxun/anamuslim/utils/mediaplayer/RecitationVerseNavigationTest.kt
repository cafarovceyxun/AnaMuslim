package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.repository.QuranVerseStructure
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers what the player's next/previous verse buttons do, including the chapter transitions that
 * `AudioEndBehaviour.NEXT_CHAPTER` rides on (`handlePlaybackEnded` calls `nextVerse()`).
 *
 * Reachable as a plain unit test since [QuranVerseStructure] narrowed the dependency to two
 * structural questions; the fixture below answers them from a table, which for chapter/verse counts
 * is as faithful as the bundled database.
 */
class RecitationVerseNavigationTest {

    /**
     * Real verse counts for the chapters the tests move between, so the boundary cases are the ones
     * the app actually meets: Al-Fatiha (7) is the first chapter, An-Nas (6) the last, and 2 → 3 is
     * an ordinary interior transition.
     */
    private val structure = FakeVerseStructure(
        verseCounts = mapOf(
            1 to 7,    // Al-Fatiha
            2 to 286,  // Al-Baqarah
            3 to 200,  // Al-Imran
            113 to 5,  // Al-Falaq
            114 to 6,  // An-Nas
        ),
    )

    private fun stateAt(chapterNo: Int, verseNo: Int) =
        RecitationServiceState(currentVerse = ChapterVersePair(chapterNo, verseNo))

    @Test
    fun stepsForwardInsideAChapter() = runTest {
        assertEquals(ChapterVersePair(2, 2), stateAt(2, 1).getNextVerse(structure))
        assertEquals(ChapterVersePair(2, 286), stateAt(2, 285).getNextVerse(structure))
    }

    @Test
    fun stepsBackwardInsideAChapter() = runTest {
        assertEquals(ChapterVersePair(2, 254), stateAt(2, 255).getPreviousVerse(structure))
    }

    /** The last verse of a chapter continues into verse 1 of the next — the "next chapter" behaviour. */
    @Test
    fun rollsForwardIntoTheNextChapter() = runTest {
        assertEquals(ChapterVersePair(2, 1), stateAt(1, 7).getNextVerse(structure))
        assertEquals(ChapterVersePair(3, 1), stateAt(2, 286).getNextVerse(structure))
    }

    /** Going back from verse 1 lands on the *last* verse of the previous chapter, not its first. */
    @Test
    fun rollsBackwardIntoTheEndOfThePreviousChapter() = runTest {
        assertEquals(ChapterVersePair(1, 7), stateAt(2, 1).getPreviousVerse(structure))
        assertEquals(ChapterVersePair(113, 5), stateAt(114, 1).getPreviousVerse(structure))
    }

    /** The two ends of the Quran stop rather than wrapping around. */
    @Test
    fun stopsAtBothEndsOfTheQuran() = runTest {
        assertNull(stateAt(1, 1).getPreviousVerse(structure), "nothing before 1:1")
        assertNull(stateAt(114, 6).getNextVerse(structure), "nothing after 114:6")
    }

    /**
     * A stored position that no longer resolves (a shorter chapter, a corrupted preference) must
     * yield null rather than a guess — the player then simply does not move.
     */
    @Test
    fun refusesToNavigateFromAnInvalidPosition() = runTest {
        assertNull(stateAt(2, 999).getNextVerse(structure))
        assertNull(stateAt(2, 999).getPreviousVerse(structure))
        assertNull(stateAt(115, 1).getNextVerse(structure), "chapter past the end")
        assertNull(stateAt(0, 1).getNextVerse(structure), "chapter before the start")
        assertNull(stateAt(1, 0).getNextVerse(structure), "verse 0 is not a verse")
    }

    /** The default state is 1:1 and must be navigable — the player starts there before any playback. */
    @Test
    fun navigatesFromTheDefaultState() = runTest {
        assertEquals(ChapterVersePair(1, 2), RecitationServiceState.EMPTY.getNextVerse(structure))
    }

    private class FakeVerseStructure(private val verseCounts: Map<Int, Int>) : QuranVerseStructure {

        override suspend fun getChapterVerseCount(chapterNo: Int): Int = verseCounts[chapterNo] ?: 0

        override suspend fun isVerseValid4Chapter(chapterNo: Int, verseNo: Int): Boolean =
            verseNo in 1..getChapterVerseCount(chapterNo)
    }
}
