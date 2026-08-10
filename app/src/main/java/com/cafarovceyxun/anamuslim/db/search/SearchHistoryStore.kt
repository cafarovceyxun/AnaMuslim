package com.cafarovceyxun.anamuslim.db.search

import android.content.Context
import com.cafarovceyxun.anamuslim.components.search.SearchHistoryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

/** Android implementation of the shared [SearchHistorySource] seam (legacy raw SQLite). */
class SearchHistoryStore(context: Context) : SearchHistorySource, Closeable {
    private val appContext = context.applicationContext

    private val helper: SearchHistoryDBHelper by lazy { SearchHistoryDBHelper(appContext) }

    override fun close() {
        helper.close()
    }

    override suspend fun loadAll(): List<SearchHistoryEntry> = withContext(Dispatchers.IO) {
        helper.getHistories("").mapNotNull { model ->
            (model as? SearchHistoryModel)?.let {
                SearchHistoryEntry(it.id, it.text.toString(), it.date)
            }
        }
    }

    override suspend fun add(trimmedQuery: String) = withContext(Dispatchers.IO) {
        if (trimmedQuery.isBlank()) return@withContext
        helper.addToHistory(trimmedQuery, null)
    }

    override suspend fun remove(id: Int) = withContext(Dispatchers.IO) {
        helper.removeFromHistory(id, null)
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        helper.clearHistories()
    }
}
