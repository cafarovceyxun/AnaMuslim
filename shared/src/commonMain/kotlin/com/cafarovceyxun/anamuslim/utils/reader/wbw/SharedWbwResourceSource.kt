package com.cafarovceyxun.anamuslim.utils.reader.wbw

import androidx.datastore.preferences.core.intPreferencesKey
import com.cafarovceyxun.anamuslim.api.models.wbw.AvailableWbwInfoModel
import com.cafarovceyxun.anamuslim.api.models.wbw.WbwLanguageInfo
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
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
 * Multiplatform [WbwResourceSource]: the shared catalogue ([WbwManifest]) plus coroutine-driven pack
 * installs ([WbwPackInstaller]). Android keeps its WorkManager implementation
 * (`AndroidWbwResourceSource`); iOS registers this one.
 *
 * Installed versions live in the shared DataStore here, whereas Android records them in its own
 * `SharedPreferences` (`WbwVersionStore`) — the value is only ever compared against the manifest, so
 * the two stores never need to agree across platforms.
 */
class SharedWbwResourceSource(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : WbwResourceSource {

    private val jobs = mutableMapOf<String, Job>()

    private val events = MutableSharedFlow<Pair<String, ResourceDownloadStatus>>(
        extraBufferCapacity = 64,
    )

    override suspend fun getAvailable(forceRefresh: Boolean): AvailableWbwInfoModel? =
        WbwManifest.getAvailable(forceRefresh)

    override fun getResourceVersion(id: String): Int =
        DataStoreManager.read(versionKey(id), 0)

    override fun observeDownloads(): Flow<Pair<String, ResourceDownloadStatus>> = events.asSharedFlow()

    override fun startDownload(info: WbwLanguageInfo) {
        // Android uses REPLACE here (a re-download supersedes the running one); same semantics.
        jobs[info.id]?.cancel()

        jobs[info.id] = scope.launch {
            events.emit(info.id to ResourceDownloadStatus.Started)
            try {
                val version = WbwPackInstaller.install(info) { progress ->
                    events.emit(info.id to ResourceDownloadStatus.InProgress(progress ?: 0))
                }
                DataStoreManager.write(versionKey(info.id), version)
                events.emit(info.id to ResourceDownloadStatus.Completed)
            } catch (e: CancellationException) {
                events.emit(info.id to ResourceDownloadStatus.Cancelled)
                throw e
            } catch (e: Exception) {
                AppLogger.saveError(e, "SharedWbwResourceSource:${info.id}")
                events.emit(info.id to ResourceDownloadStatus.Failed(e.message))
            } finally {
                jobs.remove(info.id)
            }
        }
    }

    override fun stopDownload(id: String) {
        jobs[id]?.cancel()
    }

    private fun versionKey(id: String) = intPreferencesKey("wbw_resource_version_$id")
}
