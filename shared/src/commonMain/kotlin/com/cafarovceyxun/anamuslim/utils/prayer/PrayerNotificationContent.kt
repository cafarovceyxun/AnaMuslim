package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.prayerNotificationBody
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.notify.NotificationBudget
import org.jetbrains.compose.resources.getString

/** Göstərilməyə hazır bildiriş. [key] dublikat qoruyucusudur. */
data class PrayerNotification(
    val prayer: Prayer,
    val dateIso: String,
    val key: String,
    val atMillis: Long,
    val title: String,
    val body: String,
)

/**
 * Bildiriş mətnini qurur — **nazik və şərtsiz** qat.
 *
 * Bütün qərarlar (hansı vaxt, nə zaman, artıq çalınıbmı) saf [PrayerNotificationPlan]-dədır və
 * orada test olunur; burada yalnız preference oxunuşu və `getString` var. Bölgü qəsdlidir:
 * `VotdNotificationContent` üçünü bir obyektdə birləşdirdiyi üçün heç vaxt test edilmədi.
 */
object PrayerNotificationContent {

    /** Qaçırılmış bildirişin hələ də mənası olduğu pəncərə — namaz vaxtı tez keçir. */
    const val DEFAULT_GRACE_MILLIS = 60L * 60L * 1000L

    suspend fun upcoming(
        nowMillis: Long = currentEpochMillis(),
        limit: Int = NotificationBudget.PRAYER,
    ): List<PrayerNotification> = PrayerNotificationPlan
        .upcoming(
            settings = PrayerPreferences.getSettings(),
            nowMillis = nowMillis,
            limit = limit,
            delivered = PrayerPreferences.getDelivered(),
        )
        .map { it.toNotification() }

    suspend fun due(
        nowMillis: Long = currentEpochMillis(),
        graceMillis: Long = DEFAULT_GRACE_MILLIS,
    ): List<PrayerNotification> = PrayerNotificationPlan
        .due(
            settings = PrayerPreferences.getSettings(),
            nowMillis = nowMillis,
            graceMillis = graceMillis,
            delivered = PrayerPreferences.getDelivered(),
        )
        .map { it.toNotification() }

    /**
     * Konkret yuva — Android alarm receiver-i bunu oxuyur.
     *
     * Ayar sönülüdürsə və ya vaxt artıq çatdırılıbsa null: alarm təkrar işə düşsə də bildiriş
     * iki dəfə çalmır.
     */
    suspend fun forKey(dateIso: String, prayer: Prayer): PrayerNotification? {
        val settings = PrayerPreferences.getSettings()
        if (!settings.canSchedule || prayer !in settings.notify) return null

        val key = PrayerNotificationPlan.keyOf(dateIso, prayer)
        if (key in PrayerPreferences.getDelivered()) return null

        val point = settings.point ?: return null
        val time = PrayerTimes.calculate(dateIso, point, settings.params)?.get(prayer) ?: return null

        return PrayerNotificationRef(prayer, dateIso, time.atMillis).toNotification()
    }

    suspend fun markDelivered(notification: PrayerNotification) {
        PrayerPreferences.markDelivered(notification.key, currentEpochMillis())
    }

    private suspend fun PrayerNotificationRef.toNotification(): PrayerNotification {
        val name = getString(PrayerUiFormat.labelOf(prayer))

        return PrayerNotification(
            prayer = prayer,
            dateIso = dateIso,
            key = key,
            atMillis = atMillis,
            title = name,
            body = getString(Res.string.prayerNotificationBody, name),
        )
    }
}
