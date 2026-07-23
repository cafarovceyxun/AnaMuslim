package com.cafarovceyxun.anamuslim.compose.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cafarovceyxun.anamuslim.utils.workers.VerseOfTheDayWorker
import java.util.concurrent.TimeUnit

object VerseOfTheDayScheduler {
    private const val ID = "votd_reminder"

    /**
     * Four polls a day. The content is published to Supabase by an admin at no fixed hour, so a
     * single daily check could sit on stale content for almost 24 hours; the worker itself stays
     * silent on the polls that find nothing new.
     */
    private const val REPEAT_INTERVAL_HOURS = 6L

    fun scheduleDailyNotification(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<VerseOfTheDayWorker>(
            repeatInterval = REPEAT_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(
            // Every run starts with a Supabase request; offline it could only burn a wake-up.
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                ID,
                // UPDATE, not KEEP: installs that already enqueued the old 24-hour work would
                // otherwise keep it forever.
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )
    }

    fun cancelDailyNotification(context: Context) {
        WorkManager
            .getInstance(context)
            .cancelUniqueWork(ID)
    }
}
