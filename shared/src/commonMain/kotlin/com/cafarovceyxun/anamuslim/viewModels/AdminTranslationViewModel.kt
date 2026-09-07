package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.supabase.ImportRow
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationCatalogBook
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationCatalogRepository
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationImportRepository
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * İdxal və kataloq ekranlarının vəziyyəti.
 *
 * Kataloq siyahısı, seçilmiş surənin ayə sayı/dolğunluğu və yükləmə nəticəsi bir yerdə saxlanılır —
 * iki ekran da eyni ViewModel-i qurur, amma ayrı instansiyalarda (paylaşılan vəziyyət yoxdur).
 */
class AdminTranslationViewModel : ViewModel() {

    private val quranRepository get() = RepositoryProvider.quranRepository

    private val _books = MutableStateFlow<List<TranslationCatalogBook>>(emptyList())
    val books = _books.asStateFlow()

    private val _chapterNames = MutableStateFlow<Map<Int, String>>(emptyMap())
    val chapterNames = _chapterNames.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    /** Seçilmiş surədə bu kitab üçün dolu ayə sayı — `null` hələ oxunmayıb deməkdir. */
    private val _filledCount = MutableStateFlow<Int?>(null)
    val filledCount = _filledCount.asStateFlow()

    private val _verseCount = MutableStateFlow(0)
    val verseCount = _verseCount.asStateFlow()

    /** Seçilmiş surədə bu kitabın hazırkı mətnləri — önizləmədəki müqayisə üçün. */
    private val _chapterTexts = MutableStateFlow<Map<Int, String>>(emptyMap())
    val chapterTexts = _chapterTexts.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isBusy.value = true
            _chapterNames.value = quranRepository.getChapterNames(QuranMeta.chapterRange.toList())
            _books.value = TranslationCatalogRepository.refresh()
            _isBusy.value = false
        }
    }

    fun selectChapter(book: TranslationCatalogBook?, chapterNo: Int) {
        viewModelScope.launch {
            _verseCount.value = quranRepository.getChapterVerseCount(chapterNo)
            _filledCount.value = null
            _chapterTexts.value = emptyMap()
            if (book != null) {
                _filledCount.value = TranslationImportRepository.filledVerseCount(book, chapterNo)
                _chapterTexts.value = TranslationImportRepository.chapterTexts(book, chapterNo)
            }
        }
    }

    fun setPublic(slug: String, isPublic: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val ok = TranslationCatalogRepository.setPublic(slug, isPublic)
            _books.value = TranslationCatalogRepository.cached()
            _message.value = if (ok) {
                if (isPublic) "Kitab bütün istifadəçilərə açıldı." else "Kitab gizlədildi."
            } else {
                // RLS bloklayanda PostgREST boş nəticə qaytarır — «uğurlu» görünüb heç nə etmir.
                "Dəyişiklik yazılmadı (icazə yoxdur ya da şəbəkə xətası)."
            }
            _isBusy.value = false
        }
    }

    fun upload(book: TranslationCatalogBook, chapterNo: Int, rows: List<ImportRow>) {
        if (rows.isEmpty()) {
            _message.value = "Yüklənəcək ayə yoxdur."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            TranslationImportRepository.importChapter(book.slug, chapterNo, rows)
                .onSuccess { updated ->
                    _message.value = if (updated == 0) {
                        "Heç bir sətir yazılmadı — icazəni yoxlayın."
                    } else {
                        "$updated ayə yazıldı."
                    }
                    _filledCount.value = TranslationImportRepository.filledVerseCount(book, chapterNo)
                    _chapterTexts.value = TranslationImportRepository.chapterTexts(book, chapterNo)
                }
                .onFailure { _message.value = "Xəta: ${it.message}" }
            _isBusy.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
