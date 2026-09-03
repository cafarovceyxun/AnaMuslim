package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.epochMillisAtLocalTime
import com.cafarovceyxun.anamuslim.utils.hijriDate

/**
 * Bir qəməri ayın miladi günləri.
 *
 * Platformada qəməri **ay aralığı** verən API yoxdur — yalnız «bu an hansı qəməri gündür»
 * ([hijriDate]). Ona görə ay sərhədləri günbəgün axtarılır: ucuzdur (ayda ≤ 30 çevirmə) və hər iki
 * platformada eyni Ümmül-Qüra cədvəlindən gəldiyi üçün nəticə də eynidir.
 *
 * Hər gün **yerli günorta** anı ilə təmsil olunur. Gecəyarısı götürsəydik yay vaxtı keçidində
 * saat bir saat sürüşəndə gün ya təkrarlanar, ya da atlanardı; günorta hər iki tərəfə 11 saat
 * ehtiyat saxlayır.
 */
object LunarMonth {

    private const val MILLIS_PER_DAY = 86_400_000L

    /** Bir qəməri ay: nömrəsi (1–12), ili və miladi günlərinin **yerli günorta** anları. */
    data class Span(
        val month: Int,
        val year: Int,
        val days: List<Long>,
    ) {
        /** Növbəti/əvvəlki aya keçmək üçün lövbər anları. */
        val nextAnchor: Long get() = days.last() + MILLIS_PER_DAY
        val previousAnchor: Long get() = days.first() - MILLIS_PER_DAY
    }

    /**
     * [anchorMillis] anının düşdüyü qəməri ay, [offsetDays] gün düzəlişi tətbiq olunmuş halda.
     *
     * Platforma çevirməni dəstəkləmirsə (Android API < 26) null qaytarır — çağıran tərəf təqvimi
     * ümumiyyətlə göstərmir.
     */
    fun spanContaining(anchorMillis: Long, offsetDays: Int): Span? {
        val noon = localNoon(anchorMillis) ?: return null
        val (_, month, year) = lunarAt(noon, offsetDays) ?: return null

        // Qəməri ay 29–30 gündür; [MAX_DAYS_IN_MONTH] həm sərhədi tapmağa bəs edir, həm də pozuq
        // çevirmədə sonsuz dövrənin qarşısını alır.
        var first = noon
        var steps = 0
        while (steps++ < MAX_DAYS_IN_MONTH) {
            val previous = first - MILLIS_PER_DAY
            val at = lunarAt(previous, offsetDays) ?: break
            if (at.second != month || at.third != year) break
            first = previous
        }

        val days = ArrayList<Long>(MAX_DAYS_IN_MONTH)
        var cursor = first
        while (days.size < MAX_DAYS_IN_MONTH) {
            val at = lunarAt(cursor, offsetDays) ?: break
            if (at.second != month || at.third != year) break
            days.add(cursor)
            cursor += MILLIS_PER_DAY
        }

        return days.takeIf { it.isNotEmpty() }?.let { Span(month, year, it) }
    }

    private const val MAX_DAYS_IN_MONTH = 40

    /**
     * Anı **yerli** günortaya sürüşdürür. Yerli tarix mətn üzərindən alınır, çünki commonMain-də
     * qurşaq hesabı yoxdur — namaz qatının qalan hissəsi də eyni seam-ləri işlədir.
     */
    private fun localNoon(epochMillis: Long): Long? =
        epochMillisAtLocalTime(PrayerDay.localDateOfDevice(epochMillis), hour = 12, minute = 0)

    private fun lunarAt(epochMillis: Long, offsetDays: Int): Triple<Int, Int, Int>? =
        hijriDate(epochMillis + offsetDays * MILLIS_PER_DAY)
}
