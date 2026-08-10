package com.cafarovceyxun.anamuslim.utils.univ

import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationAudioFileDownloader
import okio.Path

/**
 * Android already has the mechanism this abstraction is about: every download runs inside a
 * WorkManager foreground worker, which keeps going when the user leaves the app. So the transfer
 * itself stays exactly what it was — the shared Ktor streamer — and this is a pass-through.
 *
 * [continuesWithoutApp] is false because the *coroutine* still owns the transfer: cancel the worker
 * and the download stops, which is the behaviour the download screens are written against.
 */
actual object BackgroundFileTransfer {

    actual val continuesWithoutApp: Boolean = false

    actual suspend fun download(
        url: String,
        target: Path,
        label: String?,
        onProgress: suspend (consumed: Long, total: Long) -> Unit,
    ) = RecitationAudioFileDownloader.downloadToFile(url, target, onProgress)
}
