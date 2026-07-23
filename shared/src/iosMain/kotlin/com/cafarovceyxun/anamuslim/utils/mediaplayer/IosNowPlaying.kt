package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

/**
 * Publishes what is playing to the iOS system UI — lock screen, Control Centre, CarPlay — and
 * routes the controls there back into [RecitationPlayer].
 *
 * This is the iOS counterpart of what Android's `MediaLibraryService` gets for free from its media
 * session: the shared player has no notion of a system transport, so the bridging lives here.
 * Requires the `audio` background mode in `Info.plist`, otherwise iOS suspends playback (and this
 * metadata) as soon as the app leaves the foreground.
 */
class IosNowPlaying(
    private val player: RecitationPlayer,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) {

    private val infoCenter = MPNowPlayingInfoCenter.defaultCenter()

    /** Starts mirroring player state to the system and accepting its commands. Call once. */
    fun start() {
        registerRemoteCommands()
        observePlayer()
    }

    /** Republishes whenever the verse or the play/pause state changes. */
    private fun observePlayer() {
        scope.launch {
            combine(
                player.state.map { it.currentVerse }.distinctUntilChanged(),
                player.isPlayingState,
            ) { verse, isPlaying -> verse to isPlaying }
                .collect { (verse, isPlaying) ->
                    publish(verse, isPlaying)
                }
        }

        // iOS extrapolates elapsed time from the last published anchor, so state changes alone are
        // not enough: the anchor is set before buffering finishes (it would run ~a second fast), and
        // a seek moves the playhead without changing verse or play state at all. Re-anchoring on a
        // slow tick keeps the lock screen honest in both cases.
        scope.launch {
            while (true) {
                delay(REANCHOR_INTERVAL_MS)

                if (player.isPlaying) {
                    publish(player.state.value.currentVerse, isPlaying = true)
                }
            }
        }
    }

    private suspend fun publish(verse: ChapterVersePair, isPlaying: Boolean) {
        if (!verse.isValid) {
            infoCenter.nowPlayingInfo = null
            return
        }

        val chapterName = RepositoryProvider.quranRepository.getChapterName(verse.chapterNo)
        val reciter = RecitationModelManager.getCurrentReciterNameForAudioOption()

        infoCenter.nowPlayingInfo = mapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to "$chapterName, ${verse.verseNo}",
            MPMediaItemPropertyArtist to reciter,
            MPMediaItemPropertyAlbumTitle to chapterName,
            MPMediaItemPropertyPlaybackDuration to player.durationMs / 1000.0,
            MPNowPlayingInfoPropertyElapsedPlaybackTime to player.currentPositionMs / 1000.0,
            // Zero rate is how iOS renders the paused state; the elapsed time then stops advancing.
            MPNowPlayingInfoPropertyPlaybackRate to if (isPlaying) player.state.value.settings.speed.toDouble() else 0.0,
        )
    }

    /**
     * The command set mirrors the in-app player controls, so the lock screen can do everything the
     * mini player can. Previous/next move by verse, not by chapter — that is what those buttons do
     * in the app, and on Android too.
     */
    private fun registerRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()

        center.playCommand.addTargetWithHandler { handled { player.resume() } }
        center.pauseCommand.addTargetWithHandler { handled { player.pause() } }
        center.togglePlayPauseCommand.addTargetWithHandler { handled { player.playPause(null) } }
        center.nextTrackCommand.addTargetWithHandler { handled { player.nextVerse() } }
        center.previousTrackCommand.addTargetWithHandler { handled { player.previousVerse() } }
        center.skipForwardCommand.addTargetWithHandler { handled { player.seekRight() } }
        center.skipBackwardCommand.addTargetWithHandler { handled { player.seekLeft() } }

        center.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val position = (event as? MPChangePlaybackPositionCommandEvent)?.positionTime

            handled { position?.let { player.seekTo((it * 1000.0).toLong()) } }
        }
    }

    private companion object {
        /** How often the elapsed-time anchor is refreshed while playing. */
        const val REANCHOR_INTERVAL_MS = 3_000L
    }

    /** Every command handler does the same thing: run the action and report success. */
    private inline fun handled(action: () -> Unit): MPRemoteCommandHandlerStatus {
        action()
        return MPRemoteCommandHandlerStatusSuccess
    }
}
