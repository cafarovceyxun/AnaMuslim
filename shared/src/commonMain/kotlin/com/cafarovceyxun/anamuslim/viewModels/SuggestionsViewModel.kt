package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.api.NetworkConfig
import com.cafarovceyxun.anamuslim.repository.supabase.SuggestionRepository
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.appPlatformId
import com.cafarovceyxun.anamuslim.utils.supabase.Suggestion
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionCategory
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionLocalStore
import com.cafarovceyxun.anamuslim.utils.supabase.SuggestionTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Təklif siyahısının sıralaması. */
enum class SuggestionSort { Popular, Newest }

enum class SuggestionSubmitError { TooShort, RateLimited, Cooldown, Failed }

sealed interface SuggestionSubmitState {
    data object Idle : SuggestionSubmitState
    data object Submitting : SuggestionSubmitState
    data object Success : SuggestionSubmitState
    data class Error(val reason: SuggestionSubmitError) : SuggestionSubmitState
}

/**
 * İstifadəçi tərəfi: təsdiqlənmiş təkliflər, səsvermə və göndərmə.
 *
 * Səs vəziyyəti və göndəriş qəbzləri **cihazda** saxlanılır ([SuggestionLocalStore]) — serverdə
 * kimlik yoxdur. Ona görə səsvermə optimistdir: yerli vəziyyət dərhal dəyişir, RPC uğursuz olarsa
 * geri qaytarılır.
 */
class SuggestionsViewModel : ViewModel() {

    private val repository = SuggestionRepository()

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _myTickets = MutableStateFlow<List<SuggestionTicket>>(emptyList())
    val myTickets = _myTickets.asStateFlow()

    private val _votedIds = MutableStateFlow<Set<Long>>(emptySet())
    val votedIds = _votedIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _sort = MutableStateFlow(SuggestionSort.Popular)
    val sort = _sort.asStateFlow()

    /** `null` = bütün kateqoriyalar. */
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _submitState = MutableStateFlow<SuggestionSubmitState>(SuggestionSubmitState.Idle)
    val submitState = _submitState.asStateFlow()

    fun setSort(value: SuggestionSort) {
        _sort.value = value
    }

    fun setCategoryFilter(value: String?) {
        _categoryFilter.value = value
    }

    fun resetSubmitState() {
        _submitState.value = SuggestionSubmitState.Idle
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _suggestions.value = repository.fetchApproved()
                _votedIds.value = SuggestionLocalStore.votedIds()
                loadMyTickets()
            } catch (e: Exception) {
                AppLogger.d(TAG, "Fetch failed: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cihazdakı qəbzlərin statusu. Bazada olmayan qəbz (admin sətri silib) yerli siyahıdan da
     * atılır — əks halda o qəbz hər açılışda boş yerə soruşulardı.
     */
    private suspend fun loadMyTickets() {
        val tickets = SuggestionLocalStore.tickets()
        if (tickets.isEmpty()) {
            _myTickets.value = emptyList()
            return
        }

        val rows = repository.fetchTickets(tickets)
        _myTickets.value = rows
        SuggestionLocalStore.retainTickets(rows.map { it.ticket }.toSet())
    }

    fun toggleVote(suggestion: Suggestion) {
        val id = suggestion.id
        val wasVoted = id in _votedIds.value
        val delta = if (wasVoted) -1 else 1

        // Optimist: siyahı dərhal cavab verir, RPC arxada gedir.
        applyVoteLocally(id, voted = !wasVoted, delta = delta)

        viewModelScope.launch {
            SuggestionLocalStore.setVoted(id, !wasVoted)

            repository.vote(id, delta)
                .onSuccess { newCount -> setVoteCount(id, newCount) }
                .onFailure { e ->
                    AppLogger.d(TAG, "Vote failed: ${e.message}")
                    applyVoteLocally(id, voted = wasVoted, delta = -delta)
                    SuggestionLocalStore.setVoted(id, wasVoted)
                    _error.value = e.message
                }
        }
    }

    private fun applyVoteLocally(id: Long, voted: Boolean, delta: Int) {
        _votedIds.value = if (voted) _votedIds.value + id else _votedIds.value - id
        _suggestions.value = _suggestions.value.map {
            if (it.id == id) it.copy(vote_count = (it.vote_count + delta).coerceAtLeast(0)) else it
        }
    }

    private fun setVoteCount(id: Long, count: Int) {
        _suggestions.value = _suggestions.value.map {
            if (it.id == id) it.copy(vote_count = count) else it
        }
    }

    fun submit(body: String, category: String) {
        val trimmed = body.trim()
        if (trimmed.length < MIN_BODY_LENGTH) {
            _submitState.value = SuggestionSubmitState.Error(SuggestionSubmitError.TooShort)
            return
        }

        viewModelScope.launch {
            if (SuggestionLocalStore.submitCooldownRemaining() > 0L) {
                _submitState.value = SuggestionSubmitState.Error(SuggestionSubmitError.Cooldown)
                return@launch
            }

            _submitState.value = SuggestionSubmitState.Submitting

            repository.submit(
                body = trimmed.take(MAX_BODY_LENGTH),
                category = category.takeIf { it in SuggestionCategory.ALL } ?: SuggestionCategory.OTHER,
                appVersion = NetworkConfig.appVersionName().takeIf { it.isNotBlank() },
                platform = appPlatformId,
            ).onSuccess { ticket ->
                SuggestionLocalStore.addTicket(ticket)
                SuggestionLocalStore.markSubmitted()
                _submitState.value = SuggestionSubmitState.Success
                loadMyTickets()
            }.onFailure { e ->
                AppLogger.d(TAG, "Submit failed: ${e.message}")
                _submitState.value = SuggestionSubmitState.Error(e.toSubmitError())
            }
        }
    }

    /** Server tərəfdəki `raise exception` mətnləri — bax `submit_suggestion()`. */
    private fun Throwable.toSubmitError(): SuggestionSubmitError {
        val text = message.orEmpty()
        return when {
            text.contains("suggestion_limit_rate") -> SuggestionSubmitError.RateLimited
            text.contains("suggestion_too_short") -> SuggestionSubmitError.TooShort
            else -> SuggestionSubmitError.Failed
        }
    }

    companion object {
        const val MIN_BODY_LENGTH = 10
        const val MAX_BODY_LENGTH = 1000

        private const val TAG = "Suggestions"
    }
}
