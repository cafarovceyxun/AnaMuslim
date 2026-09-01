package com.cafarovceyxun.anamuslim.compose.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotificationContent
import com.cafarovceyxun.anamuslim.utils.receivers.PrayerAlarmReceiver
import com.cafarovceyxun.anamuslim.utils.univ.Codes
import com.cafarovceyxun.anamuslim.utils.workers.PrayerRearmWorker
import com.cafarovceyxun.anamuslim.views.prayer.updateAllPrayerWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Namaz bildirişlərinin Android planlaşdırıcısı.
 *
 * ### Niyə `AlarmManager`, `WorkManager` yox
 * Günün ayəsi qəsdən `WorkManager` işlədir ([VerseOfTheDayScheduler]) — orada bir neçə dəqiqəlik
 * yayınma problem deyil. Namaz üçün isə `setInitialDelay` yararsızdır: Doze rejimində iş
 * maintenance window-a qədər gözləyir və gecə Fəcr bildirişi bir-iki saat gecikə bilər.
 *
 * ### Nərdivan
 * | Şərt | Metod | İcazə |
 * |---|---|---|
 * | `SDK < 31` | `setExactAndAllowWhileIdle` | — |
 * | `canScheduleExactAlarms()` | `setExactAndAllowWhileIdle` | `SCHEDULE_EXACT_ALARM` |
 * | əks halda | `setAndAllowWhileIdle` | — |
 *
 * `setAndAllowWhileIdle` **heç bir icazə istəmir** və Doze-u deşir; sapması bir neçə dəqiqədir.
 * ⚠️ `USE_EXACT_ALARM` **işlədilmir**: Play siyasəti onu əsas funksiyası zəngli saat/təqvim olan
 * tətbiqlərlə məhdudlaşdırır, tətbiq isə artıq hər iki mağazada canlıdır.
 *
 * ### Eyni anda yalnız BİR alarm
 * 35 alarm qurulmur: [armNext] yalnız növbəti vaxtı qurur, [PrayerAlarmReceiver] bildirişi göndərib
 * dərhal növbətisini qurur. Özünü bərpa edən zəncirdir və `PendingIntent` idarəsini triviallaşdırır
 * — bir dəfəyə bir alarm olduğu üçün tək `requestCode` kifayətdir.
 *
 * `WorkManager` burada **xilasetmə qatıdır**, çatdırma qatı deyil: [PrayerRearmWorker] altı saatdan
 * bir alarmın yerində olduğunu yoxlayır və qaçırılmış vaxtı çalır.
 */
object PrayerAlarmScheduler {

    private const val REARM_WORK = "prayer_rearm"
    private const val REARM_INTERVAL_HOURS = 6L
    private const val LOG_TAG = "prayer.alarm"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Seam-dən çağırılan atəş-və-unut giriş nöqtəsi. */
    fun schedule(context: Context) {
        scope.launch { armNext(context) }
        enqueueRearmWorker(context)
        // Ayar dəyişikliyi vidcetdə də dərhal görünsün — o, 30 dəqiqəlik dövrü gözləməsin.
        updateAllPrayerWidgets(context)
    }

    fun cancel(context: Context) {
        alarmManager(context)?.cancel(alarmIntent(context))
        WorkManager.getInstance(context).cancelUniqueWork(REARM_WORK)
        updateAllPrayerWidgets(context)
    }

    /**
     * Növbəti bildirişi qurur; qurulası heç nə yoxdursa alarmı ləğv edir.
     *
     * `suspend`, çünki mətn qurulması `getString` çağırır — receiver-lər bunu `goAsync()` içində
     * çağırmalıdır.
     */
    suspend fun armNext(context: Context) {
        val manager = alarmManager(context) ?: return
        val next = PrayerNotificationContent.upcoming(limit = 1).firstOrNull()

        if (next == null) {
            manager.cancel(alarmIntent(context))
            return
        }

        val intent = alarmIntent(context)

        runCatching {
            if (canBeExact(context, manager)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
            } else {
                // İcazəsiz yol. Doze-u yenə deşir, sadəcə bir neçə dəqiqə sapa bilər.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
            }
        }.onFailure {
            // `SecurityException`: istifadəçi icazəni biz oxuduqdan sonra geri alıb.
            AppLogger.saveError(it, LOG_TAG)
            runCatching {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
            }
        }
    }

    /**
     * Nərdivanın **yeganə** yoxlanma nöqtəsi.
     *
     * İki yerdə yoxlanan şərt gec-tez ayrılır; UI-dəki «icazə ver» sətri istisnadır, o da yalnız
     * göstərmək üçün oxuyur.
     */
    private fun canBeExact(context: Context, manager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        Codes.NOTIF_ID_PRAYER_BASE,
        Intent(context, PrayerAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Altı saatlıq xilasetmə. Şəbəkə şərti **yoxdur** — hesablama tamamilə lokaldır. */
    private fun enqueueRearmWorker(context: Context) {
        val request = PeriodicWorkRequestBuilder<PrayerRearmWorker>(
            repeatInterval = REARM_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(Constraints.Builder().build()).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REARM_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
