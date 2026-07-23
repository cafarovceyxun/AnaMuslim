package com.cafarovceyxun.anamuslim.utils.managers

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strTitleHadith
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.DownloadNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Multiplatform [HadithSyncSource]: runs [HadithSyncEngine] on a coroutine scope.
 *
 * Android keeps its WorkManager implementation (`AndroidHadithSyncSource`) because the sync there
 * must survive backgrounding and show a notification; the *work* is the same shared engine in both
 * cases. iOS registers this class, so the hadith tab can be populated without a scheduler.
 *
 * Status is a [MutableStateFlow] rather than an event flow because it mirrors WorkManager's
 * observable state: a late subscriber must immediately see that a sync is running, not wait for
 * the next progress tick.
 */
class SharedHadithSync(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : HadithSyncSource {

    private val status = MutableStateFlow<ResourceDownloadStatus>(ResourceDownloadStatus.Idle)
    private var job: Job? = null

    override fun initialize() = Unit

    override fun observeSyncStatus(): Flow<ResourceDownloadStatus> = status.asStateFlow()

    override fun startSync() {
        // WorkManager's KEEP policy: a second request while one runs is ignored.
        if (job?.isActive == true) return

        status.value = ResourceDownloadStatus.Started
        job = scope.launch {
            try {
                HadithSyncEngine.sync { _, progress ->
                    status.value = ResourceDownloadStatus.InProgress(progress)
                }
                status.value = ResourceDownloadStatus.Completed
                DownloadNotifier.completed(getString(Res.string.strTitleHadith))
            } catch (e: CancellationException) {
                status.value = ResourceDownloadStatus.Cancelled
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "SharedHadithSync")
                status.value = ResourceDownloadStatus.Failed(e.message)
                DownloadNotifier.failed(getString(Res.string.strTitleHadith))
            }
        }
    }

    override fun stopSync() {
        job?.cancel()
    }
}
