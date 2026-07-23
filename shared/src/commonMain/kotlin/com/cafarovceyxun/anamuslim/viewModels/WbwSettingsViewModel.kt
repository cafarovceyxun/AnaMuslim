package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwResourceProvider
import com.cafarovceyxun.anamuslim.api.models.wbw.WbwLanguageInfo
import com.cafarovceyxun.anamuslim.compose.utils.DataLoadError
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WbwUiModel(
    val info: WbwLanguageInfo,
    val isDownloaded: Boolean,
    val isUpdateAvailable: Boolean,
)

/**
 * The only word-by-word languages the app offers, in display order.
 *
 * The manifest carries dozens; everything beyond these three is noise for this audience, so the
 * list is cut here — at the source — rather than in each screen, and onboarding and settings cannot
 * drift apart. Adding a language is this one line.
 */
private val OFFERED_WBW_LANG_CODES = listOf("tr", "en", "ru")

/** At most one pack per [OFFERED_WBW_LANG_CODES] entry, in that order. */
private fun List<WbwLanguageInfo>.onlyOfferedLanguages(): List<WbwLanguageInfo> =
    OFFERED_WBW_LANG_CODES.mapNotNull { code ->
        firstOrNull { it.langCode.equals(code, ignoreCase = true) }
    }

data class WbwSettingsUiState(
    val isLoading: Boolean = true,
    val error: DataLoadError? = null,
    val selectedWbwId: String? = null,
    val rows: List<WbwUiModel> = emptyList(),
    val downloadStates: Map<String, ResourceDownloadStatus> = emptyMap(),
)

class WbwSettingsViewModel : ViewModel() {
    private val db get() = RepositoryProvider.externalQuranDatabase
    private val wbwSource get() = WbwResourceProvider.source

    private val _uiState = MutableStateFlow(WbwSettingsUiState())
    val uiState: StateFlow<WbwSettingsUiState> = _uiState.asStateFlow()

    init {
        observeSelection()
        observeDownloads()
        load(force = false)
    }

    private fun observeSelection() {
        viewModelScope.launch {
            ReaderPreferences.wbwIdFlow().collect { selected ->
                _uiState.update { it.copy(selectedWbwId = selected) }
            }
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            wbwSource.observeDownloads().collect { (id, status) ->
                _uiState.update { state ->
                    val next = state.downloadStates.toMutableMap()
                    when (status) {
                        is ResourceDownloadStatus.Completed,
                        is ResourceDownloadStatus.Cancelled -> next.remove(id)

                        else -> next[id] = status
                    }
                    state.copy(downloadStates = next)
                }

                if (status is ResourceDownloadStatus.Completed) {
                    refreshRows()
                }
            }
        }
    }

    fun load(force: Boolean) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val manifest = wbwSource.getAvailable(forceRefresh = force)

            if (manifest == null) {
                _uiState.update { it.copy(isLoading = false, error = DataLoadError.Failed) }
                return@launch
            }

            val rows = buildRows(manifest.wbw)
            val selected = resolveSelectedId(rows)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (rows.isEmpty()) DataLoadError.NoData else null,
                    rows = rows,
                    selectedWbwId = selected,
                )
            }
        }
    }

    private suspend fun refreshRows() {
        val rows = _uiState.value.rows
        if (rows.isEmpty()) return
        val refreshed = buildRows(rows.map { it.info })
        val selected = resolveSelectedId(refreshed)

        _uiState.update {
            it.copy(
                rows = refreshed,
                selectedWbwId = selected,
                error = if (refreshed.isEmpty()) DataLoadError.NoData else null,
            )
        }
    }

    private suspend fun buildRows(
        languages: List<WbwLanguageInfo>
    ): List<WbwUiModel> = withContext(Dispatchers.IO) {
        if (languages.isEmpty()) return@withContext emptyList()

        val offered = languages.onlyOfferedLanguages()
        if (offered.isEmpty()) return@withContext emptyList()

        val wbwIds = offered.map { it.id }.distinct()
        val downloadedIds = db
            .wbwDao()
            .getDownloadedWbwIds(wbwIds)
            .toSet()

        return@withContext offered
            .map { info ->
                val isDownloaded = downloadedIds.contains(info.id)
                val localVersion = wbwSource.getResourceVersion(info.id)
                val isUpdateAvailable = isDownloaded && info.version > localVersion
                WbwUiModel(
                    info = info,
                    isDownloaded = isDownloaded,
                    isUpdateAvailable = isUpdateAvailable
                )
            }
    }

    private suspend fun resolveSelectedId(rows: List<WbwUiModel>): String? {
        val downloadedIds = rows.filter { it.isDownloaded }.map { it.info.id }.toSet()
        val preferred = ReaderPreferences.getWbwId()

        return validateWbwId(preferred, downloadedIds)
    }

    private fun validateWbwId(
        id: String?,
        availableIds: Set<String>,
        fallback: String? = null,
    ): String? {
        val normalizedId = id?.takeIf { it.isNotBlank() }

        if (normalizedId != null && availableIds.contains(normalizedId)) {
            return normalizedId
        }

        return null
    }

    fun selectLanguage(id: String) {
        val selectedRow = _uiState.value.rows.firstOrNull { it.info.id == id } ?: return
        if (!selectedRow.isDownloaded) return

        viewModelScope.launch {
            ReaderPreferences.setWbwId(id)
        }
    }

    fun startDownload(id: String) {
        val info = _uiState.value.rows.firstOrNull { it.info.id == id }?.info ?: return
        wbwSource.startDownload(info)
        _uiState.update {
            it.copy(downloadStates = it.downloadStates + (id to ResourceDownloadStatus.Started))
        }
    }

    fun cancelDownload(id: String) {
        wbwSource.stopDownload(id)
        _uiState.update {
            it.copy(downloadStates = it.downloadStates - id)
        }
    }

    fun deleteWbwData(id: String) {
        viewModelScope.launch {
            db.wbwDao().deleteByWbwId(id)
            refreshRows()
        }
    }
}
