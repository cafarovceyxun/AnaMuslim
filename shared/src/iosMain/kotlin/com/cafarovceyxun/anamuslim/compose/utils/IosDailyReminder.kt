package com.cafarovceyxun.anamuslim.compose.utils

import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.verse.VotdNotificationContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS counterpart of `VerseOfTheDayScheduler` + `VerseOfTheDayWorker`.
 *
 * The content is the `daily_content` row published to Supabase, so there is nothing to pre-compute:
 * the reminder is *checked* — at launch, when the toggle is switched on, and from the background
 * refresh task in [com.cafarovceyxun.anamuslim.utils.background.IosBackgroundTasks] — and posted
 * only when the row differs from the one already shown ([VotdNotificationContent.buildIfUnseen]).
 *
 * That replaces the earlier repeating `UNCalendarNotificationTrigger`, which fired at a fixed hour
 * with a snapshot taken days earlier and could not tell new content from content already seen. The
 * cost of the inversion is iOS's own: a check only happens when the system grants a wake-up, so a
 * newly published verse may arrive later than it would on Android.
 */
object IosDailyReminder : DailyReminderScheduler {

    private const val REQUEST_ID = "votd_reminder"
    private const val KEY_CHAPTER = "chapterNo"
    private const val KEY_VERSE = "verseNo"

    /** Not zero: iOS rejects a time-interval trigger of 0 seconds. */
    private const val DELIVERY_DELAY_SECONDS = 1.0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun schedule() {
        scope.launch { deliverIfNew() }
    }

    override fun cancel() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(REQUEST_ID))
    }

    /** Called at startup: shows today's content if it was published since the last check. */
    fun refresh() {
        scope.launch { deliverIfNew() }
    }

    /**
     * Posts the published verse/hadith unless the user has already been notified about it. Safe to
     * call as often as the system allows — every extra call is one Supabase read and no
     * notification.
     */
    suspend fun deliverIfNew() {
        if (!VersePreferences.getVOTDReminderEnabled()) {
            cancel()
            return
        }

        val notification = VotdNotificationContent.buildIfUnseen() ?: return

        val content = UNMutableNotificationContent().apply {
            setTitle(notification.title)
            if (notification.reference.isNotBlank()) setSubtitle(notification.reference)
            setBody(notification.body)
            setUserInfo(
                mapOf(
                    KEY_CHAPTER to notification.chapterNo?.toString().orEmpty(),
                    KEY_VERSE to notification.verseNo?.toString().orEmpty(),
                )
            )
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = REQUEST_ID,
            content = content,
            trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = DELIVERY_DELAY_SECONDS,
                repeats = false,
            ),
        )

        if (post(request)) {
            // Only after the system accepted it, so a rejected post is retried by the next check.
            VotdNotificationContent.markNotified(notification)
        }
    }

    private suspend fun post(request: UNNotificationRequest): Boolean =
        suspendCancellableCoroutine { continuation ->
            // Adding with the same identifier replaces any previous request.
            UNUserNotificationCenter.currentNotificationCenter()
                .addNotificationRequest(request) { error ->
                    if (error != null) {
                        AppLogger.d("IosDailyReminder: bildiriş göndərilmədi — ${error.localizedDescription}")
                    }
                    continuation.resume(error == null)
                }
        }

    /** Routes a tapped notification to the verse it was built from. */
    fun installTapHandler() {
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
    }

    private val delegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            didReceiveNotificationResponse: UNNotificationResponse,
            withCompletionHandler: () -> Unit,
        ) {
            val info = didReceiveNotificationResponse.notification.request.content.userInfo
            val chapterNo = (info[KEY_CHAPTER] as? String)?.toIntOrNull()
            val verseNo = (info[KEY_VERSE] as? String)?.toIntOrNull()

            // A hadith carries no verse; tapping it just brings the app up on the home screen.
            if (chapterNo != null && verseNo != null) {
                ReaderUiHooks.openVerse?.invoke(chapterNo, verseNo)
            }

            withCompletionHandler()
        }

        // Without this the notification is swallowed while the app is in the foreground.
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: platform.UserNotifications.UNNotification,
            withCompletionHandler: (platform.UserNotifications.UNNotificationPresentationOptions) -> Unit,
        ) {
            withCompletionHandler(
                UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound
            )
        }
    }
}
