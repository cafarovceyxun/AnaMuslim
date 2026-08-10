package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgSomethingWrong
import com.cafarovceyxun.anamuslim.resources.wbwAudioBulkWaitForChapterDownloads
import com.cafarovceyxun.anamuslim.resources.wbwAudioChapterWaitForBulk
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwActiveDownloads
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioDownloadProgressBus
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioDownloadProvider
import com.cafarovceyxun.anamuslim.utils.network.canProceedOnline
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

data class WbwAudioDownloadUiState(
    val downloadedChapters: Set<Int> = emptySet(),
    val activeChapters: Set<Int> = emptySet(),
    val bulkDownloadActive: Boolean = false,
    val hasActiveSingleChapterWork: Boolean = false,
)

sealed interface WbwAudioDownloadUiEvent {
    data class ShowMessage(val message: String) : WbwAudioDownloadUiEvent
}

class WbwAudioDownloadViewModel : ViewModel() {

    private val source get() = WbwAudioDownloadProvider.source

    /** The active audio dataset id, so UI does not need the platform repository. */
    val audioId: String get() = source.audioId

    private val recomputeMutex = Mutex()

    private val _uiState = MutableStateFlow(WbwAudioDownloadUiState())
    val uiState: StateFlow<WbwAudioDownloadUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WbwAudioDownloadUiEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            source.activeDownloads.collect { recomputeStates(it) }
        }

        viewModelScope.launch { triggerRecompute() }
    }

    fun refresh() {
        viewModelScope.launch { triggerRecompute() }
    }

    fun downloadChapter(audioId: String, chapterNo: Int) {
        viewModelScope.launch {
            if (!canProceedOnline()) return@launch

            try {
                if (source.currentActiveDownloads().isBulkActive(audioId)) {
                    _events.emit(
                        WbwAudioDownloadUiEvent.ShowMessage(
                            getString(Res.string.wbwAudioChapterWaitForBulk),
                        ),
                    )
                    return@launch
                }

                source.startChapter(audioId, chapterNo)
            } catch (e: Exception) {
                AppLogger.saveError(e, "WbwAudioDownloadViewModel.downloadChapter")
                _events.emit(
                    WbwAudioDownloadUiEvent.ShowMessage(
                        e.message ?: getString(Res.string.strMsgSomethingWrong),
                    ),
                )
            }
        }
    }

    fun cancelChapter(chapterNo: Int) {
        viewModelScope.launch {
            try {
                source.cancelChapter(chapterNo)
                triggerRecompute()
            } catch (e: Exception) {
                AppLogger.saveError(e, "WbwAudioDownloadViewModel.cancelChapter")
            }
        }
    }

    fun deleteChapter(chapterNo: Int) {
        viewModelScope.launch {
            try {
                source.deleteChapter(chapterNo)
                triggerRecompute()
            } catch (e: Exception) {
                AppLogger.saveError(e, "WbwAudioDownloadViewModel.deleteChapter")
            }
        }
    }

    fun startBulkDownload(audioId: String) {
        viewModelScope.launch {
            if (!canProceedOnline()) return@launch

            try {
                val active = source.currentActiveDownloads()

                if (active.isBulkActive(audioId)) return@launch

                if (active.activeChapters(audioId).isNotEmpty()) {
                    _events.emit(
                        WbwAudioDownloadUiEvent.ShowMessage(
                            getString(Res.string.wbwAudioBulkWaitForChapterDownloads),
                        ),
                    )
                    return@launch
                }

                source.startBulk(audioId)
            } catch (e: Exception) {
                AppLogger.saveError(e, "WbwAudioDownloadViewModel.startBulkDownload")
                _events.emit(
                    WbwAudioDownloadUiEvent.ShowMessage(
                        e.message ?: getString(Res.string.strMsgSomethingWrong),
                    ),
                )
            }
        }
    }

    fun cancelBulkDownload(audioId: String) {
        viewModelScope.launch {
            try {
                source.cancelBulk(audioId)
                triggerRecompute()
            } catch (e: Exception) {
                AppLogger.saveError(e, "WbwAudioDownloadViewModel.cancelBulkDownload")
            }
        }
    }

    private suspend fun recomputeStates(active: WbwActiveDownloads) {
        recomputeMutex.withLock {
            val audioId = source.audioId
            val downloaded = source.downloadedChapters()

            pruneProgressStaleEntries(audioId, active, downloaded)

            val bulkActive = active.isBulkActive(audioId)
            val activeSingles = active.activeChapters(audioId)
            val activeChapters = if (bulkActive) {
                QuranMeta.chapterRange.filter { it !in downloaded }.toSet()
            } else {
                activeSingles
            }

            _uiState.update {
                WbwAudioDownloadUiState(
                    downloadedChapters = downloaded,
                    activeChapters = activeChapters,
                    bulkDownloadActive = bulkActive,
                    hasActiveSingleChapterWork = activeSingles.isNotEmpty(),
                )
            }
        }
    }

    private fun pruneProgressStaleEntries(
        audioId: String,
        active: WbwActiveDownloads,
        downloaded: Set<Int>,
    ) {
        val activeKeys = HashSet<String>()
        active.activeChapters(audioId).forEach { ch ->
            activeKeys.add(WbwAudioDownloadProgressBus.key(audioId, ch))
        }

        WbwAudioDownloadProgressBus.prune { key ->
            if (key in activeKeys) return@prune false

            val (aId, ch) = WbwAudioDownloadProgressBus.parseBusKey(key) ?: return@prune true

            if (aId != audioId) return@prune true

            if (active.isBulkActive(aId) && ch !in downloaded) return@prune false

            true
        }
    }

    private suspend fun triggerRecompute() {
        recomputeStates(source.currentActiveDownloads())
    }
}
