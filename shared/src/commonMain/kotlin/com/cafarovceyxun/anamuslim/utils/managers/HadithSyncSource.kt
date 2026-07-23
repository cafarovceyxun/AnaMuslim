package com.cafarovceyxun.anamuslim.utils.managers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Platform-neutral view of the hadith background sync that shared ViewModels consume. The Android
 * implementation (`HadithDownloadManager`, backed by WorkManager/Context) stays in `:app`;
 * commonMain depends only on this interface, registered via [HadithSyncProvider] at startup.
 *
 * Mirrors the manager-source seam pattern of
 * [com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelSource].
 */
interface HadithSyncSource {
    fun initialize()
    fun startSync()
    fun stopSync()
    fun observeSyncStatus(): Flow<ResourceDownloadStatus>
}

/**
 * Startup-provider seam for [HadithSyncSource].
 *
 * Unlike the other provider seams this one **falls back to a no-op** rather than throwing: hadith
 * browsing must work on iOS before background sync exists there (BGTaskScheduler — Faza 4/6). The
 * fallback reports [ResourceDownloadStatus.Idle], so the UI simply shows "not syncing".
 */
object HadithSyncProvider {
    private var provider: (() -> HadithSyncSource)? = null

    fun setSource(provider: () -> HadithSyncSource) {
        this.provider = provider
    }

    val source: HadithSyncSource
        get() = provider?.invoke() ?: NoOpHadithSyncSource

    private object NoOpHadithSyncSource : HadithSyncSource {
        override fun initialize() = Unit
        override fun startSync() = Unit
        override fun stopSync() = Unit
        override fun observeSyncStatus(): Flow<ResourceDownloadStatus> =
            flowOf(ResourceDownloadStatus.Idle)
    }
}
