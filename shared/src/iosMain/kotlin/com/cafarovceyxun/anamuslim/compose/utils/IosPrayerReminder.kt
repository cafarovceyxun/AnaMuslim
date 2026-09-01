package com.cafarovceyxun.anamuslim.compose.utils

import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.notify.NotificationBudget
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotification
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotificationContent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.math.min

/**
 * Namaz bildirişlərinin iOS tərəfi.
 *
 * iOS tətbiq bağlıykən oyanıb bildiriş qura bilmir, ona görə gələcək vaxtlar **əvvəlcədən** yazılır
 * — `IosDailyReminder` ilə eyni model. Fərqlər qəsdlidir:
 *
 * - **Ayrı prefiks** (`prayer_`): [removeScheduled] yalnız öz tələblərini silir, günün ayəsinə
 *   toxunmur.
 * - **Səs var.** `IosDailyReminder` qəsdən səssizdir; namaz vaxtı isə sistem default səsi ilə
 *   çalınmalıdır — istifadəçi telefona baxmadan bilməlidir.
 * - **Ön plana qayıdış müşahidəçisi.** Üfüq məhduddur (büdcə 64 tələblik sistem limitindən
 *   pay alır), ona görə tətbiq hər açılanda növbə yenidən yazılır.
 */
@OptIn(ExperimentalForeignApi::class)
object IosPrayerReminder : PrayerReminderScheduler {

    private const val REQUEST_PREFIX = "prayer_"
    private const val KIND = "prayer"

    /** iOS sıfır saniyəlik tetikleyicini rədd edir. */
    private const val MIN_DELAY_SECONDS = 1.0

    /** Büdcə az vaxta bölünəndə üfüq bu qədər günü keçmir — cədvəl köhnəlir. */
    private const val MAX_DAYS_AHEAD = 14

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun schedule() {
        scope.launch { sync() }
    }

    override fun cancel() {
        scope.launch { removeScheduled() }
    }

    /** Açılışda, ayar dəyişəndə və fon yenilənməsində çağırılır. */
    fun refresh() {
        scope.launch { sync() }
    }

    /**
     * Toxunuşu namaz ekranına yönləndirir.
     *
     * Ekran açmaq üçün ayrıca hook yoxdur — bildirişə toxunmaq tətbiqi açır, istifadəçi isə onsuz
     * da ana ekranda kartı görür. Ayrı naviqasiya seam-i əlavə etmək iki hostda doldurulmalı yeni
     * bir lambda deməkdir və o tələ CLAUDE.md-də ayrıca qeyd olunub.
     */
    fun registerTapHandler() {
        IosNotificationCenterDelegate.register(KIND, REQUEST_PREFIX) { }
    }

    /** Ön plana qayıdanda növbəni yenidən yazır — məhdud üfüqün əsas kompensasiyası budur. */
    fun install() {
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = null,
        ) { _ -> refresh() }
    }

    suspend fun sync() {
        val settings = PrayerPreferences.getSettings()
        if (!settings.canSchedule) {
            removeScheduled()
            return
        }

        // Az vaxt seçiləndə eyni büdcə daha çox günə çatır; boş yerə 35 tələb yazmağın mənası yoxdur.
        val perDay = settings.notify.size.coerceAtLeast(1)
        val limit = min(NotificationBudget.PRAYER, MAX_DAYS_AHEAD * perDay)

        val upcoming = PrayerNotificationContent.upcoming(
            nowMillis = currentEpochMillis(),
            limit = limit,
        )

        removeScheduled()
        upcoming.forEach { post(it) }
    }

    private suspend fun post(notification: PrayerNotification) {
        val delaySeconds = (notification.atMillis - currentEpochMillis()) / 1000.0
        if (delaySeconds < MIN_DELAY_SECONDS) return

        val content = UNMutableNotificationContent().apply {
            setTitle(notification.title)
            setBody(notification.body)
            // ⚠️ Günün ayəsindən fərqli olaraq səs VAR — namaz vaxtı telefona baxmadan bilinməlidir.
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(
                mapOf(
                    IosNotificationCenterDelegate.KEY_KIND to KIND,
                    KEY_PRAYER to notification.prayer.name,
                    KEY_DATE to notification.dateIso,
                )
            )
        }

        // Təqvim yox, interval tetikleyicisi: bizim anlarımız mütləqdir, qurşaq dəyişəndə divar
        // saatı yox, **an** düz qalmalıdır.
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = REQUEST_PREFIX + notification.key,
            content = content,
            trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = delaySeconds,
                repeats = false,
            ),
        )

        add(request)
    }

    /** Yalnız `prayer_` prefiksli tələbləri silir — günün ayəsinin növbəsinə toxunmur. */
    private suspend fun removeScheduled() {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        val identifiers = suspendCancellableCoroutine<List<String>> { continuation ->
            center.getPendingNotificationRequestsWithCompletionHandler { requests ->
                continuation.resume(
                    requests
                        ?.filterIsInstance<UNNotificationRequest>()
                        ?.map { it.identifier }
                        ?.filter { it.startsWith(REQUEST_PREFIX) }
                        ?: emptyList()
                )
            }
        }

        if (identifiers.isNotEmpty()) {
            center.removePendingNotificationRequestsWithIdentifiers(identifiers)
        }
    }

    private suspend fun add(request: UNNotificationRequest): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .addNotificationRequest(request) { error ->
                    if (error != null) {
                        AppLogger.d("IosPrayerReminder: bildiriş qurulmadı — ${error.localizedDescription}")
                    }
                    continuation.resume(error == null)
                }
        }

    private const val KEY_PRAYER = "prayer"
    private const val KEY_DATE = "date"
}
