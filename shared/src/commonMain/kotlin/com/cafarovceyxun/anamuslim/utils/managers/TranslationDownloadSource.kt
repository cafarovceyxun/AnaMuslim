package com.cafarovceyxun.anamuslim.utils.managers

import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Platform-neutral view of translation downloads that shared ViewModels consume. The Android
 * implementation (`TranslationDownloadManager`, backed by WorkManager/Context) stays in `:app`;
 * commonMain depends only on this interface, registered via [TranslationDownloadProvider] at
 * startup.
 *
 * Mirrors [HadithSyncSource] — same manager-source seam pattern, including its no-op fallback.
 */
interface TranslationDownloadSource {
    fun initialize()
    fun startDownload(bookInfo: TranslationBookInfoModel)
    fun stopDownload(slug: String)
    fun isDownloading(slug: String): Boolean

    /** Emits `slug to status` as downloads progress. */
    fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>>
}

/**
 * Startup-provider seam for [TranslationDownloadSource].
 *
 * Like [HadithSyncProvider] this **falls back to a no-op** instead of throwing: browsing and
 * reading already-downloaded translations must work on iOS before background downloads exist there
 * (BGTaskScheduler — Faza 4/6). The fallback simply never reports a download.
 */
object TranslationDownloadProvider {
    private var provider: (() -> TranslationDownloadSource)? = null

    fun setSource(provider: () -> TranslationDownloadSource) {
        this.provider = provider
    }

    val source: TranslationDownloadSource
        get() = provider?.invoke() ?: NoOpTranslationDownloadSource

    private object NoOpTranslationDownloadSource : TranslationDownloadSource {
        override fun initialize() = Unit
        override fun startDownload(bookInfo: TranslationBookInfoModel) = Unit
        override fun stopDownload(slug: String) = Unit
        override fun isDownloading(slug: String): Boolean = false
        override fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> = emptyFlow()
    }
}
