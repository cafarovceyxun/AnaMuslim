package com.cafarovceyxun.anamuslim.api.models.mediaplayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the verse-timing lookups that drive recitation highlighting. Both platforms read these:
 * `SharedRecitationPlayer` polls [ChapterTimingMetadata.getVerseAtPosition] every 200 ms to move the
 * highlight and applies the repeat budget against [ChapterTimingMetadata.getVerseTiming], while
 * Android's media3 service uses the same model.
 *
 * Worth pinning down because neither method is straightforward: the position lookup is a binary
 * search that assumes sorted, non-overlapping windows, and the per-verse lookup carries a fallback
 * chain for manifests whose last verse is zeroed — a real quirk of the published timing files.
 */
class ChapterTimingMetadataTest {

    // Three verses with a deliberate gap (3000–3500) between the second and third: real manifests
    // leave silence between verses, and a position there must not report a verse.
    private val metadata = ChapterTimingMetadata(
        chapterNo = 1,
        durationMs = 10_000L,
        verses = listOf(
            VerseTiming(verseNo = 1, startMs = 0L, endMs = 1_500L),
            VerseTiming(verseNo = 2, startMs = 1_500L, endMs = 3_000L),
            VerseTiming(verseNo = 3, startMs = 3_500L, endMs = 6_000L),
        ),
    )

    // ==================== getVerseAtPosition ====================

    @Test
    fun findsVerseContainingPosition() {
        assertEquals(1, metadata.getVerseAtPosition(700L)?.verseNo)
        assertEquals(2, metadata.getVerseAtPosition(2_000L)?.verseNo)
        assertEquals(3, metadata.getVerseAtPosition(5_999L)?.verseNo)
    }

    /** Start is inclusive and end exclusive, so adjacent verses never both claim a position. */
    @Test
    fun treatsStartAsInclusiveAndEndAsExclusive() {
        assertEquals(1, metadata.getVerseAtPosition(0L)?.verseNo)
        assertEquals(2, metadata.getVerseAtPosition(1_500L)?.verseNo, "verse end must not win over the next start")
        assertNull(metadata.getVerseAtPosition(6_000L), "the end bound belongs to no verse")
    }

    @Test
    fun returnsNullOutsideAnyVerseWindow() {
        assertNull(metadata.getVerseAtPosition(3_200L), "silence between verses")
        assertNull(metadata.getVerseAtPosition(9_000L), "trailing audio after the last verse")
    }

    /**
     * The lookup is a binary search, so a linear fixture proves little. A long chapter exercises
     * every branch and would expose an off-by-one at any depth.
     */
    @Test
    fun binarySearchAgreesWithLinearScanAcrossALongChapter() {
        val verses = (1..286).map { verseNo ->
            val start = (verseNo - 1) * 1_000L
            VerseTiming(verseNo = verseNo, startMs = start, endMs = start + 900L)
        }
        val long = ChapterTimingMetadata(chapterNo = 2, durationMs = 286_000L, verses = verses)

        for (verseNo in 1..286) {
            val start = (verseNo - 1) * 1_000L
            assertEquals(verseNo, long.getVerseAtPosition(start)?.verseNo, "at start of $verseNo")
            assertEquals(verseNo, long.getVerseAtPosition(start + 899L)?.verseNo, "at end of $verseNo")
            assertNull(long.getVerseAtPosition(start + 950L), "gap after $verseNo")
        }
    }

    @Test
    fun returnsNullWhenTheChapterHasNoVerseTiming() {
        val bare = ChapterTimingMetadata(chapterNo = 1, durationMs = 10_000L, verses = null)

        assertFalse(bare.hasVerseTiming)
        assertNull(bare.getVerseAtPosition(500L))
    }

    // ==================== getVerseTiming ====================

    @Test
    fun returnsTheStoredTimingForAKnownVerse() {
        val timing = metadata.getVerseTiming(2)

        assertEquals(1_500L, timing?.startMs)
        assertEquals(3_000L, timing?.endMs)
    }

    /**
     * Some published manifests end with a zeroed verse. Rather than losing the last verse, the
     * lookup synthesizes a window from the end of the last real one to the end of the file.
     */
    @Test
    fun synthesizesAWindowForAZeroedTrailingVerse() {
        val withZeroedLast = ChapterTimingMetadata(
            chapterNo = 1,
            durationMs = 10_000L,
            verses = listOf(
                VerseTiming(verseNo = 1, startMs = 0L, endMs = 1_500L),
                VerseTiming(verseNo = 2, startMs = 0L, endMs = 0L),
            ),
        )

        val timing = withZeroedLast.getVerseTiming(2)

        assertEquals(1_500L, timing?.startMs, "starts where the last real verse ended")
        assertEquals(10_000L, timing?.endMs, "runs to the end of the audio")
    }

    /** When the file is shorter than the last real verse, the synthetic window still has length. */
    @Test
    fun keepsTheSyntheticWindowNonEmptyWhenDurationIsUseless() {
        val shortDuration = ChapterTimingMetadata(
            chapterNo = 1,
            durationMs = 0L,
            verses = listOf(
                VerseTiming(verseNo = 1, startMs = 0L, endMs = 4_000L),
                VerseTiming(verseNo = 2, startMs = 0L, endMs = 0L),
            ),
        )

        val timing = shortDuration.getVerseTiming(2)

        assertEquals(4_000L, timing?.startMs)
        assertEquals(5_000L, timing?.endMs, "falls back to a one-second window")
        assertTrue(isValidTimingWindow(timing!!.startMs, timing.endMs))
    }

    /** A verse past everything the manifest knows about is treated like the zeroed-last case. */
    @Test
    fun synthesizesForAVerseBeyondTheLastKnownOne() {
        val timing = metadata.getVerseTiming(7)

        assertEquals(6_000L, timing?.startMs)
        assertEquals(10_000L, timing?.endMs)
    }

    /** Verse 1 must always resolve: without it playback would have nowhere to start. */
    @Test
    fun fallsBackToTheWholeFileForVerseOneWithoutAnyTiming() {
        val bare = ChapterTimingMetadata(chapterNo = 1, durationMs = 8_000L, verses = null)

        val timing = bare.getVerseTiming(1)

        assertEquals(0L, timing?.startMs)
        assertEquals(8_000L, timing?.endMs)
    }

    @Test
    fun returnsNullForANonFirstVerseWithoutAnyTiming() {
        val bare = ChapterTimingMetadata(chapterNo = 1, durationMs = 8_000L, verses = null)

        assertNull(bare.getVerseTiming(2))
    }

    // ==================== hasCompleteTimingFor ====================

    @Test
    fun reportsWhetherARangeIsFullyCovered() {
        assertTrue(metadata.hasCompleteTimingFor(1, 3))
        assertFalse(metadata.hasCompleteTimingFor(1, 4), "verse 4 is not in the manifest")

        val bare = ChapterTimingMetadata(chapterNo = 1, durationMs = 1_000L, verses = null)
        assertFalse(bare.hasCompleteTimingFor(1, 1))
    }

    // ==================== VerseTiming ====================

    @Test
    fun verseDurationIsZeroForAnUnusableWindow() {
        assertEquals(1_500L, VerseTiming(1, 0L, 1_500L).durationMs)
        assertEquals(0L, VerseTiming(1, 1_500L, 1_500L).durationMs, "empty window")
        assertEquals(0L, VerseTiming(1, 2_000L, 1_000L).durationMs, "reversed window")
        assertEquals(0L, VerseTiming(1, TIME_UNSET, 1_000L).durationMs, "unset start")
    }

    @Test
    fun containsPositionMatchesTheHalfOpenWindow() {
        val verse = VerseTiming(1, 1_000L, 2_000L)

        assertTrue(verse.containsPosition(1_000L))
        assertTrue(verse.containsPosition(1_999L))
        assertFalse(verse.containsPosition(2_000L))
        assertFalse(verse.containsPosition(999L))
    }

    @Test
    fun findsWordSegmentsWithinAVerse() {
        val verse = VerseTiming(
            verseNo = 1,
            startMs = 0L,
            endMs = 2_000L,
            segments = listOf(
                listOf(1L, 0L, 500L),
                listOf(2L, 500L, 1_200L),
            ),
        )

        assertEquals(2, verse.seg.size)
        assertEquals(1, verse.getSegmentAtPosition(0L)?.index)
        assertEquals(2, verse.getSegmentAtPosition(500L)?.index, "segment end is exclusive")
        assertNull(verse.getSegmentAtPosition(1_200L), "past the last segment")
    }

    // ==================== isValidTimingWindow ====================

    @Test
    fun rejectsUnsetNegativeAndEmptyWindows() {
        assertTrue(isValidTimingWindow(0L, 1L))

        assertFalse(isValidTimingWindow(TIME_UNSET, 1_000L))
        assertFalse(isValidTimingWindow(0L, TIME_UNSET))
        assertFalse(isValidTimingWindow(-1L, 1_000L))
        assertFalse(isValidTimingWindow(0L, -1L))
        assertFalse(isValidTimingWindow(1_000L, 1_000L))
        assertFalse(isValidTimingWindow(1_000L, 999L))
    }
}
