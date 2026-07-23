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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    init {
        loadAvailableTranslations()
    }

    private val debouncedQuery = _searchQuery
        .debounce(200)
        .distinctUntilChanged()
        .shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

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
            _searchHistory.value = searchHistoryStore.loadAll()
        }
    }

    // Call when the user commits a search outcome
    fun recordSearchQuery(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            searchHistoryStore.add(trimmed)
            _searchHistory.value = searchHistoryStore.loadAll()
        }
    }

    fun recordCurrentSearchQuery() {
        recordSearchQuery(_searchQuery.value)
    }

    fun removeSearchHistory(id: Int) {
        viewModelScope.launch {
            searchHistoryStore.remove(id)
            _searchHistory.value = searchHistoryStore.loadAll()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryStore.clear()
            _searchHistory.value = emptyList()
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
}
