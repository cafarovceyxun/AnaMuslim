package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.error
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL

/**
 * AVFoundation implementation of the playback mechanism [SharedRecitationPlayer] drives.
 *
 * Deliberately thin: one `AVPlayer` with a single item, no queue. Verse boundaries, repeats and
 * chapter transitions are the shared player's job, so this only has to load a URI, move the
 * playhead and report what it is doing.
 */
@OptIn(ExperimentalForeignApi::class)
class AVFoundationAudioOutput : RecitationAudioOutput {

    private val player = AVPlayer()

    private var listener: RecitationAudioOutputListener? = null

    /** Playback rate to restore on `play()` — AVPlayer expresses speed as the rate itself. */
    private var speed: Float = 1.0f

    private var endObserver: Any? = null
    private var timeObserver: Any? = null
    private var reportedPlaying = false
    private var reportedBuffering = false
    private var reportedItemError = false

    /**
     * Playback category, so recitation keeps playing with the ring switch silenced and while the
     * screen is locked — the iOS counterpart of Android's media-style foreground service.
     */
    init {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        } catch (e: Exception) {
            AppLogger.saveError(e, "AVFoundationAudioOutput.audioSession")
        }

        observeAudioSession()
    }

    /**
     * Pauses for the two events iOS expects an audio app to handle: an interruption (a call, or
     * another app taking the session) and the current output going away (headphones unplugged).
     * Without the route-change half, unplugging headphones would carry on reciting out loud through
     * the speaker — the same reason Android has `RecitationHeadsetReceiver`.
     */
    private fun observeAudioSession() {
        val center = NSNotificationCenter.defaultCenter

        center.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification: NSNotification? ->
            val type = (notification?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber)
                ?.unsignedLongValue

            if (type == AVAudioSessionInterruptionTypeBegan) {
                pause()
            }
            // Deliberately not auto-resuming when the interruption ends: the reader UI shows a
            // paused player and the listener decides, which is what Android does after audio-focus
            // loss too.
        }

        center.addObserverForName(
            name = AVAudioSessionRouteChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification: NSNotification? ->
            val reason = (notification?.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)
                ?.unsignedLongValue

            if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
                pause()
            }
        }
    }

    override val positionMs: Long
        get() = player.currentTime().toMillisOrZero()

    override val durationMs: Long
        get() = player.currentItem?.duration?.toMillisOrZero() ?: 0L

    override val isPlaying: Boolean
        get() = player.timeControlStatus == AVPlayerTimeControlStatusPlaying

    override fun load(uri: String, startPositionMs: Long, speed: Float) {
        this.speed = speed

        val url = NSURL.URLWithString(uri)

        if (url == null) {
            listener?.onError(IllegalArgumentException("Unplayable audio URI: $uri"))
            return
        }

        removeEndObserver()
        reportedItemError = false

        AppLogger.d("AVFoundationAudioOutput", "load $uri @${startPositionMs}ms")

        // MIME hint: the GitHub-hosted reciters redirect to a storage host that answers
        // `application/octet-stream` from an extensionless path, leaving AVFoundation with nothing
        // to infer the container from. (ExoPlayer sniffs the bytes, which is why Android never
        // needed this.) The key is not in the Kotlin/Native bindings, hence the literal name.
        val asset = AVURLAsset(
            uRL = url,
            options = mapOf<Any?, Any?>(OUT_OF_BAND_MIME_TYPE_KEY to AUDIO_MIME_TYPE),
        )
        val item = AVPlayerItem(asset = asset)
        player.replaceCurrentItemWithPlayerItem(item)

        observeEnd(item)
        observePlayerState()

        if (startPositionMs > 0L) {
            seekTo(startPositionMs)
        }

        play()
    }

    override fun play() {
        player.play()
        // `play()` resets the rate to 1.0, so the chosen speed has to be reapplied after it.
        player.rate = speed
        syncFromPlayer()
    }

    override fun pause() {
        player.pause()
        syncFromPlayer()
    }

    override fun stop() {
        player.pause()
        removeEndObserver()
        player.replaceCurrentItemWithPlayerItem(null)
        reportedItemError = false
        publishPlaying(false)
        publishBuffering(false)
    }

    override fun seekTo(positionMs: Long) {
        player.seekToTime(CMTimeMakeWithSeconds(positionMs / 1000.0, NSEC_PER_SEC))
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed

        // Changing the rate on a paused player would start it, so only live playback is retimed.
        if (isPlaying) {
            player.rate = speed
        }
    }

    override fun setListener(listener: RecitationAudioOutputListener?) {
        this.listener = listener
    }

    /**
     * Mirrors AVPlayer's own state instead of assuming the transport calls took effect: an item can
     * fail to load, or sit waiting for data, long after `play()` returned. Also the source of the
     * buffering signal — `waitingToPlayAtSpecifiedRate` is AVFoundation's "stalled" state.
     */
    private fun syncFromPlayer() {
        val status = player.timeControlStatus

        publishPlaying(status == AVPlayerTimeControlStatusPlaying)
        publishBuffering(status == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate)

        val error = player.currentItem?.error ?: player.error

        if (error != null && !reportedItemError) {
            reportedItemError = true
            listener?.onError(IllegalStateException("AVPlayer: ${error.localizedDescription}"))
        }
    }

    /** Drives [syncFromPlayer] while an item is loaded; AVPlayer has no state callback of its own. */
    private fun observePlayerState() {
        removeTimeObserver()

        timeObserver = player.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(STATE_POLL_SECONDS, NSEC_PER_SEC),
            queue = null,
        ) { _ ->
            syncFromPlayer()
        }
    }

    private fun removeTimeObserver() {
        timeObserver?.let { player.removeTimeObserver(it) }
        timeObserver = null
    }

    private fun observeEnd(item: AVPlayerItem) {
        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = NSOperationQueue.mainQueue,
        ) { _: NSNotification? ->
            publishPlaying(false)
            listener?.onEnded()
        }
    }

    private fun removeEndObserver() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
    }

    private fun publishPlaying(playing: Boolean) {
        if (reportedPlaying == playing) return
        reportedPlaying = playing
        listener?.onPlayingChanged(playing)
    }

    private fun publishBuffering(buffering: Boolean) {
        if (reportedBuffering == buffering) return
        reportedBuffering = buffering
        listener?.onBufferingChanged(buffering)
    }

    /** `CMTime` is invalid/indefinite until an item is ready; both map to "no position yet". */
    private fun CValue<CMTime>.toMillisOrZero(): Long {
        val seconds = CMTimeGetSeconds(this)

        if (seconds.isNaN() || seconds.isInfinite() || seconds < 0.0) return 0L

        return (seconds * 1000.0).toLong()
    }

    private companion object {
        const val NSEC_PER_SEC = 1_000_000_000

        /** How often AVPlayer's state is re-read while an item is loaded. */
        const val STATE_POLL_SECONDS = 0.25

        /** `AVURLAssetOutOfBandMIMETypeKey` — absent from the Kotlin/Native AVFoundation bindings. */
        const val OUT_OF_BAND_MIME_TYPE_KEY = "AVURLAssetOutOfBandMIMETypeKey"

        /** Every reciter source in the catalog serves MPEG audio. */
        const val AUDIO_MIME_TYPE = "audio/mpeg"
    }
}
