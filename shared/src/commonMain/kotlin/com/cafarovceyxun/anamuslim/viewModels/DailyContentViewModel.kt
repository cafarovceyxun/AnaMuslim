package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgDailyContentSetFailed
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class DailyContentViewModel : ViewModel() {
    private val repository = DailyContentRepository()

    private val _todayContent = MutableStateFlow<DailyContent?>(null)
    val todayContent: StateFlow<DailyContent?> = _todayContent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchTodayContent()
    }

    fun fetchTodayContent() {
        viewModelScope.launch {
            _isLoading.value = true
            _todayContent.value = repository.fetchTodayContent()
            _isLoading.value = false
        }
    }

    fun setDailyContent(content: DailyContent, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.setDailyContent(content)
            if (success) {
                // Fetch again to ensure we have the correct ID and timestamp from server
                _todayContent.value = repository.fetchTodayContent()
                onSuccess()
            } else {
                // `daily_content`-ə yazmaq yalnız adminə açıqdır (edits_hardening.sql) — RLS və ya
                // şəbəkə səbəbi ilə alınmadıqda əməliyyat səssiz qalmasın.
                PlatformUtils.showToast(getString(Res.string.strMsgDailyContentSetFailed))
            }
            _isLoading.value = false
        }
    }
}
