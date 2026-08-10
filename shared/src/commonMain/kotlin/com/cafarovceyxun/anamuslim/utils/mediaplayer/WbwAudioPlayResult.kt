package com.cafarovceyxun.anamuslim.utils.mediaplayer

/** Outcome of a word-by-word audio play request; consumed by the reader to surface feedback. */
sealed class WbwAudioPlayResult {
    data object Success : WbwAudioPlayResult()
    data object NoInternet : WbwAudioPlayResult()
    data object TimingsNotLoaded : WbwAudioPlayResult()
    data object InvalidTiming : WbwAudioPlayResult()
    data object NoChapterAudio : WbwAudioPlayResult()
}
