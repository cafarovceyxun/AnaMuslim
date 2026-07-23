package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the reader/player UI needs from the recitation player, with no platform playback types in
 * the signatures.
 *
 * Android implements it with `RecitationController` (media3 `MediaController` + a foreground
 * `MediaLibraryService`); iOS will implement it over AVFoundation. The implementation is obtained
 * through [RecitationPlayerProvider], so shared view models and composables never touch a
 * `Context`.
 *
 * The surface mirrors the existing controller exactly — it was derived from its measured call
 * sites (reader/player UI, 13 files) rather than designed anew, so moving those files to
 * `commonMain` needs no behavioural change.
 */
interface RecitationPlayer {

    // ==================== State ====================

    /** Playback state published by the platform playback service. */
    val state: StateFlow<RecitationServiceState>

    /** Whether the UI is attached to a live playback session. */
    val isConnectedState: StateFlow<Boolean>

    val isPlayingState: StateFlow<Boolean>

    /** True only once buffering lasts long enough to be worth showing (short stalls don't flicker). */
    val isBufferingState: StateFlow<Boolean>

    val isPlaying: Boolean

    /** True while resolving a chapter's audio or buffering it. */
    val isLoading: Boolean

    val currentPositionMs: Long

    val durationMs: Long

    // ==================== Connection ====================

    /**
     * Attaches to the playback session, starting it if needed. Reference-counted: every [connect]
     * needs a matching [disconnect], and the session is released only when the last owner leaves.
     */
    fun connect()

    fun disconnect()

    // ==================== Playback ====================

    /** Plays [verse], or toggles play/pause when it is already the current one. */
    fun playControl(verse: ChapterVersePair)

    fun start(verse: ChapterVersePair? = null)

    /** Toggles play/pause; starts [suggestedVerse] when nothing is loaded yet. */
    fun playPause(suggestedVerse: ChapterVersePair? = null)

    fun pause()

    fun resume()

    fun stop()

    fun seekTo(positionMs: Long)

    fun seekLeft()

    fun seekRight()

    fun previousVerse()

    fun nextVerse()

    // ==================== Settings ====================

    fun setSpeed(speed: Float)

    fun setRepeatCount(repeatCount: Int)

    fun setAudioEndBehaviour(behaviour: AudioEndBehaviour)

    fun setAudioOption(option: AudioOption)

    fun setVerseGroupSize(size: Int)

    fun setReciter(id: String, kind: RecitationAudioKind)
}

/**
 * Startup seam handing shared code the platform's [RecitationPlayer], mirroring
 * `RepositoryProvider`.
 *
 * Registration points: Android `QuranApp.onCreate()`, iOS `initSharedForIos()` (once an
 * AVFoundation implementation exists).
 */
object RecitationPlayerProvider {

    private var provider: (() -> RecitationPlayer)? = null

    /** Registers how the [RecitationPlayer] is obtained on this platform. Call once at startup. */
    fun setProvider(provider: () -> RecitationPlayer) {
        this.provider = provider
    }

    /**
     * The platform [RecitationPlayer], or an inert one where playback is not implemented yet
     * (currently iOS) — the same "empty subsystem instead of a crash" rule the download seams use.
     *
     * This must not throw: [com.cafarovceyxun.anamuslim.viewModels.ReaderProviderViewModel] reads it
     * in its constructor, so an unset provider would take down the whole reader rather than just
     * the player controls.
     */
    val player: RecitationPlayer
        get() = provider?.invoke() ?: NoRecitationPlayer
}

/**
 * Silently ignores every command and reports a never-playing, never-connected session, so player UI
 * renders in its idle state on platforms without a playback implementation.
 */
private object NoRecitationPlayer : RecitationPlayer {
    override val state = MutableStateFlow(RecitationServiceState()).asStateFlow()
    override val isConnectedState = MutableStateFlow(false).asStateFlow()
    override val isPlayingState = MutableStateFlow(false).asStateFlow()
    override val isBufferingState = MutableStateFlow(false).asStateFlow()
    override val isPlaying = false
    override val isLoading = false
    override val currentPositionMs = 0L
    override val durationMs = 0L

    override fun connect() = Unit
    override fun disconnect() = Unit
    override fun playControl(verse: ChapterVersePair) = Unit
    override fun start(verse: ChapterVersePair?) = Unit
    override fun playPause(suggestedVerse: ChapterVersePair?) = Unit
    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun seekLeft() = Unit
    override fun seekRight() = Unit
    override fun previousVerse() = Unit
    override fun nextVerse() = Unit
    override fun setSpeed(speed: Float) = Unit
    override fun setRepeatCount(repeatCount: Int) = Unit
    override fun setAudioEndBehaviour(behaviour: AudioEndBehaviour) = Unit
    override fun setAudioOption(option: AudioOption) = Unit
    override fun setVerseGroupSize(size: Int) = Unit
    override fun setReciter(id: String, kind: RecitationAudioKind) = Unit
}
