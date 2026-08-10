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
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils.createForegroundInfoFallback
import com.cafarovceyxun.anamuslim.utils.reader.ScriptFontInstaller
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import com.cafarovceyxun.anamuslim.utils.univ.Keys

class ScriptFontsDownloadWorker(
    private val ctx: Context, params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    companion object {
        const val KEY_SCRIPT = "script_key"
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val scriptKey = inputData.getString(KEY_SCRIPT)
            ?: return createForegroundInfoFallback(ctx)

        return createForegroundInfo(scriptKey, 0, false)
    }

    /**
     * Downloading and unpacking now live in the multiplatform [ScriptFontInstaller] (okio +
     * a shared tar reader, replacing Apache Commons Compress, which was JVM-only); the worker keeps
     * WorkManager's part — background survival, the progress notification and cancellation.
     */
    override suspend fun doWork(): Result {
        val scriptKey = inputData.getString(KEY_SCRIPT) ?: return Result.failure()

        updateProgres(scriptKey, 0)

        return try {
            ScriptFontInstaller.install(scriptKey) { progress -> updateProgres(scriptKey, progress) }
            Result.success()
        } catch (e: Exception) {
            Log.saveError(e, "KFQPCScriptFontsDownloadWorker")
            Result.failure()
        }
    }

    suspend fun updateProgres(
        script: String,
        progress: Int,
    ) {
        setForeground(
            createForegroundInfo(
                script,
                if (progress <= 100) null else progress,
                progress > 100
            )
        )

        setProgress(workDataOf(KEY_PROGRESS to progress))
    }

    private fun createForegroundInfo(
        scriptKey: String, progress: Int?, isExtracting: Boolean
    ): ForegroundInfo {
        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx, scriptKey.hashCode(), Intent(ctx, ActivitySettings::class.java).putExtra(
                Keys.NAV_DESTINATION, SettingRoutes.SCRIPT
            ), flag
        )

        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val builder = NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(R.string.msgDownloadingFonts))
            .setContentText(scriptKey.getQuranScriptName())
            .setProgress(100, progress ?: 0, progress == null || isExtracting).setSubText(
                if (isExtracting) ctx.getString(R.string.msgExtractingFonts)
                else if (progress != null) "$progress%" else ctx.getString(R.string.textDownloading)
            ).setOngoing(true).setShowWhen(false).setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS).addAction(
                R.drawable.dr_icon_close, ctx.getString(R.string.strLabelCancel), cancelIntent
            )

        return ForegroundInfo(
            scriptKey.hashCode(), builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
