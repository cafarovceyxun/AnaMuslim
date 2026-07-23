package com.cafarovceyxun.anamuslim.utils.reader.wbw

import com.cafarovceyxun.anamuslim.api.models.wbw.AvailableWbwInfoModel
import com.cafarovceyxun.anamuslim.api.models.wbw.WbwLanguageInfo
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Platform-neutral view of the word-by-word dataset catalog and its downloads, for shared
 * ViewModels. The Android implementations (`WbwManager` + `WbwDownloadManager`, backed by
 * Context/WorkManager/filesystem) stay in `:app`; commonMain depends only on this interface,
 * registered via [WbwResourceProvider] at startup.
 *
 * Mirrors the manager-source seam used for recitation ([com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelSource]).
 */
interface WbwResourceSource {

    /** The available-datasets manifest, refreshed from the network when [forceRefresh] is true. */
    suspend fun getAvailable(forceRefresh: Boolean = false): AvailableWbwInfoModel?

    /** Locally installed version of dataset [id], or 0 when it is not installed. */
    fun getResourceVersion(id: String): Int

    /** Starts (or resumes) downloading [info]'s dataset. */
    fun startDownload(info: WbwLanguageInfo)

    /** Cancels the download of dataset [id]. */
    fun stopDownload(id: String)

    /**
     * Download progress as `id to status`. Defaults to an empty flow, so platforms without a
     * download pipeline need no implementation.
     */
    fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> = emptyFlow()
}

/**
 * Startup seam handing shared code the platform's [WbwResourceSource].
 *
 * Registration points: Android `QuranApp.onCreate()`; iOS once a WBW download pipeline exists.
 */
object WbwResourceProvider {

    private var provider: (() -> WbwResourceSource)? = null

    /** Registers how the [WbwResourceSource] is obtained on this platform. Call once at startup. */
    fun setSource(provider: () -> WbwResourceSource) {
        this.provider = provider
    }

    val source: WbwResourceSource
        get() = provider?.invoke()
            ?: NoWbwResourceSource
}

/**
 * Inert source for platforms without a word-by-word resource implementation (currently iOS): no
 * languages available, downloads are no-ops. Same rule as the other download seams.
 */
private object NoWbwResourceSource : WbwResourceSource {
    override suspend fun getAvailable(forceRefresh: Boolean): AvailableWbwInfoModel? = null
    override fun getResourceVersion(id: String): Int = 0
    override fun startDownload(info: WbwLanguageInfo) = Unit
    override fun stopDownload(id: String) = Unit
}
