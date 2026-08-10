package com.cafarovceyxun.anamuslim.utils.managers

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cafarovceyxun.anamuslim.utils.workers.HadithDownloadWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object HadithDownloadManager {
    private const val SYNC_WORK_NAME = "HadithFullSync"

    private val syncState = MutableLiveData<WorkInfo?>()

    fun initialize(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.getWorkInfosForUniqueWorkLiveData(SYNC_WORK_NAME).observeForever { workInfos ->
            syncState.postValue(workInfos.firstOrNull { !it.state.isFinished } ?: workInfos.firstOrNull())
        }
    }

    fun startSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<HadithDownloadWorker>()
            .build()

        val wm = WorkManager.getInstance(context)
        wm.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun stopSync(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(SYNC_WORK_NAME)
    }

    fun observeSyncStatus(): Flow<ResourceDownloadStatus> = callbackFlow {
        val observer = Observer<WorkInfo?> { workInfo ->
            val status = when (workInfo?.state) {
                WorkInfo.State.ENQUEUED -> ResourceDownloadStatus.Started
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getInt("progress", 0)
                    ResourceDownloadStatus.InProgress(progress)
                }
                WorkInfo.State.SUCCEEDED -> ResourceDownloadStatus.Completed
                WorkInfo.State.FAILED -> ResourceDownloadStatus.Failed(null)
                WorkInfo.State.CANCELLED -> ResourceDownloadStatus.Cancelled
                else -> ResourceDownloadStatus.Idle
            }
            trySend(status)
        }

        syncState.observeForever(observer)
        awaitClose {
            syncState.removeObserver(observer)
        }
    }

    fun isSyncing(): Boolean {
        val state = syncState.value?.state ?: return false
        return state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING
    }
}
