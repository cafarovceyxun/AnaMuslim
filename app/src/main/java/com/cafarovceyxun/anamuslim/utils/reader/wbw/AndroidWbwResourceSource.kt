package com.cafarovceyxun.anamuslim.utils.reader.wbw

import android.content.Context
import com.cafarovceyxun.anamuslim.api.models.wbw.AvailableWbwInfoModel
import com.cafarovceyxun.anamuslim.api.models.wbw.WbwLanguageInfo
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Android [WbwResourceSource]: the Context-bound `WbwManager` (manifest) and `WbwDownloadManager`
 * (WorkManager downloads) behind the shared seam. Registered in `QuranApp.onCreate()`.
 */
class AndroidWbwResourceSource(context: Context) : WbwResourceSource {

    private val appContext = context.applicationContext

    init {
        // Was `WbwSettingsViewModel.init`; the download manager is a platform concern, so it is
        // initialised where the platform source is built instead.
        WbwDownloadManager.initialize(appContext)
    }

    override suspend fun getAvailable(forceRefresh: Boolean): AvailableWbwInfoModel? =
        WbwManager.getAvailable(appContext, forceRefresh)

    override fun getResourceVersion(id: String): Int =
        WbwManager.getResourceVersion(appContext, id)

    override fun startDownload(info: WbwLanguageInfo) =
        WbwDownloadManager.startDownload(appContext, info)

    override fun stopDownload(id: String) =
        WbwDownloadManager.stopDownload(appContext, id)

    override fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> =
        WbwDownloadManager.observeDownloadsAsFlow()
}
