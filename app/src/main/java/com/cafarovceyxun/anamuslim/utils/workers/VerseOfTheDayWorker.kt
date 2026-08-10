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
 * Posts the daily verse/hadith notification.
 *
 * *What* to show is decided by the shared [VotdNotificationContent], which reads the published
 * `daily_content` row from Supabase; this worker contributes only what WorkManager/Android actually
 * bring — background survival and the notification post itself. The iOS side feeds the same builder
 * into `UNUserNotificationCenter`.
 *
 * Runs every six hours because the row may be published at any hour, and stays quiet on the polls
 * that find nothing new: `buildIfUnseen` returns null until the content actually changes.
 */
class VerseOfTheDayWorker constructor(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Null covers both "nothing published" and "already notified about this one".
        val content = VotdNotificationContent.buildIfUnseen()
            ?: return@withContext Result.success()

        sendNotification(content)
        // Only after the post, so a failure is retried by the next poll instead of being swallowed.
        VotdNotificationContent.markNotified(content)

        // This poll is the earliest moment the app learns that the published verse changed, so the
        // home screen widget is refreshed here instead of waiting out its own update period.
        updateAllVotdWidgets(applicationContext)

        return@withContext Result.success()
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

        val pendingIntent = PendingIntent.getActivity(
            context,
            Codes.NOTIF_ID_VOTD,
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

        manager.notify(Codes.NOTIF_ID_VOTD, notification)
    }
}
