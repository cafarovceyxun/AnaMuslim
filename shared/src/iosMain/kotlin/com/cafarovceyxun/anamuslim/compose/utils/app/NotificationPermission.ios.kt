package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS always requires an explicit authorization request, so this never returns null.
 *
 * Known gap vs. the Android actual: the status is read once per composition rather than on every
 * foreground return, so a permission the user flips in Settings while the app is backgrounded is
 * not picked up until the screen is recomposed. Harmless for the current call sites, which only
 * use the flag to decide whether to prompt.
 */
@Composable
actual fun rememberNotificationPermission(): NotificationPermissionState? {
    var granted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { granted = readAuthorizationGranted() }

    return remember(scope) {
        object : NotificationPermissionState {
            override val isGranted: Boolean get() = granted

            // iOS prompts at most once per install; there is no "ask again" state to report.
            override val shouldShowRationale: Boolean get() = false

            override fun request() {
                scope.launch { granted = requestAuthorization() }
            }
        }
    }
}

private suspend fun readAuthorizationGranted(): Boolean =
    isGrantedStatus(readAuthorizationStatus())

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
