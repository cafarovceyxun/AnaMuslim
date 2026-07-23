package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.utils.supabase.ResourceUpdateManager
import com.cafarovceyxun.anamuslim.utils.supabase.ResourceUpdateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResourceAdminViewModel : ViewModel() {
    private val _status = MutableStateFlow<ResourceUpdateStatus?>(null)
    val status = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun fetchStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = ResourceUpdateManager.getAdminUpdateStatus()
            if (result != null) {
                _status.value = result
            } else {
                _error.value = "Məlumat alınmadı (RLS və ya Cədvəl xətası)"
            }
            _isLoading.value = false
        }
    }

    fun updateVersion(newVersion: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            ResourceUpdateManager.updateVersionByAdmin(newVersion)
            fetchStatus() // Refresh
        }
    }
}
