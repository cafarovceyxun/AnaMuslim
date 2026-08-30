package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.verse.DailyContentFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * «Günün ayəsi» növbəsinin admin paneli.
 *
 * Növbə `(tarix, slot)` cütləri ilə sıralanır; panel isə sırf **sıra** kimi işləyir — element
 * yuxarı/aşağı sürüşdürüləndə və ya silinəndə bütün növbə bugündən başlayaraq yenidən ardıcıl
 * yuvalara yazılır ([DailyContentRepository.reschedule]). «10 ayə seçilibsə səhərə davam etsin»
 * tələbi buradan çıxır: beşdən sonrakı elementlər özləri növbəti günə keçir.
 *
 * Mətnlər **cihazdakı** bazadan oxunur (Quran tərcüməsi, hədis mətni), çünki bildiriş və kart da
 * onları göstərir; Supabase-ə yalnız nəticə yazılır.
 */
class DailyContentManagementViewModel : ViewModel() {

    private val repository = DailyContentRepository()

    private val _queue = MutableStateFlow<List<DailyContent>>(emptyList())
    val queue: StateFlow<List<DailyContent>> = _queue.asStateFlow()

    private val _history = MutableStateFlow<List<DailyContent>>(emptyList())
    val history: StateFlow<List<DailyContent>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            runCatching {
                _queue.value = repository.fetchQueue()
                _history.value = repository.fetchHistory()
            }.onFailure { _error.value = it.message }

            _isLoading.value = false
        }
    }

    /**
     * Ayə (və ya ayə aralığı) əlavə edir. Mətn cihazdakı Quran bazasından və seçilmiş tərcümədən
     * oxunur; ayə tapılmasa növbə dəyişmir və [error] doldurulur.
     */
    fun addVerses(chapterNo: Int, verseStart: Int, verseEnd: Int?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            withSaving {
                val content = DailyContentFactory.verseContent(chapterNo, verseStart, verseEnd)
                    ?: throw IllegalArgumentException(ERROR_VERSE_NOT_FOUND)

                repository.enqueue(content).getOrThrow()
                onDone()
            }
        }
    }

    /** Keçmişdəki elementi yenidən növbənin sonuna qoyur. */
    fun requeue(item: DailyContent) {
        viewModelScope.launch {
            withSaving { repository.enqueue(item.copy(id = null)).getOrThrow() }
        }
    }

    fun moveUp(item: DailyContent) = move(item, -1)

    fun moveDown(item: DailyContent) = move(item, +1)

    private fun move(item: DailyContent, delta: Int) {
        // Vaxtı keçmiş yuva tərpənmir: artıq bildirilib, yerini dəyişmək onu ikinci dəfə çalardı.
        val now = currentEpochMillis()
        if (item.isPast(now)) return

        val current = _queue.value
        val index = current.indexOfFirst { it.id == item.id }
        val target = index + delta

        if (index < 0 || target !in current.indices) return
        if (current[target].isPast(now)) return

        val reordered = current.toMutableList().apply {
            add(target, removeAt(index))
        }

        // Yeni sıra dərhal göstərilir; server rədd etsə [refresh] onu geri qaytarır.
        _queue.value = reordered

        viewModelScope.launch {
            withSaving { repository.reschedule(reordered).getOrThrow() }
        }
    }

    fun delete(item: DailyContent) {
        val id = item.id ?: return

        viewModelScope.launch {
            withSaving {
                repository.delete(id).getOrThrow()

                // Silinən elementin yeri boş qalmasın — qalan növbə sıxlaşdırılır.
                val remaining = _queue.value.filterNot { it.id == id }
                if (remaining.isNotEmpty()) repository.reschedule(remaining).getOrThrow()
            }
        }
    }

    /**
     * Hədisin göstəriləcək hissəsini yazır. Boş mətn çıxarışı **silir** — element yenidən tam
     * mətnlə göstərilir.
     */
    fun setExcerpt(item: DailyContent, excerptAz: String, excerptAr: String) {
        viewModelScope.launch {
            withSaving {
                repository.update(
                    item.copy(
                        excerpt_az = excerptAz.trim().takeIf { it.isNotBlank() },
                        excerpt_ar = excerptAr.trim().takeIf { it.isNotBlank() },
                    )
                ).getOrThrow()
            }
        }
    }

    /** Ayə elementinin aralığını dəyişir — mətnlər yenidən oxunur. */
    fun setVerseRange(item: DailyContent, verseStart: Int, verseEnd: Int?) {
        val chapterNo = item.chapter_no ?: return

        viewModelScope.launch {
            withSaving {
                val rebuilt = DailyContentFactory.verseContent(chapterNo, verseStart, verseEnd)
                    ?: throw IllegalArgumentException(ERROR_VERSE_NOT_FOUND)

                repository.update(
                    item.copy(
                        verse_no = rebuilt.verse_no,
                        verse_end = rebuilt.verse_end,
                        text_ar = rebuilt.text_ar,
                        text_az = rebuilt.text_az,
                        source = rebuilt.source,
                    )
                ).getOrThrow()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun withSaving(block: suspend () -> Unit) {
        _isSaving.value = true
        _error.value = null

        runCatching { block() }
            .onFailure { _error.value = it.message ?: ERROR_GENERIC }

        _isSaving.value = false
        refreshQuiet()
    }

    private suspend fun refreshQuiet() {
        runCatching {
            _queue.value = repository.fetchQueue()
            _history.value = repository.fetchHistory()
        }
    }

    private companion object {
        const val ERROR_VERSE_NOT_FOUND = "Ayə tapılmadı"
        const val ERROR_GENERIC = "Əməliyyat alınmadı"
    }
}
