package com.cafarovceyxun.anamuslim.utils.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cafarovceyxun.anamuslim.compose.utils.PrayerAlarmScheduler
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotificationContent

/**
 * Altı saatlıq **xilasetmə** qatı — çatdırma qatı deyil.
 *
 * İki işi görür: (a) alarm hər hansı səbəbdən itibsə yenidən qurur (OEM təmizləyicisi, `force stop`,
 * icazə dəyişikliyi), (b) pəncərədə qalmış qaçırılmış vaxtı çalır.
 *
 * Şəbəkə şərti **yoxdur**: namaz vaxtı koordinat və riyaziyyatdan hesablanır, internet lazım deyil.
 * Günün ayəsinin sinxronizasiyası isə `NetworkType.CONNECTED` tələb edir — fərq qəsdlidir.
 */
class PrayerRearmWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            PrayerNotificationContent.due().forEach { PrayerNotificationContent.markDelivered(it) }
            PrayerAlarmScheduler.armNext(applicationContext)
            Result.success()
        } catch (throwable: Throwable) {
            AppLogger.saveError(throwable, "prayer.rearm")
            Result.success()
        }
    }
}
