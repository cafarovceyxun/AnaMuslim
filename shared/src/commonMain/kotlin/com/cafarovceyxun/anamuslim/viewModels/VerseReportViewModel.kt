package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.api.NetworkConfig
import com.cafarovceyxun.anamuslim.repository.supabase.VerseReportRepository
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import com.cafarovceyxun.anamuslim.utils.supabase.VerseReport
import com.cafarovceyxun.anamuslim.utils.supabase.VerseReportSubmission
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object VerseReportStatus {
    const val PENDING = "pending"
    const val REVIEWING = "reviewing"
    const val RESOLVED = "resolved"
    const val REJECTED = "rejected"
}

sealed interface VerseReportSubmitState {
    data object Idle : VerseReportSubmitState
    data object Submitting : VerseReportSubmitState
    data object Success : VerseReportSubmitState
    data class Error(val message: String?) : VerseReportSubmitState
}

/**
 * Həm bildiriş göndərmə formu, həm də ayarlardakı idarəetmə paneli bu view model-dən istifadə edir;
 * hər ekran öz nüsxəsini yaradır, ona görə iki axın bir-birinə qarışmır.
 */
class VerseReportViewModel : ViewModel() {
    private val repository = VerseReportRepository()

    private val _submitState = MutableStateFlow<VerseReportSubmitState>(VerseReportSubmitState.Idle)
    val submitState = _submitState.asStateFlow()

    private val _reports = MutableStateFlow<List<VerseReport>>(emptyList())
    val reports = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _filter = MutableStateFlow(FILTER_ALL)
    val filter = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun setFilter(filter: String) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetSubmitState() {
        _submitState.value = VerseReportSubmitState.Idle
    }

    fun submit(
        chapterNo: Int,
        verseNo: Int,
        verseKey: String,
        message: String,
        slugs: Set<String>,
    ) {
        val trimmed = message.trim()
        if (trimmed.length < MIN_MESSAGE_LENGTH) return

        viewModelScope.launch {
            _submitState.value = VerseReportSubmitState.Submitting

            val submission = VerseReportSubmission(
                chapter_no = chapterNo,
                verse_no = verseNo,
                verse_key = verseKey,
                message = trimmed.take(MAX_MESSAGE_LENGTH),
                slugs = slugs.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(","),
                app_version = NetworkConfig.appVersionName().takeIf { it.isNotBlank() },
                user_id = SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id,
            )

            _submitState.value = repository.submit(submission).fold(
                onSuccess = { VerseReportSubmitState.Success },
                onFailure = { e ->
                    AppLogger.d("VerseReport", "Submit failed: ${e.message}")
                    VerseReportSubmitState.Error(e.message)
                },
            )
        }
    }

    fun fetchReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _reports.value = repository.fetchAll()
            } catch (e: Exception) {
                AppLogger.d("VerseReport", "Fetch failed: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStatus(report: VerseReport, status: String) {
        val id = report.id ?: return
        viewModelScope.launch {
            repository.updateStatus(id, status)
                .onSuccess {
                    _reports.value = _reports.value.map {
                        if (it.id == id) it.copy(status = status) else it
                    }
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun delete(report: VerseReport) {
        val id = report.id ?: return
        viewModelScope.launch {
            repository.delete(id)
                .onSuccess { _reports.value = _reports.value.filterNot { it.id == id } }
                .onFailure { _error.value = it.message }
        }
    }

    companion object {
        const val FILTER_ALL = "All"
        const val MIN_MESSAGE_LENGTH = 3
        const val MAX_MESSAGE_LENGTH = 2000
    }
}
