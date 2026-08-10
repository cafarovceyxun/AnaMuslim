package com.cafarovceyxun.anamuslim.utils.managers

import com.cafarovceyxun.anamuslim.api.resolveInventoryUrl
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.univ.BackgroundFileTransfer
import com.cafarovceyxun.anamuslim.utils.reader.ScriptFontInstaller
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasFiles
import com.cafarovceyxun.anamuslim.utils.reader.atlas.SharedAtlasImporter
import com.cafarovceyxun.anamuslim.utils.reader.toAtlasBundleDownloadKey
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Multiplatform [ScriptResourceSource]: KFQPC page fonts via [ScriptFontInstaller] and atlas
 * bundles via [SharedAtlasImporter]. Android keeps its WorkManager implementation
 * (`AndroidScriptResourceSource`); iOS registers this one.
 *
 * The atlas half was a no-op until iOS grew a shared renderer ([SharedAtlasBundleLoader]); now the
 * download mirrors Android's `AtlasDownloadWorker` — stream the inventory zip to disk, then hand it
 * to the shared importer. Both downloads report progress through the same [events] flow, keyed by
 * script; a script's font and atlas jobs are tracked separately so one does not cancel the other,
 * exactly as Android's two managers behave.
 */
class SharedScriptResourceSource(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ScriptResourceSource {

    private val fontJobs = mutableMapOf<String, Job>()
    private val atlasJobs = mutableMapOf<String, Job>()

    private val events = MutableSharedFlow<Pair<String, ResourceDownloadStatus>>(
        extraBufferCapacity = 64,
    )

    override fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> = events.asSharedFlow()

    override fun startFontDownload(script: String) {
        if (fontJobs[script]?.isActive == true) return

        fontJobs[script] = scope.launch {
            events.emit(script to ResourceDownloadStatus.Started)
            try {
                ScriptFontInstaller.install(script) { progress ->
                    events.emit(script to ResourceDownloadStatus.InProgress(progress))
                }
                events.emit(script to ResourceDownloadStatus.Completed)
            } catch (e: CancellationException) {
                events.emit(script to ResourceDownloadStatus.Cancelled)
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "SharedScriptResourceSource:$script")
                events.emit(script to ResourceDownloadStatus.Failed(e.message))
            } finally {
                fontJobs.remove(script)
            }
        }
    }

    override fun startAtlasDownload(scriptKey: String, densityLevel: Int) {
        if (atlasJobs[scriptKey]?.isActive == true) return

        atlasJobs[scriptKey] = scope.launch {
            events.emit(scriptKey to ResourceDownloadStatus.Started)

            val tmpFile = AtlasFiles.tempFile("$scriptKey.tmp")
            try {
                val url = "ghraw://AlfaazPlus/QuranAppInventory/master/atlas/" +
                    "${scriptKey.toAtlasBundleDownloadKey()}/${densityLevel}x.zip"

                var lastPercent = -1
                BackgroundFileTransfer.download(
                    resolveInventoryUrl(url),
                    tmpFile,
                ) { consumed, total ->
                    val percent = if (total > 0L) ((consumed * 100L) / total).toInt() else 0
                    if (percent != lastPercent) {
                        lastPercent = percent
                        events.emit(scriptKey to ResourceDownloadStatus.InProgress(percent))
                    }
                }

                SharedAtlasImporter.importFromZip(
                    tmpFile,
                    scriptKey,
                    RepositoryProvider.externalQuranDatabase,
                )
                events.emit(scriptKey to ResourceDownloadStatus.Completed)
            } catch (e: CancellationException) {
                events.emit(scriptKey to ResourceDownloadStatus.Cancelled)
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "SharedScriptResourceSource.atlas:$scriptKey")
                events.emit(scriptKey to ResourceDownloadStatus.Failed(e.message))
            } finally {
                AppFileSystem.delete(tmpFile)
                atlasJobs.remove(scriptKey)
            }
        }
    }

    /** Cancels whichever download owns [key] — a key can be a font or an atlas job, or both. */
    override fun cancelDownload(key: String) {
        fontJobs[key]?.cancel()
        atlasJobs[key]?.cancel()
    }
}
