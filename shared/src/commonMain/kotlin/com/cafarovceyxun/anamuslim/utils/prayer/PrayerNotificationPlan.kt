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
    /**
     * Vaxtın özündən sürüşmə, **işarəli dəqiqə**: `0` = vaxtın özü, `>0` = həmin qədər dəqiqə
     * əvvəlki xəbərdarlıq, `<0` = həmin qədər dəqiqə sonrakı xatırlatma.
     *
     * Tək işarəli sahə qəsdlidir: açar, sıralama, dublikat qoruması və platforma id-si üç halda da
     * eyni yoldan keçir. Ayarda isə iki müstəqil xəritə var
     * ([PrayerSettings.reminderMinutes] / [PrayerSettings.followUpMinutes]) — bir vaxt üçün həm
     * əvvəl, həm sonra qurula bilsin.
     */
    val offsetMinutes: Int = 0,
    /**
     * Zikr yuvası; `null` = adi namaz bildirişi.
     *
     * Dolu olanda [prayer] **lövbərdir**, bildirişin mövzusu deyil ([AdhkarSlot.anchor]) — mətn də,
     * platforma id-si də yuvadan qurulur.
     */
    val adhkar: AdhkarSlot? = null,
) {
    val key: String
        get() = adhkar?.let { PrayerNotificationPlan.keyOf(dateIso, it) }
            ?: PrayerNotificationPlan.keyOf(dateIso, prayer, offsetMinutes)
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
     * ⚠️ Vaxtın öz açarı **dəyişməz** qalır (`tarix#NAMAZ`) — sürüşmüş bildirişlər özlərinə ayrıca
     * son hissə alır. Əks halda yeniləmədən sonra `delivered` dəstindəki bütün köhnə açarlar
     * uyğunsuz olar və artıq çalınmış bildirişlər bir daha çalardı.
     *
     * Sonrakı xatırlatma mənfi işarə ilə yazılır (`tarix#NAMAZ#-15`), ona görə əvvəlki xəbərdarlığın
     * açarı (`tarix#NAMAZ#15`) ilə heç vaxt toqquşmur.
     */
    fun keyOf(dateIso: String, prayer: Prayer, offsetMinutes: Int = 0): String =
        if (offsetMinutes == 0) "$dateIso#${prayer.name}" else "$dateIso#${prayer.name}#$offsetMinutes"

    /**
     * Zikrin açarı — **sürüşmə daxil edilmir**, yalnız gün və yuva.
     *
     * Qəsdlidir: sürüşmə açarda olsaydı, istifadəçi səhər zikri çalandan sonra dəqiqəni dəyişən kimi
     * açar yeniləşər və **eyni gün ikinci dəfə** çalardı. Namaz xəbərdarlığında sürüşmə açardadır,
     * çünki orada eyni vaxt üçün bir neçə fərqli an (özü + əvvəl + sonra) planlaşdırılır; burada
     * gündə bir yuvadan bir bildiriş var.
     *
     * Prefiks `ZIKR_`-dir ki, `2026-09-16#SUNRISE` (Günəş bildirişi) ilə heç vaxt toqquşmasın.
     */
    fun keyOf(dateIso: String, slot: AdhkarSlot): String = "$dateIso#ZIKR_${slot.name}"

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

        // Əvvəl və sonra bildirişləri də sayılır: yalnız vaxtları saysaydıq üfüq üç qat uzun
        // hesablanar, büdcə isə üçdə birində bitər — iOS artığını SƏSSİZCƏ atır.
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
        if (!settings.canScheduleAny) return emptyList()

        val point = settings.point ?: return emptyList()
        val startDay = PrayerDay.utcEpochDay(nowMillis) - 1
        val result = ArrayList<PrayerNotificationRef>(settings.notificationsPerDay * (daysAhead + 2))

        // İki müstəqil mənbə: namaz bildirişləri söndürülüb zikr açıq qala bilər və əksinə.
        val schedulesPrayers = settings.canSchedule

        for (index in 0..(daysAhead + 1)) {
            val dateIso = IsoDate.fromEpochDay(startDay + index)
            val day = PrayerTimes.calculate(dateIso, point, settings.params) ?: continue

            // Zikr lövbəri cədvəlin öz anıdır (gün çıxma / gün batma), ona görə istifadəçinin
            // həmin vaxt üçün etdiyi dəqiqə düzəlişi ([PrayerParams.offsetMinutes]) buraya da
            // keçir — ekranda gördüyü rəqəmlə xatırlatma arasında fərq qalmamalıdır.
            for (slot in AdhkarSlot.entries) {
                if (slot !in settings.adhkar) continue

                val anchor = day[slot.anchor] ?: continue
                val offsetMinutes = settings.adhkarOffsetOf(slot)
                val atMillis = anchor.atMillis - offsetMinutes * 60_000L

                if (!keep(atMillis)) continue
                if (keyOf(dateIso, slot) in delivered) continue

                result += PrayerNotificationRef(
                    prayer = slot.anchor,
                    dateIso = dateIso,
                    atMillis = atMillis,
                    offsetMinutes = offsetMinutes,
                    adhkar = slot,
                )
            }

            if (!schedulesPrayers) continue

            for (time in day.times) {
                if (time.prayer !in settings.notify) continue

                // Vaxtın özü, ondan əvvəlki xəbərdarlıq və sonrakı xatırlatma — üçü də ayrı
                // açardadır, ona görə biri çatdırılsa da digərləri planda qalır.
                val lead = settings.reminderOf(time.prayer)
                val followUp = settings.followUpOf(time.prayer)

                val moments = ArrayList<Pair<Int, Long>>(3)
                moments += 0 to time.atMillis
                if (lead > 0) moments += lead to time.atMillis - lead * 60_000L
                // Mənfi işarə «sonra» deməkdir; an isə vaxtın üstünə gəlir.
                if (followUp > 0) moments += -followUp to time.atMillis + followUp * 60_000L

                for ((offsetMinutes, atMillis) in moments) {
                    if (!keep(atMillis)) continue
                    if (keyOf(dateIso, time.prayer, offsetMinutes) in delivered) continue

                    result += PrayerNotificationRef(time.prayer, dateIso, atMillis, offsetMinutes)
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
