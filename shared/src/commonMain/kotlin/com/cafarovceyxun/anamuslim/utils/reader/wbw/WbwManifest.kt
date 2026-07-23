package com.cafarovceyxun.anamuslim.utils.reader.wbw

import com.cafarovceyxun.anamuslim.api.GithubApi
import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.models.wbw.AvailableWbwInfoModel
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okio.Path

/**
 * The catalogue of available word-by-word packs: in-memory cache over an on-disk copy, refreshed
 * from the inventory. Ported from Android's `WbwManager` — the only Android parts were the
 * `Context`-derived directory (now [AppFileSystem]) and the version store, which stays platform-side
 * because Android keeps it in `SharedPreferences`.
 */
object WbwManifest {
    private const val DIR_NAME = "wbw"
    private const val MANIFEST_FILENAME = "available_wbw_info_v2.json"

    private val rootDirPath: String =
        AppFileSystem.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, DIR_NAME)

    private var cachedManifest: AvailableWbwInfoModel? = null
    private val lock = Mutex()

    private fun rootDir(): Path = AppFileSystem.makeAndGetAppResourceDir(rootDirPath)

    private fun manifestFile(): Path = rootDir() / MANIFEST_FILENAME

    /** Temp destination for a pack download; the installer deletes it when it is done. */
    fun tempDownloadPath(id: String): Path = rootDir() / "$id.tmp"

    suspend fun getAvailable(forceRefresh: Boolean = false): AvailableWbwInfoModel? {
        cachedManifest?.let { if (!forceRefresh) return it }

        return lock.withLock {
            cachedManifest?.let { if (!forceRefresh) return@withLock it }

            if (!forceRefresh) {
                loadLocal()?.let {
                    cachedManifest = it
                    return@withLock it
                }
            }

            loadNetwork()?.also { cachedManifest = it }
        }
    }

    private suspend fun loadLocal(): AvailableWbwInfoModel? = withContext(Dispatchers.IO) {
        val file = manifestFile()
        if ((AppFileSystem.size(file) ?: 0L) <= 0L) return@withContext null

        try {
            JsonHelper.json.decodeFromString<AvailableWbwInfoModel>(AppFileSystem.readText(file))
        } catch (e: Exception) {
            AppLogger.saveError(e, "WbwManifest.loadLocal")
            null
        }
    }

    private suspend fun loadNetwork(): AvailableWbwInfoModel? = withContext(Dispatchers.IO) {
        val manifest = try {
            GithubApi.getAvailableWbwInfo()
        } catch (e: Exception) {
            AppLogger.saveError(e, "WbwManifest.loadNetwork")
            return@withContext null
        }

        AppFileSystem.writeText(manifestFile(), JsonHelper.json.encodeToString(manifest))
        manifest
    }
}
