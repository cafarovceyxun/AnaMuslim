package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ChapterTimingMetadata
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioTrack
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ResolvedAudioResult
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.VerseTiming
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.utils.preferences.TestDataStore
import com.cafarovceyxun.anamuslim.repository.QuranVerseStructure
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The half of [SharedRecitationPlayer] the transport tests could not reach: **chapter resolution**
 * (`playChapter` → resolver → load) and the **verse-tracking loop** (following the playhead through
 * a chapter's timing, applying the repeat budget, and honouring the end-of-chapter behaviour).
 *
 * Both were previously blocked on a real `QuranRepository` (Room) plus network and audio files. The
 * two narrow seams on the constructor open them here: a [FakeVerseStructure] answers the two
 * structural questions from a table, and a fake resolver returns crafted [ChapterTimingMetadata] in
 * place of a download — so the *policy* is exercised while the mechanism stays stubbed. This is the
 * same "mechanism on the edge, policy in commonMain" split the player itself documents.
 */
class SharedRecitationPlaybackTest {

    @BeforeTest
    fun setUp() {
        // playChapter persists the last-played verse through DataStore; give it a temp-file store.
        TestDataStore.ensureInitialized()
    }

    // Real verse counts for the boundary cases the tests touch: Al-Fatiha (7) is chapter 1, so 1:7 →
    // 2:1 is the "next chapter" transition; An-Nas (6) is the last chapter.
    private val structure = FakeVerseStructure(
        verseCounts = mapOf(1 to 7, 2 to 286, 114 to 6),
    )

    // ==================== Chapter resolution ====================

    /** An out-of-range verse must never reach the resolver or touch the transport. */
    @Test
    fun playChapterRejectsAnInvalidVerse() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver()
        val player = player(output, resolver)

        player.start(ChapterVersePair(1, 99)) // Al-Fatiha has 7 verses
        runCurrent()

        assertTrue(resolver.requestedChapters.isEmpty(), "resolution must not be attempted")
        assertNull(output.loadedUri)
    }

    /** While resolving, `resolvingChapterNo` marks which chapter — the UI shows a spinner on it. */
    @Test
    fun resolvingStateIsSetDuringResolutionAndClearedAfter() = runTest {
        val output = FakeAudioOutput()
        val gate = CompletableDeferred<Unit>()
        val resolver = FakeResolver(gateFirstCallOn = gate)
        val player = player(output, resolver)

        player.start(ChapterVersePair(1, 1))
        runCurrent()
        assertEquals(1, player.state.value.resolvingChapterNo, "spinner belongs to chapter 1")

        gate.complete(Unit)
        runCurrent()
        assertNull(player.state.value.resolvingChapterNo, "cleared once resolution ends")
    }

    /** A resolved chapter loads its track and starts the listener at the requested verse's timing. */
    @Test
    fun resolvedChapterLoadsTheTrackAtTheVerseStart() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            resolvedQuran(chapterNo, timing(chapterNo, verse(1, 0, 1000), verse(2, 1000, 2000)))
        }
        val player = player(output, resolver)

        player.start(ChapterVersePair(1, 2))
        runCurrent()

        assertEquals("file://quran/1.mp3", output.loadedUri)
        assertEquals(1000L, output.positionMs, "loaded at verse 2's start")
        assertEquals(ChapterVersePair(1, 2), player.state.value.currentVerse)
        assertTrue(player.state.value.isVerseSyncAvailable, "chapter has verse timing")
    }

    /**
     * A slow resolution must not overwrite a newer play request. The guard is the request id in
     * `playChapter`: the first call is held mid-resolution while a second runs to completion, then
     * the first is released and must abandon its result.
     */
    @Test
    fun aSlowResolutionDoesNotOvertakeANewerRequest() = runTest {
        val output = FakeAudioOutput()
        val gate = CompletableDeferred<Unit>()
        val resolver = FakeResolver(gateFirstCallOn = gate) { chapterNo ->
            resolvedQuran(chapterNo, timing(chapterNo, verse(1, 0, 1000)))
        }
        val player = player(output, resolver)

        player.start(ChapterVersePair(1, 1)) // suspends at the gate
        runCurrent()

        player.start(ChapterVersePair(2, 1)) // newer request, ungated
        runCurrent()
        assertEquals("file://quran/2.mp3", output.loadedUri, "the newer chapter loaded")

        gate.complete(Unit) // the stale first request resumes
        runCurrent()
        assertEquals("file://quran/2.mp3", output.loadedUri, "the stale result was discarded")
    }

    /** Re-playing a verse of the already-loaded chapter is a seek, not a re-download. */
    @Test
    fun replayingWithinTheLoadedChapterSeeksInsteadOfResolvingAgain() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            resolvedQuran(chapterNo, timing(chapterNo, verse(1, 0, 1000), verse(3, 2000, 3000)))
        }
        val player = player(output, resolver)

        player.start(ChapterVersePair(1, 1))
        runCurrent()
        assertEquals(1, resolver.requestedChapters.size)

        player.start(ChapterVersePair(1, 3))
        runCurrent()

        assertEquals(1, resolver.requestedChapters.size, "no second resolution")
        assertEquals(2000L, output.lastSeekMs, "seeked to verse 3's start")
        assertTrue(output.playCalled)
        assertEquals(ChapterVersePair(1, 3), player.state.value.currentVerse)
    }

    /** With no Quran track the resolver's translation track is used, and its reciter is published. */
    @Test
    fun fallsBackToTheTranslationTrackWhenThereIsNoQuranTrack() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            ResolvedAudioResult.Resoved(
                chapter = chapterNo,
                quran = null,
                translation = RecitationAudioTrack(
                    kind = RecitationAudioKind.TRANSLATION,
                    chapterNo = chapterNo,
                    reciterId = "translator-7",
                    audioUri = "file://tr/$chapterNo.mp3",
                    timingMetadata = timing(chapterNo, verse(1, 0, 1000)),
                ),
            )
        }
        val player = player(output, resolver)

        player.start(ChapterVersePair(1, 1))
        runCurrent()

        assertEquals("file://tr/1.mp3", output.loadedUri)
        assertEquals("translator-7", player.state.value.settings.translationReciter)
    }

    /** A resolution error must clear the spinner and leave the transport untouched, never crash. */
    @Test
    fun aResolutionErrorClearsResolvingAndLoadsNothing() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { ResolvedAudioResult.Error(IllegalStateException("offline")) }
        val player = player(output, resolver)

        player.start(ChapterVersePair(1, 1))
        runCurrent()

        assertNull(output.loadedUri)
        assertNull(player.state.value.resolvingChapterNo)
    }

    // ==================== Verse-tracking loop ====================

    /** As the playhead crosses verse boundaries the highlighted verse follows it. */
    @Test
    fun theHighlightedVerseFollowsThePlayhead() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            resolvedQuran(
                chapterNo,
                timing(chapterNo, verse(1, 0, 1000), verse(2, 1000, 2000), verse(3, 2000, 3000)),
            )
        }
        val player = player(output, resolver)
        player.setRepeatCount(0) // no repeats: advance purely by position

        player.start(ChapterVersePair(1, 1))
        runCurrent()

        output.beginPlaying(positionMs = 1500)
        startTracking(output)
        assertEquals(2, player.state.value.currentVerse.verseNo, "1500ms is inside verse 2")

        output.positionMs = 2500
        tick()
        assertEquals(3, player.state.value.currentVerse.verseNo, "2500ms is inside verse 3")
    }

    /** With repeats left the loop seeks back to the verse start before the verse ends. */
    @Test
    fun theRepeatBudgetRewindsTheVerseThenReleasesIt() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            resolvedQuran(chapterNo, timing(chapterNo, verse(1, 0, 1000), verse(2, 1000, 2000)))
        }
        val player = player(output, resolver)
        player.setRepeatCount(1) // one extra play of each verse

        player.start(ChapterVersePair(1, 1))
        runCurrent()

        // Near the end of verse 1 with budget: rewind to its start, verse unchanged.
        output.beginPlaying(positionMs = 950)
        startTracking(output)
        assertEquals(0L, output.lastSeekMs, "rewound to verse 1's start")
        assertEquals(1, player.state.value.currentVerse.verseNo)

        // Budget spent: the next boundary is allowed through into verse 2.
        output.positionMs = 1500
        tick()
        assertEquals(2, player.state.value.currentVerse.verseNo)
    }

    /** Without verse timing there is nothing to follow; the playhead moving never changes the verse. */
    @Test
    fun aChapterWithoutTimingDoesNotTrackVerses() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo -> resolvedQuran(chapterNo, timing = null) }
        val player = player(output, resolver)

        player.start(ChapterVersePair(1, 1))
        runCurrent()
        assertTrue(!player.state.value.isVerseSyncAvailable, "no timing means no sync")

        output.beginPlaying(positionMs = 5000)
        startTracking(output)
        assertEquals(1, player.state.value.currentVerse.verseNo, "verse cannot move without timing")
    }

    /** Once playback stops the loop stops; later playhead movement is ignored. */
    @Test
    fun trackingStopsWhenPlaybackStops() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            resolvedQuran(chapterNo, timing(chapterNo, verse(1, 0, 1000), verse(2, 1000, 2000)))
        }
        val player = player(output, resolver)
        player.setRepeatCount(0)

        player.start(ChapterVersePair(1, 1))
        runCurrent()

        output.beginPlaying(positionMs = 100)
        startTracking(output)
        assertEquals(1, player.state.value.currentVerse.verseNo)

        output.isPlaying = false
        output.listener?.onPlayingChanged(false)
        runCurrent()

        output.positionMs = 1500 // would be verse 2 if the loop were still running
        tick()
        assertEquals(1, player.state.value.currentVerse.verseNo, "stopped loop must not advance")
    }

    // ==================== End-of-chapter behaviour ====================

    /** STOP just stops: no new chapter, the verse stays put. */
    @Test
    fun endOfChapterWithStopDoesNothingFurther() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            resolvedQuran(chapterNo, timing(chapterNo, verse(1, 0, 1000), verse(2, 1000, 2000)))
        }
        val player = player(output, resolver)
        player.setAudioEndBehaviour(AudioEndBehaviour.STOP_PLAYBACK)

        player.start(ChapterVersePair(1, 2))
        runCurrent()
        val resolutions = resolver.requestedChapters.size

        output.listener?.onEnded()
        runCurrent()

        assertEquals(resolutions, resolver.requestedChapters.size, "no new chapter resolved")
        assertEquals(ChapterVersePair(1, 2), player.state.value.currentVerse)
    }

    /** NEXT_CHAPTER rolls off the last verse of a chapter into verse 1 of the next. */
    @Test
    fun endOfChapterWithNextChapterAdvancesToTheFollowingChapter() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            resolvedQuran(chapterNo, timing(chapterNo, verse(1, 0, 1000)))
        }
        val player = player(output, resolver)
        player.setAudioEndBehaviour(AudioEndBehaviour.NEXT_CHAPTER)

        player.start(ChapterVersePair(1, 7)) // last verse of Al-Fatiha
        runCurrent()

        output.listener?.onEnded()
        runCurrent()

        assertTrue(resolver.requestedChapters.contains(2), "resolved the next chapter")
        assertEquals("file://quran/2.mp3", output.loadedUri)
        assertEquals(ChapterVersePair(2, 1), player.state.value.currentVerse)
    }

    /** REPEAT_CHAPTER restarts the same chapter from verse 1, reusing the loaded audio (a seek). */
    @Test
    fun endOfChapterWithRepeatChapterRestartsFromVerseOne() = runTest {
        val output = FakeAudioOutput()
        val resolver = FakeResolver { chapterNo ->
            resolvedQuran(chapterNo, timing(chapterNo, verse(1, 0, 1000), verse(2, 1000, 2000)))
        }
        val player = player(output, resolver)
        player.setAudioEndBehaviour(AudioEndBehaviour.REPEAT_CHAPTER)

        player.start(ChapterVersePair(1, 2))
        runCurrent()
        assertEquals(1, resolver.requestedChapters.size)

        output.listener?.onEnded()
        runCurrent()

        assertEquals(1, resolver.requestedChapters.size, "loaded audio is reused, not re-resolved")
        assertEquals(0L, output.lastSeekMs, "seeked back to verse 1's start")
        assertEquals(ChapterVersePair(1, 1), player.state.value.currentVerse)
    }

    // ==================== Fixtures ====================

    private fun TestScope.player(output: FakeAudioOutput, resolver: FakeResolver) =
        SharedRecitationPlayer(
            output = output,
            scope = backgroundScope,
            verseStructureSeam = structure,
            resolveAudio = resolver::resolve,
        )

    /** Fires the output's "now playing" callback, which is what launches the verse-tracking loop. */
    private fun TestScope.startTracking(output: FakeAudioOutput) {
        output.listener?.onPlayingChanged(true)
        runCurrent()
    }

    /** Runs one verse-tracking iteration: past the 200 ms poll interval, then any 0-delay work. */
    private fun TestScope.tick() {
        advanceTimeBy(250)
        runCurrent()
    }

    private fun verse(verseNo: Int, startMs: Long, endMs: Long) = VerseTiming(verseNo, startMs, endMs)

    private fun timing(chapterNo: Int, vararg verses: VerseTiming) = ChapterTimingMetadata(
        chapterNo = chapterNo,
        durationMs = verses.maxOfOrNull { it.endMs } ?: 0L,
        verses = verses.toList(),
    )

    private fun resolvedQuran(chapterNo: Int, timing: ChapterTimingMetadata?) =
        ResolvedAudioResult.Resoved(
            chapter = chapterNo,
            quran = RecitationAudioTrack(
                kind = RecitationAudioKind.QURAN,
                chapterNo = chapterNo,
                reciterId = "reciter-x",
                audioUri = "file://quran/$chapterNo.mp3",
                timingMetadata = timing,
            ),
            translation = null,
        )

    private class FakeVerseStructure(private val verseCounts: Map<Int, Int>) : QuranVerseStructure {
        override suspend fun getChapterVerseCount(chapterNo: Int): Int = verseCounts[chapterNo] ?: 0
        override suspend fun isVerseValid4Chapter(chapterNo: Int, verseNo: Int): Boolean =
            verseNo in 1..getChapterVerseCount(chapterNo)
    }

    /**
     * Stands in for `RecitationAudioResolver`. Records which chapters were asked for (so "resolved
     * again vs. reused" is observable) and can hold its first call open on [gateFirstCallOn] to
     * stage the request-id race.
     */
    private class FakeResolver(
        private val gateFirstCallOn: CompletableDeferred<Unit>? = null,
        private val result: (chapterNo: Int) -> ResolvedAudioResult = { chapterNo ->
            ResolvedAudioResult.Resoved(chapterNo, quran = null, translation = null)
        },
    ) {
        val requestedChapters = mutableListOf<Int>()

        suspend fun resolve(chapterNo: Int, settings: PlayerSettings): ResolvedAudioResult {
            val isFirstCall = requestedChapters.isEmpty()
            requestedChapters += chapterNo
            if (isFirstCall) gateFirstCallOn?.await()
            return result(chapterNo)
        }
    }

    private class FakeAudioOutput : RecitationAudioOutput {
        override var positionMs: Long = 0L
        override var durationMs: Long = 0L
        override var isPlaying: Boolean = false

        var listener: RecitationAudioOutputListener? = null
            private set

        var playCalled = false
        var loadedUri: String? = null
        var lastSeekMs: Long? = null

        override fun load(uri: String, startPositionMs: Long, speed: Float) {
            loadedUri = uri
            positionMs = startPositionMs
            durationMs = LOADED_DURATION_MS
        }

        override fun play() {
            playCalled = true
        }

        override fun pause() {}

        override fun stop() {
            positionMs = 0L
            durationMs = 0L
        }

        override fun seekTo(positionMs: Long) {
            lastSeekMs = positionMs
            this.positionMs = positionMs
        }

        override fun setSpeed(speed: Float) {}

        override fun setListener(listener: RecitationAudioOutputListener?) {
            this.listener = listener
        }

        /** Marks the transport as playing at [positionMs]; the caller then starts the tracking loop. */
        fun beginPlaying(positionMs: Long) {
            this.positionMs = positionMs
            isPlaying = true
        }

        private companion object {
            const val LOADED_DURATION_MS = 300_000L
        }
    }
}
