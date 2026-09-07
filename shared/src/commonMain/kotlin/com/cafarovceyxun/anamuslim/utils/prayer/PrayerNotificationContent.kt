package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.prayerNotificationBody
import com.cafarovceyxun.anamuslim.resources.prayerReminderBody
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
    /** Bu vaxt üçün seçilmiş səs — platforma qatı kanalı/fayl adını buradan alır. */
    val sound: AdhanSound = AdhanSound.DEFAULT,
    /** 0 = vaxtın özü; >0 = əvvəlcədən xəbərdarlıq. Platforma qatı id-ni buna görə ayırır. */
    val leadMinutes: Int = 0,
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
    ): List<PrayerNotification> {
        val settings = PrayerPreferences.getSettings()

        return PrayerNotificationPlan
            .upcoming(
                settings = settings,
                nowMillis = nowMillis,
                limit = limit,
                delivered = PrayerPreferences.getDelivered(),
            )
            .map { it.toNotification(settings) }
    }

    suspend fun due(
        nowMillis: Long = currentEpochMillis(),
        graceMillis: Long = DEFAULT_GRACE_MILLIS,
    ): List<PrayerNotification> {
        val settings = PrayerPreferences.getSettings()

        return PrayerNotificationPlan
            .due(
                settings = settings,
                nowMillis = nowMillis,
                graceMillis = graceMillis,
                delivered = PrayerPreferences.getDelivered(),
            )
            .map { it.toNotification(settings) }
    }

    /**
     * Konkret yuva — Android alarm receiver-i bunu oxuyur.
     *
     * Ayar sönülüdürsə və ya vaxt artıq çatdırılıbsa null: alarm təkrar işə düşsə də bildiriş
     * iki dəfə çalmır.
     */
    suspend fun forKey(dateIso: String, prayer: Prayer): PrayerNotification? {
        val settings = PrayerPreferences.getSettings()
        if (!settings.canSchedule || prayer !in settings.notify) return null

        val key = PrayerNotificationPlan.keyOf(dateIso, prayer)  // yalnız vaxtın özü
        if (key in PrayerPreferences.getDelivered()) return null

        val point = settings.point ?: return null
        val time = PrayerTimes.calculate(dateIso, point, settings.params)?.get(prayer) ?: return null

        return PrayerNotificationRef(prayer, dateIso, time.atMillis).toNotification(settings)
    }

    suspend fun markDelivered(notification: PrayerNotification) {
        PrayerPreferences.markDelivered(notification.key, currentEpochMillis())
    }

    private suspend fun PrayerNotificationRef.toNotification(
        settings: PrayerSettings,
    ): PrayerNotification {
        // Ad iki dəfə düzəldilir:
        //  1. Gün **yerli**dir, [dateIso] deyil — plan günləri UTC ilə açarlayır və uzaq qurşaqlarda
        //     (UTC+13/+14) yerli cümə günortası hələ UTC cümə axşamına düşür, ad «Zöhr» qalardı.
        //  2. Gün **namazın öz anındandır**, bildirişin anından yox — gecə yarısına yaxın düşən
        //     xəbərdarlıq bir gün geriyə sürüşüb «Cümə»ni itirərdi.
        val prayerAtMillis = atMillis + leadMinutes * 60_000L
        val name = getString(
            PrayerUiFormat.notificationLabelOf(prayer, PrayerUiFormat.localDate(prayerAtMillis)),
        )

        val sound = settings.soundOf(prayer)

        return PrayerNotification(
            prayer = prayer,
            dateIso = dateIso,
            key = key,
            atMillis = atMillis,
            title = name,
            body = if (leadMinutes > 0) {
                getString(Res.string.prayerReminderBody, leadMinutes, name)
            } else {
                getString(Res.string.prayerNotificationBody, name)
            },
            // Xəbərdarlıqda əzan çalınmır — on dəqiqə əvvəl tam əzan yanlış siqnaldır. Amma
            // istifadəçi həmin vaxtı səssiz seçibsə xəbərdarlıq da səssiz qalır.
            sound = if (leadMinutes > 0 && sound != AdhanSound.SILENT) AdhanSound.DEFAULT else sound,
            leadMinutes = leadMinutes,
        )
    }
}
