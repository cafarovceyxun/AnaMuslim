package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateChecker
import com.cafarovceyxun.anamuslim.utils.supabase.AppRelease
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Outcome of a save, so the screen can say what actually happened rather than just "OK". */
enum class ReleaseSaveResult { SAVED, BLOCKED, INVALID }

/**
 * Admin editor state for the `app_releases` table (Settings → Buraxılış Bildirişi).
 *
 * [save] reports [ReleaseSaveResult.BLOCKED] when the row came back empty: RLS answers a forbidden
 * write with no rows instead of an error, so "nothing was written" and "success" look identical
 * unless the affected row is read back.
 */
class AppReleaseAdminViewModel : ViewModel() {
    private val _releases = MutableStateFlow<List<AppRelease>>(emptyList())
    val releases = _releases.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _lastResult = MutableStateFlow<ReleaseSaveResult?>(null)
    val lastResult = _lastResult.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _releases.value = AppUpdateChecker.fetchAllReleases()
            _isLoading.value = false
        }
    }

    fun clearResult() {
        _lastResult.value = null
    }

    fun releaseFor(platform: String): AppRelease? =
        _releases.value.firstOrNull { it.platform == platform }

    fun save(release: AppRelease) {
        if (release.platform.isBlank() || release.latest_version <= 0) {
            _lastResult.value = ReleaseSaveResult.INVALID
            return
        }

        viewModelScope.launch {
            _isSaving.value = true

            val saved = AppUpdateChecker.saveRelease(release)
            if (saved == null) {
                _lastResult.value = ReleaseSaveResult.BLOCKED
            } else {
                _releases.value = _releases.value.filterNot { it.platform == saved.platform } + saved
                // The admin's own device should see the banner change now, not after the six-hour
                // throttle — and the banner only re-reads on refresh.
                AppUpdateChecker.invalidateThrottle()
                AppUpdateChecker.refresh(force = true)
                _lastResult.value = ReleaseSaveResult.SAVED
            }

            _isSaving.value = false
        }
    }
}
