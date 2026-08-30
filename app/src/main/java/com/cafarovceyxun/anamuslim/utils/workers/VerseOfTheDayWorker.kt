package com.cafarovceyxun.anamuslim.utils.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.MainActivity
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams
import com.cafarovceyxun.anamuslim.utils.reader.toIntent
import com.cafarovceyxun.anamuslim.utils.univ.Codes
import com.cafarovceyxun.anamuslim.utils.verse.VotdNotification
import com.cafarovceyxun.anamuslim.utils.verse.VotdNotificationContent
import com.cafarovceyxun.anamuslim.views.reader.updateAllVotdWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Növbənin **bir yuvasını** çalır.
 *
 * Gündə beş belə iş olur; hər biri öz `(tarix, slot)` cütü ilə gecikməli olaraq
 * [com.cafarovceyxun.anamuslim.compose.utils.VerseOfTheDayScheduler] tərəfindən qurulur. *Nə*
 * göstəriləcəyini paylaşılan [VotdNotificationContent] deyir; bu iş yalnız Android-in verdiyi şeyi
 * əlavə edir — fon icazəsi və bildirişin özü. iOS tərəfdə eyni məzmun
 * `UNUserNotificationCenter`-ə verilir.
 *
 * ⚠️ Şəbəkə tələb etmir: məzmun [com.cafarovceyxun.anamuslim.repository.supabase.DailyContentRepository]
 * keşindən oxunur, keşi isə [VotdSyncWorker] altı saatdan bir yeniləyir. Bildiriş saatında
 * internetin olmaması yuvanı susdurmamalıdır.
 */
class VerseOfTheDayWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val date = inputData.getString(KEY_DATE) ?: return@withContext Result.success()
        val slot = inputData.getInt(KEY_SLOT, -1)
        if (slot < 0) return@withContext Result.success()

        // Null: yuva boşdur, artıq bildirilib, və ya istifadəçi xatırlatmanı söndürüb.
        val content = VotdNotificationContent.forSlot(date, slot)
            ?: return@withContext Result.success()

        sendNotification(content)
        // Yalnız göndərişdən sonra, ki uğursuz cəhdi növbəti yoxlama təkrarlasın.
        VotdNotificationContent.markDelivered(content)

        // Bildiriş anı vidcetin də köhnəldiyi andır.
        updateAllVotdWidgets(applicationContext)

        Result.success()
    }

    private fun sendNotification(content: VotdNotification) {
        val context = applicationContext

        val manager = ContextCompat.getSystemService(
            context, NotificationManager::class.java
        ) ?: return

        val chapterNo = content.chapterNo
        val verseNo = content.verseNo

        // A hadith has no reader destination, so it opens the app on the home screen instead.
        val contentIntent = if (chapterNo != null && verseNo != null) {
            ReaderLaunchParams(
                data = ReaderIntentData.FullChapter(
                    chapterNo,
                    ChapterVersePair(chapterNo, verseNo),
                ),
                slugs = content.slugs,
            ).toIntent().apply {
                setClass(context, MainActivity::class.java)
                action = INTENT_ACTION_OPEN_READER
            }
        } else {
            Intent(context, MainActivity::class.java)
        }

        // Hər yuvanın öz id-si var: eyni request code ilə `FLAG_UPDATE_CURRENT` qonşu yuvaların
        // PendingIntent-lərinin extra-larını da əvəzləyərdi (hamısı eyni ayəyə aparardı).
        val notificationId = Codes.NOTIF_ID_VOTD_SLOT_BASE + content.slotIndex

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat
            .Builder(applicationContext, NotificationUtils.CHANNEL_ID_VOTD)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(
                NotificationCompat
                    .BigTextStyle()
                    .bigText(content.body)
            )
            .setSubText(content.reference.takeIf { it.isNotBlank() })
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }

    companion object {
        const val KEY_DATE = "date"
        const val KEY_SLOT = "slot"
    }
}
