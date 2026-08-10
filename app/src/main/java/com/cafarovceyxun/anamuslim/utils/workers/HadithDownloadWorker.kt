package com.cafarovceyxun.anamuslim.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.managers.HadithSyncEngine
import com.cafarovceyxun.anamuslim.utils.univ.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HadithDownloadWorker(
    val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(ctx.getString(R.string.textDownloading), 0)
    }

    /**
     * The sync itself now lives in the multiplatform [HadithSyncEngine]; this worker supplies only
     * what WorkManager brings — background survival, the progress notification and cancellation.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForeground(createForegroundInfo(ctx.getString(R.string.textDownloading), 0))

        try {
            HadithSyncEngine.sync { label, progress -> updateProgress(label, progress) }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun updateProgress(label: String, progress: Int) {
        val currentProgress = if (progress > 100) 100 else progress
        setProgress(workDataOf("progress" to currentProgress, "label" to label))
        setForeground(createForegroundInfo(label, currentProgress))
    }

    private fun createForegroundInfo(label: String, progress: Int): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(ctx.getString(R.string.strTitleDownloadHadiths))
            setContentText(label)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress, false)
        }

        val activityIntent = Intent(ctx, ActivitySettings::class.java).apply {
            putExtra(Keys.NAV_DESTINATION, SettingRoutes.TRANSLATIONS)
        }
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(ctx, 1001, activityIntent, flag)
        builder.setContentIntent(pendingIntent)

        val cancelIntent = WorkManager.getInstance(ctx).createCancelPendingIntent(id)
        builder.addAction(R.drawable.dr_icon_close, ctx.getString(R.string.strLabelCancel), cancelIntent)

        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1001, notification)
        }
    }
}
