package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewCategory
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewDecisionResult
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewSaveResult
import com.cafarovceyxun.anamuslim.repository.supabase.ReviewSuggestion
import com.cafarovceyxun.anamuslim.repository.supabase.TranslationReviewRepository
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ReviewDecision { Approve, Reject }

/** Bu sessiyada verilən qərarlar — növbənin başlığında göstərilir. */
data class ReviewTally(val approved: Int = 0, val edited: Int = 0, val rejected: Int = 0) {
    fun plus(decision: ReviewDecision, edited: Boolean, delta: Int) = when (decision) {
        ReviewDecision.Approve -> copy(approved = approved + delta, edited = this.edited + if (edited) delta else 0)
        ReviewDecision.Reject -> copy(rejected = rejected + delta)
    }
}

/** Hələ serverə yazılmamış son qərar — «Geri al» onu ləğv edir. */
data class ReviewUndo(
    val decision: ReviewDecision,
    val item: ReviewSuggestion,
    val edited: Boolean,
    val token: Long,
)

enum class ReviewNoticeKind { NotAllowed, Failed }

/** Gecikmiş yazma alınmadı — təklif növbəyə qaytarılıb. */
data class ReviewNotice(val kind: ReviewNoticeKind, val chapterNo: Int, val verseNo: Int)

/** Serverə yazılmış qərar — host oxucunun cihazdakı tərcümə nüsxələrini yeniləyir. */
data class ReviewCommitted(val decision: ReviewDecision, val item: ReviewSuggestion)

data class TranslationReviewState(
    val items: List<ReviewSuggestion> = emptyList(),
    val isLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** [ReviewCategory.of] açarı; `null` — hamısı. */
    val filter: String? = null,
    /** [visible] daxilində mövqe. */
    val index: Int = 0,
    val isOpen: Boolean = false,
    val isSaving: Boolean = false,
    val lastSave: ReviewSaveResult? = null,
    val tally: ReviewTally = ReviewTally(),
    val undo: ReviewUndo? = null,
    val notice: ReviewNotice? = null,
) {
    val visible: List<ReviewSuggestion> by lazy {
        if (filter == null) items else items.filter { ReviewCategory.of(it.edit.reason) == filter }
    }

    val current: ReviewSuggestion? get() = visible.getOrNull(index)

    /** Süzgəc çipləri: əvvəl «ciddi», sonra say üzrə azalan. */
    val categories: List<Pair<String, Int>> by lazy {
        items.mapNotNull { ReviewCategory.of(it.edit.reason) }
            .groupingBy { it }.eachCount()
            .entries
            .sortedWith(compareBy<Map.Entry<String, Int>> { it.key != ReviewCategory.SERIOUS }.thenByDescending { it.value })
            .map { it.key to it.value }
    }

    /** Ayə düyməsi üçün: hansı ayələrin gözləyən təklifi var. */
    val verseKeys: Set<Long> by lazy { items.mapTo(HashSet()) { key(it.chapterNo, it.verseNo) } }

    fun hasSuggestion(chapterNo: Int, verseNo: Int) = key(chapterNo, verseNo) in verseKeys

    companion object {
        fun key(chapterNo: Int, verseNo: Int): Long = chapterNo * 1000L + verseNo
    }
}

private val ReviewSuggestion.key get() = TranslationReviewState.key(chapterNo, verseNo)

/**
 * Oxucudakı «Yoxlanış təklifləri» növbəsinin vəziyyəti.
 *
 * **App-scoped** qurulmalıdır (`appScopedViewModelStoreOwner()`): növbəni üç yer açır — üst paneldəki
 * düymə, ayənin altındakı düymə və növbənin özü — və hamısı eyni siyahını, eyni mövqeyi görməlidir.
 * Siyahı yalnız giriş etmiş istifadəçi üçün yüklənir ([ensureLoaded] host-dan çağırılır).
 *
 * Təsdiq/rədd **gecikmə ilə** yazılır ([UNDO_WINDOW_MS]): təklif növbədən dərhal çıxır, «Geri al»
 * həmin müddətdə yazmanı ləğv edir. Təsdiq trigger-i mətni əsas cədvələ dərhal köçürdüyü üçün sonradan
 * geri almaq köhnə mətni əl ilə bərpa etmək demək olardı. Yazma viewModelScope-dadır — növbə bağlansa
 * da yarımçıq qalmır.
 */
class TranslationReviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(TranslationReviewState())
    val state = _state.asStateFlow()

    private val _committed = Channel<ReviewCommitted>(Channel.BUFFERED)

    /** Buferlidir: oxucu o an açıq deyilsə, yeniləmə növbəti dəfə açılanda tətbiq olunur. */
    val committed: Flow<ReviewCommitted> = _committed.receiveAsFlow()

    private var tokenSeq = 0L
    private val pending = HashMap<Long, Pair<Long, Job>>()

    fun ensureLoaded() {
        val s = _state.value
        if (s.isLoaded || s.isLoading) return
        reload()
    }

    fun reload() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // Gecikmiş yazması gözləyən təkliflər serverdə hələ «gözləyən»dir — geri qayıtmasınlar.
                val waiting = pending.values.mapTo(HashSet()) { it.first }
                val items = TranslationReviewRepository.fetchAll().filterNot { it.edit.id in waiting }
                _state.update { old ->
                    val next = old.copy(items = items, isLoaded = true, isLoading = false)
                    // Mövqe ayəyə görə saxlanılır — admin bəzilərini təsdiqləyəndə indeks sürüşür.
                    val keep = old.current?.let { cur -> next.visible.indexOfFirst { it.key == cur.key } } ?: -1
                    next.copy(index = if (keep >= 0) keep else old.index.coerceIn(0, next.lastIndex))
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
     * Növbəni [chapterNo]:[verseNo]-də açır; həmin ayədə təklif yoxdursa ondan **sonrakı** ilk təklifdə
     * (sonda isə ilkində) — üst paneldəki düymə oxucunun durduğu yerdən başlasın.
     */
    fun openAt(chapterNo: Int?, verseNo: Int?) {
        _state.update { s ->
            val idx = if (chapterNo == null || verseNo == null) s.index else s.visible.firstAtOrAfter(
                TranslationReviewState.key(chapterNo, verseNo)
            )
            s.copy(isOpen = true, index = idx.coerceIn(0, s.lastIndex), lastSave = null, notice = null)
        }
    }

    fun close() {
        _state.update { it.copy(isOpen = false, lastSave = null, notice = null) }
    }

    fun step(delta: Int) {
        _state.update { s ->
            if (s.visible.isEmpty()) return@update s
            s.copy(index = (s.index + delta).coerceIn(0, s.lastIndex), lastSave = null, notice = null)
        }
    }

    /** Süzgəci dəyişir və cari ayədən sonrakı ilk uyğun təklifdə dayanır. */
    fun setFilter(category: String?) {
        _state.update { s ->
            if (s.filter == category) return@update s
            val at = s.current?.key
            val next = s.copy(filter = category, lastSave = null, notice = null)
            next.copy(index = (at?.let { next.visible.firstAtOrAfter(it) } ?: 0).coerceIn(0, next.lastIndex))
        }
    }

    fun approve() {
        _state.value.current?.let { decide(it, ReviewDecision.Approve, edited = false) }
    }

    fun reject() {
        _state.value.current?.let { decide(it, ReviewDecision.Reject, edited = false) }
    }

    /** Son qərarı ləğv edir — yalnız yazma hələ başlamayıbsa. */
    fun undo() {
        val u = _state.value.undo ?: return
        val job = pending.remove(u.token)?.second ?: return
        job.cancel()
        restore(u.item, u.decision, u.edited, focus = true, notice = null)
    }

    /** Redaktə nəticəsini və yazma xətasını zolaqdan götürür (geri al təklifinə toxunmur). */
    fun clearMessages() {
        _state.update { it.copy(lastSave = null, notice = null) }
    }

    /**
     * Təklifi [newText] ilə əvəzləyir. Uğurlu olanda [onApplied] çağırılır — host onunla cihazdakı
     * ikinci tərcümə nüsxəsini yeniləyir (yalnız təklifin özü dəyişəndə; redaktorun öz düzəlişi
     * yoxlanış kitabına düşmür). [approveAfter] — yazma uğurlu olsa təklif dərhal təsdiqə gedir.
     */
    fun save(
        newText: String,
        approveAfter: Boolean = false,
        onApplied: (ReviewSuggestion, String) -> Unit,
    ) {
        val item = _state.value.current ?: return
        _state.update { it.copy(isSaving = true, lastSave = null, notice = null) }
        viewModelScope.launch {
            val result = TranslationReviewRepository.updateText(item.chapterNo, item.verseNo, newText)
            val updated = item.copy(edit = item.edit.copy(new_text = newText))
            _state.update { s ->
                val items = if (result == ReviewSaveResult.SuggestionUpdated) {
                    s.items.map { if (it.edit.id == item.edit.id) updated else it }
                } else {
                    s.items
                }
                s.copy(items = items, isSaving = false, lastSave = result)
            }
            if (result == ReviewSaveResult.SuggestionUpdated) {
                onApplied(item, newText)
                if (approveAfter) decide(updated, ReviewDecision.Approve, edited = true)
            }
        }
    }

    private fun decide(item: ReviewSuggestion, decision: ReviewDecision, edited: Boolean) {
        val id = item.edit.id ?: return
        val token = ++tokenSeq
        _state.update { s ->
            val next = s.copy(
                items = s.items.filterNot { it.edit.id == id },
                tally = s.tally.plus(decision, edited, 1),
                undo = ReviewUndo(decision, item, edited, token),
                notice = null,
                lastSave = null,
            )
            // Çıxarılan təklifin yerinə növbəti sürüşür — indeks eyni qalır, sonda isə geri çəkilir.
            next.copy(index = s.index.coerceIn(0, next.lastIndex))
        }
        pending[token] = id to viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            // Yazma başlayır — bundan sonra geri almaq mümkün deyil.
            _state.update { if (it.undo?.token == token) it.copy(undo = null) else it }
            val result = when (decision) {
                ReviewDecision.Approve -> TranslationReviewRepository.approve(id)
                ReviewDecision.Reject -> TranslationReviewRepository.reject(id)
            }
            pending.remove(token)
            when (result) {
                ReviewDecisionResult.Done -> _committed.trySend(ReviewCommitted(decision, item))
                ReviewDecisionResult.NotAllowed, ReviewDecisionResult.Failed -> restore(
                    item, decision, edited, focus = false,
                    notice = ReviewNotice(
                        if (result == ReviewDecisionResult.NotAllowed) ReviewNoticeKind.NotAllowed else ReviewNoticeKind.Failed,
                        item.chapterNo,
                        item.verseNo,
                    ),
                )
            }
        }
    }

    /** Təklifi ayə sırasındakı yerinə qaytarır; [focus] — növbə onun üstünə keçir. */
    private fun restore(
        item: ReviewSuggestion,
        decision: ReviewDecision,
        edited: Boolean,
        focus: Boolean,
        notice: ReviewNotice?,
    ) {
        _state.update { s ->
            if (s.items.any { it.edit.id == item.edit.id }) return@update s
            val at = s.items.indexOfFirst { it.key > item.key }.let { if (it < 0) s.items.size else it }
            val items = s.items.toMutableList().apply { add(at, item) }
            val cur = s.current
            val next = s.copy(
                items = items,
                tally = s.tally.plus(decision, edited, -1),
                undo = if (s.undo?.item?.edit?.id == item.edit.id) null else s.undo,
                notice = notice ?: s.notice,
            )
            val focusIdx = if (focus) next.visible.indexOfFirst { it.edit.id == item.edit.id } else -1
            val keepIdx = cur?.let { c -> next.visible.indexOfFirst { it.edit.id == c.edit.id } } ?: -1
            next.copy(index = (if (focusIdx >= 0) focusIdx else if (keepIdx >= 0) keepIdx else s.index).coerceIn(0, next.lastIndex))
        }
    }

    private val TranslationReviewState.lastIndex get() = (visible.size - 1).coerceAtLeast(0)

    private fun List<ReviewSuggestion>.firstAtOrAfter(key: Long): Int =
        indexOfFirst { it.key >= key }.takeIf { it >= 0 } ?: 0

    companion object {
        const val UNDO_WINDOW_MS = 5_000L
    }
}
