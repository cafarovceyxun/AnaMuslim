package com.cafarovceyxun.anamuslim.utils.receivers

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.ActivityPrayerTimes
import com.cafarovceyxun.anamuslim.compose.utils.PrayerAlarmScheduler
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotification
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotificationContent
import com.cafarovceyxun.anamuslim.utils.univ.Codes
import com.cafarovceyxun.anamuslim.views.prayer.updateAllPrayerWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Alarm çalır → qaçırılmış vaxtları göstər → **dərhal** növbətini qur.
 *
 * Zəncir özünü bərpa edir: hər bildiriş bir sonrakını doğurur, ona görə eyni anda yalnız bir alarm
 * saxlanılır.
 *
 * ⚠️ `goAsync()` **məcburidir**: mətn qurulması `getString` (suspend) çağırır və növbəti alarm
 * preference oxuyur. `onReceive`-in əsas thread-ində bunları sinxron etmək ANR riski deməkdir.
 * `goAsync` təxminən 10 saniyə verir, bizim iş isə millisaniyələrlədir.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                // Qaçırılmışlar da göstərilir: cihaz yatıb qalsa alarm gecikə bilər, istifadəçi isə
                // vaxtın keçdiyini bilməlidir.
                PrayerNotificationContent.due().forEach { notification ->
                    show(appContext, notification)
                    PrayerNotificationContent.markDelivered(notification)
                }

                PrayerAlarmScheduler.armNext(appContext)
                // Vidcet növbəti vaxtı göstərir — bildirişlə birlikdə təzələnməlidir.
                updateAllPrayerWidgets(appContext)
            } catch (throwable: Throwable) {
                AppLogger.saveError(throwable, "prayer.alarm.receiver")
            } finally {
                pending.finish()
            }
        }
    }

    private fun show(context: Context, notification: PrayerNotification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        val notificationId = Codes.NOTIF_ID_PRAYER_BASE + notification.prayer.ordinal

        // ⚠️ requestCode = notificationId. Eyni requestCode ilə `FLAG_UPDATE_CURRENT` qonşu
        // bildirişlərin extra-larını əzərdi (VerseOfTheDayWorker-dəki eyni tələ).
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, ActivityPrayerTimes::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val built = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_PRAYER)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(notificationId, built) }
            .onFailure { AppLogger.saveError(it, "prayer.alarm.notify") }
    }
}
