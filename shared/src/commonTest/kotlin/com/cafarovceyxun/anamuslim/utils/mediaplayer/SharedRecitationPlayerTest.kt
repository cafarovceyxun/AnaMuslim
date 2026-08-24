package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.TestDataStore
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the shared playback policy that drives iOS recitation — the class between the UI and
 * AVFoundation. Android deliberately stays on its media3 service, so on that side this is the
 * behaviour a listener never sees; on iOS it is the whole player.
 *
 * The [FakeAudioOutput] stands in for the platform transport, which lets the tests assert the two
 * things that actually go wrong in playback code: **what the transport was told to do**, and **what
 * state was published back**. Two areas stay out of reach and are covered elsewhere: chapter
 * resolution needs a real `QuranRepository` (concrete class over Room), and verse-to-verse
 * navigation is unit-tested through `QuranVerseStructure` in `RecitationVerseNavigationTest`.
 */
class SharedRecitationPlayerTest {

    @BeforeTest
    fun setUp() {
        TestDataStore.ensureInitialized()
    }

    // ==================== Transport delegation ====================

    /**
     * With nothing loaded the toggle must route into chapter resolution rather than poking the
     * transport — calling `play()` on an output with no audio would silently do nothing.
     *
     * The resolution it launches is not driven here: it needs a real `QuranRepository`, so the
     * coroutine is left unstarted (and cancelled with the test scope). What is asserted is the
     * decision, which is the part that lives in this class.
     */
    @Test
    fun playPauseDoesNotTouchTheTransportWhenNothingIsLoaded() = runTest {
        val output = FakeAudioOutput()
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.playPause()

        assertFalse(output.playCalled, "must not call play() on an empty output")
        assertFalse(output.pauseCalled)
    }

    @Test
    fun playPauseTogglesTheLoadedTrack() = runTest {
        val output = FakeAudioOutput(durationMs = 60_000L)
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.playPause()
        assertTrue(output.playCalled)

        output.isPlaying = true
        player.playPause()
        assertTrue(output.pauseCalled)
    }

    /** Stopping must clear the published state too, or the UI keeps showing a spinner or a pause icon. */
    @Test
    fun stopClearsTransportAndPublishedState() = runTest {
        val output = FakeAudioOutput(durationMs = 60_000L)
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.stop()

        assertTrue(output.stopCalled)
        assertFalse(player.isPlaying)
        assertFalse(player.state.value.isPlaying)
        assertFalse(player.state.value.isBuffering)
    }

    // ==================== Seeking ====================

    @Test
    fun seekIsClampedToTheLoadedDuration() = runTest {
        val output = FakeAudioOutput(durationMs = 10_000L)
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.seekTo(-5_000L)
        assertEquals(0L, output.lastSeekMs, "negative positions clamp to the start")

        player.seekTo(999_999L)
        assertEquals(10_000L, output.lastSeekMs, "past the end clamps to the duration")
    }

    /** Before the duration is known a seek must still pass through — nothing to clamp against yet. */
    @Test
    fun seekIsNotClampedWhileTheDurationIsUnknown() = runTest {
        val output = FakeAudioOutput(durationMs = 0L)
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.seekTo(30_000L)

        assertEquals(30_000L, output.lastSeekMs)
    }

    @Test
    fun seekButtonsStepFiveSecondsAndStayInRange() = runTest {
        val output = FakeAudioOutput(durationMs = 60_000L, positionMs = 20_000L)
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.seekRight()
        assertEquals(25_000L, output.lastSeekMs)

        // Steps are relative to where the playhead now is, not to where it started.
        player.seekLeft()
        assertEquals(20_000L, output.lastSeekMs)

        // Near the edges the step is absorbed rather than overshooting.
        output.positionMs = 2_000L
        player.seekLeft()
        assertEquals(0L, output.lastSeekMs)

        output.positionMs = 58_000L
        player.seekRight()
        assertEquals(60_000L, output.lastSeekMs)
    }

    // ==================== Buffering visibility ====================

    /**
     * Short stalls must not flicker a spinner, so buffering is only published after it has lasted
     * long enough to be worth showing (the same 500 ms rule Android's controller uses).
     */
    @Test
    fun shortStallsNeverSurfaceAsBuffering() = runTest {
        val output = FakeAudioOutput()
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        output.listener?.onBufferingChanged(true)
        advanceTimeBy(300)
        assertFalse(player.state.value.isBuffering, "too early to show a spinner")

        output.listener?.onBufferingChanged(false)
        advanceTimeBy(1_000)
        assertFalse(player.state.value.isBuffering, "the pending reveal must be cancelled")
    }

    @Test
    fun aStallLongerThanTheThresholdSurfaces() = runTest {
        val output = FakeAudioOutput()
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        output.listener?.onBufferingChanged(true)
        advanceTimeBy(600)

        assertTrue(player.state.value.isBuffering)
        assertTrue(player.isLoading)

        output.listener?.onBufferingChanged(false)
        runCurrent()
        assertFalse(player.state.value.isBuffering)
    }

    // ==================== Playing state ====================

    /** Playback state is published from the output's callback, never assumed from a play() call. */
    @Test
    fun playingStateFollowsTheOutputCallback() = runTest {
        val output = FakeAudioOutput(durationMs = 60_000L)
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.playPause()
        assertFalse(player.isPlaying, "play() returning is not proof that audio started")

        output.listener?.onPlayingChanged(true)
        assertTrue(player.isPlaying)
        assertTrue(player.state.value.isPlaying)
    }

    /** Resuming clears the headset-pause flag; otherwise the UI keeps explaining a pause that ended. */
    @Test
    fun resumingClearsThePausedByHeadsetFlag() = runTest {
        val output = FakeAudioOutput(durationMs = 60_000L)
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        output.listener?.onPlayingChanged(false)
        assertFalse(player.state.value.isPlaying)

        output.listener?.onPlayingChanged(true)
        assertFalse(player.state.value.pausedByHeadset)
    }

    // ==================== Settings ====================

    @Test
    fun speedIsPublishedAndPushedToTheOutput() = runTest {
        val output = FakeAudioOutput()
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.setSpeed(1.5f)

        assertEquals(1.5f, output.lastSpeed)
        assertEquals(1.5f, player.state.value.settings.speed)
    }

    /** A negative repeat count would make the verse-tracking loop seek backwards forever. */
    @Test
    fun repeatCountIsCoercedToItsMinimum() = runTest {
        val output = FakeAudioOutput()
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.setRepeatCount(-3)
        assertEquals(
            RecitationPreferences.RECITATION_MIN_REPEAT_COUNT,
            player.state.value.settings.repeatCount,
        )

        player.setRepeatCount(4)
        assertEquals(4, player.state.value.settings.repeatCount)
    }

    @Test
    fun audioEndBehaviourIsStoredAsGiven() = runTest {
        val output = FakeAudioOutput()
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.setAudioEndBehaviour(AudioEndBehaviour.REPEAT_CHAPTER)

        assertEquals(AudioEndBehaviour.REPEAT_CHAPTER, player.state.value.settings.audioEndBehaviour)
    }

    /**
     * The audio option is honoured now that the clipped Quran+translation sequence exists — this
     * used to collapse to Quran-only. Nothing is reloaded while the player is idle: the option
     * takes effect on the next chapter that starts.
     */
    @Test
    fun audioOptionIsStored() = runTest {
        val output = FakeAudioOutput()
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.setAudioOption(AudioOption.ONLY_TRANSLATION)

        assertEquals(AudioOption.ONLY_TRANSLATION, player.state.value.settings.audioOption)
        assertFalse(output.stopCalled, "an idle player has nothing to reload")
    }

    // ==================== Connection ====================

    /**
     * Connection is reference-counted: a second screen attaching and detaching must not tear down
     * playback for the one still listening.
     */
    @Test
    fun connectionSurvivesUntilTheLastOwnerDetaches() = runTest {
        val output = FakeAudioOutput()
        val player = SharedRecitationPlayer(output, scope = backgroundScope)

        player.connect()
        player.connect()
        assertTrue(player.isConnectedState.value)

        player.disconnect()
        assertTrue(player.isConnectedState.value, "one owner is still attached")

        player.disconnect()
        assertFalse(player.isConnectedState.value)

        // Over-releasing must not drive the count negative and wedge the next connect().
        player.disconnect()
        player.connect()
        assertTrue(player.isConnectedState.value)
    }

    private class FakeAudioOutput(
        override var durationMs: Long = 0L,
        override var positionMs: Long = 0L,
    ) : RecitationAudioOutput {

        override var isPlaying: Boolean = false

        var listener: RecitationAudioOutputListener? = null
            private set

        var playCalled = false
        var pauseCalled = false
        var stopCalled = false
        var lastSeekMs: Long? = null
        var lastSpeed: Float? = null
        var loadedUri: String? = null

        override fun load(uri: String, startPositionMs: Long, speed: Float) {
            loadedUri = uri
            positionMs = startPositionMs
            lastSpeed = speed
            loadedClips = emptyList()
            currentClipIndex = -1
        }

        var loadedClips: List<AudioClip> = emptyList()
            private set

        override var currentClipIndex: Int = -1
            private set

        override fun loadClips(clips: List<AudioClip>, startIndex: Int, speed: Float) {
            loadedClips = clips
            lastSpeed = speed
            currentClipIndex = startIndex
            positionMs = clips.getOrNull(startIndex)?.startMs ?: 0L
            listener?.onClipChanged(startIndex)
        }

        override fun seekToClip(index: Int, offsetInClipMs: Long) {
            currentClipIndex = index
            positionMs = (loadedClips.getOrNull(index)?.startMs ?: 0L) + offsetInClipMs
            lastSeekMs = positionMs
            listener?.onClipChanged(index)
        }

        override fun play() {
            playCalled = true
        }

        override fun pause() {
            pauseCalled = true
        }

        override fun stop() {
            stopCalled = true
            positionMs = 0L
            durationMs = 0L
        }

        override fun seekTo(positionMs: Long) {
            lastSeekMs = positionMs
            this.positionMs = positionMs
        }

        override fun setSpeed(speed: Float) {
            lastSpeed = speed
        }

        override fun setListener(listener: RecitationAudioOutputListener?) {
            this.listener = listener
        }
    }
}
