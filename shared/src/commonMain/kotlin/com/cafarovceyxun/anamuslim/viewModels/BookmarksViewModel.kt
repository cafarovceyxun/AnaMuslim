package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithBookmarkEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BookmarksUiState(
    val isLoading: Boolean = true,
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val hadithBookmarks: List<HadithBookmarkEntity> = emptyList(),
    val chapterNames: Map<Int, String> = emptyMap(),
)

class BookmarksViewModel : ViewModel() {
    private val userRepository = RepositoryProvider.userRepository
    private val quranRepository = RepositoryProvider.quranRepository

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getHadithBookmarksFlow().collectLatest { hadithBookmarks ->
                _uiState.update { it.copy(hadithBookmarks = hadithBookmarks) }
            }
        }

        viewModelScope.launch {
            userRepository.getBookmarksFlow().collectLatest { bookmarks ->
                val chapterNames = loadMissingChapterNames(bookmarks)

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        bookmarks = bookmarks,
                        chapterNames = state.chapterNames + chapterNames,
                    )
                }
            }
        }
    }

    /** Returns whether the hadith bookmark was removed; the UI layer shows the resulting message. */
    suspend fun removeHadithBookmark(hadithId: Long): Boolean =
        userRepository.removeHadithBookmark(hadithId)

    suspend fun removeHadithBookmarks(ids: Set<Long>): Boolean {
        if (ids.isEmpty()) return false
        return userRepository.removeHadithBookmarksBulk(ids.toList()) >= 1
    }

    suspend fun removeAllHadithBookmarks() {
        userRepository.removeAllHadithBookmarks()
    }

    suspend fun updateHadithBookmarkNote(hadithId: Long, note: String?) {
        userRepository.updateHadithBookmarkNote(hadithId, note)
    }

    /** Returns whether the bookmark was removed; the UI layer shows the resulting message. */
    suspend fun removeBookmark(id: Long): Boolean =
        userRepository.removeBookmarksBulk(longArrayOf(id))

    /** Returns whether the bookmarks were removed; the UI layer shows the resulting message. */
    suspend fun removeBookmarks(ids: Set<Long>): Boolean {
        if (ids.isEmpty()) return false
        return userRepository.removeBookmarksBulk(ids.toLongArray())
    }

    suspend fun removeAllBookmarks() {
        userRepository.removeAllBookmarks()
    }

    private suspend fun loadMissingChapterNames(
        bookmarks: List<BookmarkEntity>
    ): Map<Int, String> {
        val existing = _uiState.value.chapterNames
        val missing = bookmarks
            .map { it.chapterNo }
            .distinct()
            .filter { it > 0 && !existing.containsKey(it) }

        if (missing.isEmpty()) {
            return emptyMap()
        }

        return withContext(Dispatchers.IO) {
            missing.associateWith { quranRepository.getChapterName(it) }
        }
    }
}
