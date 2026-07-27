package com.cafarovceyxun.anamuslim.utils.app

import android.content.Context
import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.GithubApi
import com.cafarovceyxun.anamuslim.api.models.ResourcesVersions
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.Logger
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelManager
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioRepository
import com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwManager
import com.cafarovceyxun.anamuslim.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

enum class ResourceUpdateState {
    IDLE, CHECKING, UPDATING, COMPLETED, FAILED
}

class ResourceUpdateManager private constructor(private val ctx: Context) {
    companion object {
        private var INSTANCE: ResourceUpdateManager? = null

        fun getInstance(context: Context): ResourceUpdateManager {
            if (INSTANCE == null) {
                INSTANCE = ResourceUpdateManager(context.applicationContext)
            }
            return INSTANCE!!
        }
    }

    private val _updateState = MutableStateFlow(ResourceUpdateState.IDLE)
    val updateState: StateFlow<ResourceUpdateState> = _updateState.asStateFlow()

    private val fileUtils = FileUtils.newInstance(ctx)

    private fun getLocalVersions(): ResourcesVersions? {
        val file = fileUtils.resourcesVersionsFile
        if (!file.exists() || file.length() == 0L) return null

        return try {
            JsonHelper.json.decodeFromString<ResourcesVersions>(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkAndPerformUpdates(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (_updateState.value == ResourceUpdateState.CHECKING || _updateState.value == ResourceUpdateState.UPDATING) return@withContext

        _updateState.value = ResourceUpdateState.CHECKING

        try {
            val remoteVersions = GithubApi.getResourcesVersions()
            val localVersions = getLocalVersions()

            if (force || localVersions == null || isAnyUpdateAvailable(
                    localVersions,
                    remoteVersions
                )
            ) {
                _updateState.value = ResourceUpdateState.UPDATING

                Logger.print("Resources update available: ", remoteVersions)
                performUpdates(localVersions, remoteVersions, force)

                saveLocalVersions(remoteVersions)

                _updateState.value = ResourceUpdateState.COMPLETED
            } else {
                _updateState.value = ResourceUpdateState.IDLE
            }
        } catch (e: Exception) {
            Log.saveError(e, "ResourceUpdateManager.checkAndPerformUpdates")
            _updateState.value = ResourceUpdateState.FAILED
        }
    }

    // `urlsVersion` is deliberately not checked: the app no longer consumes the upstream
    // `urls.json` (its support links are this project's own), so a bump there is not an update.
    private fun isAnyUpdateAvailable(local: ResourcesVersions, remote: ResourcesVersions): Boolean {
        return remote.translationsVersion > local.translationsVersion ||
                remote.recitationsVersion > local.recitationsVersion ||
                remote.recitationTranslationsVersion > local.recitationTranslationsVersion ||
                remote.wbwVersion > local.wbwVersion ||
                remote.wbwAudioVersion > local.wbwAudioVersion
    }

    private suspend fun performUpdates(
        local: ResourcesVersions?,
        remote: ResourcesVersions,
        force: Boolean
    ) = withContext(Dispatchers.IO) {
        supervisorScope {
            // Recitations
            launch {
                if (force || local == null || remote.recitationsVersion > local.recitationsVersion ||
                    remote.recitationTranslationsVersion > local.recitationTranslationsVersion
                ) {
                    try {
                        RecitationModelManager.refreshManifests()
                    } catch (e: Exception) {
                        Log.saveError(e, "ResourceUpdateManager.updateRecitations")
                    }
                }
            }

            // WBW
            launch {
                if (force || local == null || remote.wbwVersion > local.wbwVersion) {
                    try {
                        WbwManager.getAvailable(ctx, forceRefresh = true)
                    } catch (e: Exception) {
                        Log.saveError(e, "ResourceUpdateManager.updateWbw")
                    }
                }
            }

            // WBW chapter word-audio timings
            launch {
                if (force || local == null || remote.wbwAudioVersion > local.wbwAudioVersion) {
                    try {
                        WbwAudioRepository.refreshTimingsFromRemote(ctx)
                    } catch (e: Exception) {
                        Log.saveError(e, "ResourceUpdateManager.updateWbwAudio")
                    }
                }
            }
        }
    }

    private fun saveLocalVersions(versions: ResourcesVersions) {
        try {
            val file = fileUtils.resourcesVersionsFile
            if (fileUtils.createFile(file)) {
                file.writeText(JsonHelper.json.encodeToString(versions))
            }
        } catch (e: Exception) {
            Log.saveError(e, "ResourceUpdateManager.saveLocalVersions")
        }
    }
}
