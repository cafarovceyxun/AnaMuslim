package com.cafarovceyxun.anamuslim.utils.managers

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Android [HadithSyncSource]: adapts the WorkManager-backed [HadithDownloadManager] (which needs a
 * [Context]) to the Context-free seam that the shared `HadithViewModel` consumes. Registered in
 * `QuranApp.onCreate`.
 */
class AndroidHadithSyncSource(private val context: Context) : HadithSyncSource {
    override fun initialize() = HadithDownloadManager.initialize(context)

    override fun startSync() = HadithDownloadManager.startSync(context)

    override fun stopSync() = HadithDownloadManager.stopSync(context)

    override fun observeSyncStatus(): Flow<ResourceDownloadStatus> =
        HadithDownloadManager.observeSyncStatus()
}
