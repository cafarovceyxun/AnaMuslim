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
import com.cafarovceyxun.anamuslim.api.resolveInventoryUrl
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationAudioFileDownloader
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.ActivitySettings
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.db.DatabaseProvider
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasFiles
import okio.Path.Companion.toPath
import com.cafarovceyxun.anamuslim.utils.reader.atlas.SharedAtlasImporter
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils.createForegroundInfoFallback
import com.cafarovceyxun.anamuslim.utils.reader.toAtlasBundleDownloadKey
import com.cafarovceyxun.anamuslim.utils.univ.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class AtlasDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val scriptKey = inputData.getString("scriptKey") ?: return createForegroundInfoFallback(ctx)

        return createForegroundInfo(scriptKey, 0)
    }

    override suspend fun doWork(): Result {
        val scriptKey = inputData.getString("scriptKey")
        val densityLevel = inputData.getInt("densityLevel", -1)

        if (scriptKey == null || densityLevel == -1) {
            return Result.failure()
        }

        setForeground(createForegroundInfo(scriptKey, 0))

        return try {
            downloadAndStore(scriptKey, densityLevel)
            Result.success()
        } catch (e: Exception) {
            val msg = e.message ?: if (e is IOException) {
                "Atlas download failed"
            } else {
                "Atlas import failed"
            }
            Result.failure(workDataOf("error" to msg))
        }
    }

    private suspend fun downloadAndStore(
        scriptKey: String,
        densityLevel: Int,
    ) = withContext(Dispatchers.IO) {
        val tmpFile = java.io.File(AtlasFiles.tempFile("$scriptKey.tmp").toString())

        try {
            downloadGithubRawContentToFile(
                url = "ghraw://AlfaazPlus/QuranAppInventory/master/atlas/${scriptKey.toAtlasBundleDownloadKey()}/${densityLevel}x.zip",
                dest = tmpFile,
            ) { progress ->
                if (!isStopped) {
                    setProgressAsync(workDataOf("progress" to (progress ?: 0)))
                    setForeground(createForegroundInfo(scriptKey, progress))
                }
            }

            val db = DatabaseProvider.getExternalQuranDatabase(ctx)
            SharedAtlasImporter.importFromZip(tmpFile.absolutePath.toPath(), scriptKey, db)
        } catch (e: Exception) {
            Log.saveError(e, "AtlasDownloadWorker.downloadAndStore")
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    private fun createForegroundInfo(
        scriptKey: String,
        progress: Int?,
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle(ctx.getString(R.string.textDownloading))
            setContentText(scriptKey)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress ?: 0, progress == null)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(ctx, ActivitySettings::class.java).apply {
            putExtra(Keys.NAV_DESTINATION, SettingRoutes.SCRIPT)
        }

        builder.setContentIntent(
            PendingIntent.getActivity(
                ctx,
                scriptKey.hashCode(),
                activityIntent,
                flag,
                ),
            )

        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
        )

        return ForegroundInfo(
            scriptKey.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}

/**
 * Streams an inventory URL to [dest], reporting whole-percent progress.
 *
 * The transfer itself is the shared [RecitationAudioFileDownloader] (okio, `.part` + atomic move);
 * this wrapper keeps the `java.io.File` signature the worker uses, resolves the `ghraw://` mirror
 * scheme, and throttles progress to one update every 2s — the shared downloader ticks every 200ms,
 * which is far more often than a foreground notification should be rebuilt.
 */
suspend fun downloadGithubRawContentToFile(
    url: String,
    dest: File,
    setProgress: suspend (Int?) -> Unit,
) {
    var lastUpdateTime = 0L

    RecitationAudioFileDownloader.downloadToFile(
        resolveInventoryUrl(url),
        dest.absolutePath,
    ) { consumed, total ->
        val now = System.currentTimeMillis()
        val isFinished = total > 0L && consumed == total

        if (now - lastUpdateTime >= 2000L || isFinished) {
            lastUpdateTime = now
            setProgress(if (total > 0L) ((consumed * 100L) / total).toInt() else null)
        }
    }
}
