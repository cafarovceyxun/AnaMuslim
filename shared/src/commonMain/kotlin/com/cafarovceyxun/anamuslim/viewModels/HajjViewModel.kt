package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.DuaDuplicateException
import com.cafarovceyxun.anamuslim.repository.supabase.GuideEvidenceRepository
import com.cafarovceyxun.anamuslim.repository.supabase.HajjRepository
import com.cafarovceyxun.anamuslim.repository.supabase.SalahRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.duaMsgDeleteFailed
import com.cafarovceyxun.anamuslim.resources.duaMsgDeleted
import com.cafarovceyxun.anamuslim.resources.duaMsgDuplicate
import com.cafarovceyxun.anamuslim.resources.duaMsgSaveFailed
import com.cafarovceyxun.anamuslim.resources.duaMsgSaved
import com.cafarovceyxun.anamuslim.utils.supabase.GuideEvidence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Yazma sayğacları — **instansiyadan kənarda**, bələdçi başına bir.
 *
 * Dəlil oxucudakı seçim ekranından (öz `viewModel { … }` instansiyası) yazılır, bələdçi isə başqa
 * instansiyanı oxuyur. Sayğac olmasa bələdçi yeni dəlili yalnız yenidən açılanda görərdi
 * (CLAUDE.md → «App-scoped ViewModel keşi iOS-da proses boyu yaşayır»).
 */
private val hajjContentRevision = MutableStateFlow(0)
private val salahContentRevision = MutableStateFlow(0)

/** Bələdçinin dəlilləri: keşdən dərhal, sonra serverdən; admin yazmaları sayğacı artırır. */
open class GuideEvidenceViewModel(
    private val repository: GuideEvidenceRepository,
    private val contentRevision: MutableStateFlow<Int>,
) : ViewModel() {

    private val _evidence = MutableStateFlow(repository.cached())
    val evidence: StateFlow<List<GuideEvidence>> = _evidence.asStateFlow()

    /** Bu sessiyada serverdən cavab gəlibmi — boş siyahı «yoxdur», yoxsa «hələ gəlməyib». */
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val revision: StateFlow<Int> = contentRevision.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.fetchAll().onSuccess { _evidence.value = it }
            _isLoaded.value = true
        }
    }

    fun saveEvidence(evidence: GuideEvidence, onSaved: () -> Unit = {}) = write(
        action = { repository.add(evidence).map { } },
        successMessage = { getString(Res.string.duaMsgSaved) },
        failureMessage = { error ->
            // Dublikat nə icazə, nə də bağlantı problemidir — ayrıca mesaj lazımdır.
            getString(if (error is DuaDuplicateException) Res.string.duaMsgDuplicate else Res.string.duaMsgSaveFailed)
        },
        onDone = onSaved,
    )

    fun updateEvidence(evidence: GuideEvidence, onSaved: () -> Unit = {}) = write(
        action = { repository.update(evidence) },
        successMessage = { getString(Res.string.duaMsgSaved) },
        failureMessage = { getString(Res.string.duaMsgSaveFailed) },
        onDone = onSaved,
    )

    fun deleteEvidence(id: Long, onDeleted: () -> Unit = {}) = write(
        action = { repository.delete(id) },
        successMessage = { getString(Res.string.duaMsgDeleted) },
        failureMessage = { getString(Res.string.duaMsgDeleteFailed) },
        onDone = onDeleted,
    )

    private fun write(
        action: suspend () -> Result<Unit>,
        successMessage: suspend () -> String,
        failureMessage: suspend (Throwable) -> String,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            action()
                .onSuccess {
                    contentRevision.value = contentRevision.value + 1
                    repository.fetchAll().onSuccess { _evidence.value = it }
                    PlatformUtils.showToast(successMessage())
                    onDone()
                }
                .onFailure { PlatformUtils.showToast(failureMessage(it)) }
            _isLoading.value = false
        }
    }
}

/** Həcc və Ümrə bələdçisi. */
class HajjViewModel : GuideEvidenceViewModel(HajjRepository(), hajjContentRevision)

/** Namaz bələdçisi (2026-10-04). */
class SalahViewModel : GuideEvidenceViewModel(SalahRepository(), salahContentRevision)
