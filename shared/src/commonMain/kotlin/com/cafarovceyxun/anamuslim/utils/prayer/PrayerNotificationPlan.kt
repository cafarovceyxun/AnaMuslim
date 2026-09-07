package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate

/**
 * Planlaşdırılacaq tək bildiriş. [key] dublikat qoruyucusudur — Android işi təkrar cəhd edəndə və
 * iOS eyni tələbi yenidən yazanda eyni açar iki dəfə çalmağın qarşısını alır.
 */
data class PrayerNotificationRef(
    val prayer: Prayer,
    val dateIso: String,
    val atMillis: Long,
    /** 0 = vaxtın özü; >0 = həmin qədər dəqiqə əvvəlki xəbərdarlıq. */
    val leadMinutes: Int = 0,
) {
    val key: String get() = PrayerNotificationPlan.keyOf(dateIso, prayer, leadMinutes)
}

/**
 * «Hansı bildiriş nə vaxt» qatı — **tamamilə saf**: preference oxumur, resurs oxumur, repository
 * saxlamır. Giriş [PrayerSettings] + `nowMillis` + çatdırılmış açarlar, çıxış siyahı.
 *
 * Bu bölgü qəsdlidir. `VotdNotificationContent` eyni obyektdə həm plan, həm mətn, həm preference
 * oxuduğu üçün heç vaxt test olunmadı; həmin anti-naxış burada təkrarlanmır. Mətn qurmaq və
 * preference oxumaq işi nazik, şərtsiz `PrayerNotificationContent`-indir.
 *
 * Tarixlər **UTC mülci günü** ilə açarlanır ([PrayerTimes] ilə eyni), ona görə açar qurşaq
 * dəyişəndə sürüşmür — istifadəçi səyahət edəndə bildiriş iki dəfə çalmır.
 */
object PrayerNotificationPlan {

    /** İstifadəçi az vaxt seçəndə də üfüq bundan uzağa getmir — cədvəl köhnəlir. */
    const val MAX_DAYS_AHEAD = 14

    /**
     * ⚠️ Vaxtın öz açarı **dəyişməz** qalır (`tarix#NAMAZ`) — xəbərdarlıq özünə ayrıca son hissə
     * alır. Əks halda yeniləmədən sonra `delivered` dəstindəki bütün köhnə açarlar uyğunsuz olar
     * və artıq çalınmış bildirişlər bir daha çalardı.
     */
    fun keyOf(dateIso: String, prayer: Prayer, leadMinutes: Int = 0): String =
        if (leadMinutes <= 0) "$dateIso#${prayer.name}" else "$dateIso#${prayer.name}#$leadMinutes"

    /**
     * [nowMillis]-dən sonrakı, hələ çatdırılmamış bildirişlər — ən çoxu [limit] ədəd.
     *
     * [limit] iOS-un 64 gözləyən tələb limitindən gəlir
     * ([com.cafarovceyxun.anamuslim.utils.notify.NotificationBudget]). Az vaxt seçiləndə eyni büdcə
     * daha çox günə çatır, ona görə üfüq dinamik hesablanır.
     */
    fun upcoming(
        settings: PrayerSettings,
        nowMillis: Long,
        limit: Int,
        delivered: Set<String> = emptySet(),
    ): List<PrayerNotificationRef> {
        if (limit <= 0) return emptyList()

        // Xəbərdarlıqlar da sayılır: yalnız vaxtları saysaydıq üfüq iki qat uzun hesablanar,
        // büdcə isə yarısında bitər — iOS artığını SƏSSİZCƏ atır.
        val perDay = settings.notificationsPerDay
        if (perDay == 0) return emptyList()

        val days = (limit / perDay).coerceIn(1, MAX_DAYS_AHEAD)

        return collect(settings, nowMillis, daysAhead = days, delivered = delivered) { it > nowMillis }
            .take(limit)
    }

    /**
     * Vaxtı keçmiş, amma hələ [graceMillis] pəncərəsində olan və çatdırılmamış bildirişlər.
     *
     * Android tərəf bunu «qaçırılmış siqnalı indi çal» üçün oxuyur: cihaz söndürülüb açılanda və ya
     * alarm itəndə istifadəçi vaxtın keçdiyini bilməlidir.
     */
    fun due(
        settings: PrayerSettings,
        nowMillis: Long,
        graceMillis: Long,
        delivered: Set<String> = emptySet(),
    ): List<PrayerNotificationRef> {
        if (graceMillis <= 0L) return emptyList()

        val floor = nowMillis - graceMillis

        return collect(settings, nowMillis, daysAhead = 0, delivered = delivered) {
            it in (floor + 1)..nowMillis
        }
    }

    /**
     * Ortaq generator. UTC günləri ilə işləyir — yerli tarix seam-i **lazım deyil**, çünki açar da,
     * hesablama da UTC mülci gününə bağlıdır.
     *
     * Pəncərə həmişə bir gün geriyə də açılır: yerli gecə yarısından sonra düşən vaxtlar dünənki
     * UTC gününə aid ola bilər.
     */
    private inline fun collect(
        settings: PrayerSettings,
        nowMillis: Long,
        daysAhead: Int,
        delivered: Set<String>,
        keep: (Long) -> Boolean,
    ): List<PrayerNotificationRef> {
        if (!settings.canSchedule) return emptyList()

        val point = settings.point ?: return emptyList()
        val startDay = PrayerDay.utcEpochDay(nowMillis) - 1
        val result = ArrayList<PrayerNotificationRef>(settings.notificationsPerDay * (daysAhead + 2))

        for (index in 0..(daysAhead + 1)) {
            val dateIso = IsoDate.fromEpochDay(startDay + index)
            val day = PrayerTimes.calculate(dateIso, point, settings.params) ?: continue

            for (time in day.times) {
                if (time.prayer !in settings.notify) continue

                // Vaxtın özü, sonra (varsa) ondan əvvəlki xəbərdarlıq. İkisi ayrı açardadır, ona
                // görə biri çatdırılsa da digəri planda qalır.
                val lead = settings.reminderOf(time.prayer)
                val moments = if (lead > 0) {
                    listOf(0 to time.atMillis, lead to time.atMillis - lead * 60_000L)
                } else {
                    listOf(0 to time.atMillis)
                }

                for ((leadMinutes, atMillis) in moments) {
                    if (!keep(atMillis)) continue
                    if (keyOf(dateIso, time.prayer, leadMinutes) in delivered) continue

                    result += PrayerNotificationRef(time.prayer, dateIso, atMillis, leadMinutes)
                }
            }
        }

        return result.sortedBy { it.atMillis }
    }

    /**
     * Çatdırılmış açarların təmizlənməsi: [keepDays] gündən köhnələr atılır.
     *
     * Preference-də saxlanan dəst əks halda sonsuz böyüyür — VOTD tərəfdə bu, `daily_content_delivered`
     * açarında illərlə yığılmışdı.
     */
    fun pruneDelivered(delivered: Set<String>, nowMillis: Long, keepDays: Int = 3): Set<String> {
        val floorIso = IsoDate.fromEpochDay(PrayerDay.utcEpochDay(nowMillis) - keepDays)

        return delivered.filterTo(HashSet()) { key ->
            val dateIso = key.substringBefore('#')
            dateIso >= floorIso
        }
    }
}
