package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.repository.supabase.ContentBackupRepository
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Yedəyin vəziyyəti. [Ready] gələndə ekran sistem «hara saxlayım?» seçicisini açır. */
sealed interface ContentBackupStatus {
    data object Idle : ContentBackupStatus
    data object Running : ContentBackupStatus
    data class Ready(val json: String, val fileName: String) : ContentBackupStatus

    /** Fayl seçicisinə verilib. Mətn burada **saxlanmır** — yaddaşda bir nüsxə azalsın deyə. */
    data object Saving : ContentBackupStatus
    data object Failed : ContentBackupStatus
}

/**
 * Ana ekrandakı xatırlatmanın və admin bölməsindəki düymənin arxası.
 *
 * Fayl **viewModelScope**-da qurulur, kompozisiyanın scope-unda yox: yığılma bir neçə saniyə çəkir
 * və bu müddətdə ekran dəyişsə (və ya sistem seçicisi açılanda kompozisiya yenidən qurulsa) iş
 * yarıda qalardı — bax CLAUDE.md, «`rememberCoroutineScope()` çox addımlı iş üçün etibarlı deyil».
 */
class ContentBackupViewModel : ViewModel() {

    val lastBackupAt: StateFlow<Long> = AppPreferences.contentBackupAtFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _status = MutableStateFlow<ContentBackupStatus>(ContentBackupStatus.Idle)
    val status: StateFlow<ContentBackupStatus> = _status.asStateFlow()

    /** Seçici ləğv edilərsə bura qayıdırıq. */
    private var previousBackupAt: Long? = null

    fun start() {
        if (_status.value == ContentBackupStatus.Running) return

        viewModelScope.launch {
            _status.value = ContentBackupStatus.Running
            runCatching { ContentBackupRepository.build() }
                .onSuccess {
                    _status.value = ContentBackupStatus.Ready(it, ContentBackupRepository.fileName())
                }
                .onFailure { error ->
                    AppLogger.d(TAG, "Backup failed: ${error.message}")
                    _status.value = ContentBackupStatus.Failed
                }
        }
    }

    /**
     * Fayl seçicisi **açılan anda** çağırılır və tarix qabaqcadan yazılır.
     *
     * ⚠️ Niyə qabaqcadan: 2026-09-18-də cihazda ölçüldü — 16 MB-lıq fayl Drive-a yazılarkən tətbiq
     * arxa fonda qalır, sistem prosesi **öldürür**, fayl isə yazılıb qurtarır. Yəni «saxlanıldı»
     * geri çağırışı heç vaxt gəlmir: tarix yalnız ona bağlı olsaydı, uğurlu yedəkdən sonra da
     * xatırlatma ekranda qalardı (istifadəçinin gördüyü davranış budur).
     *
     * İstifadəçi seçicini ləğv edərsə [onSaved] köhnə tarixi geri qaytarır.
     */
    fun onSaveLaunched() {
        previousBackupAt = AppPreferences.getContentBackupAt()
        _status.value = ContentBackupStatus.Saving
        viewModelScope.launch { AppPreferences.setContentBackupAt(currentEpochMillis()) }
    }

    /** Sistem seçicisi bağlananda çağırılır. Ləğv/xəta olubsa qabaqcadan yazılan tarix geri alınır. */
    fun onSaved(success: Boolean) {
        val previous = previousBackupAt
        previousBackupAt = null

        viewModelScope.launch {
            if (!success && previous != null) AppPreferences.setContentBackupAt(previous)
            _status.value = ContentBackupStatus.Idle
        }
    }

    fun onErrorShown() {
        _status.value = ContentBackupStatus.Idle
    }

    private companion object {
        const val TAG = "ContentBackup"
    }
}
