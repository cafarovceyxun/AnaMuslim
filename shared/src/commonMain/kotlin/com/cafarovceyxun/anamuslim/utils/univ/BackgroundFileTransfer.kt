package com.cafarovceyxun.anamuslim.utils.univ

import okio.Path

/**
 * Downloads one file, using whatever the platform offers to keep the transfer alive while the app
 * is not in the foreground.
 *
 * Why this exists: on Android a download runs inside a WorkManager foreground worker, which already
 * survives backgrounding, so the actual there is simply the shared Ktor streamer. iOS suspends the
 * process instead — a Ktor transfer stops the moment the user leaves the app — so its actual hands
 * the transfer to a background `NSURLSession`, which the system continues (and, if the app has been
 * killed, finishes on its own and relaunches the app to report it).
 *
 * The contract is deliberately the same as the direct downloader's: suspend until the file is at
 * [target], report byte progress meanwhile, throw on failure. What differs is what happens when the
 * caller's process goes away mid-transfer — see [continuesWithoutApp].
 */
expect object BackgroundFileTransfer {

    /**
     * True when the transfer outlives the calling coroutine — i.e. the file can still appear after
     * the app is suspended or killed, and the platform layer, not the caller, reports it.
     *
     * Callers use this to decide whether "the coroutine was cancelled" means "the download stopped".
     */
    val continuesWithoutApp: Boolean

    /**
     * [label] names the download for the case where the app is gone when it finishes: the platform
     * layer then posts the "finished / failed" notification itself, because the caller that would
     * normally do it no longer exists. Ignored where [continuesWithoutApp] is false.
     */
    suspend fun download(
        url: String,
        target: Path,
        label: String? = null,
        onProgress: suspend (consumed: Long, total: Long) -> Unit,
    )
}
