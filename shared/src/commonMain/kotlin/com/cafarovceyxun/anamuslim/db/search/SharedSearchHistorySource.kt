package com.cafarovceyxun.anamuslim.db.search

import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.compose.utils.formatDateTime
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrefKey
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/**
 * Multiplatform [SearchHistorySource] backed by the shared DataStore.
 *
 * Android keeps its legacy raw-SQLite store (`SearchHistoryStore`) — that table already holds the
 * user's history and migrating it would be a data change, not a port. iOS starts empty anyway, so
 * it gets this simpler store instead of a second SQLite helper.
 *
 * Behaviour is matched to the Android helper: re-searching an existing term (case-insensitively)
 * *moves it to the top* rather than duplicating it, and entries come back newest-first.
 */
class SharedSearchHistorySource : SearchHistorySource {

    private val mutex = Mutex()

    override suspend fun loadAll(): List<SearchHistoryEntry> =
        read().sortedByDescending { it.timestamp }.map { it.toEntry() }

    override suspend fun add(trimmedQuery: String) {
        if (trimmedQuery.isBlank()) return

        mutex.withLock {
            val current = read()
            // Case-insensitive de-duplication, as `updateHistory` does on Android.
            val kept = current.filterNot { it.text.equals(trimmedQuery, ignoreCase = true) }
            val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
            val reused = current.firstOrNull { it.text.equals(trimmedQuery, ignoreCase = true) }?.id

            write(kept + StoredEntry(reused ?: nextId, trimmedQuery, currentEpochMillis()))
        }
    }

    override suspend fun remove(id: Int) {
        mutex.withLock { write(read().filterNot { it.id == id }) }
    }

    override suspend fun clear() {
        mutex.withLock { write(emptyList()) }
    }

    private suspend fun read(): List<StoredEntry> {
        val raw = DataStoreManager.readFirst(KEY)
        if (raw.isBlank()) return emptyList()

        return try {
            JsonHelper.json.decodeFromString<List<StoredEntry>>(raw)
        } catch (e: Exception) {
            // A corrupt blob must not break search; start over rather than crash the screen.
            AppLogger.saveError(e, "SharedSearchHistorySource.read")
            emptyList()
        }
    }

    private suspend fun write(entries: List<StoredEntry>) {
        val trimmed = entries.sortedByDescending { it.timestamp }.take(MAX_ENTRIES)
        DataStoreManager.write(KEY, JsonHelper.json.encodeToString(ListSerializer(StoredEntry.serializer()), trimmed))
    }

    /** Stores a real timestamp and formats on read, so ordering never depends on the date format. */
    @Serializable
    private data class StoredEntry(val id: Int, val text: String, val timestamp: Long)

    private fun StoredEntry.toEntry() =
        SearchHistoryEntry(id, text, formatDateTime(timestamp, DATE_PATTERN))

    private companion object {
        val KEY = PrefKey(stringPreferencesKey("search_history_entries"), "")
        const val MAX_ENTRIES = 50
        const val DATE_PATTERN = "dd MMM yyyy, HH:mm"
    }
}
