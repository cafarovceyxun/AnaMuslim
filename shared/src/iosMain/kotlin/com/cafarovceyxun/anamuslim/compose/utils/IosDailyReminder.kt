package com.cafarovceyxun.anamuslim.compose.utils

import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.verse.VotdNotification
import com.cafarovceyxun.anamuslim.utils.verse.VotdNotificationContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS counterpart of `VerseOfTheDayScheduler` + `VerseOfTheDayWorker`.
 *
 * Növbə Supabase-də dayanır və gündə [com.cafarovceyxun.anamuslim.utils.supabase.DailyContentSlots.COUNT]
 * yuvaya bölünür, ona görə burada bildirişlər **əvvəlcədən** yazılır: sinxronizasiya anında (açılış,
 * ayarın açılması, fon yenilənməsi) gələcək yuvaların hər biri üçün bir `UNNotificationRequest`
 * qurulur. Bu, iOS-un öz məhdudiyyətinə görə Android-dən fərqlidir — tətbiq bağlıykən oyanıb
 * bildiriş qurmaq imkanı yoxdur, sistemin özü isə əvvəlcədən yazılmış tələbi vaxtında çalır.
 *
 * Eyni səbəbdən **çalınmışları qeyd etmirik**: identifikator `(tarix, slot)`-dan qurulduğu üçün
 * eyni tələbin təkrar əlavəsi köhnəsini əvəz edir, dublikat yaranmır. Android tərəfdəki
 * `isSlotDelivered` yoxlaması orada lazımdır, çünki orada bildirişi *iş* göndərir və iş təkrar
 * cəhd edə bilər.
 */
object IosDailyReminder : DailyReminderScheduler {

    private const val REQUEST_PREFIX = "votd_slot_"

    /** [IosNotificationCenterDelegate] registry-sindəki modul adı. */
    private const val KIND = "votd"
    private const val KEY_CHAPTER = "chapterNo"
    private const val KEY_VERSE = "verseNo"

    /** iOS sıfır saniyəlik tetikleyicini rədd edir. */
    private const val MIN_DELAY_SECONDS = 1.0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun schedule() {
        scope.launch { sync() }
    }

    override fun cancel() {
        scope.launch { removeScheduled() }
    }

    /** Açılışda çağırılır: növbə dəyişibsə gələcək bildirişlər yenidən qurulur. */
    fun refresh() {
        scope.launch { sync() }
    }

    /**
     * Növbəni oxuyub gələcək yuvaların bildirişlərini yazır.
     *
     * Hər çağırışda əvvəlcə köhnə tələblər silinir: admin növbənin sırasını dəyişəndə köhnə plan
     * qalmamalıdır. Sonra ən yaxın yuvalar yazılır — sayı [VotdNotificationContent] tərəfindən
     * iOS-un 64 tələblik limitindən aşağı saxlanılır.
     */
    suspend fun sync() {
        if (!VersePreferences.getVOTDReminderEnabled()) {
            removeScheduled()
            return
        }

        val upcoming = VotdNotificationContent.upcoming(nowMillis = currentEpochMillis())

        removeScheduled()

        upcoming.forEach { notification -> post(notification) }
    }

    private suspend fun post(notification: VotdNotification) {
        val delaySeconds = (notification.atMillis - currentEpochMillis()) / 1000.0
        if (delaySeconds < MIN_DELAY_SECONDS) return

        val content = UNMutableNotificationContent().apply {
            setTitle(notification.title)
            if (notification.reference.isNotBlank()) setSubtitle(notification.reference)
            setBody(notification.body)
            setUserInfo(
                mapOf(
                    IosNotificationCenterDelegate.KEY_KIND to KIND,
                    KEY_CHAPTER to notification.chapterNo?.toString().orEmpty(),
                    KEY_VERSE to notification.verseNo?.toString().orEmpty(),
                )
            )
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = REQUEST_PREFIX + notification.slotKey,
            content = content,
            trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                timeInterval = delaySeconds,
                repeats = false,
            ),
        )

        add(request)
    }

    /** Yalnız bu modulun yazdığı tələbləri silir — tətbiqin digər bildirişlərinə toxunmur. */
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
                        AppLogger.d("IosDailyReminder: bildiriş qurulmadı — ${error.localizedDescription}")
                    }
                    continuation.resume(error == null)
                }
        }

    /**
     * Toxunuşu oxucuya yönləndirir.
     *
     * ⚠️ `delegate` **artıq burada qurulmur**: `UNUserNotificationCenter.delegate` tək qlobaldır və
     * namaz bildirişləri ikinci sahib gətirdi. İndi ortaq [IosNotificationCenterDelegate] registry-si
     * var; bu funksiya yalnız öz `kind`-ını qeyd edir. Prefiks arqumenti köhnə, `kind`-siz
     * tələblərin tanınması üçündür.
     */
    fun registerTapHandler() {
        IosNotificationCenterDelegate.register(KIND, REQUEST_PREFIX) { info ->
            val chapterNo = (info[KEY_CHAPTER] as? String)?.toIntOrNull()
            val verseNo = (info[KEY_VERSE] as? String)?.toIntOrNull()

            // A hadith carries no verse; tapping it just brings the app up on the home screen.
            if (chapterNo != null && verseNo != null) {
                ReaderUiHooks.openVerse?.invoke(chapterNo, verseNo)
            }
        }
    }
}
