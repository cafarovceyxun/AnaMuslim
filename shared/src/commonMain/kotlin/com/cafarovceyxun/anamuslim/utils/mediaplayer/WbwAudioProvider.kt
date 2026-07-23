package com.cafarovceyxun.anamuslim.utils.mediaplayer

/**
 * Platform seam for the word-by-word audio subsystem, mirroring [RecitationPlayerProvider]. The
 * Android app registers the media3-backed player and the file-backed manifest reader at startup
 * (`QuranApp.onCreate()`); `commonMain` reader UI calls through here without touching `Context`,
 * media3, or the filesystem. iOS leaves the providers unset until an audio pipeline exists there —
 * [play] then reports [WbwAudioPlayResult.NoChapterAudio] and [langCode] returns null.
 */
object WbwAudioProvider {

    /** Resolves the BCP-47 language code of the selected WBW dataset, for RTL layout decisions. */
    var langCodeProvider: (suspend (wbwId: String) -> String?)? = null

    /** Plays the audio for a single word and reports the outcome. */
    var playProvider: (suspend (chapterNo: Int, verseNo: Int, wordIndex: Int) -> WbwAudioPlayResult)? = null

    /**
     * Pre-warms the platform player so the first word tap is not delayed. Purely an optimisation —
     * leaving it unset only costs latency.
     */
    var warmUpProvider: (suspend () -> Unit)? = null

    suspend fun langCode(wbwId: String): String? = langCodeProvider?.invoke(wbwId)

    suspend fun warmUp() {
        warmUpProvider?.invoke()
    }

    suspend fun play(chapterNo: Int, verseNo: Int, wordIndex: Int): WbwAudioPlayResult =
        playProvider?.invoke(chapterNo, verseNo, wordIndex) ?: WbwAudioPlayResult.NoChapterAudio
}
