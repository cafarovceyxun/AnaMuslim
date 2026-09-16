package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.adhkarEveningBody
import com.cafarovceyxun.anamuslim.resources.adhkarEveningTitle
import com.cafarovceyxun.anamuslim.resources.adhkarMorningBody
import com.cafarovceyxun.anamuslim.resources.adhkarMorningTitle
import com.cafarovceyxun.anamuslim.resources.prayerNotificationBody
import com.cafarovceyxun.anamuslim.resources.prayerFollowUpBody
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
    /**
     * İşarəli sürüşmə: `0` = vaxtın özü, `>0` = əvvəlcədən xəbərdarlıq, `<0` = sonrakı xatırlatma.
     * Platforma qatı bildiriş id-sini buna görə ayırır.
     */
    val offsetMinutes: Int = 0,
    /** Zikr yuvası; `null` = namaz bildirişi. Platforma qatı id-ni və toxunuş hədəfini buna görə seçir. */
    val adhkar: AdhkarSlot? = null,
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
        adhkar?.let { return toAdhkarNotification(it) }

        // Ad iki dəfə düzəldilir:
        //  1. Gün **yerli**dir, [dateIso] deyil — plan günləri UTC ilə açarlayır və uzaq qurşaqlarda
        //     (UTC+13/+14) yerli cümə günortası hələ UTC cümə axşamına düşür, ad «Zöhr» qalardı.
        //  2. Gün **namazın öz anındandır**, bildirişin anından yox — gecə yarısına yaxın düşən
        //     xəbərdarlıq bir gün geriyə sürüşüb «Cümə»ni itirərdi. Sonrakı xatırlatmada sürüşmə
        //     mənfidir, ona görə eyni düstur onu da geri qaytarır.
        val prayerAtMillis = atMillis + offsetMinutes * 60_000L
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
            body = when {
                offsetMinutes > 0 -> getString(Res.string.prayerReminderBody, offsetMinutes, name)
                offsetMinutes < 0 -> getString(Res.string.prayerFollowUpBody, name, -offsetMinutes)
                else -> getString(Res.string.prayerNotificationBody, name)
            },
            // Nə xəbərdarlıqda, nə sonrakı xatırlatmada tam çağırış çalınmır — vaxt hələ girməyib
            // (və ya artıq keçib), «Hadi namaza» isə vaxtın özünün siqnalıdır. Amma istifadəçi
            // həmin vaxtı səssiz seçibsə ikisi də səssiz qalır.
            //
            // ⚠️ `SYSTEM_DEFAULT`, `DEFAULT` DEYİL: defolt artıq çağırışın özüdür, ona görə
            // `DEFAULT` yazsaydıq «15 dəqiqə qaldı» bildirişi də tam çağırışı çalardı.
            sound = if (offsetMinutes != 0 && sound != AdhanSound.SILENT) {
                AdhanSound.SYSTEM_DEFAULT
            } else {
                sound
            },
            offsetMinutes = offsetMinutes,
        )
    }

    /**
     * Zikr bildirişi — namazdan **tamamilə ayrı mətn**: adı «Səhər/Axşam zikrləri»dir, cümləsində
     * nə namazın adı, nə də dəqiqə var.
     *
     * ⚠️ Səs həmişə [AdhanSound.SYSTEM_DEFAULT]-dur, lövbər namazın seçilmiş səsi **deyil**: azan
     * zikr üçün yanlış siqnaldır, üstəlik lövbər (Günəş/Axşam) istifadəçinin heç görmədiyi bir
     * ayara bağlı olduğu üçün səsin haradan gəldiyi tapılmaz olardı.
     */
    private suspend fun PrayerNotificationRef.toAdhkarNotification(
        slot: AdhkarSlot,
    ): PrayerNotification = PrayerNotification(
        prayer = prayer,
        dateIso = dateIso,
        key = key,
        atMillis = atMillis,
        title = getString(
            when (slot) {
                AdhkarSlot.MORNING -> Res.string.adhkarMorningTitle
                AdhkarSlot.EVENING -> Res.string.adhkarEveningTitle
            }
        ),
        body = getString(
            when (slot) {
                AdhkarSlot.MORNING -> Res.string.adhkarMorningBody
                AdhkarSlot.EVENING -> Res.string.adhkarEveningBody
            }
        ),
        sound = AdhanSound.SYSTEM_DEFAULT,
        offsetMinutes = offsetMinutes,
        adhkar = slot,
    )
}
