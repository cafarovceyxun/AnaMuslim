package com.cafarovceyxun.anamuslim.db.search

data class SearchHistoryEntry(
    val id: Int,
    val text: String,
    val date: String,
)

/**
 * The user's recent search queries. Android backs this with a legacy raw-SQLite helper
 * (`SearchHistoryDBHelper`, the same legacy family as `BookmarkDbHelper`), which is why the
 * storage stays platform-side behind this interface rather than moving to Room/okio.
 */
interface SearchHistorySource {
    suspend fun loadAll(): List<SearchHistoryEntry>
    suspend fun add(trimmedQuery: String)
    suspend fun remove(id: Int)
    suspend fun clear()
}

/**
 * Registered at startup (Android `QuranApp.onCreate()`), mirroring [
 * com.cafarovceyxun.anamuslim.repository.RepositoryProvider]. Unset — currently iOS — behaves as
 * an empty, write-ignoring history rather than crashing.
 */
object SearchHistoryProvider {
    private var provider: (() -> SearchHistorySource)? = null

    fun setProvider(value: () -> SearchHistorySource) {
        provider = value
    }

    val source: SearchHistorySource
        get() = provider?.invoke() ?: EmptySearchHistorySource
}

private object EmptySearchHistorySource : SearchHistorySource {
    override suspend fun loadAll(): List<SearchHistoryEntry> = emptyList()
    override suspend fun add(trimmedQuery: String) = Unit
    override suspend fun remove(id: Int) = Unit
    override suspend fun clear() = Unit
}
