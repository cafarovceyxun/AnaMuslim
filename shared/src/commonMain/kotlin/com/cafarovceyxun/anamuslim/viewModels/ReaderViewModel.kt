package com.cafarovceyxun.anamuslim.viewModels

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgInvalidChapterNo
import com.cafarovceyxun.anamuslim.resources.strMsgInvalidJuzNo
import org.jetbrains.compose.resources.getString
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.reader.QuranPageItem
import com.cafarovceyxun.anamuslim.compose.components.reader.QuranPageLineItem
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderLayoutItem
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderPreparedData
import com.cafarovceyxun.anamuslim.compose.components.reader.TranslationPageItem
import com.cafarovceyxun.anamuslim.compose.components.reader.TranslationPageSection
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.others.ReadHistoryShortcuts
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.ComposeUiConfig
import com.cafarovceyxun.anamuslim.utils.reader.PageBuilderParams
import com.cafarovceyxun.anamuslim.utils.reader.QuranScript
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReadType
import com.cafarovceyxun.anamuslim.utils.reader.ReaderChangeManager
import com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData
import com.cafarovceyxun.anamuslim.utils.reader.ReaderItemsBuilder
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams
import com.cafarovceyxun.anamuslim.utils.reader.ReaderObserveAction
import com.cafarovceyxun.anamuslim.utils.reader.TextBuilderParams
import com.cafarovceyxun.anamuslim.utils.reader.TranslationPageBuilderParams
import com.cafarovceyxun.anamuslim.utils.reader.VerseActions
import com.cafarovceyxun.anamuslim.utils.reader.toQuranMushafId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class ReaderViewType {
    data class Chapter(val chapterNo: Int) : ReaderViewType()
    data class Juz(val juzNo: Int) : ReaderViewType()
    data class Hizb(val hizbNo: Int) : ReaderViewType()
}

data class ReaderUiState(
    var error: String? = null,
    val viewType: ReaderViewType? = null,
)

data class MushafSession(
    val layout: QuranScript,
    val pageCount: Int,
    val currentPageNo: Int?,
    val version: Int,
)

/** Platform-neutral reader navigation direction; each platform maps its own input onto this. */
enum class ReaderScrollKey {
    FORWARD,
    BACKWARD,
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel : ReaderProviderViewModel() {
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    var selectedNavigationTabIndex = mutableIntStateOf(0)

    val surahs = repository.getAllSurahs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val juzs = repository.getJuzs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hizbs = repository.getHizbs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * VM-owned so mode changes are visible synchronously: the DataStore flow emits with a delay,
     * and during that window the outgoing mode's UI is still composed and can consume navigation
     * requests meant for the incoming mode. All changes go through [applyReaderMode].
     */
    private val _readerMode = MutableStateFlow<ReaderMode?>(null)
    val readerMode: StateFlow<ReaderMode?> = _readerMode.asStateFlow()

    var autoScrollSpeed = mutableStateOf<Float?>(null)
    var isAutoScrollGestureMode = mutableStateOf(false)
    var autoScrollStep = mutableIntStateOf(ReaderPreferences.getAutoScrollStepSync())
    var lastAutoScrollSpeed = 0.5f

    var playerVerseSync = mutableStateOf(false)
    
    private val _volumeToggleFeedback = MutableStateFlow<Boolean?>(null)
    val volumeToggleFeedback = _volumeToggleFeedback.asStateFlow()

    fun triggerVolumeToggleFeedback(enabled: Boolean) {
        viewModelScope.launch {
            _volumeToggleFeedback.value = enabled
            delay(1500)
            if (_volumeToggleFeedback.value == enabled) {
                _volumeToggleFeedback.value = null
            }
        }
    }

    var isReadingActive = false

    /** Continuously updated by the active mode to track the user's reading position. */
    private val _lastKnownVerse = MutableStateFlow<ChapterVersePair?>(null)
    val lastKnownVerse: ChapterVersePair? get() = _lastKnownVerse.value

    /** Observable form of [lastKnownVerse], so the host can publish the reading position (e.g. the
     * player's "go to reciting surah" button, which hides while that surah is already open). */
    val lastKnownVerseState: StateFlow<ChapterVersePair?> = _lastKnownVerse.asStateFlow()

    private val _navigateToPage = MutableStateFlow<Int?>(null)
    val navigateToPage: StateFlow<Int?> = _navigateToPage.asStateFlow()

    private val _navigateToVerse = MutableStateFlow<ChapterVersePair?>(null)
    val navigateToVerse: StateFlow<ChapterVersePair?> = _navigateToVerse.asStateFlow()

    private val _verseByVersePrepared = MutableStateFlow(
        ReaderPreparedData(emptyList(), emptyMap()),
    )

    val verseByVersePrepared: StateFlow<ReaderPreparedData> =
        _verseByVersePrepared.asStateFlow()

    private val _scrollEvent = MutableSharedFlow<Int>()

    /**
     * Direction only (1 down / -1 up), never a pixel amount: the step is a share of the viewport
     * ([com.cafarovceyxun.anamuslim.utils.reader.ReaderScrollStep]) and only the collector knows how
     * tall its viewport is.
     */
    val scrollEvent: SharedFlow<Int> = _scrollEvent.asSharedFlow()

    private val _smartScrollEvent = MutableSharedFlow<Int>() // 1 for next/down, -1 for prev/up
    val smartScrollEvent: SharedFlow<Int> = _smartScrollEvent.asSharedFlow()

    private val _mushafSession = MutableStateFlow(
        MushafSession(
            layout = QuranScript(QuranScriptUtils.SCRIPT_DEFAULT, null),
            pageCount = 0,
            currentPageNo = null,
            version = 0,
        ),
    )
    val mushafSession = _mushafSession.asStateFlow()
    private val mushafSessionMutex = Mutex()

    private var _pageItems = MutableStateFlow<Map<Int, QuranPageItem>>(emptyMap())
    val pageItems = _pageItems.asStateFlow()

    private var _translationPageItems = MutableStateFlow<Map<Int, TranslationPageItem>>(emptyMap())
    val translationPageItems = _translationPageItems.asStateFlow()

    /**
     * Moves the reader one step. [isVolumeKey] is a parameter because volume-key navigation is
     * opt-in; the platform decides which of its keys map to which direction (Android:
     * `ReaderViewModel.handleKeyEvent`).
     */
    fun handleScrollKey(key: ReaderScrollKey, isVolumeKey: Boolean): Boolean {
        if (!isReadingActive) return false

        if (isVolumeKey && !AppPreferences.getVolumeKeyNavigationEnabled()) return false

        val isForward = key == ReaderScrollKey.FORWARD

        AppLogger.d("ReaderKey", "Scroll key: $key (volume=$isVolumeKey), Mode: ${readerMode.value}")

        val mode = readerMode.value
        val session = mushafSession.value

        return when (mode) {
            ReaderMode.Reading -> {
                val currentPage = session.currentPageNo
                if (isForward) {
                    if (currentPage != null && currentPage < session.pageCount) {
                        requestPageNavigation(currentPage + 1)
                        true
                    } else false
                } else {
                    if (currentPage != null && currentPage > 1) {
                        requestPageNavigation(currentPage - 1)
                        true
                    } else false
                }
            }

            // Both translation layouts live in `ReaderLayoutTranslationPageMode`, and both of its
            // branches listen on [smartScrollEvent] only. Routing the vertical (scroll) one to
            // [scrollEvent] instead published into a flow nothing collects in that mode — the
            // key was consumed and then silently dropped.
            ReaderMode.Translation, ReaderMode.TranslationVertical -> {
                viewModelScope.launch {
                    _smartScrollEvent.emit(if (isForward) 1 else -1)
                }
                true
            }

            // Verse-by-verse is the one layout built on [scrollEvent] (`ReaderLayoutVerseMode`).
            ReaderMode.VerseByVerse -> {
                viewModelScope.launch {
                    _scrollEvent.emit(if (isForward) 1 else -1)
                }
                true
            }

            else -> false
        }
    }

    override fun saveTranslation(
        translSlug: String,
        chapterNo: Int,
        verseNo: Int,
        newText: String,
        newNote: String?
    ) {
        super.saveTranslation(translSlug, chapterNo, verseNo, newText, newNote)
        _translationPageItems.value = emptyMap()
    }

    private var lastTranslationReaderContentKey: String? = null

    private val pagesLoadingMutex = Mutex()
    private val initReaderMutex = Mutex()
    private var lastInitReaderSignature: String? = null

    init {
        controller.connect()

        viewModelScope.launch {
            // Seed from the persisted preference unless an initReader call got there first.
            _readerMode.compareAndSet(null, ReaderPreferences.getReaderMode())
        }

        viewModelScope.launch {
            val code = ReaderPreferences.getQuranScript()
            val variant = ReaderPreferences.getQuranScriptVariant()

            _mushafSession.update {
                it.copy(
                    layout = QuranScript(code, variant),
                )
            }
        }
    }

    override fun onCleared() {
        saveReadHistory()
        controller.disconnect()
        super.onCleared()
    }

    fun updateCurrentPageNo(pageNo: Int) {
        _mushafSession.update { it.copy(currentPageNo = pageNo) }
    }

    /**
     * Collects UI-driving prefs and rebuilds reader content. Intended to run only while this
     * screen is visible.
     */
    suspend fun observeChanges(
        uiConfig: ComposeUiConfig,
        verseActions: VerseActions,
    ) {
        readerMode
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { mode ->
                when (mode) {
                    ReaderMode.VerseByVerse -> combine(
                        ReaderChangeManager.verseModeFlow(),
                        _uiState.map { it.viewType }.distinctUntilChanged(),
                        refreshTrigger,
                    ) { action, _, _ ->
                        action
                    }

                    ReaderMode.Reading -> ReaderChangeManager.mushafModeFlow()
                    ReaderMode.Translation, ReaderMode.TranslationVertical -> combine(
                        ReaderChangeManager.translationModeFlow(),
                        refreshTrigger
                    ) { action, _ -> action }
                }
            }
            .collectLatest { action ->
                when (action) {
                    is ReaderObserveAction.BuildVerse -> {
                        val params = TextBuilderParams(
                            uiConfig = uiConfig,
                            verseActions = verseActions,
                            fontResolver = fontResolver,
                            arabicEnabled = action.cfg.arabicEnabled,
                            arabicSizeMultiplier = action.cfg.arabicSize,
                            translationSizeMultiplier = action.cfg.translationSize,
                            script = action.cfg.script.scriptCode,
                            slugs = action.cfg.translations,
                            highlightParentheses = action.cfg.highlightParentheses,
                            showParentheses = action.cfg.showParentheses,
                            tajweedColorsEnabled = action.cfg.tajweedColorsEnabled,
                        )

                        buildVerseByVerseItems(
                            params = params,
                            state = _uiState.value,
                            readerMode = ReaderMode.VerseByVerse,
                        )
                    }

                    is ReaderObserveAction.SwitchMushaf -> {
                        val layout = action.cfg.script

                        if (_mushafSession.value.layout != layout) {
                            switchScript(layout)
                        } else if (_mushafSession.value.pageCount <= 0) {
                            ensureSessionPageCount(layout)
                        }
                    }

                    is ReaderObserveAction.BuildTranslation -> {
                        val layout = action.cfg.script

                        if (_mushafSession.value.layout != layout) {
                            lastTranslationReaderContentKey = null
                            switchScript(layout)
                        } else if (_mushafSession.value.pageCount <= 0) {
                            ensureSessionPageCount(layout)
                        }

                        val key = action.cfg.toCacheKey()

                        if (lastTranslationReaderContentKey != key) {
                            _translationPageItems.value = emptyMap()
                            lastTranslationReaderContentKey = key
                        }
                    }
                }
            }
    }

    suspend fun initReaderIfNeeded(params: ReaderLaunchParams) {
        val signature = params.toInitSignature()
        var shouldInit = false

        initReaderMutex.withLock {
            if (lastInitReaderSignature != signature) {
                lastInitReaderSignature = signature
                shouldInit = true
            }
        }

        if (!shouldInit) {
            val d = params.data
            if (d.initialVerse != null) {
                requestVerseNavigation(d.initialVerse!!.chapterNo, d.initialVerse!!.verseNo)
            } else if (d is ReaderIntentData.MushafPage) {
                requestPageNavigation(d.pageNo)
            }
            return
        }


        try {
            initReader(params)
        } catch (t: Throwable) {
            initReaderMutex.withLock {
                if (lastInitReaderSignature == signature) {
                    lastInitReaderSignature = null
                }
            }
            throw t
        }
    }

    suspend fun initReader(params: ReaderLaunchParams) {
        saveReadHistory()
        AppLogger.d("INIT Reader with params: $params")
        playerVerseSync.value = true

        params.slugs?.let { ReaderPreferences.setTranslations(it) }

        val data = params.data

        selectedNavigationTabIndex.intValue = when (data) {
            is ReaderIntentData.FullJuz -> 1
            is ReaderIntentData.FullHizb -> 2
            is ReaderIntentData.MushafPage -> 3
            else -> 0
        }

        // Check if explicitly requested mushaf mode
        if (data is ReaderIntentData.MushafPage) {
            initMushafPage(data, params.readerMode)
            return
        }

        val targetMode = params.readerMode ?: ReaderMode.VerseByVerse

        val state = ReaderUiState().resolveIntent(data)
        _uiState.update { state }

        // Initialize lastKnownVerse before the mode switch becomes visible, so any transition
        // logic reads the position of the content we are navigating to, not the previous one.
        if (data.initialVerse != null) {
            _lastKnownVerse.value = data.initialVerse
        } else {
            when (val vt = state.viewType) {
                is ReaderViewType.Chapter -> {
                    _lastKnownVerse.value = ChapterVersePair(vt.chapterNo, 1)
                }

                is ReaderViewType.Juz -> {
                    val ranges = withContext(Dispatchers.IO) {
                        repository.getChapterVerseRangesInJuz(vt.juzNo)
                    }
                    if (ranges.isNotEmpty()) {
                        val (c, r) = ranges.first()
                        _lastKnownVerse.value = ChapterVersePair(c, r.first)
                    }
                }

                is ReaderViewType.Hizb -> {
                    val ranges = withContext(Dispatchers.IO) {
                        repository.getChapterVerseRangesInHizb(vt.hizbNo)
                    }
                    if (ranges.isNotEmpty()) {
                        val (c, r) = ranges.first()
                        _lastKnownVerse.value = ChapterVersePair(c, r.first)
                    }
                }

                else -> {}
            }
        }

        applyReaderMode(targetMode)

        // Try to navigate to initial verse (works in both mode)
        if (data.initialVerse != null) {
            requestVerseNavigation(data.initialVerse!!.chapterNo, data.initialVerse!!.verseNo)
        }
        // fallback to manual resolution for reader mode
        else if (targetMode != ReaderMode.VerseByVerse) {
            val targetPage = when (state.viewType) {
                is ReaderViewType.Chapter -> {
                    resolvePageNo(state.viewType.chapterNo)
                }

                is ReaderViewType.Juz -> {
                    withContext(Dispatchers.IO) {
                        repository.getFirstPageOfJuz(state.viewType.juzNo)
                    }
                }

                is ReaderViewType.Hizb -> {
                    withContext(Dispatchers.IO) {
                        repository.getFirstPageOfHizb(state.viewType.hizbNo)
                    }
                }

                else -> null
            }

            targetPage?.let { requestPageNavigation(it) }
        }
    }

    private suspend fun initMushafPage(
        data: ReaderIntentData.MushafPage,
        readerMode: ReaderMode?,
    ) {
        applyReaderMode(readerMode ?: ReaderMode.Reading)

        if (data.mushafCode != null) {
            ReaderPreferences.setQuranScriptWithVariant(data.mushafCode, data.mushafVariant)

            val script = QuranScript(
                ReaderPreferences.getQuranScript(),
                ReaderPreferences.getQuranScriptVariant(),
            )
            val pageCount = mushafPageCount(script.toMushafId())

            _mushafSession.update {
                it.copy(
                    layout = script,
                    pageCount = pageCount,
                )
            }
        }

        if (data.pageNo > 0) {
            _uiState.update {
                ReaderUiState(
                    viewType = ReaderViewType.Chapter(data.fallbackChapterNo.coerceIn(QuranMeta.chapterRange)),
                )
            }

            requestPageNavigation(data.pageNo)
            // explicit mushaf page wins over [initialVerse] for positioning; verse is only for
            // history/sync — do not call [requestVerseNavigation] or translation/mushaf UI will
            // resolve the verse to a (possibly different) page after scroll.
            data.initialVerse?.takeIf { it.isValid }?.let { _lastKnownVerse.value = it }
        } else if (QuranMeta.isChapterValid(data.fallbackChapterNo) && data.fallbackVerseNo > 0) {
            val page = resolvePageNo(
                data.fallbackChapterNo,
                data.fallbackVerseNo,
                data.mushafCode?.toQuranMushafId(data.mushafVariant)
            )

            _uiState.update {
                ReaderUiState(
                    viewType = ReaderViewType.Chapter(data.fallbackChapterNo),
                )
            }

            if (page != null) requestPageNavigation(page)

            _lastKnownVerse.value = data.initialVerse?.takeIf { it.isValid }
                ?: ChapterVersePair(data.fallbackChapterNo, data.fallbackVerseNo)
        } else {
            _uiState.update {
                ReaderUiState(viewType = ReaderViewType.Chapter(1))
            }
            data.initialVerse?.takeIf { it.isValid }?.let {
                requestVerseNavigation(it.chapterNo, it.verseNo)
            }
        }
    }

    private suspend fun ReaderUiState.resolveIntent(data: ReaderIntentData): ReaderUiState = when (data) {
        is ReaderIntentData.FullChapter -> {
            if (QuranMeta.isChapterValid(data.chapterNo)) copy(
                viewType = ReaderViewType.Chapter(data.chapterNo)
            )
            else copy(error = getString(Res.string.strMsgInvalidChapterNo, data.chapterNo))
        }

        is ReaderIntentData.FullJuz -> {
            if (QuranMeta.isJuzValid(data.juzNo)) copy(viewType = ReaderViewType.Juz(data.juzNo))
            else copy(error = getString(Res.string.strMsgInvalidJuzNo, data.juzNo))
        }

        is ReaderIntentData.FullHizb -> {
            if (QuranMeta.isHizbValid(data.hizbNo)) copy(viewType = ReaderViewType.Hizb(data.hizbNo))
            else copy(error = getString(Res.string.strMsgInvalidJuzNo, data.hizbNo))
        }

        is ReaderIntentData.MushafPage -> {
            this
        }
    }


    fun updateLastKnownVerseFromItems(firstVisibleIndex: Int) {
        val items = _verseByVersePrepared.value.items

        for (i in firstVisibleIndex until items.size) {
            val item = items[i]
            if (item is ReaderLayoutItem.VerseUI) {
                _lastKnownVerse.value = ChapterVersePair(item.verse)
                AppLogger.d("SyncDbg", "updateLastKnownVerseFromItems idx=$firstVisibleIndex -> ${item.verse.chapterNo}:${item.verse.verseNo}")
                return
            }
        }
    }

    fun updateLastKnownVerseFromPage(pageNo: Int) {
        val page = _pageItems.value[pageNo]

        viewModelScope.launch(Dispatchers.IO) {
            if (anchorAlreadyOnPage(pageNo)) return@launch

            val ayahId = page?.lines
                ?.filterIsInstance<QuranPageLineItem.Text>()
                ?.firstOrNull()?.words?.firstOrNull()?.ayahId
                ?: repository.getFirstAyahIdOnPage(pageNo)
                ?: return@launch

            // Drop the result if the user has already moved to another page.
            if (_mushafSession.value.currentPageNo != pageNo) return@launch

            _lastKnownVerse.value = QuranMeta.getVerseNoFromAyahId(ayahId).let {
                ChapterVersePair(it.first, it.second)
            }
            AppLogger.d("SyncDbg", "updateLastKnownVerseFromPage page=$pageNo -> ${_lastKnownVerse.value?.chapterNo}:${_lastKnownVerse.value?.verseNo}")
        }
    }

    fun updateLastKnownVerseFromTranslationPage(pageNo: Int) {
        val page = _translationPageItems.value[pageNo]

        viewModelScope.launch(Dispatchers.IO) {
            if (anchorAlreadyOnPage(pageNo)) return@launch

            val firstVerse = page?.sections
                ?.filterIsInstance<TranslationPageSection.Text>()
                ?.firstOrNull()
                ?.verses
                ?.firstOrNull()

            val pair = if (firstVerse != null) {
                ChapterVersePair(firstVerse.chapterNo, firstVerse.verseNo)
            } else {
                val ayahId = repository.getFirstAyahIdOnPage(pageNo) ?: return@launch
                QuranMeta.getVerseNoFromAyahId(ayahId).let {
                    ChapterVersePair(it.first, it.second)
                }
            }

            if (_mushafSession.value.currentPageNo != pageNo) return@launch

            _lastKnownVerse.value = pair
        }
    }

    /**
     * The anchor is more precise than "first verse of the page": keep it whenever it already lies
     * on [pageNo], so verse -> page-mode -> verse round trips return to the exact verse.
     */
    private suspend fun anchorAlreadyOnPage(pageNo: Int): Boolean {
        val current = _lastKnownVerse.value ?: return false
        if (!current.isValid) return false

        val mushafId = _mushafSession.value.layout.toMushafId()

        return repository.getPageForVerse(current.chapterNo, current.verseNo, mushafId) == pageNo
    }

    fun saveReadHistory() {
        val state = _uiState.value
        val mushafSession = _mushafSession.value
        val viewType = state.viewType ?: return
        val mode = readerMode.value ?: return
        val verse = lastKnownVerse

        viewModelScope.launch(Dispatchers.IO) {
            val mushafCode = ReaderPreferences.getQuranScript()
            val mushafVariant = ReaderPreferences.getQuranScriptVariant()?.value

            val entity = when (viewType) {
                is ReaderViewType.Chapter -> ReadHistoryEntity(
                    readType = ReadType.Chapter.value,
                    readerMode = mode.value,
                    chapterNo = viewType.chapterNo,
                    fromVerseNo = verse?.verseNo ?: 1,
                    toVerseNo = verse?.verseNo ?: 1,
                    mushafCode = mushafCode,
                    mushafVariant = mushafVariant,
                    pageNo = mushafSession.currentPageNo,
                )

                is ReaderViewType.Juz -> ReadHistoryEntity(
                    readType = ReadType.Juz.value,
                    readerMode = mode.value,
                    divisionNo = viewType.juzNo,
                    chapterNo = verse?.chapterNo ?: 0,
                    fromVerseNo = verse?.verseNo ?: 0,
                    toVerseNo = verse?.verseNo ?: 0,
                    mushafCode = mushafCode,
                    mushafVariant = mushafVariant,
                    pageNo = mushafSession.currentPageNo,
                )

                is ReaderViewType.Hizb -> ReadHistoryEntity(
                    readType = ReadType.Hizb.value,
                    readerMode = mode.value,
                    divisionNo = viewType.hizbNo,
                    chapterNo = verse?.chapterNo ?: 0,
                    fromVerseNo = verse?.verseNo ?: 0,
                    toVerseNo = verse?.verseNo ?: 0,
                    mushafCode = mushafCode,
                    mushafVariant = mushafVariant,
                    pageNo = mushafSession.currentPageNo,
                )
            }

            userRepository.saveReadHistory(entity)
            ReadHistoryShortcuts.pushLastVerses(entity)
        }
    }

    suspend fun handleModeTransition(to: ReaderMode, anchor: ChapterVersePair? = lastKnownVerse) {
        val verse = anchor ?: return
        val (chapterNo, verseNo) = verse
        AppLogger.d("SyncDbg", "handleModeTransition to=$to anchor=$chapterNo:$verseNo")

        // reset if any
        consumePageNavigation()
        consumeVerseNavigation()

        when (to) {
            ReaderMode.Reading,
            ReaderMode.Translation,
            ReaderMode.TranslationVertical -> {
                val page = resolvePageNo(chapterNo, verseNo) ?: _mushafSession.value.currentPageNo
                AppLogger.d("SyncDbg", "  -> resolved page=$page for $chapterNo:$verseNo")

                if (page != null) {
                    updateCurrentPageNo(page)
                    selectedNavigationTabIndex.intValue = 3
                    requestPageNavigation(page)
                    // Restore the exact verse: page tracking coarsens the anchor to the page's
                    // first verse, which would lose the position on the way back to verse mode.
                    _lastKnownVerse.value = verse
                }
            }

            ReaderMode.VerseByVerse -> {
                val vt = _uiState.value.viewType

                val verseInCurrentView = when (vt) {
                    is ReaderViewType.Chapter -> vt.chapterNo == chapterNo

                    is ReaderViewType.Juz -> withContext(Dispatchers.IO) {
                        repository.getChapterVerseRangesInJuz(vt.juzNo)
                    }.any { (c, range) -> c == chapterNo && verseNo in range }

                    is ReaderViewType.Hizb -> withContext(Dispatchers.IO) {
                        repository.getChapterVerseRangesInHizb(vt.hizbNo)
                    }.any { (c, range) -> c == chapterNo && verseNo in range }

                    null -> false
                }

                // Keep a Juz/Hizb view that still contains the verse; only fall back to a
                // chapter view when the anchor left the current division.
                if (!verseInCurrentView) {
                    _uiState.update {
                        it.copy(viewType = ReaderViewType.Chapter(chapterNo))
                    }

                    selectedNavigationTabIndex.intValue = 0
                }

                requestVerseNavigation(chapterNo, verseNo)
            }
        }
    }

    /** Single entry point for mode changes: emits synchronously, persists in the background. */
    private fun applyReaderMode(mode: ReaderMode) {
        if (_readerMode.value == mode) return
        _readerMode.value = mode
        viewModelScope.launch { ReaderPreferences.setReaderMode(mode) }
    }

    fun setReaderMode(mode: ReaderMode) {
        viewModelScope.launch {
            val current = _readerMode.value
            if (current == mode) return@launch

            // Capture the anchor before switching: while both modes' UIs are briefly composed,
            // their position trackers can overwrite it.
            val anchor = lastKnownVerse

            val isTranslationSwitch =
                (current == ReaderMode.Translation && mode == ReaderMode.TranslationVertical) ||
                        (current == ReaderMode.TranslationVertical && mode == ReaderMode.Translation)

            applyReaderMode(mode)

            // Translation <-> TranslationVertical share mushafSession as the source of truth,
            // so no repositioning is needed between them.
            if (!isTranslationSwitch) {
                handleModeTransition(mode, anchor)
            }
        }
    }

    fun requestPageNavigation(pageNo: Int) {
        _navigateToPage.value = pageNo
    }

    fun consumePageNavigation() {
        _navigateToPage.value = null
    }

    fun requestVerseNavigation(chapterNo: Int, verseNo: Int) {
        _navigateToVerse.value = ChapterVersePair(chapterNo, verseNo)
    }

    fun consumeVerseNavigation() {
        _navigateToVerse.value = null
    }

    suspend fun syncToPlayingVerse() {
        val verse = controller.state.value.currentVerse
        if (!verse.isValid) return

        val mode = readerMode.value ?: return
        val (chapterNo, verseNo) = verse

        when (mode) {
            ReaderMode.Reading, ReaderMode.Translation, ReaderMode.TranslationVertical -> {
                val page = resolvePageNo(chapterNo, verseNo)
                if (page != null) requestPageNavigation(page)
            }

            ReaderMode.VerseByVerse -> {
                val isInView = _verseByVersePrepared.value.items.any { item ->
                    item is ReaderLayoutItem.VerseUI &&
                            item.verse.chapterNo == chapterNo &&
                            item.verse.verseNo == verseNo
                }

                if (!isInView) {
                    val state = ReaderUiState(
                        viewType = ReaderViewType.Chapter(chapterNo),
                    )

                    _uiState.update { state }
                }

                requestVerseNavigation(chapterNo, verseNo)
            }
        }
    }

    private suspend fun buildVerseByVerseItems(
        params: TextBuilderParams,
        state: ReaderUiState,
        readerMode: ReaderMode,
    ) {
        _verseByVersePrepared.value = withContext(Dispatchers.IO) {
            when (val vt = state.viewType) {
                is ReaderViewType.Juz -> ReaderItemsBuilder.buildJuzVersesForTranslationMode(
                    params, repository, vt.juzNo
                )

                is ReaderViewType.Hizb -> ReaderItemsBuilder.buildHizbVersesForTranslationMode(
                    params, repository, vt.hizbNo
                )

                is ReaderViewType.Chapter -> ReaderItemsBuilder.buildChapterVersesForTranslationMode(
                    params, vt.chapterNo,
                )

                null -> ReaderPreparedData(emptyList(), emptyMap())
            }
        } ?: ReaderPreparedData(emptyList(), emptyMap())
    }

    suspend fun resolvePageNo(chapterNo: Int, verseNo: Int = 1, mushafId: Int? = null): Int? {
        val mId = mushafId ?: _mushafSession.value.layout.toMushafId()

        return withContext(Dispatchers.IO) {
            repository.getPageForVerse(chapterNo, verseNo, mId)
        }
    }

    suspend fun mushafPageCount(mushafId: Int): Int {
        return repository.getNumberOfPages(mushafId)
    }

    suspend fun fetchMushafPages(
        anchorPages: Collection<Int>,
        params: PageBuilderParams
    ) {
        val builderKey = params.toKey()
        val session = _mushafSession.value
        val totalPages = session.pageCount
        val targets = mushafPrefetchTargets(anchorPages, totalPages)
        if (targets.isEmpty()) return

        val missing = pagesLoadingMutex.withLock {
            targets.filter { page ->
                val item = _pageItems.value[page]

                item == null || item.cacheKey != builderKey
            }
        }

        if (missing.isEmpty()) return

        val built = withContext(Dispatchers.IO) {
            fontResolver.prefetch(
                session.layout.scriptCode,
                missing,
                params.isDark,
            )

            ReaderItemsBuilder.buildMushafPages(
                fontResolver,
                missing,
                params
            )
        }

        withContext(Dispatchers.Main.immediate) {
            _pageItems.update { old -> old + built }
        }
    }

    suspend fun fetchTranslationPages(
        anchorPages: Collection<Int>,
        buildParams: TranslationPageBuilderParams,
    ) {
        val session = _mushafSession.value
        val totalPages = session.pageCount

        val targets = mushafPrefetchTargets(anchorPages, totalPages)
        if (targets.isEmpty()) return

        val missing = pagesLoadingMutex.withLock {
            targets.filter { page ->
                !_translationPageItems.value.containsKey(page)
            }
        }

        if (missing.isEmpty()) return

        val slug = ReaderPreferences.primaryTranslationSlug()

        val built = withContext(Dispatchers.IO) {
            ReaderItemsBuilder.buildTranslationPages(
                repository,
                missing,
                slug,
                buildParams,
            )
        }

        withContext(Dispatchers.Main.immediate) {
            _translationPageItems.update { old -> old + built }
        }
    }

    private suspend fun ensureSessionPageCount(script: QuranScript) {
        val current = _mushafSession.value

        if (current.pageCount > 0 || current.layout != script) return

        val pageCount = mushafPageCount(script.toMushafId())

        _mushafSession.update {
            it.copy(
                pageCount = pageCount,
                version = it.version + 1
            )
        }
    }

    suspend fun switchScript(newScript: QuranScript) {
        mushafSessionMutex.withLock {
            val old = _mushafSession.value

            val verse = resolveAnchorVerse(old)
            val newCount = mushafPageCount(newScript.toMushafId())

            val newPage = verse?.let {
                repository.getPageForVerse(verse.chapterNo, verse.verseNo, newScript.toMushafId())
            } ?: 1

            _pageItems.value = emptyMap()
            _translationPageItems.value = emptyMap()

            _lastKnownVerse.value = verse

            _mushafSession.value = old.copy(
                layout = newScript,
                pageCount = newCount,
                currentPageNo = newPage,
                version = old.version + 1,
            )

            requestPageNavigation(newPage)
        }
    }

    private suspend fun resolveAnchorVerse(session: MushafSession): ChapterVersePair? {
        val verseFromMemory = withContext(Dispatchers.Main.immediate) {
            lastKnownVerse?.takeIf { it.isValid }
        }

        if (verseFromMemory != null) return verseFromMemory

        val currentPage = session.currentPageNo ?: return null
        val oldMushafId = session.layout.toMushafId()

        if (currentPage <= 0 || oldMushafId <= 0) return null

        val ayahId = repository.getFirstAyahIdOnPage(oldMushafId, currentPage) ?: return null

        val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(ayahId)

        return ChapterVersePair(chapterNo, verseNo)
    }
}

const val MUSHAF_PREFETCH_RADIUS = 4

private fun mushafPrefetchTargets(anchorPages: Collection<Int>, totalPages: Int): Set<Int> {
    if (totalPages <= 0) return emptySet()

    val targets = linkedSetOf<Int>()

    for (anchorPage in anchorPages) {
        if (anchorPage !in 1..totalPages) continue
        for (d in -MUSHAF_PREFETCH_RADIUS..MUSHAF_PREFETCH_RADIUS) {
            val page = anchorPage + d
            if (page in 1..totalPages) targets += page
        }
    }

    return targets
}

private fun ReaderUiState.rebuildEquals(other: ReaderUiState): Boolean =
    viewType == other.viewType &&
            error == other.error
