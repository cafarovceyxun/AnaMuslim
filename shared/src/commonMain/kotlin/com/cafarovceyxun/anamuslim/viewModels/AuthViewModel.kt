package com.cafarovceyxun.anamuslim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgAuthError
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class AuthViewModel : ViewModel() {
    private val _session = MutableStateFlow(SupabaseProvider.client.auth.currentSessionOrNull())
    val session = _session.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val isAuthenticated: Boolean
        get() = session.value != null

    fun isAuthorized(): Boolean {
        val user = session.value?.user
        AppLogger.d("AuthCheck", "Session user: ${user?.email}")
        return isAuthenticated
    }

    private val _isAdmin = MutableStateFlow(false)

    /**
     * Being signed in *is* the admin check.
     *
     * There is no self-service sign-up: the login sheet only opens behind the settings screen's
     * five-tap gesture, and it authenticates against Supabase accounts that only maintainers hold.
     *
     * This used to probe the `user_activity` table and treat "RLS let me read a row" as proof of
     * admin. That table went away with the telemetry removal (2026-07-18), so the probe started
     * failing for everyone and the management section silently disappeared on both platforms.
     */
    val isAdmin = _isAdmin.asStateFlow()

    init {
        viewModelScope.launch {
            SupabaseProvider.client.auth.sessionStatus.collect { status ->
                val session = if (status is SessionStatus.Authenticated) status.session else null
                _session.value = session
                _isAdmin.value = session != null
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                SupabaseProvider.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: getString(Res.string.strMsgAuthError)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            SupabaseProvider.client.auth.signOut()
        }
    }
}
