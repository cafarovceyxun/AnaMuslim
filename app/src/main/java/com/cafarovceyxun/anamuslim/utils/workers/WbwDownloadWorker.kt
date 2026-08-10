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
import com.cafarovceyxun.anamuslim.api.models.wbw.WbwLanguageInfo
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils.createForegroundInfoFallback
import com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwManager
import com.cafarovceyxun.anamuslim.utils.reader.wbw.WbwPackInstaller
import com.cafarovceyxun.anamuslim.utils.univ.Keys
import kotlinx.serialization.json.Json

class WbwDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {


    override suspend fun getForegroundInfo(): ForegroundInfo {
        val wbwInfoJson = inputData.getString("wbwInfo") ?: return createForegroundInfoFallback(ctx)
        val info = Json.decodeFromString<WbwLanguageInfo>(wbwInfoJson)

        return createForegroundInfo(info, 0)
    }

    override suspend fun doWork(): Result {
        val wbwInfoJson = inputData.getString("wbwInfo") ?: return Result.failure()
        val info = Json.decodeFromString<WbwLanguageInfo>(wbwInfoJson)

        setForeground(createForegroundInfo(info, 0))

        return try {
            downloadAndStore(info)
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "WBW download failed")))
        }
    }

    /**
     * Fetching, gunzipping, parsing and storing the pack now lives in the multiplatform
     * [WbwPackInstaller]; the worker keeps what WorkManager brings — background survival, the
     * progress notification and cancellation — plus the Android-only version store.
     */
    private suspend fun downloadAndStore(info: WbwLanguageInfo) {
        val version = WbwPackInstaller.install(info) { progress ->
            if (!isStopped) {
                setProgressAsync(workDataOf("progress" to (progress ?: 0)))
                setForeground(createForegroundInfo(info, progress))
            }
        }

        WbwManager.markResourceVersion(context = ctx, id = info.id, version = version)
    }

    private fun createForegroundInfo(
        info: WbwLanguageInfo,
        progress: Int?
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(ctx.getString(R.string.textDownloading))
            setContentText(info.langName)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress ?: 0, progress == null)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(ctx, ActivitySettings::class.java).apply {
            putExtra(Keys.NAV_DESTINATION, SettingRoutes.WWB)
        }

        builder.setContentIntent(
            PendingIntent.getActivity(
                ctx,
                info.id.hashCode(),
                activityIntent,
                flag
            )
        )

        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        )

        return ForegroundInfo(
            info.id.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
