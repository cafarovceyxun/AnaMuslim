package com.cafarovceyxun.anamuslim.utils.workers

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils.createForegroundInfoFallback
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationAudioFileDownloader
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationAudioRepository
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationDownloadProgressBus
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelManager
import com.cafarovceyxun.anamuslim.utils.mediaplayer.getRecitationAudioFile
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class RecitationBulkDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    private val cancelPendingIntent =
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val reciterId = inputData.getString(KEY_RECITER_ID)
        val kindName = inputData.getString(KEY_KIND)
        val kind = try {
            RecitationAudioKind.valueOf(kindName ?: "")
        } catch (_: Exception) {
            null
        }

        if (reciterId == null || kind == null) {
            return createForegroundInfoFallback(ctx)
        }

        return createEnqueueForegroundInfo(
            "",
            0,
            0,
        )
    }

    override suspend fun doWork(): Result {
        val reciterId = inputData.getString(KEY_RECITER_ID)
        val kindName = inputData.getString(KEY_KIND)
        val kind = try {
            RecitationAudioKind.valueOf(kindName ?: "")
        } catch (_: Exception) {
            null
        }

        val urlTemplate = inputData.getString(KEY_URL_TEMPLATE)
        val displayTitle = inputData.getString(KEY_DISPLAY_TITLE).orEmpty().ifBlank { "Recitation" }

        if (reciterId == null || kind == null || urlTemplate == null) {
            return Result.failure()
        }

        val modelManager = RecitationModelManager

        val pendingChapters = buildList {
            for (chapterNo in QuranMeta.chapterRange) {
                val audioFile = modelManager.getRecitationAudioFile(reciterId, chapterNo)

                if (audioFile.exists() && audioFile.length() > 0L) continue

                val url = RecitationAudioRepository.prepareAudioUrl(urlTemplate, chapterNo)
                    ?: continue

                add(
                    PendingChapter(
                        chapterNo = chapterNo,
                        url = url,
                        outputPath = audioFile.absolutePath,
                    ),
                )
            }
        }

        val total = pendingChapters.size
        setForeground(
            createEnqueueForegroundInfo(
                displayTitle,
                0,
                total.coerceAtLeast(1),
            ),
        )

        val completed = AtomicInteger(0)
        val limitedDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_DOWNLOADS)
        val notificationMutex = kotlinx.coroutines.sync.Mutex()
        var lastNotificationTime = 0L

        suspend fun updateForeground(done: Int) {
            notificationMutex.withLock {
                val now = System.currentTimeMillis()

                if (now - lastNotificationTime >= 2000L || done == total) {
                    lastNotificationTime = now
                    setForeground(createEnqueueForegroundInfo(displayTitle, done, total))
                }
            }
        }

        try {
            withContext(limitedDispatcher) {
                pendingChapters.map { pending ->
                    async {
                        currentCoroutineContext().ensureActive()

                        val outputFile = File(pending.outputPath)
                        val parent = outputFile.parentFile

                        if (parent != null && !parent.exists() && !parent.mkdirs()) {
                            updateForeground(completed.incrementAndGet())
                            return@async
                        }

                        try {
                            RecitationDownloadProgressBus.set(
                                reciterId,
                                pending.chapterNo,
                                0L,
                                -1L,
                            )
                            RecitationAudioFileDownloader.downloadToFile(
                                pending.url,
                                outputFile.absolutePath,
                            ) { consumed, total ->
                                RecitationDownloadProgressBus.set(
                                    reciterId,
                                    pending.chapterNo,
                                    consumed,
                                    total,
                                )
                            }
                        } catch (e: Exception) {
                            outputFile.delete()
                            Log.saveError(e, "RecitationBulkDownload: ${pending.chapterNo}")
                        } finally {
                            RecitationDownloadProgressBus.clear(reciterId, pending.chapterNo)
                        }

                        updateForeground(completed.incrementAndGet())
                    }
                }.awaitAll()
            }

            updateForeground(completed.get())

            // The progress notification is tied to the foreground service, so it disappears the
            // moment this worker ends — leaving no trace that the download ever finished. This one
            // is an ordinary notification, so it stays in the shade.
            notifyCompleted(displayTitle, completed.get())

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun notifyCompleted(displayTitle: String, downloadedCount: Int) {
        val manager = ContextCompat.getSystemService(ctx, NotificationManager::class.java) ?: return

        val notification = NotificationCompat
            .Builder(ctx, NotificationUtils.CHANNEL_ID_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(displayTitle)
            .setContentText(ctx.getString(R.string.strLabelDownloaded))
            .setSubText(
                ctx.getString(
                    R.string.recitationDownloadChaptersProgress,
                    downloadedCount,
                    downloadedCount,
                ),
            )
            .setAutoCancel(true)
            .build()

        // Per reciter, so downloading two of them does not overwrite one another's result.
        manager.notify(COMPLETION_NOTIF_ID_BASE + displayTitle.hashCode(), notification)
    }


    private fun createEnqueueForegroundInfo(
        displayTitle: String,
        completedCount: Int,
        chapterTotal: Int,
    ): ForegroundInfo {
        val builder =
            NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID_DOWNLOADS).apply {
                setAutoCancel(false)
                setOngoing(true)
                setShowWhen(false)
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setSubText(ctx.getString(R.string.textDownloading))
                setContentTitle(displayTitle)
                setContentText(
                    ctx.getString(
                        R.string.recitationDownloadChaptersProgress,
                        completedCount,
                        chapterTotal,
                    ),
                )
                setCategory(NotificationCompat.CATEGORY_PROGRESS)

                val max = chapterTotal.coerceAtLeast(1)
                setProgress(max, completedCount.coerceAtMost(max), false)
            }

        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            cancelPendingIntent,
        )

        return ForegroundInfo(
            id.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private data class PendingChapter(
        val chapterNo: Int,
        val url: String,
        val outputPath: String,
    )

    companion object {
        const val TAG = "recitation_bulk_download"
        private const val BULK_TAG_PREFIX = "recitation_bulk_reciter:"

        private const val MAX_PARALLEL_DOWNLOADS = 6

        /** Kept clear of the ids the app posts elsewhere (see `Codes`). */
        private const val COMPLETION_NOTIF_ID_BASE = 9_100

        private const val KEY_RECITER_ID = "bulk_reciter_id"
        private const val KEY_KIND = "bulk_kind"
        private const val KEY_URL_TEMPLATE = "bulk_url_template"
        private const val KEY_DISPLAY_TITLE = "bulk_display_title"

        const val KEY_BULK_PROGRESS_CHAPTER_NO = "bulk_progress_chapter_no"
        const val KEY_BULK_PROGRESS_RECITER_ID = "bulk_progress_reciter_id"

        fun uniqueWorkName(reciterId: String, kind: RecitationAudioKind): String {
            return "recitation-bulk:$reciterId:${kind.name}"
        }

        fun reciterTag(reciterId: String, kind: RecitationAudioKind): String {
            return "$BULK_TAG_PREFIX${kind.name}:$reciterId"
        }

        fun parseBulkReciterTag(tag: String): Pair<RecitationAudioKind, String>? {
            if (!tag.startsWith(BULK_TAG_PREFIX)) return null

            val rest = tag.removePrefix(BULK_TAG_PREFIX)
            val idx = rest.indexOf(':')

            if (idx <= 0 || idx >= rest.length - 1) return null
            val kindName = rest.substring(0, idx)
            val reciterId = rest.substring(idx + 1)

            val kind = try {
                RecitationAudioKind.valueOf(kindName)
            } catch (_: Exception) {
                return null
            }

            return kind to reciterId
        }

        fun inputData(
            reciterId: String,
            kind: RecitationAudioKind,
            urlTemplate: String,
            displayTitle: String,
        ): Data {
            return workDataOf(
                KEY_RECITER_ID to reciterId,
                KEY_KIND to kind.name,
                KEY_URL_TEMPLATE to urlTemplate,
                KEY_DISPLAY_TITLE to displayTitle,
            )
        }

        fun oneTimeRequest(
            reciterId: String,
            kind: RecitationAudioKind,
            urlTemplate: String,
            displayTitle: String,
        ) = OneTimeWorkRequestBuilder<RecitationBulkDownloadWorker>()
            .setInputData(inputData(reciterId, kind, urlTemplate, displayTitle))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(TAG)
            .addTag(reciterTag(reciterId, kind))
            .build()
    }
}
