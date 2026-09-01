package com.cafarovceyxun.anamuslim.compose.utils

/**
 * Namaz bildirişlərinin planlaşdırılması.
 *
 * [DailyReminderScheduler] ilə eyni səbəbdən seam-dir: Android tətbiqi `:app`-dakı `AlarmManager`
 * planlayıcısına və receiver-lərinə söykənir, `shared/androidMain` isə `:app`-ı görmür.
 *
 * ⚠️ Qeydiyyatsız qalanda **çökür** (`error(...)`), inert default yoxdur — hər iki platformada
 * tətbiqi var, ona görə qurulmamış provider yalnız wiring səhvi ola bilər. CLAUDE.md-dəki seam
 * qaydası: «hər platformada tətbiqi olan seam → `?: error(...)`».
 */
interface PrayerReminderScheduler {
    /** Ayarlardan gələn cari vəziyyətə görə bildirişləri (yenidən) qurur. İdempotentdir. */
    fun schedule()

    fun cancel()
}

/** Qeydiyyat: Android `QuranApp.onCreate()`, iOS `initSharedForIos()`. */
object PrayerReminderProvider {
    private var provider: (() -> PrayerReminderScheduler)? = null

    fun setProvider(value: () -> PrayerReminderScheduler) {
        provider = value
    }

    val scheduler: PrayerReminderScheduler
        get() = provider?.invoke()
            ?: error("PrayerReminderProvider qurulmayıb — platforma bootstrap-ında setProvider çağır")
}
