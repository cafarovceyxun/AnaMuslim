package com.cafarovceyxun.anamuslim.utils.mediaplayer

/**
 * The playback mechanism a platform must provide: load a URI, transport controls, and where the
 * playhead is. Nothing about verses, reciters or repeat policy — that all lives in
 * [SharedRecitationPlayer], which drives this.
 *
 * This is the same split the download seams use (`RecitationDownloadSource` and friends):
 * mechanism on the platform, policy in `commonMain`. Android does not implement it — its playback
 * runs inside a media3 `MediaLibraryService` that owns transport itself; iOS implements it over
 * AVFoundation.
 */
interface RecitationAudioOutput {

    /** Current playhead position in milliseconds, or 0 when nothing is loaded. */
    val positionMs: Long

    /** Duration of the loaded audio in milliseconds, or 0 while unknown (still loading). */
    val durationMs: Long

    val isPlaying: Boolean

    /**
     * Loads [uri] (a `file://` path or streaming URL) and starts playing at [startPositionMs].
     * Any previously loaded audio is discarded.
     */
    fun load(uri: String, startPositionMs: Long, speed: Float)

    fun play()

    fun pause()

    /** Stops playback and releases the loaded audio; [positionMs] and [durationMs] go back to 0. */
    fun stop()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    fun setListener(listener: RecitationAudioOutputListener?)
}

/** Playback events the shared player reacts to. Always delivered on the main thread. */
interface RecitationAudioOutputListener {

    fun onPlayingChanged(isPlaying: Boolean)

    /** True once the output is stalled waiting for data — the UI shows a spinner after a delay. */
    fun onBufferingChanged(isBuffering: Boolean)

    /** The loaded audio reached its end (not fired on [RecitationAudioOutput.stop]). */
    fun onEnded()

    fun onError(error: Throwable)
}
