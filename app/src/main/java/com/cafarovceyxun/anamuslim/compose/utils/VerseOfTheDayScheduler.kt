package com.cafarovceyxun.anamuslim.compose.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.currentLocalDateIsoString
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContentSlots
import com.cafarovceyxun.anamuslim.utils.verse.VotdNotification
import com.cafarovceyxun.anamuslim.utils.workers.VerseOfTheDayWorker
import com.cafarovceyxun.anamuslim.utils.workers.VotdSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Günün ayəsi bildirişlərinin Android planlaşdırıcısı.
 *
 * İki qat:
 * 1. **Sinxronizasiya** — [VotdSyncWorker] altı saatdan bir şəbəkəyə çıxır, növbəni keşləyir və
 *    növbəti yuvaların işlərini qurur. Şəbəkə yalnız buradadır.
 * 2. **Yuvalar** — hər biri gecikməli tək işdir ([VerseOfTheDayWorker]) və öz `(tarix, slot)`
 *    cütünü daşıyır. Gündə [DailyContentSlots.COUNT] yuva; keşdən oxuduğu üçün internet tələb etmir.
 *
 * `AlarmManager` **qəsdən işlədilmir**: dəqiq siqnal Android 12+-da ayrıca icazə istəyir, gündəlik
 * xatırlatma üçün isə WorkManager-in bir neçə dəqiqəlik yayınması problem deyil.
 */
object VerseOfTheDayScheduler {
    private const val SYNC_ID = "votd_reminder"
    private const val SLOT_WORK_PREFIX = "votd_slot_"

    /**
     * Sinxronizasiya nə qədər tez-tez işləyir. Növbə istənilən saatda dəyişə bilər (admin yeni
     * element əlavə edir və ya sırasını dəyişir), ona görə gündə bir dəfə azdır.
     */
    private const val SYNC_INTERVAL_HOURS = 6L

    /**
     * Nə qədər irəli yuva qurulur. Sinxronizasiya altı saatdan bir işlədiyi üçün 26 saat kifayət
     * qədər ehtiyatlıdır; daha uzunu WorkManager növbəsini boş yerə doldurardı.
     */
    private const val ARM_HORIZON_MILLIS = 26L * 60L * 60L * 1000L

    fun scheduleDailyNotification(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<VotdSyncWorker>(
            repeatInterval = SYNC_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(
            // Hər işə salma Supabase sorğusu ilə başlayır; oflayn yalnız oyanmanı yandırardı.
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                SYNC_ID,
                // UPDATE, not KEEP: installs that already enqueued the old work would otherwise
                // keep it forever.
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )
    }

    fun cancelDailyNotification(context: Context) {
        val manager = WorkManager.getInstance(context)

        manager.cancelUniqueWork(SYNC_ID)

        // Qurulmuş yuvalar da getməlidir, yoxsa xatırlatma söndürüldükdən sonra da çalardılar.
        // Adları məlum olduğu üçün üfüqdəki bütün günlərin yuvaları ləğv edilir.
        allSlotNames().forEach { manager.cancelUniqueWork(it) }
    }

    /**
     * [upcoming] siyahısındakı yuvalar üçün gecikməli işlər qurur.
     *
     * Ad `(tarix, slot)`-dan gəlir və `REPLACE` işlədilir: eyni yuva təkrar qurulanda köhnəsi əvəz
     * olunur, yəni admin növbəni dəyişəndə köhnə plan qalmır.
     */
    fun armUpcomingSlots(context: Context, upcoming: List<VotdNotification>) {
        val manager = WorkManager.getInstance(context)
        val now = currentEpochMillis()

        upcoming
            .filter { it.atMillis > now && it.atMillis - now <= ARM_HORIZON_MILLIS }
            .forEach { notification ->
                manager.enqueueUniqueWork(
                    slotWorkName(notification.date, notification.slotIndex),
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<VerseOfTheDayWorker>()
                        .setInitialDelay(notification.atMillis - now, TimeUnit.MILLISECONDS)
                        .setInputData(slotData(notification.date, notification.slotIndex))
                        .build(),
                )
            }
    }

    /** Qaçırılmış yuvanı dərhal çalır — [VotdSyncWorker] gecikmiş yuvaları belə tutur. */
    fun fireNow(context: Context, date: String, slotIndex: Int) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            slotWorkName(date, slotIndex),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<VerseOfTheDayWorker>()
                .setInputData(slotData(date, slotIndex))
                .build(),
        )
    }

    private fun slotData(date: String, slotIndex: Int): Data = Data.Builder()
        .putString(VerseOfTheDayWorker.KEY_DATE, date)
        .putInt(VerseOfTheDayWorker.KEY_SLOT, slotIndex)
        .build()

    private fun slotWorkName(date: String, slotIndex: Int) = "$SLOT_WORK_PREFIX$date#$slotIndex"

    /**
     * Ləğv üçün mümkün yuva adları: bugündən üfüqün sonuna qədər (iki gün) bütün `(tarix, slot)`
     * cütləri. `cancelAllWorkByTag` işlədilmir — o, sinxronizasiya işini də aparardı.
     */
    private fun allSlotNames(): List<String> {
        val today = currentLocalDateIsoString()

        return (0..2).flatMap { dayOffset ->
            val date = IsoDate.plusDays(today, dayOffset)
                ?: return@flatMap emptyList()

            (0 until DailyContentSlots.COUNT).map { slot -> slotWorkName(date, slot) }
        }
    }
}
