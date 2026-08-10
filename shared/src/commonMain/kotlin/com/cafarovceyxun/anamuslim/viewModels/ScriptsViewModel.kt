package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.managers.ScriptResourceProvider
import com.cafarovceyxun.anamuslim.utils.network.canProceedOnline
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptVariant
import com.cafarovceyxun.anamuslim.utils.reader.isPrebuiltAtlas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScriptsUiState(
    val scripts: Map<String, List<QuranScriptVariant>> = QuranScriptUtils.availableScripts(),
    val downloadStates: Map<String, ResourceDownloadStatus> = emptyMap(),
)


sealed interface ScriptEvent {
    data class DownloadScript(val key: String) : ScriptEvent
    data class DownloadAtlas(val key: String, val densityLevel: Int) : ScriptEvent
    data class CancelDownload(val key: String) : ScriptEvent
}


class ScriptsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ScriptsUiState())
    val uiState: StateFlow<ScriptsUiState> = _uiState.asStateFlow()

    private val scriptSource get() = ScriptResourceProvider.source

    init {
        observeDownloadStates()
    }

    private fun observeDownloadStates() {
        viewModelScope.launch {
            scriptSource.observeDownloads().collect { (key, status) ->
                updateDownloadStatus(key, status)
            }
        }
    }

    suspend fun isAtlasInstalled(script: String): Boolean = withContext(Dispatchers.IO) {
        if (script.isPrebuiltAtlas()) return@withContext true

        RepositoryProvider.externalQuranDatabase
            .atlasWordShapeDao()
            .getBundleByKey(script) != null
    }

    fun onEvent(event: ScriptEvent) {
        when (event) {
            is ScriptEvent.DownloadScript -> downloadScript(event.key)
            is ScriptEvent.DownloadAtlas -> downloadAtlas(event.key, event.densityLevel)
            is ScriptEvent.CancelDownload -> cancelDownload(event.key)
        }
    }

    // The connectivity gate is suspend in commonMain (its "no internet" toast reads a string
    // resource), so the check moved into viewModelScope; the outcome is unchanged.
    private fun downloadScript(key: String) {
        viewModelScope.launch {
            if (!canProceedOnline()) return@launch

            scriptSource.startFontDownload(key)
            updateDownloadStatus(key, ResourceDownloadStatus.Started)
        }
    }

    private fun downloadAtlas(key: String, densityLevel: Int) {
        viewModelScope.launch {
            if (!canProceedOnline()) return@launch

            scriptSource.startAtlasDownload(key, densityLevel)
            updateDownloadStatus(key, ResourceDownloadStatus.Started)
        }
    }

    private fun cancelDownload(key: String) {
        scriptSource.cancelDownload(key)
        updateDownloadStatus(key, ResourceDownloadStatus.Cancelled)
    }

    private fun updateDownloadStatus(key: String, status: ResourceDownloadStatus) {
        _uiState.update { state ->
            val newDownloadStates = state.downloadStates.toMutableMap()

            when (status) {
                is ResourceDownloadStatus.Completed -> {
                    newDownloadStates.remove(key)
                }

                is ResourceDownloadStatus.Cancelled -> {
                    newDownloadStates.remove(key)
                }

                else -> {
                    newDownloadStates[key] = status
                }
            }

            state.copy(
                downloadStates = newDownloadStates,
            )
        }
    }
}
