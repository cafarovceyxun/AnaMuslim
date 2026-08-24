package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ChapterTimingMetadata
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioTrack
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.TIME_UNSET
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.VerseTiming
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The order this planner emits is what a listener actually hears in "Quran and translation" mode,
 * and both platforms depend on it being identical: Android turns the clips into media3 items, iOS
 * into an AVPlayer clip queue. Worth pinning because the interesting parts are invisible at the
 * call site — the group loop interleaves *tracks inside a group*, not verse by verse, and the last
 * verse deliberately gets no end boundary.
 */
class VerseClipPlannerTest {

    private fun timing(vararg verses: Int) = ChapterTimingMetadata(
        chapterNo = 1,
        durationMs = verses.size * 1_000L,
        verses = verses.map { VerseTiming(it, (it - 1) * 1_000L, it * 1_000L) },
    )

    private fun track(kind: RecitationAudioKind, uri: String, timing: ChapterTimingMetadata?) =
        RecitationAudioTrack(
            kind = kind,
            chapterNo = 1,
            reciterId = if (kind == RecitationAudioKind.QURAN) "quran_reciter" else "tts_az_v1",
            audioUri = uri,
            timingMetadata = timing,
        )

    private val quran = track(RecitationAudioKind.QURAN, "file://quran.mp3", timing(1, 2, 3, 4))
    private val translation =
        track(RecitationAudioKind.TRANSLATION, "file://az.mp3", timing(1, 2, 3, 4))

    // ==================== clippableTracks ====================

    @Test
    fun putsQuranBeforeTranslation() {
        val tracks = VerseClipPlanner.clippableTracks(quran, translation)

        assertEquals(listOf(RecitationAudioKind.QURAN, RecitationAudioKind.TRANSLATION), tracks.map { it.kind })
    }

    /** A track with no verse timing cannot be cut into verses, so it must not reach the planner. */
    @Test
    fun dropsTracksWithoutVerseTiming() {
        val untimed = track(RecitationAudioKind.TRANSLATION, "file://az.mp3", null)

        val tracks = VerseClipPlanner.clippableTracks(quran, untimed)

        assertEquals(listOf(RecitationAudioKind.QURAN), tracks.map { it.kind })
    }

    // ==================== build ====================

    @Test
    fun alternatesVerseAndTranslationWhenGroupSizeIsOne() {
        val clips = VerseClipPlanner.build(1, 4, listOf(quran, translation), groupSize = 1)

        assertEquals(
            listOf(1 to "quran.mp3", 1 to "az.mp3", 2 to "quran.mp3", 2 to "az.mp3",
                   3 to "quran.mp3", 3 to "az.mp3", 4 to "quran.mp3", 4 to "az.mp3"),
            clips.map { it.verseNo to it.uri.substringAfterLast('/') },
        )
    }

    /** Larger groups read several verses in Arabic first, then the same verses translated. */
    @Test
    fun playsWholeGroupPerTrack() {
        val clips = VerseClipPlanner.build(1, 4, listOf(quran, translation), groupSize = 3)

        assertEquals(
            listOf(1, 2, 3, 1, 2, 3, 4, 4),
            clips.map { it.verseNo },
        )
        assertEquals(
            listOf("quran.mp3", "quran.mp3", "quran.mp3", "az.mp3", "az.mp3", "az.mp3",
                   "quran.mp3", "az.mp3"),
            clips.map { it.uri.substringAfterLast('/') },
            "the trailing group holds only verse 4",
        )
    }

    /**
     * The last verse of a chapter runs to the end of its file. Published timing files often end a
     * few hundred ms early, and clipping to that boundary cuts the final verse short.
     */
    @Test
    fun lastVerseOfTheChapterPlaysToTheEndOfItsFile() {
        val clips = VerseClipPlanner.build(1, 4, listOf(quran), groupSize = 1)

        val last = clips.last()
        assertEquals(4, last.verseNo)
        assertTrue(last.openEnded)
        // The measured boundary survives: playback ignores it, the progress bar does not.
        assertEquals(4_000L, last.endMs)
        assertEquals(1_000L, last.durationMs)

        assertTrue(clips.dropLast(1).none { it.openEnded })
    }

    /**
     * Regression, found on a device (2026-08-24): in "Quran and translation" mode the last verse's
     * translation was never heard. The Arabic clip of the last verse was also marked "play to the
     * end of the file", so the Arabic track ran past its verse boundary to the end of its own file
     * while a translation clip was still queued behind it. Only the **final** clip of the whole
     * sequence may be open-ended.
     */
    @Test
    fun onlyTheVeryLastClipRunsToTheEndOfItsFile() {
        val clips = VerseClipPlanner.build(1, 4, listOf(quran, translation), groupSize = 1)

        val openEnded = clips.filter { it.openEnded }

        assertEquals(1, openEnded.size, "exactly one clip may be open-ended")
        assertEquals(clips.last(), openEnded.single())
        assertEquals(RecitationAudioKind.TRANSLATION, openEnded.single().kind)

        val arabicLastVerse = clips.first { it.verseNo == 4 && it.kind == RecitationAudioKind.QURAN }
        assertEquals(4_000L, arabicLastVerse.endMs, "the Arabic clip keeps its measured boundary")
        assertTrue(!arabicLastVerse.openEnded, "and it is not allowed to run to the end of its file")
    }

    @Test
    fun skipsVersesWithAnUnusableWindow() {
        val broken = ChapterTimingMetadata(
            chapterNo = 1,
            durationMs = 4_000L,
            verses = listOf(
                VerseTiming(1, 0L, 1_000L),
                VerseTiming(2, 2_000L, 2_000L),   // empty window
                VerseTiming(3, 2_000L, 3_000L),
            ),
        )

        val clips = VerseClipPlanner.build(
            1, 3, listOf(track(RecitationAudioKind.QURAN, "file://q.mp3", broken)), groupSize = 1,
        )

        assertEquals(listOf(1, 3), clips.map { it.verseNo })
    }

    @Test
    fun returnsNothingWithoutTracksOrVerses() {
        assertTrue(VerseClipPlanner.build(1, 4, emptyList(), groupSize = 1).isEmpty())
        assertTrue(VerseClipPlanner.build(1, 0, listOf(quran), groupSize = 1).isEmpty())
    }
}

/**
 * The clip queue plays a chopped-up timeline, but the seek bar shows one continuous track — this
 * class is the mapping between them, so an off-by-one here moves the playhead to the wrong verse.
 */
class ClipTimelineTest {

    private fun clip(verse: Int, start: Long, end: Long) = AudioClip(
        chapterNo = 1,
        verseNo = verse,
        kind = RecitationAudioKind.QURAN,
        uri = "file://q.mp3",
        startMs = start,
        endMs = end,
    )

    private val timeline = ClipTimeline(
        listOf(
            clip(1, 0L, 1_000L),        // virtual 0–1000
            clip(2, 5_000L, 6_500L),    // virtual 1000–2500
            clip(3, 9_000L, 11_000L),   // virtual 2500–4500
        )
    )

    @Test
    fun addsUpClipLengthsIntoOneTimeline() {
        assertEquals(4_500L, timeline.totalDurationMs)
        assertEquals(1_000L, timeline.clipStartMs(1))
        assertEquals(2_500L, timeline.clipStartMs(2))
    }

    /** Position inside a clip is measured from the clip's own start, not the file's. */
    @Test
    fun mapsAPositionInsideAClipOntoTheTimeline() {
        assertEquals(1_400L, timeline.virtualPositionAt(1, 400L))
        assertEquals(2_500L, timeline.virtualPositionAt(1, 5_000L), "clamped to the clip's length")
    }

    @Test
    fun locatesTheClipHoldingAVirtualPosition() {
        assertEquals(0 to 500L, timeline.locate(500L))
        assertEquals(1 to 0L, timeline.locate(1_000L))
        assertEquals(2 to 1_000L, timeline.locate(3_500L))
    }

    /** Seeking past the end lands in the last clip rather than falling off the timeline. */
    @Test
    fun clampsPositionsOutsideTheTimeline() {
        assertEquals(0 to 0L, timeline.locate(-500L))
        assertEquals(2, timeline.locate(99_000L).first)
    }

    @Test
    fun findsTheFirstClipOfAVerse() {
        val withTranslation = ClipTimeline(
            listOf(clip(1, 0L, 1_000L), clip(1, 0L, 2_000L), clip(2, 1_000L, 2_000L))
        )

        assertEquals(0, withTranslation.firstIndexForVerse(1))
        assertEquals(2, withTranslation.firstIndexForVerse(2))
        assertEquals(2, withTranslation.firstIndexForVerse(9), "past the end → last clip")
    }

    /** The final clip of a file has no length until the player reports one. */
    @Test
    fun foldsInAMeasuredDuration() {
        val openEnded = ClipTimeline(listOf(clip(1, 0L, 1_000L), clip(2, 1_000L, TIME_UNSET)))

        assertEquals(1_000L, openEnded.totalDurationMs)

        openEnded.withMeasuredDuration(1, 2_000L)

        assertEquals(3_000L, openEnded.totalDurationMs)
        assertEquals(1 to 500L, openEnded.locate(1_500L))
    }
}
