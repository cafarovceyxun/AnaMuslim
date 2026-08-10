package com.cafarovceyxun.anamuslim.utils.app

import com.cafarovceyxun.anamuslim.compose.utils.app.isGrantedStatus
import com.cafarovceyxun.anamuslim.compose.utils.app.readAuthorizationStatus
import com.cafarovceyxun.anamuslim.compose.utils.app.requestAuthorization
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSUUID
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS implementation of [DownloadNotifier]: an immediate local notification, the counterpart of the
 * notification an Android download worker keeps in the shade.
 *
 * Three deliberate choices:
 * - **No trigger.** A null trigger delivers as soon as the request is added, which is what "the
 *   download just ended" means. The verse-of-the-day reminder is the opposite case (a future
 *   calendar trigger), so the two share no code.
 * - **No tap handler.** Nothing needs opening: the user is on the screen that started the download,
 *   or returns to it. Taps are absorbed by the delegate `IosDailyReminder` installs — it looks for
 *   verse keys in `userInfo`, finds none here, and does nothing. That same delegate is what makes
 *   the banner visible while the app is foregrounded, which is the common case for these (an iOS
 *   download only progresses while the app runs).
 * - **Asks for authorization on first use.** Unlike Android, where the download notification rides
 *   on a foreground service, nothing is shown here without permission — and a user who never turned
 *   on the daily verse has never been asked. Denied stays denied; the app does not re-prompt.
 *
 * A fresh identifier per notification, so a second finished download does not replace the first.
 */
object IosDownloadNotifier {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun install() {
        DownloadNotifier.setSink { title, body -> post(title, body) }
    }

    private fun post(title: String, body: String) {
        scope.launch {
            if (!ensureAuthorized()) return@launch

            val content = UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
            }

            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = "download_${NSUUID().UUIDString}",
                content = content,
                trigger = null,
            )

            UNUserNotificationCenter.currentNotificationCenter()
                .addNotificationRequest(request) { error ->
                    if (error != null) {
                        AppLogger.d("IosDownloadNotifier: post failed — ${error.localizedDescription}")
                    }
                }
        }
    }

    private suspend fun ensureAuthorized(): Boolean {
        val status = readAuthorizationStatus()
        if (isGrantedStatus(status)) return true
        if (status != UNAuthorizationStatusNotDetermined) return false

        return requestAuthorization()
    }
}
