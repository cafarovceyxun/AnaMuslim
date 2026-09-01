package com.cafarovceyxun.anamuslim.utils.receivers

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.cafarovceyxun.anamuslim.compose.utils.PrayerAlarmScheduler
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * İstifadəçi «dəqiq siqnal» icazəsini **verən an** alarmı yenidən qurur.
 *
 * Bu olmasa istifadəçi bizim göstərdiyimiz ayarda icazəni verir, geri qayıdır — və alarm növbəti
 * tətbiq açılışına qədər hələ də qeyri-dəqiq yolda qalır. Yəni bu, «istifadəçi sənin dediyini
 * elədi» anıdır və cavabsız qalmamalıdır.
 *
 * ℹ️ İcazə **geri alınanda** broadcast gəlmir — sistem tətbiqi dayandırır və exact alarmları ləğv
 * edir. Həmin halı hər açılışdakı təkrar qurma örtür (`QuranApp.onCreate`).
 */
@RequiresApi(Build.VERSION_CODES.S)
class ExactAlarmStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            return
        }

        val pending = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                PrayerAlarmScheduler.armNext(appContext)
            } catch (throwable: Throwable) {
                AppLogger.saveError(throwable, "prayer.exact_alarm_state")
            } finally {
                pending.finish()
            }
        }
    }
}
