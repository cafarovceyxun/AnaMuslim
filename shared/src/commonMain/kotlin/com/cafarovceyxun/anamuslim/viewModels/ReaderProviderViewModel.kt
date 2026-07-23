package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayerProvider
import com.cafarovceyxun.anamuslim.utils.reader.FontResolver
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Base for the reader view models, holding the dependencies its subtree needs.
 *
 * Everything it needs comes from the shared startup seams (`RepositoryProvider`,
 * `RecitationPlayerProvider`, `AppLogger`) rather than an `Application`, so it lives in
 * `commonMain`; the app's `viewModel()` call sites are unchanged (same package, same API).
 */
open class ReaderProviderViewModel : ViewModel() {
    val controller = RecitationPlayerProvider.player
    val userRepository get() = RepositoryProvider.userRepository
    val repository get() = RepositoryProvider.quranRepository
    val fontResolver = FontResolver.getInstance()
    val externalQuranDb get() = RepositoryProvider.externalQuranDatabase

    private val _editingVerse = MutableStateFlow<ChapterVersePair?>(null)
    val editingVerse = _editingVerse.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    fun toggleEditing(verse: ChapterVersePair?) {
        _editingVerse.value = if (_editingVerse.value == verse) null else verse
    }

    open fun saveTranslation(
        translSlug: String,
        chapterNo: Int,
        verseNo: Int,
        newText: String,
        newNote: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val factory = QuranTranslationFactory()
            factory.updateTranslation(translSlug, chapterNo, verseNo, newText, newNote)
            try {
                factory.updateSupabaseTranslation(translSlug, chapterNo, verseNo, newText, newNote)
            } catch (e: Exception) {
                AppLogger.saveError(e, "saveTranslationSupabase")
            }

            withContext(Dispatchers.Main) {
                _editingVerse.value = null
                _refreshTrigger.value += 1
            }
        }
    }
}
