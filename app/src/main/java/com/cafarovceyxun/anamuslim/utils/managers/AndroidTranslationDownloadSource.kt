package com.cafarovceyxun.anamuslim.utils.managers

import android.content.Context
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import kotlinx.coroutines.flow.Flow

/**
 * Android [TranslationDownloadSource]: adapts the WorkManager-backed [TranslationDownloadManager]
 * (which needs a [Context]) to the Context-free seam that the shared `TranslationViewModel`
 * consumes. Registered in `QuranApp.onCreate`; mirrors [AndroidHadithSyncSource].
 */
class AndroidTranslationDownloadSource(private val context: Context) : TranslationDownloadSource {
    override fun initialize() = TranslationDownloadManager.initialize(context)

    override fun startDownload(bookInfo: TranslationBookInfoModel) =
        TranslationDownloadManager.startDownload(context, bookInfo)

    override fun stopDownload(slug: String) = TranslationDownloadManager.stopDownload(context, slug)

    override fun isDownloading(slug: String): Boolean =
        TranslationDownloadManager.isDownloading(slug)

    override fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> =
        TranslationDownloadManager.observeDownloadsAsFlow()
}
