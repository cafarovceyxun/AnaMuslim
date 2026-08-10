package com.cafarovceyxun.anamuslim.compose.utils

/**
 * Scheduling for the daily verse-of-the-day reminder.
 *
 * A settable sink rather than `expect`/`actual` for a concrete reason: the Android implementation
 * needs `VerseOfTheDayWorker`, which lives in `:app` and is therefore invisible to shared
 * `androidMain`. Same shape and rationale as the other background-work seams
 * (`RecitationDownloadProvider`, `WbwAudioDownloadProvider`).
 *
 * iOS registers `IosDailyReminder`, which pre-registers a repeating `UNCalendarNotificationTrigger`
 * instead of computing at fire time. Where neither is registered the sink stays inert, so the
 * settings UI toggles the preference without crashing.
 */
interface DailyReminderScheduler {
    /** Starts (or keeps) the once-a-day reminder. Idempotent — Android enqueues with `KEEP`. */
    fun schedule()

    fun cancel()
}

/** Registered at startup: Android `QuranApp.onCreate()`, iOS `initSharedForIos()`. */
object DailyReminderProvider {
    private var provider: (() -> DailyReminderScheduler)? = null

    fun setProvider(value: () -> DailyReminderScheduler) {
        provider = value
    }

    val scheduler: DailyReminderScheduler
        get() = provider?.invoke() ?: NoDailyReminderScheduler
}

private object NoDailyReminderScheduler : DailyReminderScheduler {
    override fun schedule() = Unit
    override fun cancel() = Unit
}
