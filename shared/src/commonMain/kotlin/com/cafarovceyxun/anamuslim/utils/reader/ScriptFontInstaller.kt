package com.cafarovceyxun.anamuslim.utils.reader

import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.BackgroundFileTransfer
import com.cafarovceyxun.anamuslim.utils.univ.ScriptFiles
import com.cafarovceyxun.anamuslim.utils.univ.TarGzExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Downloads and unpacks a KFQPC script's page-font archive.
 *
 * The archives are `.tar.gz`; Android unpacked them with Apache Commons Compress, which is JVM-only,
 * so the shared path uses [TarGzExtractor]. Everything else was already portable — the URL is fixed
 * and the destination is the shared [ScriptFiles] directory that [FontResolver] already reads on
 * both platforms, which is why installing these fonts on iOS immediately improves rendering.
 */
object ScriptFontInstaller {

    private const val RELEASE_BASE_URL =
        "https://github.com/AlfaazPlus/QuranAppInventory/releases/download/qpc/"

    /** Progress is 0..100 while downloading, then [EXTRACTING] while unpacking. */
    const val EXTRACTING = 101

    fun archiveNameFor(scriptKey: String): String? = when (scriptKey) {
        QuranScriptUtils.SCRIPT_KFQPC_V1 -> "qpc_v1_by_page.tar.gz"
        QuranScriptUtils.SCRIPT_KFQPC_V2 -> "qpc_v2_by_page.tar.gz"
        QuranScriptUtils.SCRIPT_KFQPC_V4 -> "qpc_v4_tajweed_by_page.tar.gz"
        else -> null
    }

    suspend fun install(
        scriptKey: String,
        onProgress: suspend (Int) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val fileName = archiveNameFor(scriptKey)
            ?: throw IllegalArgumentException("Unknown script key: $scriptKey")

        val tempFile = ScriptFiles.scriptFontDir() / "$scriptKey.tar.gz.tmp"

        try {
            // No notification label: the archive still has to be extracted after the transfer, so a
            // file that lands while the app is gone is not a finished install.
            BackgroundFileTransfer.download(RELEASE_BASE_URL + fileName, tempFile) { consumed, total ->
                onProgress(if (total > 0L) ((consumed * 100L) / total).toInt() else 0)
            }

            onProgress(EXTRACTING)
            TarGzExtractor.extract(tempFile, ScriptFiles.kfqpcScriptFontDir(scriptKey))
        } finally {
            AppFileSystem.delete(tempFile)
        }
    }
}
