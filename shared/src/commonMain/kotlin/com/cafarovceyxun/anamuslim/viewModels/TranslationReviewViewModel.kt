package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewSaveResult
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewSuggestion
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationReviewRepository
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslationReviewState(
    val items: List<ReviewSuggestion> = emptyList(),
    val isLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val index: Int = 0,
    val isOpen: Boolean = false,
    val isSaving: Boolean = false,
    val lastSave: ReviewSaveResult? = null,
) {
    val current: ReviewSuggestion? get() = items.getOrNull(index)

    /** Ayə düyməsi üçün: hansı ayələrin gözləyən təklifi var. */
    val verseKeys: Set<Long> by lazy { items.mapTo(HashSet()) { key(it.chapterNo, it.verseNo) } }

    fun hasSuggestion(chapterNo: Int, verseNo: Int) = key(chapterNo, verseNo) in verseKeys

    companion object {
        fun key(chapterNo: Int, verseNo: Int): Long = chapterNo * 1000L + verseNo
    }
}

/**
 * Oxucudakı «Yoxlanış təklifləri» vərəqinin vəziyyəti.
 *
 * **App-scoped** qurulmalıdır (`appScopedViewModelStoreOwner()`): vərəqi üç yer açır — üst paneldəki
 * düymə, ayənin altındakı düymə və vərəqin özü — və hamısı eyni siyahını, eyni mövqeyi görməlidir.
 * Siyahı yalnız giriş etmiş istifadəçi üçün yüklənir ([ensureLoaded] host-dan çağırılır).
 */
class TranslationReviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(TranslationReviewState())
    val state = _state.asStateFlow()

    fun ensureLoaded() {
        val s = _state.value
        if (s.isLoaded || s.isLoading) return
        reload()
    }

    fun reload() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = TranslationReviewRepository.fetchAll()
                _state.update { old ->
                    // Mövqe ayəyə görə saxlanılır — admin bəzilərini təsdiqləyəndə indeks sürüşür.
                    val keep = old.current?.let { cur ->
                        items.indexOfFirst { it.chapterNo == cur.chapterNo && it.verseNo == cur.verseNo }
                    } ?: -1
                    old.copy(
                        items = items,
                        isLoaded = true,
                        isLoading = false,
                        index = if (keep >= 0) keep else old.index.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.d("TranslReview", "Fetch failed: ${e.message}")
                _state.update { it.copy(isLoading = false, error = e.message ?: "error") }
            }
        }
    }

    /** Sessiya bitəndə təkliflər gizlənməlidir — oxu icazəsi girişə bağlıdır. */
    fun clear() {
        _state.value = TranslationReviewState()
    }

    /**
     * Vərəqi [chapterNo]:[verseNo]-də açır; həmin ayədə təklif yoxdursa ondan **sonrakı** ilk təklifdə
     * (sonda isə ilkində) — üst paneldəki düymə oxucunun durduğu yerdən başlasın.
     */
    fun openAt(chapterNo: Int?, verseNo: Int?) {
        _state.update { s ->
            val idx = if (chapterNo == null || verseNo == null) {
                s.index
            } else {
                val target = TranslationReviewState.key(chapterNo, verseNo)
                s.items.indexOfFirst { TranslationReviewState.key(it.chapterNo, it.verseNo) >= target }
                    .takeIf { it >= 0 } ?: 0
            }
            s.copy(isOpen = true, index = idx.coerceIn(0, (s.items.size - 1).coerceAtLeast(0)), lastSave = null)
        }
    }

    fun close() {
        _state.update { it.copy(isOpen = false, lastSave = null) }
    }

    fun step(delta: Int) {
        _state.update { s ->
            if (s.items.isEmpty()) return@update s
            s.copy(index = (s.index + delta).coerceIn(0, s.items.size - 1), lastSave = null)
        }
    }

    /**
     * Təklifi [newText] ilə əvəzləyir. Uğurlu olanda [onApplied] çağırılır — host onunla cihazdakı
     * ikinci tərcümə nüsxəsini yeniləyir (yalnız təklifin özü dəyişəndə; redaktorun öz düzəlişi
     * yoxlanış kitabına düşmür).
     */
    fun save(newText: String, onApplied: (ReviewSuggestion, String) -> Unit) {
        val item = _state.value.current ?: return
        _state.update { it.copy(isSaving = true, lastSave = null) }
        viewModelScope.launch {
            val result = TranslationReviewRepository.updateText(item.chapterNo, item.verseNo, newText)
            _state.update { s ->
                val items = if (result == ReviewSaveResult.SuggestionUpdated) {
                    s.items.map { if (it.edit.id == item.edit.id) it.copy(edit = it.edit.copy(new_text = newText)) else it }
                } else {
                    s.items
                }
                s.copy(items = items, isSaving = false, lastSave = result)
            }
            if (result == ReviewSaveResult.SuggestionUpdated) onApplied(item, newText)
        }
    }
}
