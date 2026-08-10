package com.cafarovceyxun.anamuslim.utils.download

import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.DownloadNotifier
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * File downloads that survive the app leaving the foreground, via a background `NSURLSession`.
 *
 * This is the iOS half of [com.cafarovceyxun.anamuslim.utils.univ.BackgroundFileTransfer]. Android
 * needs nothing like it: its downloads live in WorkManager foreground workers. iOS suspends the
 * process within seconds of backgrounding, so a Ktor transfer simply stops — reciter audio, script
 * fonts and the atlas bundle are exactly the downloads a user starts and then walks away from.
 *
 * How the handover works:
 * - A background session's transfers are run by the system, not by us. The task keeps going while
 *   the app is suspended and even if it is killed; on completion the system relaunches the app in
 *   the background and replays the delegate callbacks.
 * - Because a relaunch loses all in-memory state, **the destination path travels on the task
 *   itself** (`taskDescription`), not in [waiters]. A completion with no waiter is an "orphan": the
 *   file is still published, and — since no coroutine is left to report it — the notification is
 *   posted from here.
 * - `URLSessionDidFinishEventsForBackgroundURLSession` must be answered with the completion handler
 *   iOS passed to the app delegate, or the system stops trusting the app with background time. That
 *   handler is parked in [backgroundEventsCompletion] by the Swift side.
 */
@OptIn(ExperimentalForeignApi::class)
object IosBackgroundDownloads {

    /** Stable across launches: the system reattaches pending tasks to the session with this id. */
    private const val SESSION_ID = "com.cafarovceyxun.anamuslim.downloads"

    /** `taskDescription` carries "path" or "path\nlabel" — see the class comment. */
    private const val DESCRIPTION_SEPARATOR = "\n"

    private const val PROGRESS_INTERVAL_MS = 200L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = ReentrantLock()
    // `taskIdentifier` is an NSUInteger, so the key is ULong rather than Long.
    private val waiters = mutableMapOf<ULong, Waiter>()

    private var backgroundEventsCompletion: (() -> Unit)? = null

    private class Waiter(
        val target: Path,
        val deferred: CompletableDeferred<Unit>,
        val onProgress: suspend (Long, Long) -> Unit,
    ) {
        var lastProgressAt = 0L
    }

    /**
     * Creates the session at startup so the system can deliver callbacks for transfers that
     * outlived the previous run. Without this the app would only reattach once something downloads.
     */
    fun install() {
        session
    }

    /** Called from the Swift app delegate when iOS wakes the app for this session's events. */
    fun setBackgroundEventsCompletion(handler: () -> Unit) {
        backgroundEventsCompletion = handler
    }

    /**
     * [label] names the download in the notification posted when the app was not around to see it
     * finish. When the caller is still alive it reports completion itself, and this is unused.
     */
    suspend fun download(
        url: String,
        target: Path,
        onProgress: suspend (consumed: Long, total: Long) -> Unit,
        label: String? = null,
    ) {
        val nsUrl = NSURL.URLWithString(url) ?: throw IOException("Invalid download URL: $url")

        val deferred = CompletableDeferred<Unit>()
        val task = session.downloadTaskWithURL(nsUrl)
        task.taskDescription = if (label == null) {
            target.toString()
        } else {
            "$target$DESCRIPTION_SEPARATOR$label"
        }

        lock.withLock { waiters[task.taskIdentifier] = Waiter(target, deferred, onProgress) }
        task.resume()

        try {
            deferred.await()
        } catch (e: Throwable) {
            // Cancellation included: the caller asked to stop, so the system transfer stops too.
            lock.withLock { waiters.remove(task.taskIdentifier) }
            task.cancel()
            throw e
        }
    }

    private val session: NSURLSession by lazy {
        val configuration =
            NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(SESSION_ID)
        // Wake the app when a transfer finishes while it is not running.
        configuration.sessionSendsLaunchEvents = true
        // The user asked for these downloads; do not let iOS defer them to a "convenient" moment.
        configuration.discretionary = false

        NSURLSession.sessionWithConfiguration(
            configuration = configuration,
            delegate = delegate,
            delegateQueue = null,
        )
    }

    private val delegate = object : NSObject(), NSURLSessionDownloadDelegateProtocol {

        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didFinishDownloadingToURL: NSURL,
        ) {
            // The temp file is deleted as soon as this returns, so the move happens inline here.
            val (targetPath, label) = describedTarget(downloadTask) ?: run {
                AppLogger.d("IosBackgroundDownloads: finished task without a destination, dropping")
                return
            }

            val status = (downloadTask.response as? NSHTTPURLResponse)?.statusCode
            if (status != null && status >= 400) {
                // A background download reports HTTP errors as a *successful* transfer of the error
                // body, so the status has to be checked by hand or a 404 page lands as an mp3.
                failWaiter(downloadTask, IOException("Download failed: HTTP $status"), label)
                return
            }

            val moved = movePublished(didFinishDownloadingToURL, targetPath)
            if (!moved) {
                failWaiter(downloadTask, IOException("Could not move download to $targetPath"), label)
                return
            }

            val waiter = lock.withLock { waiters.remove(downloadTask.taskIdentifier) }
            if (waiter != null) {
                waiter.deferred.complete(Unit)
            } else if (label != null) {
                // Orphan: the app was relaunched to receive this, so nobody else can report it.
                scope.launch { DownloadNotifier.completed(label) }
            }
        }

        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didWriteData: Long,
            totalBytesWritten: Long,
            totalBytesExpectedToWrite: Long,
        ) {
            val waiter = lock.withLock { waiters[downloadTask.taskIdentifier] } ?: return

            val now = currentEpochMillis()
            val done = totalBytesExpectedToWrite in 1..totalBytesWritten
            // Same throttle as the direct downloader, plus an always-emitted final tick so a bound
            // progress bar lands on 100%.
            if (!done && now - waiter.lastProgressAt < PROGRESS_INTERVAL_MS) return
            waiter.lastProgressAt = now

            scope.launch { waiter.onProgress(totalBytesWritten, totalBytesExpectedToWrite) }
        }

        override fun URLSession(
            session: NSURLSession,
            task: NSURLSessionTask,
            didCompleteWithError: NSError?,
        ) {
            val error = didCompleteWithError ?: return // success is handled in didFinishDownloading

            val label = describedTarget(task)?.second
            failWaiter(task, IOException(error.localizedDescription), label)
        }

        override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
            val handler = backgroundEventsCompletion ?: return
            backgroundEventsCompletion = null
            // UIKit requires this on the main thread.
            dispatch_async(dispatch_get_main_queue()) { handler() }
        }
    }

    /** Destination and notification label as they were written onto the task. */
    private fun describedTarget(task: NSURLSessionTask): Pair<Path, String?>? {
        val description = task.taskDescription ?: return null
        val parts = description.split(DESCRIPTION_SEPARATOR, limit = 2)
        val path = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null

        return path.toPath() to parts.getOrNull(1)
    }

    private fun movePublished(temporary: NSURL, target: Path): Boolean {
        val manager = NSFileManager.defaultManager
        val parent = target.parent?.toString()
        if (parent != null) {
            manager.createDirectoryAtPath(parent, withIntermediateDirectories = true, attributes = null, error = null)
        }

        val targetUrl = NSURL.fileURLWithPath(target.toString())
        // Replacing is expected: a retried download overwrites the previous attempt.
        manager.removeItemAtURL(targetUrl, error = null)

        return manager.moveItemAtURL(temporary, toURL = targetUrl, error = null)
    }

    private fun failWaiter(task: NSURLSessionTask, cause: Throwable, label: String?) {
        val waiter = lock.withLock { waiters.remove(task.taskIdentifier) }
        if (waiter != null) {
            waiter.deferred.completeExceptionally(cause)
        } else {
            AppLogger.d("IosBackgroundDownloads: orphan failure — ${cause.message}")
            if (label != null) scope.launch { DownloadNotifier.failed(label) }
        }
    }
}
