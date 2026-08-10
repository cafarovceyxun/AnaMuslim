package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.utils.network.isNetworkConnected
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.Foundation.NSURL

/**
 * Plays a single word of the word-by-word audio on iOS, using the CDN's per-word MP3 clip.
 *
 * ⚠️ **It deliberately never touches the downloadable chapter files.** Those are WebM/Opus, which
 * AVFoundation cannot decode — measured on the simulator: the item goes straight to
 * `AVPlayerItemStatusFailed` while the player sits in `waitingToPlayAtSpecifiedRate` and nothing is
 * heard. Android plays them because ExoPlayer bundles its own WebM support. The per-word clip is
 * therefore the only playable source here, and iOS does not offer WBW chapter downloads at all
 * (see the bootstrap comment) — 114 undecodable files would be a pure waste of the user's data.
 *
 * A clip is exactly one word, so no timing lookup is involved: the whole file is the word.
 */
@OptIn(ExperimentalForeignApi::class)
class IosWbwAudioPlayer {

    private val player = AVPlayer()
    private var sessionActivated = false

    /** Activates the audio session ahead of the first tap, which otherwise pays that latency. */
    suspend fun warmUp() = withContext(Dispatchers.Main) {
        activateSession()
    }

    suspend fun play(chapterNo: Int, verseNo: Int, wordIndex: Int): WbwAudioPlayResult {
        // Clips are fetched per tap; offline there is nothing local that could stand in.
        if (!isNetworkConnected()) return WbwAudioPlayResult.NoInternet

        val url = WbwAudioFiles.prepareOneOffWordAudioUrl(chapterNo, verseNo, wordIndex)
            ?.let { NSURL.URLWithString(it) }
            ?: return WbwAudioPlayResult.NoChapterAudio

        return withContext(Dispatchers.Main) {
            activateSession()

            player.pause()
            player.replaceCurrentItemWithPlayerItem(AVPlayerItem(AVURLAsset(url, options = null)))
            player.play()

            WbwAudioPlayResult.Success
        }
    }

    private fun activateSession() {
        if (sessionActivated) return

        runCatching {
            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
            AVAudioSession.sharedInstance().setActive(true, null)
            sessionActivated = true
        }
    }
}
