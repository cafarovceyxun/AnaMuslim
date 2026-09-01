package com.cafarovceyxun.anamuslim.utils.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cafarovceyxun.anamuslim.compose.utils.PrayerAlarmScheduler
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Yenidən başlanğıcdan sonra alarmı bərpa edir.
 *
 * ⚠️ **Məcburidir.** `androidx.work`-un `RescheduleReceiver`-i `BOOT_COMPLETED`-i tutur, amma yalnız
 * **WorkManager işlərini** bərpa edir; `AlarmManager` alarmları yenidən başlanğıcda sistem tərəfindən
 * silinir və onları geri qoyan yalnız bu receiver-dir. Günün ayəsi bu qata ehtiyac duymur, çünki
 * o, tamamilə WorkManager üzərindədir.
 *
 * `MY_PACKAGE_REPLACED` də dinlənilir: tətbiq yenilənəndə də alarmlar itir.
 */
class PrayerBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pending = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                PrayerAlarmScheduler.armNext(appContext)
            } catch (throwable: Throwable) {
                AppLogger.saveError(throwable, "prayer.boot")
            } finally {
                pending.finish()
            }
        }
    }
}
