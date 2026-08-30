package com.cafarovceyxun.anamuslim.utils.background

import com.cafarovceyxun.anamuslim.compose.utils.IosDailyReminder
import com.cafarovceyxun.anamuslim.shared.initSharedForIos
import com.cafarovceyxun.anamuslim.shared.runIosResourceMaintenance
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.managers.HadithSyncProvider
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.managers.TranslationDownloadProvider
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.dateByAddingTimeInterval
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Resource sync that runs while the app is not on screen — the iOS counterpart of the WorkManager
 * jobs Android schedules from `AppActions.scheduleActions`.
 *
 * File transfers are already covered by the background `NSURLSession` in
 * [com.cafarovceyxun.anamuslim.utils.download.IosBackgroundDownloads]: the system runs those even
 * when the app is suspended. Hadith sync and translation downloads cannot use it — they are
 * Supabase requests plus database writes, i.e. *our* code has to be executing. iOS gives that in
 * two shapes, and this object uses both:
 *
 * - **[BGProcessingTaskRequest]** — the system wakes the app minutes or hours later, at a moment it
 *   considers cheap, and grants it a long run. This is where the daily resource-version check
 *   belongs; it is opportunistic by nature, so a delayed run is a correct run.
 * - **`beginBackgroundTask`** — a few extra seconds of foreground-grade execution right after the
 *   user leaves the app. This is what saves a sync that was *already running* when they switched
 *   away, which the scheduled task cannot do (it may not fire for hours).
 *
 * ⚠️ [register] must run before the app finishes launching, or `BGTaskScheduler` throws — so it is
 * called from the Swift app delegate rather than from `initSharedForIos()`, which only runs when
 * the first composition happens (and never at all on a background launch).
 */
@OptIn(ExperimentalForeignApi::class)
object IosBackgroundTasks {

    /** Must match `BGTaskSchedulerPermittedIdentifiers` in `Info.plist` or registration fails. */
    private const val REFRESH_TASK_ID = "com.cafarovceyxun.anamuslim.refresh"

    /**
     * Earliest the system may run the task — six hours, i.e. up to four wake-ups a day, matching
     * the Android poll interval. Asking sooner would only burn wake-ups (the resource check is
     * guarded to once a day and the reminder is deduped), asking later would let a freshly
     * published daily verse sit unseen.
     */
    private const val EARLIEST_BEGIN_SECONDS = 6.0 * 60 * 60

    private const val POLL_INTERVAL_MS = 1_000L

    /** Upper bound on waiting for in-flight downloads; the system's own budget is shorter. */
    private const val MAINTENANCE_TIMEOUT_MS = 3 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var registered = false
    private var keepAliveId: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid
    private var keepAliveJob: Job? = null

    /** Called from the Swift app delegate's `didFinishLaunchingWithOptions`. */
    fun register() {
        if (registered) return
        registered = true

        val accepted = BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = REFRESH_TASK_ID,
            usingQueue = null, // a private queue; the work hops to a coroutine dispatcher anyway
        ) { task -> handle(task) }

        if (!accepted) {
            // Only happens when the identifier is missing from Info.plist — the app still works,
            // it just never gets woken up for maintenance.
            AppLogger.d("IosBackgroundTasks: '$REFRESH_TASK_ID' qeydiyyatdan keçmədi")
        }

        // Independent of the registration above: the keep-alive assertion is useful even when the
        // scheduler refused us.
        observeBackgroundEntry()
    }

    /**
     * Asks the system to wake the app for maintenance. Submitting replaces any pending request for
     * the same identifier, so this is safe to call on every background entry (Apple's recommended
     * moment) and after each run.
     */
    fun schedule() {
        val request = BGProcessingTaskRequest(REFRESH_TASK_ID).apply {
            earliestBeginDate = NSDate().dateByAddingTimeInterval(EARLIEST_BEGIN_SECONDS)
            // Every step of the maintenance talks to the network; running it offline would just
            // burn the wake-up.
            requiresNetworkConnectivity = true
            // Charging is not worth waiting for: the work is a handful of requests.
            requiresExternalPower = false
        }

        val scheduler = BGTaskScheduler.sharedScheduler
        scheduler.cancelTaskRequestWithIdentifier(REFRESH_TASK_ID)
        if (!scheduler.submitTaskRequest(request, null)) {
            // Expected on the simulator, which has no scheduler daemon.
            AppLogger.d("IosBackgroundTasks: planlaşdırma sorğusu qəbul olunmadı")
        }
    }

    private fun handle(task: BGTask?) {
        val bgTask = task ?: return

        // Chain the next run first: if the work below fails, the schedule must not die with it.
        schedule()

        val job = scope.launch { runMaintenance() }
        // The system calls this shortly before it kills the task; cancelling lets the coroutine
        // unwind so `invokeOnCompletion` still reports the (unsuccessful) outcome.
        bgTask.expirationHandler = { job.cancel() }
        job.invokeOnCompletion { cause ->
            dispatch_async(dispatch_get_main_queue()) {
                bgTask.setTaskCompletedWithSuccess(cause == null)
            }
        }
    }

    private suspend fun runMaintenance() {
        // A background launch has no composition, so nothing has wired the shared layer yet.
        initSharedForIos()
        // Cheap and independent of the resource work below: one Supabase read, then the upcoming
        // queue slots are (re)written as pending notifications.
        IosDailyReminder.sync()
        runIosResourceMaintenance()
        // The check above only *starts* a hadith sync / translation re-download; finishing them is
        // the whole reason this task exists, so hold the task open until they settle.
        awaitResourceWorkIdle(MAINTENANCE_TIMEOUT_MS)
    }

    /**
     * Buys the in-flight sync a little more time when the user leaves the app, and books the next
     * scheduled run. Both belong on background entry: the assertion is only grantable from the
     * foreground, and Apple asks for the request to be submitted as the app goes away.
     */
    private fun observeBackgroundEntry() {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            schedule()
            beginKeepAlive()
        }
    }

    /**
     * Takes a background-task assertion and holds it until the resource work goes quiet. Started
     * unconditionally rather than after an "is anything running" check because the check is
     * suspending: when nothing is in flight the assertion is released a moment later, which costs
     * nothing, whereas taking it late could miss the window entirely.
     *
     * Runs on the main thread (the notification is delivered there), which is where UIKit wants
     * `beginBackgroundTask` called.
     */
    private fun beginKeepAlive() {
        if (keepAliveId != UIBackgroundTaskInvalid) return

        keepAliveId = UIApplication.sharedApplication.beginBackgroundTaskWithName(
            taskName = "resource-sync",
        ) {
            // Time is up: iOS kills the app unless the assertion is released right now.
            endKeepAlive()
        }

        keepAliveJob = scope.launch {
            try {
                awaitResourceWorkIdle(MAINTENANCE_TIMEOUT_MS)
            } finally {
                dispatch_async(dispatch_get_main_queue()) { endKeepAlive() }
            }
        }
    }

    private fun endKeepAlive() {
        val id = keepAliveId
        if (id == UIBackgroundTaskInvalid) return

        keepAliveId = UIBackgroundTaskInvalid
        keepAliveJob?.cancel()
        keepAliveJob = null
        UIApplication.sharedApplication.endBackgroundTask(id)
    }

    /** Polls until no resource sync is running, or [timeoutMs] elapses. */
    private suspend fun awaitResourceWorkIdle(timeoutMs: Long) {
        val deadline = currentEpochMillis() + timeoutMs
        while (currentEpochMillis() < deadline) {
            if (!resourceWorkInFlight()) return
            delay(POLL_INTERVAL_MS)
        }
        AppLogger.d("IosBackgroundTasks: resurs işi vaxt həddində bitmədi")
    }

    /**
     * Whether a hadith sync or a translation download is still running. Read through the same seams
     * the UI observes rather than the concrete iOS implementations, so this stays true if the
     * registered sources ever change.
     */
    private suspend fun resourceWorkInFlight(): Boolean {
        // Both shared sources back their status with a StateFlow, so `first()` is the current value.
        val hadith = HadithSyncProvider.source.observeSyncStatus().first()
        if (hadith is ResourceDownloadStatus.Started || hadith is ResourceDownloadStatus.InProgress) {
            return true
        }

        val factory = QuranTranslationFactory()
        return try {
            factory.getDownloadedTranslationBooksInfo().keys.any { slug ->
                TranslationDownloadProvider.source.isDownloading(slug)
            }
        } finally {
            factory.close()
        }
    }
}
