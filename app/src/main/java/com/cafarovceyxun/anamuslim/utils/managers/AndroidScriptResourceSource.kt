package com.cafarovceyxun.anamuslim.utils.managers

import android.content.Context
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasDownloadManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/**
 * Android [ScriptResourceSource]: the Context-bound font and atlas download managers behind the
 * shared seam. Registered in `QuranApp.onCreate()`.
 */
class AndroidScriptResourceSource(context: Context) : ScriptResourceSource {

    private val appContext = context.applicationContext

    init {
        // Was `ScriptsViewModel.init`; initialising the download managers is a platform concern,
        // so it happens where the platform source is built instead.
        ScriptFontsDownloadManager.initialize(appContext)
        AtlasDownloadManager.initialize(appContext)
    }

    override fun startFontDownload(script: String) =
        ScriptFontsDownloadManager.startDownload(appContext, script)

    override fun startAtlasDownload(scriptKey: String, densityLevel: Int) =
        AtlasDownloadManager.startDownload(appContext, scriptKey, densityLevel)

    /** Cancels in both managers, as the view model did before — a key can belong to either. */
    override fun cancelDownload(key: String) {
        ScriptFontsDownloadManager.stopDownload(appContext, key)
        AtlasDownloadManager.stopDownload(appContext, key)
    }

    /** The two managers' streams merged, replacing the view model's two separate collectors. */
    override fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> =
        merge(
            ScriptFontsDownloadManager.observeDownloadsAsFlow(),
            AtlasDownloadManager.observeDownloadsAsFlow(),
        )
}
