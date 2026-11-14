package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.utils.app.AppUpdateChecker
import com.cafarovceyxun.anamuslim.utils.supabase.ApkStorage
import com.cafarovceyxun.anamuslim.utils.supabase.AppRelease
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.PickedLargeFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

/** Outcome of a save, so the screen can say what actually happened rather than just "OK". */
enum class ReleaseSaveResult { SAVED, BLOCKED, INVALID }

/** APK yükləməsinin vəziyyəti (Android sətri üçün). */
sealed interface ApkUploadState {
    data object Idle : ApkUploadState
    data class Uploading(val fraction: Float) : ApkUploadState
    data object Done : ApkUploadState
    data class Failed(val message: String) : ApkUploadState
}

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

    private val _apkState = MutableStateFlow<ApkUploadState>(ApkUploadState.Idle)
    val apkState = _apkState.asStateFlow()

    /** Göstərilən versiya üçün APK serverdə varmı; `null` = yoxlanılır/bilinmir. */
    private val _apkExists = MutableStateFlow<Boolean?>(null)
    val apkExists = _apkExists.asStateFlow()

    private var checkJob: Job? = null

    fun checkApk(versionCode: Long) {
        checkJob?.cancel()
        _apkExists.value = null
        if (versionCode <= 0) return
        checkJob = viewModelScope.launch {
            val exists = ApkStorage.exists(versionCode)
            _apkExists.value = exists
        }
    }

    /**
     * Yükləmə `viewModelScope`-dadır: seçici bağlanıb kompozisiya yenidən qurulsa da yarımçıq
     * qalmasın. Yerli keş nüsxəsi hər iki nəticədə silinir (130 MB).
     */
    fun uploadApk(file: PickedLargeFile, versionCode: Long) {
        if (_apkState.value is ApkUploadState.Uploading) return

        if (versionCode <= 0) {
            _apkState.value = ApkUploadState.Failed("Əvvəl versionCode yazın (0-dan böyük).")
            deleteCopy(file)
            return
        }
        if (!file.name.endsWith(".apk", ignoreCase = true)) {
            _apkState.value = ApkUploadState.Failed("Seçilən fayl .apk deyil: ${file.name}")
            deleteCopy(file)
            return
        }

        _apkState.value = ApkUploadState.Uploading(0f)
        viewModelScope.launch {
            var lastPercent = -1
            try {
                val result = ApkStorage.upload(file.path, versionCode) { fraction ->
                    // Hər 64 KB-da çağırılır — yalnız bütöv faiz dəyişəndə StateFlow-a yaz.
                    val percent = (fraction * 100).toInt()
                    if (percent != lastPercent) {
                        lastPercent = percent
                        _apkState.value = ApkUploadState.Uploading(percent / 100f)
                    }
                }
                result.onSuccess {
                    _apkState.value = ApkUploadState.Done
                    _apkExists.value = true
                }.onFailure {
                    _apkState.value = ApkUploadState.Failed(
                        "Yüklənmədi: ${it.message ?: it::class.simpleName}"
                    )
                }
            } catch (e: CancellationException) {
                _apkState.value = ApkUploadState.Idle
                throw e
            } finally {
                deleteCopy(file)
            }
        }
    }

    private fun deleteCopy(file: PickedLargeFile) {
        AppFileSystem.delete(file.path.toPath())
    }

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
