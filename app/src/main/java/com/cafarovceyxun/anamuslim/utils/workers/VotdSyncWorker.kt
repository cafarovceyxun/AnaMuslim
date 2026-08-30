package com.cafarovceyxun.anamuslim.utils.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cafarovceyxun.anamuslim.compose.utils.VerseOfTheDayScheduler
import com.cafarovceyxun.anamuslim.utils.verse.VotdNotificationContent
import com.cafarovceyxun.anamuslim.views.reader.updateAllVotdWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Növbəni Supabase-dən yeniləyir və növbəti yuvaların gecikməli işlərini qurur.
 *
 * İki addım qəsdən ayrıdır: **şəbəkə burada** olur (altı saatdan bir, `NetworkType.CONNECTED`
 * şərti ilə), bildiriş anında isə [VerseOfTheDayWorker] yalnız keşi oxuyur. Əks halda saat 08:00-da
 * internetin olmaması yuvanı susdurardı.
 *
 * Həm də **qaçırılmış** yuvaları çalır: cihaz yatıbsa və ya iş gecikibsə,
 * [VotdNotificationContent.due] pəncərəsinə düşən yuvalar burada göndərilir.
 */
class VotdSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Şəbəkədən oxuyub keşi yeniləyir; sonrakı hər şey keş üzərində işləyir.
        val upcoming = VotdNotificationContent.upcoming(refresh = true)

        VerseOfTheDayScheduler.armUpcomingSlots(applicationContext, upcoming)

        // Vaxtı keçmiş, amma çalınmamış yuvalar (cihaz yatıb, iş gecikib).
        VotdNotificationContent.due(refresh = false).forEach { missed ->
            VerseOfTheDayScheduler.fireNow(applicationContext, missed.date, missed.slotIndex)
        }

        updateAllVotdWidgets(applicationContext)

        Result.success()
    }
}
