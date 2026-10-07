package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseFailover
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class BackendSwitchRow(
    val mode: String,
    val updated_at: String? = null,
    val updated_by: String? = null,
)

@Serializable
private data class BackupSyncRow(val synced_at: String? = null)

data class BackendSwitchState(
    /** `null` — ehtiyat oxunmayıb (yüklənir və ya əlçatmazdır). */
    val forcedBackup: Boolean? = null,
    val updatedAt: String? = null,
    val updatedBy: String? = null,
    val syncedAt: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * İdarəetmə panelindəki «Server» sətri: ehtiyatdakı keçid düyməsi (`backend_switch`) və son gecəlik
 * köçürmənin vaxtı (`backup_sync_status`). Hər ikisi **yalnız ehtiyatda** yaşayır — əsas çökəndə də
 * dəyişdirilə bilsin deyə. Yazma ehtiyatın öz hesabı ilə gedir (eyni e-poçt/parol, gecəlik köçürülür);
 * RLS yalnız admin e-poçtuna icazə verir. Bax [SupabaseFailover].
 */
class BackendSwitchViewModel : ViewModel() {

    private val _state = MutableStateFlow(BackendSwitchState())
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val client = SupabaseProvider.backupClient
                val switch = client.from("backend_switch").select().decodeSingle<BackendSwitchRow>()
                val sync = client.from("backup_sync_status").select().decodeSingleOrNull<BackupSyncRow>()
                _state.update {
                    it.copy(
                        forcedBackup = switch.mode == "backup",
                        updatedAt = switch.updated_at,
                        updatedBy = switch.updated_by,
                        syncedAt = sync?.synced_at,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Ehtiyat server oxunmadı: ${e.message}") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setForcedBackup(forced: Boolean, password: String, onDone: () -> Unit) {
        val email = SupabaseProvider.client.auth.currentUserOrNull()?.email
        if (email == null) {
            _state.update { it.copy(error = "Admin sessiyası tapılmadı") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val client = SupabaseProvider.backupClient
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                // RLS rədd edəndə PostgREST xəta yox, boş nəticə qaytarır — təsirlənən sətri yoxla.
                val affected = client.from("backend_switch").update(
                    mapOf("mode" to if (forced) "backup" else "primary")
                ) {
                    select()
                    filter { eq("id", 1) }
                }.decodeList<BackendSwitchRow>().size
                if (affected == 0) {
                    _state.update { it.copy(error = "Server dəyişikliyi qəbul etmədi (icazə yoxdur)") }
                } else {
                    SupabaseFailover.invalidateSwitch()
                    onDone()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Alınmadı: ${e.message}") }
            } finally {
                runCatching { client.auth.signOut(SignOutScope.LOCAL) }
                _state.update { it.copy(isSaving = false) }
            }
            refresh()
        }
    }
}
