package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS always requires an explicit authorization request, so this never returns null.
 *
 * Android actual-ı kimi status **hər ön plana qayıdışda** yenidən oxunur. Bu, rahatlıq deyil,
 * tələbdir: onboarding-in bildiriş qapısı istifadəçini Ayarlara göndərib geri gözləyir, və status
 * yalnız `LaunchedEffect(Unit)` ilə oxunsaydı qayıdanda «Başla» düyməsi sönük qalıb istifadəçini
 * onboarding-də kilidləyərdi.
 */
@Composable
actual fun rememberNotificationPermission(): NotificationPermissionState? {
    var granted by remember { mutableStateOf(false) }
    // Statusu bilməyənə qədər «soruşula bilər» sayırıq: təmiz quruluşda ilk kadrda düymənin
    // «Ayarları aç» kimi görünüb sonra «İcazə ver»ə dönməsi pis olardı.
    var canPromptNow by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    suspend fun refresh() {
        val status = readAuthorizationStatus()
        granted = isGrantedStatus(status)
        canPromptNow = status == UNAuthorizationStatusNotDetermined
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { refresh() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(scope) {
        object : NotificationPermissionState {
            override val isGranted: Boolean get() = granted
            override val canPrompt: Boolean get() = canPromptNow

            override fun request() {
                scope.launch {
                    granted = requestAuthorization()
                    // iOS quraşdırma başına yalnız bir dəfə soruşur — bundan sonra yeganə yol Ayarlardır.
                    canPromptNow = false
                }
            }
        }
    }
}

/**
 * Raw `UNAuthorizationStatus`, or null when the settings object is missing. Shared with
 * `IosDownloadNotifier`, which needs to tell "denied" (give up) from "not asked yet" (prompt).
 */
internal suspend fun readAuthorizationStatus(): Long? =
    suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.authorizationStatus)
            }
    }

internal fun isGrantedStatus(status: Long?): Boolean =
    status == UNAuthorizationStatusAuthorized ||
        status == UNAuthorizationStatusProvisional ||
        status == UNAuthorizationStatusEphemeral

internal suspend fun requestAuthorization(): Boolean =
    suspendCancellableCoroutine { continuation ->
        val options = UNAuthorizationOptionAlert or
            UNAuthorizationOptionSound or
            UNAuthorizationOptionBadge

        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(options) { isGranted, _ ->
                continuation.resume(isGranted)
            }
    }
