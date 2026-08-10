package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * The app's one runtime-permission need: posting notifications (download progress, the
 * verse-of-the-day reminder). Deliberately narrow — a general permission framework would have to
 * model Android permission strings, which have no iOS counterpart.
 */
@Stable
interface NotificationPermissionState {
    /** Whether notifications may currently be posted. */
    val isGranted: Boolean

    /**
     * Shows the platform's permission prompt. The OS only prompts once per install, so repeat
     * calls after a denial are silently ignored by the platform — callers should treat this as
     * best-effort, not as something that will produce an answer.
     */
    fun request()

    /**
     * Whether the platform still considers a prompt useful, i.e. the user has denied once but the
     * OS would show the dialog again. Callers use it to choose between [request] and sending the
     * user to the app's system settings.
     *
     * iOS reports `false` always: it prompts at most once per install, so once the answer is "no"
     * the only route back is Settings — which is exactly what a `false` here tells the caller to do.
     */
    val shouldShowRationale: Boolean
}

/**
 * Returns the notification-permission state, or `null` where the platform grants notifications
 * without a runtime prompt (Android 12 and below). A `null` result means "nothing to ask for" —
 * callers should proceed as if granted.
 *
 * Replaces Accompanist's `rememberPermissionState(POST_NOTIFICATIONS)`, which is Android-only. The
 * Android version check moved into the actual, so call sites no longer carry `Build.VERSION`.
 */
@Composable
expect fun rememberNotificationPermission(): NotificationPermissionState?

/**
 * Prompt for the notification permission when it is missing, then run [work] **either way**.
 *
 * For anything that merely *reports* through a notification — every download in the app — the
 * permission is not a precondition. Downloads run through `NSURLSession` / `WorkManager` and finish
 * whether or not the finished-notification can be posted.
 *
 * The call sites used to gate the work on it instead: `if (!isGranted) request() else download()`.
 * That reads as "ask first, download next time", but [request] never produces an answer to act on —
 * both platforms prompt at most once per install, so after a single decline it resolves silently
 * and the branch that downloads was **never reached again**. On iOS, where onboarding has usually
 * asked already, that meant the download button did nothing at all, with no error and no log line:
 * the reciter download, the script/atlas download and the word-by-word audio download were all dead
 * for anyone who had said no to notifications.
 */
inline fun NotificationPermissionState?.promptThen(work: () -> Unit) {
    if (this != null && !isGranted) request()
    work()
}
