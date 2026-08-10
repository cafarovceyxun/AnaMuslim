package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.db.search.SearchHistoryProvider
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.db.search.SearchHistoryEntry
import com.cafarovceyxun.anamuslim.search.QuickLinkItem
import com.cafarovceyxun.anamuslim.search.SearchFilters
import com.cafarovceyxun.anamuslim.search.SearchFiltersStore
import com.cafarovceyxun.anamuslim.search.SearchPagingSource
import com.cafarovceyxun.anamuslim.search.SearchQuickLinksParser
import com.cafarovceyxun.anamuslim.search.SearchResult
import com.cafarovceyxun.anamuslim.search.TranslationOption
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class QuranSearchViewModel : ViewModel() {
    private val repository get() = RepositoryProvider.quranRepository
    private val searchHistoryStore get() = SearchHistoryProvider.source

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _quranTextEnabled = MutableStateFlow(false)
    val quranTextEnabled: StateFlow<Boolean> = _quranTextEnabled

    private val _searchHistory = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryEntry>> = _searchHistory

    private val _currentFilters = MutableStateFlow(SearchFiltersStore.read())
    val currentFilters: StateFlow<SearchFilters> = _currentFilters

    private val _availableTranslations = MutableStateFlow<List<TranslationOption>>(emptyList())
    val availableTranslations: StateFlow<List<TranslationOption>> = _availableTranslations

    /**
     * Serialises every history read-modify-write. The settle-timer, the explicit call sites and the
     * user's own delete taps all land here, and each one reads the stored list before writing it —
     * interleaved, they resurrect entries that were just removed.
     */
    private val historyMutex = Mutex()
    private var lastRecordedQuery: String? = null
    private var lastRecordedAt = 0L

    private val debouncedQuery = _searchQuery
        .debounce(200)
        .distinctUntilChanged()
        .shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    /**
     * Writes a query to history once the user stops typing on it.
     *
     * History used to be written only from UI events — switching result tabs, opening a result — so
     * a query the user typed, read and left was never saved, while the tab collector firing on first
     * composition saved whatever half-word happened to be in the field. That is why the list filled
     * up with fragments and missed the real searches. Settling on the query is the thing that
     * actually means "the user searched this"; the explicit call sites remain, and de-duplication in
     * the store makes them harmless repeats.
     */
    private fun recordSettledQueries() {
        viewModelScope.launch {
            debouncedQuery
                .debounce(QUERY_SETTLE_DELAY_MS)
                .collect { recordSearchQuery(it) }
        }
    }

    // Placed below `debouncedQuery`: initialisers run in declaration order, and starting the settle
    // collector above it would have it capture the property before it is assigned.
    init {
        loadAvailableTranslations()
        recordSettledQueries()
    }

    val quickLinks: StateFlow<List<QuickLinkItem>> = debouncedQuery
        .mapLatest { query ->
            SearchQuickLinksParser.parse(repository, query)
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList(),
        )

    val surahResults: StateFlow<List<SurahWithLocalizations>?> = debouncedQuery
        .mapLatest { query ->
            repository.searchSurahs(query)
        }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null,
        )

    val searchResults: Flow<PagingData<SearchResult>> = combine(
        debouncedQuery,
        _quranTextEnabled,
        _currentFilters,
    ) { query, sourceQuran, filters ->
        Triple(query, sourceQuran, filters)
    }
        .flatMapLatest { (query, sourceQuran, filters) ->
            if (query.isBlank()) {
                return@flatMapLatest flowOf(PagingData.empty())
            }

            Pager(
                config = PagingConfig(
                    pageSize = 50,
                    enablePlaceholders = false,
                )
            ) {
                SearchPagingSource(
                    query = query,
                    sourceQuran = sourceQuran,
                    filters = filters,
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(value: String, isQuranText: Boolean = false) {
        _searchQuery.value = value

        if (isQuranText) {
            _quranTextEnabled.value = true
        }
    }

    fun toggleQuranTextEnabled(postRun: (Boolean) -> Unit) {
        val newValue = !_quranTextEnabled.value
        _quranTextEnabled.value = newValue

        postRun(newValue)
    }

    fun toggleQuranSearch() {
        _currentFilters.update { it.copy(searchQuran = !it.searchQuran) }
    }

    fun toggleHadithSearch() {
        _currentFilters.update { it.copy(searchHadith = !it.searchHadith) }
    }

    fun setFilters(filters: SearchFilters) {
        _currentFilters.value = filters
        viewModelScope.launch {
            SearchFiltersStore.write(filters)
        }
    }

    private fun loadAvailableTranslations() {
        viewModelScope.launch {
            val options = withContext(Dispatchers.IO) {
                QuranTranslationFactory().use { factory ->
                    factory.getAvailableTranslationBooksInfo()
                        .filterKeys { factory.isTranslationDownloaded(it) }
                        .map { (slug, info) ->
                            TranslationOption(
                                slug = slug,
                                displayName = info.displayName ?: slug,
                            )
                        }
                        .sortedBy { it.displayName.lowercase() }
                }
            }
            _availableTranslations.value = options
        }
    }

    fun refreshSearchHistory() {
        viewModelScope.launch {
            historyMutex.withLock {
                _searchHistory.value = searchHistoryStore.loadAll()
            }
        }
    }

    /**
     * Saves [text] as a recent search.
     *
     * Two rules keep the list worth reading. Queries shorter than [MIN_HISTORY_QUERY_LENGTH] are
     * dropped — a single letter is a keystroke, not a search. And when this query extends the one
     * saved moments ago ("sala" → "salam"), the shorter one is deleted rather than kept alongside:
     * that pair is one search caught twice on the way through, and keeping both is what buried the
     * user's real queries under their own typing. The [HISTORY_SUPERSEDE_WINDOW_MS] cut-off is what
     * keeps it to the current burst, so a genuine older search is never eaten by a later one that
     * happens to start with the same letters.
     */
    fun recordSearchQuery(text: String) {
        val trimmed = text.trim()
        if (trimmed.length < MIN_HISTORY_QUERY_LENGTH) return

        viewModelScope.launch {
            historyMutex.withLock {
                val previous = lastRecordedQuery
                val recent = currentEpochMillis() - lastRecordedAt <= HISTORY_SUPERSEDE_WINDOW_MS

                if (recent &&
                    previous != null &&
                    previous.length < trimmed.length &&
                    trimmed.startsWith(previous, ignoreCase = true)
                ) {
                    searchHistoryStore.loadAll()
                        .firstOrNull { it.text.equals(previous, ignoreCase = true) }
                        ?.let { searchHistoryStore.remove(it.id) }
                }

                lastRecordedQuery = trimmed
                lastRecordedAt = currentEpochMillis()

                searchHistoryStore.add(trimmed)
                _searchHistory.value = searchHistoryStore.loadAll()
            }
        }
    }

    fun recordCurrentSearchQuery() {
        recordSearchQuery(_searchQuery.value)
    }

    fun removeSearchHistory(id: Int) {
        viewModelScope.launch {
            historyMutex.withLock {
                // A removed entry must stop being the supersede anchor, or the next query typed on
                // top of it would try to delete a row that is already gone.
                if (_searchHistory.value.firstOrNull { it.id == id }?.text
                        .equals(lastRecordedQuery, ignoreCase = true)
                ) {
                    lastRecordedQuery = null
                }

                searchHistoryStore.remove(id)
                _searchHistory.value = searchHistoryStore.loadAll()
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            historyMutex.withLock {
                lastRecordedQuery = null
                searchHistoryStore.clear()
                _searchHistory.value = emptyList()
            }
        }
    }

    fun historySuggestionsForDisplay(
        query: String,
        quickLinks: List<QuickLinkItem>,
    ): List<SearchHistoryEntry> {
        val q = query.trim()

        if (q.isEmpty() || quickLinks.isNotEmpty()) return emptyList()

        val ql = q.lowercase()
        val filtered = _searchHistory.value
            .asSequence()
            .filter { it.text.lowercase() != ql }
            .filter { it.text.contains(q, ignoreCase = true) }
            .toList()

        val prefix = filtered.filter { it.text.startsWith(q, ignoreCase = true) }
        val rest = filtered.filter { !it.text.startsWith(q, ignoreCase = true) }

        return (prefix + rest).distinctBy { it.id }.take(5)
    }

    private companion object {
        /** Idle time after the query stops changing before it counts as a search worth saving. */
        const val QUERY_SETTLE_DELAY_MS = 1_200L

        /** Shorter than this is a keystroke on the way somewhere, not a search. */
        const val MIN_HISTORY_QUERY_LENGTH = 2

        /** How long a saved query stays replaceable by a longer version of itself. */
        const val HISTORY_SUPERSEDE_WINDOW_MS = 60_000L
    }
}
