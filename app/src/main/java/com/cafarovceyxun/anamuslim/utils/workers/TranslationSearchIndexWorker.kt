package com.cafarovceyxun.anamuslim.utils.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.search.TranslationSearchIndexer
import com.cafarovceyxun.anamuslim.utils.app.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationSearchIndexWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_MODE = "mode"
        const val KEY_SLUG = "slug"
        const val MODE_SLUG = "slug"
        const val MODE_ALL = "all"
        const val MODE_REMOVE = "remove"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationUtils.CHANNEL_ID_DEFAULT
        )
            .setContentTitle("Indexing translations data")
            .setContentText("Preparing...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1001, notification)
    }

    /**
     * The indexing itself now lives in the multiplatform [TranslationSearchIndexer]; this worker
     * only supplies what WorkManager brings — background survival, expedition and cancellation.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        when (inputData.getString(KEY_MODE)) {
            MODE_REMOVE -> {
                val slug = inputData.getString(KEY_SLUG) ?: return@withContext Result.failure()
                TranslationSearchIndexer.removeSlug(slug)
                Result.success()
            }

            MODE_SLUG -> {
                val slug = inputData.getString(KEY_SLUG) ?: return@withContext Result.failure()
                TranslationSearchIndexer.indexSlugIfNeeded(slug)
                Result.success()
            }

            MODE_ALL -> {
                TranslationSearchIndexer.syncAll(isStopped = { isStopped })
                Result.success()
            }

            else -> Result.failure()
        }
    }
}
