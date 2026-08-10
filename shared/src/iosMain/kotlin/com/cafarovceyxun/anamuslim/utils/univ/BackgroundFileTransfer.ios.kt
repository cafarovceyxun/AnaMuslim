package com.cafarovceyxun.anamuslim.utils.univ

import com.cafarovceyxun.anamuslim.utils.download.IosBackgroundDownloads
import okio.Path

/**
 * iOS cannot keep a Ktor transfer running once the process is suspended, so the work is handed to a
 * background `NSURLSession` — see [IosBackgroundDownloads] for how the handover and the app-relaunch
 * path work.
 */
actual object BackgroundFileTransfer {

    actual val continuesWithoutApp: Boolean = true

    actual suspend fun download(
        url: String,
        target: Path,
        label: String?,
        onProgress: suspend (consumed: Long, total: Long) -> Unit,
    ) = IosBackgroundDownloads.download(url, target, onProgress, label)
}
