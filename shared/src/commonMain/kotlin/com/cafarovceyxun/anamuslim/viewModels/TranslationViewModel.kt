package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import com.cafarovceyxun.anamuslim.components.transls.TranslModel
import com.cafarovceyxun.anamuslim.components.transls.TranslationGroupModel
import com.cafarovceyxun.anamuslim.compose.utils.DataLoadError
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.managers.HadithSyncProvider
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.managers.TranslationDownloadProvider
import com.cafarovceyxun.anamuslim.utils.managers.TranslationPlatformHooks
import com.cafarovceyxun.anamuslim.utils.network.canProceedOnline
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgTranslDownloaded
import com.cafarovceyxun.anamuslim.resources.strMsgTranslFailedToDownload
import com.cafarovceyxun.anamuslim.resources.strMsgTranslSelectionLimit
import com.cafarovceyxun.anamuslim.resources.strMsgTryLater
import com.cafarovceyxun.anamuslim.resources.strTitleFailed
import com.cafarovceyxun.anamuslim.resources.strTitleInfo
import com.cafarovceyxun.anamuslim.resources.strTitleSuccess
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class TranslationUiState(
    val isLoading: Boolean = true,
    val translationGroups: List<TranslationGroupModel> = emptyList(),
    val selectedSlugs: Set<String> = emptySet(),
    val saveTranslationChanges: Boolean = true,
    val searchQuery: String = "",
    val error: DataLoadError? = null,
    val isForceUpdating: Boolean = false,
    val isForceUpdated: Boolean = false,
    val downloadStates: Map<String, ResourceDownloadStatus> = emptyMap(),
    val hadithSyncStatus: ResourceDownloadStatus = ResourceDownloadStatus.Idle,
    val isHadithDownloaded: Boolean = false
)

sealed interface TranslationEvent {
    object Refresh : TranslationEvent
    object RefreshQuiet : TranslationEvent
    data class Initialize(
        val initialSlugs: Set<String>,
        val saveTranslationChanges: Boolean
    ) : TranslationEvent

    data class ToggleGroup(val langCode: String) : TranslationEvent
    data class SelectionChanged(val translation: TranslModel, val isSelected: Boolean) :
        TranslationEvent

    data class Search(val query: String) : TranslationEvent
    data class DeleteTranslation(val slug: String) : TranslationEvent
    data class ForceUpdateTranslation(val slug: String) : TranslationEvent
    data class DownloadTranslation(val slug: String) : TranslationEvent
    data class CancelDownload(val slug: String) : TranslationEvent
    object SyncHadiths : TranslationEvent
    object CancelHadithSync : TranslationEvent
    object DeleteHadiths : TranslationEvent
}

sealed interface TranslationUiEvent {
    data class ShowMessage(val title: String, val message: String?) : TranslationUiEvent
}

class TranslationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TranslationUiEvent>()
    val events: SharedFlow<TranslationUiEvent> = _events.asSharedFlow()

    init {
        TranslationDownloadProvider.source.initialize()
        HadithSyncProvider.source.initialize()

        viewModelScope.launch {
            val initialSlugs = ReaderPreferences.getTranslations()
            _uiState.update { it.copy(selectedSlugs = initialSlugs) }

            loadTranslations()
            observeDownloadStates()
            observeHadithState()
        }
    }

    private fun observeDownloadStates() {
        viewModelScope.launch {
            TranslationDownloadProvider.source.observeDownloads().collect { (key, status) ->
                updateDownloadStatus(key, status)
            }
        }
    }

    private fun observeHadithState() {
        val hadithDao = RepositoryProvider.hadithDatabase.hadithDao()
        viewModelScope.launch {
            hadithDao.getAllBooksFlow().collect { entities ->
                _uiState.update { it.copy(isHadithDownloaded = entities.isNotEmpty()) }
            }
        }
        viewModelScope.launch {
            HadithSyncProvider.source.observeSyncStatus().collect { status ->
                _uiState.update { it.copy(hadithSyncStatus = status) }
            }
        }
    }

    fun onEvent(event: TranslationEvent) {
        when (event) {
            is TranslationEvent.Refresh -> loadTranslations(force = true)
            is TranslationEvent.RefreshQuiet -> loadTranslations(silent = true)
            is TranslationEvent.Initialize -> _uiState.update {
                it.copy(
                    selectedSlugs = event.initialSlugs,
                    saveTranslationChanges = event.saveTranslationChanges
                )
            }

            is TranslationEvent.ToggleGroup -> toggleGroup(event.langCode)
            is TranslationEvent.SelectionChanged -> onSelectionChanged(
                event.translation,
                event.isSelected
            )

            is TranslationEvent.Search -> _uiState.update { it.copy(searchQuery = event.query) }
            is TranslationEvent.DeleteTranslation -> deleteTranslation(event.slug)
            is TranslationEvent.ForceUpdateTranslation -> forceUpdateTranslation(event.slug)
            is TranslationEvent.DownloadTranslation -> downloadTranslation(event.slug)
            is TranslationEvent.CancelDownload -> cancelDownload(event.slug)
            is TranslationEvent.SyncHadiths -> HadithSyncProvider.source.startSync()
            is TranslationEvent.CancelHadithSync -> HadithSyncProvider.source.stopSync()
            is TranslationEvent.DeleteHadiths -> deleteHadiths()
        }
    }

    private fun deleteHadiths() {
        viewModelScope.launch(Dispatchers.IO) {
            val hadithDao = RepositoryProvider.hadithDatabase.hadithDao()
            hadithDao.clearAll()
            RepositoryProvider.userRepository.deleteAllHadithHistories()
        }
    }


    private fun toggleGroup(langCode: String) {
        _uiState.update { state ->
            val updatedGroups = state.translationGroups.map { group ->
                if (group.langCode == langCode) {
                    group.copy(
                        isExpanded = !group.isExpanded
                    )
                } else {
                    group
                }
            }
            state.copy(translationGroups = updatedGroups)
        }
    }

    private fun onSelectionChanged(translation: TranslModel, isSelected: Boolean) {
        val slug = translation.bookInfo.slug

        if (!translation.isDownloaded && isSelected) {
            val currentSlugs = _uiState.value.selectedSlugs.toMutableSet()
            val succeed = TranslUtils.resolveSelectionChange(currentSlugs, slug, true)
            if (succeed) {
                _uiState.update { it.copy(selectedSlugs = currentSlugs.toSet()) }
                downloadTranslation(slug)
            } else {
                emitSelectionLimitMessage()
            }
            return
        }

        val state = _uiState.value
        val newSlugs = state.selectedSlugs.toMutableSet()
        val succeed = TranslUtils.resolveSelectionChange(newSlugs, slug, isSelected)
        if (!succeed) emitSelectionLimitMessage()

        if (succeed && state.saveTranslationChanges) {
            viewModelScope.launch {
                ReaderPreferences.setTranslations(newSlugs)
            }
        }

        if (succeed) {
            val selectedSlugs = newSlugs.toSet()
            _uiState.update { current ->
                current.copy(
                    selectedSlugs = selectedSlugs,
                    translationGroups = current.translationGroups.map { group ->
                        group.copy(
                            translations = ArrayList(
                                group.translations.map { t ->
                                    t.apply { isChecked = selectedSlugs.contains(t.bookInfo.slug) }
                                }
                            )
                        )
                    }
                )
            }
        }
    }

    /** The selection-limit dialog the former `TranslUtilsAndroid.resolveSelectionChange` popped. */
    private fun emitSelectionLimitMessage() {
        viewModelScope.launch {
            _events.emit(
                TranslationUiEvent.ShowMessage(
                    title = getString(Res.string.strTitleInfo),
                    message = getString(
                        Res.string.strMsgTranslSelectionLimit,
                        TranslUtils.TRANSL_MAX_SELECTION_LIMIT,
                    ),
                )
            )
        }
    }

    private fun forceUpdateTranslation(slug: String) {
        if (slug != "az") return

        val wasSelected = _uiState.value.selectedSlugs.contains(slug)
        _uiState.update { it.copy(isForceUpdating = true, isForceUpdated = false) }

        deleteTranslation(slug)

        val azBookInfo = TranslationBookInfoModel("az").apply {
            langCode = "az"
            langName = "Azərbaycan"
            bookName = "Azərbaycan dili"
            authorName = "Mürşüd Yusifoğlu"
            displayName = "Azərbaycan dili"
        }

        TranslationDownloadProvider.source.startDownload(azBookInfo)
        
        viewModelScope.launch {
            TranslationDownloadProvider.source.observeDownloads().collectLatest { (key, status) ->
                if (key == slug && status is ResourceDownloadStatus.Completed) {
                    if (wasSelected) {
                        val currentSlugs = ReaderPreferences.getTranslations().toMutableSet()
                        currentSlugs.add(slug)
                        ReaderPreferences.setTranslations(currentSlugs)
                        _uiState.update { it.copy(selectedSlugs = currentSlugs) }
                    }

                    _uiState.update { it.copy(isForceUpdating = false, isForceUpdated = true) }
                    loadTranslations(true)
                    
                    delay(2000)
                    _uiState.update { it.copy(isForceUpdated = false) }
                } else if (key == slug && status is ResourceDownloadStatus.Failed) {
                    _uiState.update { it.copy(isForceUpdating = false, isForceUpdated = false) }
                }
            }
        }
    }

    private fun deleteTranslation(slug: String) {
        if (TranslUtils.isPrebuilt(slug)) {
            return
        }

        var updatedSelectedSlugs: Set<String> = emptySet()
        QuranTranslationFactory().use {
            it.deleteTranslation(slug)
            TranslationPlatformHooks.removeSlugFromSearchIndex?.invoke(slug)

            _uiState.update { current ->
                val updatedGroups = current.translationGroups.map { group ->
                    val filtered = group.translations.filterNot { it.bookInfo.slug == slug }

                    group.copy(
                        translations = ArrayList(filtered)
                    )
                }.filterNot { it.translations.isEmpty() }

                updatedSelectedSlugs = current.selectedSlugs - slug
                current.copy(
                    translationGroups = updatedGroups,
                    selectedSlugs = updatedSelectedSlugs
                )
            }
        }

        viewModelScope.launch {
            ReaderPreferences.setTranslations(updatedSelectedSlugs)
        }
    }


    fun loadTranslations(
        silent: Boolean = false,
        @Suppress("UNUSED_PARAMETER") force: Boolean = false
    ) {
        _uiState.update { it.copy(isLoading = !silent, error = null) }

        viewModelScope.launch {
            val currentSlugs = _uiState.value.selectedSlugs

            try {
                // Translations come only from Supabase: the single `az` book from the Postgres
                // `translations` table, added inside mergeTranslations. There is no remote
                // manifest to fetch, so the list is built entirely from local state and works
                // offline (the actual `az` download still needs the network, separately).
                val translationGroups = withContext(Dispatchers.IO) {
                    mergeTranslations(
                        availableJson = "",
                        oldGroups = _uiState.value.translationGroups,
                        selectedSlugs = currentSlugs
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        translationGroups = translationGroups,
                        error = null
                    )
                }
            } catch (e: Exception) {
                AppLogger.saveError(e, "TranslationViewModel.loadTranslations")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = DataLoadError.Failed
                    )
                }
            }
        }
    }

    private fun mergeTranslations(
        availableJson: String,
        oldGroups: List<TranslationGroupModel>,
        selectedSlugs: Set<String>
    ): List<TranslationGroupModel> {
        val translFactory = QuranTranslationFactory()
        val oldExpandedState = oldGroups.associate { it.langCode to it.isExpanded }
        val mergedGroups = mutableListOf<TranslationGroupModel>()

        try {
            val availableMap = mutableMapOf<String, MutableList<TranslModel>>()

            // 1. Parse available translations from JSON (only 'az')
            if (availableJson.isNotBlank()) {
                try {
                    val root = JsonHelper.json.parseToJsonElement(availableJson).jsonObject
                    val translations = root["translations"]?.jsonObject

                    translations?.forEach { (langCode, langValue) ->
                        if (langCode != "az") return@forEach

                        val langTransls = langValue.jsonObject
                        langTransls.forEach { (slug, translValue) ->
                            if (slug != "az") return@forEach
                            val translObject = translValue.jsonObject
                            val model = readTranslInfo(langCode, slug, translObject)
                            model.isDownloaded = translFactory.isTranslationDownloaded(slug)
                            model.isChecked = selectedSlugs.contains(slug)

                            availableMap.getOrPut(langCode) { mutableListOf() }.add(model)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Add locally available translations (only for 'az')
            val localBooks = translFactory.getAvailableTranslationBooksInfo()
            localBooks.forEach { (slug, bookInfo) ->
                if (slug != "az") return@forEach
                val langCode = bookInfo.langCode

                val langList = availableMap.getOrPut(langCode) { mutableListOf() }
                if (langList.none { it.bookInfo.slug == slug }) {
                    val model = TranslModel(bookInfo)
                    model.isDownloaded = true
                    model.isChecked = selectedSlugs.contains(slug)
                    langList.add(model)
                } else {
                    // Update local info if already in manifest list
                    val model = langList.find { it.bookInfo.slug == slug }
                    model?.isDownloaded = true
                    model?.isChecked = selectedSlugs.contains(slug)
                }
            }

            // 3. Special case for Azerbaijan (if not already there)
            val azList = availableMap.getOrPut("az") { mutableListOf() }
            if (azList.none { it.bookInfo.slug == "az" }) {
                val azBookInfo = TranslationBookInfoModel("az").apply {
                    langCode = "az"
                    langName = "Azərbaycan"
                    bookName = "Azərbaycan dili"
                    authorName = "Mürşüd Yusifoğlu"
                    displayName = "Azərbaycan dili"
                }
                val model = TranslModel(azBookInfo).apply {
                    isDownloaded = translFactory.isTranslationDownloaded("az")
                    isChecked = selectedSlugs.contains("az")
                }
                azList.add(0, model)
            }

            // 4. Create final groups
            availableMap.forEach { (langCode, translations) ->
                val groupModel = TranslationGroupModel(langCode)
                groupModel.langName = translations.firstOrNull()?.bookInfo?.langName ?: langCode
                groupModel.translations = ArrayList(translations)

                val wasExpanded = oldExpandedState[langCode] ?: false
                groupModel.isExpanded = wasExpanded || translations.any { it.isChecked }
                mergedGroups.add(groupModel)
            }

        } finally {
            translFactory.close()
        }

        return mergedGroups
    }

    private fun readTranslInfo(
        langCode: String,
        slug: String,
        translObject: JsonObject
    ): TranslModel {
        val bookInfo = TranslationBookInfoModel(slug)
        bookInfo.langCode = langCode
        bookInfo.bookName = translObject["book"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.authorName = translObject["author"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.displayName = translObject["displayName"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.langName = translObject["langName"]?.jsonPrimitive?.contentOrNull ?: ""
        bookInfo.lastUpdated = translObject["lastUpdated"]?.jsonPrimitive?.longOrNull ?: -1
        bookInfo.downloadPath = translObject["downloadPath"]?.jsonPrimitive?.contentOrNull ?: ""
        return TranslModel(bookInfo)
    }

    private fun downloadTranslation(slug: String) {
        val model = findTranslationBySlug(slug) ?: return
        val bookInfo = model.bookInfo

        if (TranslationDownloadProvider.source.isDownloading(bookInfo.slug)) {
            updateDownloadStatus(bookInfo.slug, ResourceDownloadStatus.Started)
            return
        }

        if (isTranslationDownloaded(bookInfo.slug)) {
            updateDownloadStatus(bookInfo.slug, ResourceDownloadStatus.Completed)
            return
        }

        viewModelScope.launch {
            if (!canProceedOnline()) return@launch

            updateDownloadStatus(bookInfo.slug, ResourceDownloadStatus.Started)
            TranslationDownloadProvider.source.startDownload(bookInfo)
        }
    }

    private fun cancelDownload(key: String) {
        TranslationDownloadProvider.source.stopDownload(key)
        updateDownloadStatus(key, ResourceDownloadStatus.Cancelled)
    }

    private fun updateDownloadStatus(slug: String, status: ResourceDownloadStatus) {
        _uiState.update { state ->
            val newDownloadStates = state.downloadStates.toMutableMap()
            var updatedGroups = state.translationGroups

            when (status) {
                is ResourceDownloadStatus.Failed -> {
                    viewModelScope.launch {
                        val bookName = findTranslationBySlug(slug)?.bookInfo?.bookName ?: slug
                        _events.emit(
                            TranslationUiEvent.ShowMessage(
                                title = getString(Res.string.strTitleFailed),
                                message = getString(
                                    Res.string.strMsgTranslFailedToDownload,
                                    bookName
                                ) + " " + getString(Res.string.strMsgTryLater)
                            )
                        )
                    }
                    newDownloadStates.remove(slug)
                }

                is ResourceDownloadStatus.Completed -> {
                    viewModelScope.launch {
                        val bookName = findTranslationBySlug(slug)?.bookInfo?.bookName ?: slug
                        _events.emit(
                            TranslationUiEvent.ShowMessage(
                                title = getString(Res.string.strTitleSuccess),
                                message = getString(
                                    Res.string.strMsgTranslDownloaded,
                                    bookName
                                )
                            )
                        )
                    }
                    newDownloadStates.remove(slug)

                    // Update isDownloaded and isChecked in groups
                    updatedGroups = state.translationGroups.map { group ->
                        group.copy(translations = ArrayList(group.translations.map { t ->
                            if (t.bookInfo.slug == slug) {
                                t.apply {
                                    isDownloaded = true
                                    isChecked = state.selectedSlugs.contains(slug)
                                }
                            } else t
                        }))
                    }

                    // If it was selected (pending download), save it now that it's downloaded
                    if (state.selectedSlugs.contains(slug) && state.saveTranslationChanges) {
                        viewModelScope.launch {
                            ReaderPreferences.setTranslations(state.selectedSlugs)
                        }
                    }
                }

                is ResourceDownloadStatus.Cancelled -> {
                    newDownloadStates.remove(slug)
                }

                else -> {
                    newDownloadStates[slug] = status
                }
            }

            state.copy(
                downloadStates = newDownloadStates,
                translationGroups = updatedGroups
            )
        }
    }

    private fun findTranslationBySlug(slug: String): TranslModel? {
        return _uiState.value.translationGroups.flatMap { it.translations }.find { it.bookInfo.slug == slug }
    }

    private fun isTranslationDownloaded(slug: String): Boolean {
        val translFactory = QuranTranslationFactory()
        try {
            return translFactory.isTranslationDownloaded(slug) == true
        } finally {
            translFactory.close()
        }
    }
}
