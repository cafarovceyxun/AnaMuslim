package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationModelBase
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationQuranModel
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationTranslationModel
import com.cafarovceyxun.anamuslim.compose.utils.DataLoadError
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.downloadRecitations
import com.cafarovceyxun.anamuslim.resources.recitationDownloadWaitForOtherReciter
import com.cafarovceyxun.anamuslim.resources.strTitleChapInfoChapterNo
import com.cafarovceyxun.anamuslim.resources.strTitleFailed
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationActiveDownloads
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationDownloadProgressBus
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationDownloadProvider
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelProvider
import com.cafarovceyxun.anamuslim.utils.network.canProceedOnline
import com.cafarovceyxun.anamuslim.utils.network.isNetworkConnected
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.compose.resources.getString

/** Per-reciter disk scans are expensive; keep only a few in flight at once. */
private const val RECITER_STATS_PARALLELISM = 4

data class RecitationBatchDownloadState(
    val downloadedCount: Int,
    val inProgressCount: Int,
    val totalChapters: Int = QuranMeta.chapterRange.last,
) {
    val isComplete: Boolean get() = downloadedCount >= totalChapters
    val hasActiveWork: Boolean get() = inProgressCount > 0
}

data class RecitationSelectedReciter(
    val kind: RecitationAudioKind,
    val id: String,
    val name: String,
)

data class RecitationChapterSheetData(
    val reciter: RecitationSelectedReciter,
    val downloadedChapters: Set<Int> = emptySet(),
    val activeChapters: Set<Int> = emptySet(),
    val bulkDownloadActive: Boolean = false,
)

data class RecitationDownloadUiState(
    val isLoading: Boolean = true,
    val quranReciters: List<RecitationQuranModel> = emptyList(),
    val translationReciters: List<RecitationTranslationModel> = emptyList(),
    val error: DataLoadError? = null,
    val downloadStates: Map<String, RecitationBatchDownloadState> = emptyMap(),
    val chapterSheet: RecitationChapterSheetData? = null,
)

sealed interface RecitationDownloadEvent {
    object Refresh : RecitationDownloadEvent
    data class StartDownload(val kind: RecitationAudioKind, val reciterId: String) :
        RecitationDownloadEvent

    data class CancelDownload(val kind: RecitationAudioKind, val reciterId: String) :
        RecitationDownloadEvent

    data class OpenChapterSheet(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val name: String,
    ) : RecitationDownloadEvent

    object CloseChapterSheet : RecitationDownloadEvent

    data class DownloadChapter(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val chapterNo: Int,
    ) : RecitationDownloadEvent

    data class CancelChapter(val reciterId: String, val chapterNo: Int) : RecitationDownloadEvent

    data class DeleteChapter(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val chapterNo: Int,
    ) : RecitationDownloadEvent
}

sealed interface RecitationDownloadUiEvent {
    data class ShowMessage(val title: String, val message: String?) : RecitationDownloadUiEvent
}

/**
 * Reciter catalog plus download state for the recitation download screen.
 *
 * WorkManager used to live here; it now sits behind `RecitationDownloadSource`, leaving this class
 * the portable half: which reciter may start a download, what the user is told, and the
 * downloaded-chapter cache that keeps disk scans off the hot path.
 */
class RecitationDownloadViewModel : ViewModel() {

    private val modelSource get() = RecitationModelProvider.source
    private val downloads get() = RecitationDownloadProvider.source

    private val recomputeMutex = Mutex()

    private val _uiState = MutableStateFlow(RecitationDownloadUiState())
    val uiState: StateFlow<RecitationDownloadUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RecitationDownloadUiEvent>()
    val events = _events.asSharedFlow()

    private val downloadedCache = mutableMapOf<String, Set<Int>>()
    private val cacheMutex = Mutex()

    init {
        viewModelScope.launch {
            loadReciters(forceRefresh = false)
        }

        viewModelScope.launch {
            downloads.activeDownloads.collect { active ->
                recomputeStates(active)
            }
        }
    }

    fun onEvent(event: RecitationDownloadEvent) {
        when (event) {
            is RecitationDownloadEvent.Refresh -> viewModelScope.launch {
                cacheMutex.withLock { downloadedCache.clear() }
                loadReciters(forceRefresh = true)
            }

            is RecitationDownloadEvent.StartDownload ->
                startBatchDownload(event.kind, event.reciterId)

            is RecitationDownloadEvent.CancelDownload ->
                cancelBatchDownload(event.kind, event.reciterId)

            is RecitationDownloadEvent.OpenChapterSheet -> {
                _uiState.update {
                    it.copy(
                        chapterSheet = RecitationChapterSheetData(
                            reciter = RecitationSelectedReciter(
                                kind = event.kind,
                                id = event.reciterId,
                                name = event.name,
                            ),
                        ),
                    )
                }
                viewModelScope.launch {
                    triggerRecompute()
                }
            }

            is RecitationDownloadEvent.CloseChapterSheet -> {
                _uiState.update { it.copy(chapterSheet = null) }
            }

            is RecitationDownloadEvent.DownloadChapter ->
                downloadChapter(event.kind, event.reciterId, event.chapterNo)

            is RecitationDownloadEvent.CancelChapter ->
                cancelChapterDownload(event.reciterId, event.chapterNo)

            is RecitationDownloadEvent.DeleteChapter ->
                deleteChapter(event.reciterId, event.chapterNo)
        }
    }

    private suspend fun loadReciters(forceRefresh: Boolean) {
        if (forceRefresh && !isNetworkConnected()) {
            _uiState.update { it.copy(isLoading = false, error = DataLoadError.NoConnection) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        try {
            if (forceRefresh) {
                modelSource.forceRefreshQuran = true
                modelSource.forceRefreshTranslation = true
            }

            val quran = modelSource.getAllQuranModel()
            val translation = modelSource.getAllTranslationModel()

            val qList = quran?.reciters.orEmpty()
            val tList = translation?.reciters.orEmpty()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    quranReciters = qList,
                    translationReciters = tList,
                    error = if (qList.isEmpty() && tList.isEmpty()) DataLoadError.Failed else null,
                )
            }

            triggerRecompute()
        } catch (e: Exception) {
            AppLogger.saveError(e, "RecitationDownloadViewModel.loadReciters")
            _uiState.update {
                it.copy(isLoading = false, error = DataLoadError.Failed)
            }
        }
    }

    private suspend fun recomputeStates(active: RecitationActiveDownloads) {
        recomputeMutex.withLock {
            val state = _uiState.value

            if (state.quranReciters.isEmpty() && state.translationReciters.isEmpty()) {
                return@withLock
            }

            pruneProgressBusStaleEntries(active)

            val permits = Semaphore(RECITER_STATS_PARALLELISM)

            val newMap = coroutineScope {
                val quranPairs = state.quranReciters.map { model ->
                    async {
                        val key = stateKey(RecitationAudioKind.QURAN, model.id)
                        key to permits.withPermit { computeStateForReciter(model.id, key, active) }
                    }
                }

                val transPairs = state.translationReciters.map { model ->
                    async {
                        val key = stateKey(RecitationAudioKind.TRANSLATION, model.id)
                        key to permits.withPermit { computeStateForReciter(model.id, key, active) }
                    }
                }

                (quranPairs + transPairs).associate { it.await() }
            }

            // Resolved before the update: the chapter list comes from a suspend disk read, which
            // cannot happen inside `update`'s lambda.
            val sheetReciterId = _uiState.value.chapterSheet?.reciter?.id
            val sheetDownloaded = sheetReciterId?.let { getDownloadedChapters(it) }

            _uiState.update { prev ->
                val sheet = prev.chapterSheet

                // The sheet may have been closed or switched while the scans ran; only refresh it
                // when it still shows the reciter that was scanned.
                val refreshedSheet =
                    if (sheet != null && sheetDownloaded != null &&
                        sheet.reciter.id == sheetReciterId
                    ) {
                        sheet.copy(
                            downloadedChapters = sheetDownloaded,
                            activeChapters = activeChaptersForReciter(
                                sheet.reciter.id,
                                active,
                                sheetDownloaded,
                            ),
                            bulkDownloadActive = active.isBulkActive(sheet.reciter.id),
                        )
                    } else {
                        sheet
                    }

                prev.copy(
                    downloadStates = newMap,
                    chapterSheet = refreshedSheet,
                )
            }
        }
    }

    private suspend fun pruneProgressBusStaleEntries(active: RecitationActiveDownloads) {
        val activeChapterKeys = HashSet<String>()

        active.activeChaptersByReciter.forEach { (rid, chapters) ->
            chapters.forEach { ch ->
                activeChapterKeys.add(RecitationDownloadProgressBus.key(rid, ch))
            }
        }

        // Hoisted: `prune` takes a plain lambda, but reading downloaded chapters is suspend.
        val downloadedForBulkReciters = active.activeBulkReciters.associateWith {
            getDownloadedChapters(it)
        }

        RecitationDownloadProgressBus.prune { key ->
            if (key in activeChapterKeys) return@prune false
            val (rid, ch) = RecitationDownloadProgressBus.parseBusKey(key) ?: return@prune true

            val bulkDownloaded = downloadedForBulkReciters[rid]
            if (bulkDownloaded != null && ch !in bulkDownloaded) {
                return@prune false
            }
            true
        }
    }

    private fun activeChaptersForReciter(
        reciterId: String,
        active: RecitationActiveDownloads,
        downloaded: Set<Int>,
    ): Set<Int> {
        if (active.isBulkActive(reciterId)) {
            return QuranMeta.chapterRange.filter { it !in downloaded }.toSet()
        }
        return active.activeChapters(reciterId)
    }

    private suspend fun computeStateForReciter(
        reciterId: String,
        stateKey: String,
        active: RecitationActiveDownloads,
    ): RecitationBatchDownloadState {
        val inProgress = active.inProgressCount(reciterId)

        val prevState = _uiState.value.downloadStates[stateKey]
        val hadActiveWork = prevState?.hasActiveWork == true

        val forceRecheckDisk = hadActiveWork || inProgress > 0 || !isCached(reciterId)

        val downloaded = getDownloadedChapters(reciterId, forceRecheckDisk).size

        return RecitationBatchDownloadState(
            downloadedCount = downloaded,
            inProgressCount = inProgress,
        )
    }

    private suspend fun isCached(reciterId: String): Boolean =
        cacheMutex.withLock { reciterId in downloadedCache }

    private suspend fun getDownloadedChapters(
        reciterId: String,
        forceRecheck: Boolean = false,
    ): Set<Int> {
        if (!forceRecheck) {
            cacheMutex.withLock { downloadedCache[reciterId] }?.let { return it }
        }

        val set = downloads.downloadedChapters(reciterId)
        cacheMutex.withLock { downloadedCache[reciterId] = set }
        return set
    }

    private fun downloadChapter(kind: RecitationAudioKind, reciterId: String, chapterNo: Int) {
        val model = findModel(kind, reciterId) ?: return

        viewModelScope.launch {
            try {
                if (!canProceedOnline()) return@launch

                val active = downloads.currentActiveDownloads()

                if (active.hasOtherReciterActive(reciterId)) {
                    _events.emit(
                        RecitationDownloadUiEvent.ShowMessage(
                            title = getString(Res.string.downloadRecitations),
                            message = getString(Res.string.recitationDownloadWaitForOtherReciter),
                        ),
                    )
                    return@launch
                }

                if (active.isBulkActive(reciterId)) {
                    return@launch
                }

                downloads.startChapter(
                    reciterId = reciterId,
                    kind = kind,
                    urlTemplate = model.urlTemplate,
                    chapterNo = chapterNo,
                    title = model.getReciterName(),
                    subtitle = getString(Res.string.strTitleChapInfoChapterNo) + " $chapterNo",
                )
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationDownloadViewModel.downloadChapter")

                _events.emit(
                    RecitationDownloadUiEvent.ShowMessage(
                        title = getString(Res.string.strTitleFailed),
                        message = e.message,
                    ),
                )
            }
        }
    }

    private fun cancelChapterDownload(reciterId: String, chapterNo: Int) {
        viewModelScope.launch {
            try {
                downloads.cancelChapter(reciterId, chapterNo)
                triggerRecompute()
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationDownloadViewModel.cancelChapterDownload")
            }
        }
    }

    private fun deleteChapter(reciterId: String, chapterNo: Int) {
        viewModelScope.launch {
            try {
                downloads.deleteChapter(reciterId, chapterNo)
                cacheMutex.withLock { downloadedCache.remove(reciterId) }

                triggerRecompute()
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationDownloadViewModel.deleteChapter")
            }
        }
    }

    private fun startBatchDownload(kind: RecitationAudioKind, reciterId: String) {
        val model = findModel(kind, reciterId) ?: return

        viewModelScope.launch {
            try {
                if (!canProceedOnline()) return@launch

                val active = downloads.currentActiveDownloads()
                val stateKey = stateKey(kind, reciterId)
                val myState = computeStateForReciter(reciterId, stateKey, active)
                if (myState.hasActiveWork) return@launch

                if (active.hasOtherReciterActive(reciterId)) {
                    _events.emit(
                        RecitationDownloadUiEvent.ShowMessage(
                            title = getString(Res.string.downloadRecitations),
                            message = getString(Res.string.recitationDownloadWaitForOtherReciter),
                        ),
                    )
                    return@launch
                }

                downloads.startBulk(
                    reciterId = reciterId,
                    kind = kind,
                    urlTemplate = model.urlTemplate,
                    displayTitle = model.getReciterName(),
                )
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationDownloadViewModel.startBatchDownload")

                _events.emit(
                    RecitationDownloadUiEvent.ShowMessage(
                        title = getString(Res.string.strTitleFailed),
                        message = e.message,
                    ),
                )
            }
        }
    }

    private fun cancelBatchDownload(kind: RecitationAudioKind, reciterId: String) {
        viewModelScope.launch {
            try {
                downloads.cancelBulk(reciterId, kind)
                triggerRecompute()
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationDownloadViewModel.cancelBatchDownload")
            }
        }
    }

    private fun findModel(kind: RecitationAudioKind, reciterId: String): RecitationModelBase? {
        return when (kind) {
            RecitationAudioKind.QURAN ->
                _uiState.value.quranReciters.find { it.id == reciterId }

            RecitationAudioKind.TRANSLATION ->
                _uiState.value.translationReciters.find { it.id == reciterId }
        }
    }

    private suspend fun triggerRecompute() {
        recomputeStates(downloads.currentActiveDownloads())
    }

    companion object {
        fun stateKey(kind: RecitationAudioKind, reciterId: String): String =
            "${kind.name}:$reciterId"
    }
}
