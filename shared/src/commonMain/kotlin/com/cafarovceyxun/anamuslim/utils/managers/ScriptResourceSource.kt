package com.cafarovceyxun.anamuslim.utils.managers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Platform-neutral view of the Quran-script downloads (fonts and glyph atlases) for shared
 * ViewModels. The Android implementations (`ScriptFontsDownloadManager` + `AtlasDownloadManager`,
 * backed by Context/WorkManager) stay in `:app`; commonMain depends only on this interface,
 * registered via [ScriptResourceProvider] at startup.
 *
 * Mirrors [com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwResourceSource].
 */
interface ScriptResourceSource {

    /** Starts downloading the font files for [script]. */
    fun startFontDownload(script: String)

    /** Starts downloading the glyph atlas for [scriptKey] at [densityLevel]. */
    fun startAtlasDownload(scriptKey: String, densityLevel: Int)

    /** Cancels any font or atlas download in flight for [key]. */
    fun cancelDownload(key: String)

    /**
     * Font and atlas download progress as `key to status`, merged into one stream. Defaults to an
     * empty flow, so platforms without a download pipeline need no implementation.
     */
    fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> = emptyFlow()
}

/**
 * Startup seam handing shared code the platform's [ScriptResourceSource].
 *
 * Registration points: Android `QuranApp.onCreate()`; iOS once a script download pipeline exists.
 */
object ScriptResourceProvider {

    private var provider: (() -> ScriptResourceSource)? = null

    /** Registers how the [ScriptResourceSource] is obtained on this platform. Call once at startup. */
    fun setSource(provider: () -> ScriptResourceSource) {
        this.provider = provider
    }

    val source: ScriptResourceSource
        get() = provider?.invoke()
            ?: NoScriptResourceSource
}

/**
 * Inert source for platforms without a script/atlas download implementation (currently iOS): the
 * settings UI lists scripts with nothing downloading, instead of crashing. Same rule as the other
 * download seams.
 */
private object NoScriptResourceSource : ScriptResourceSource {
    override fun startFontDownload(script: String) = Unit
    override fun startAtlasDownload(scriptKey: String, densityLevel: Int) = Unit
    override fun cancelDownload(key: String) = Unit
}
