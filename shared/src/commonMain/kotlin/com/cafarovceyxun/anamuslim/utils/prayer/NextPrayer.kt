package com.cafarovceyxun.anamuslim.utils.prayer

/** Növbəti vaxt və ondan əvvəlki — geri sayım zolağı ikisini də tələb edir. */
data class Upcoming(
    val prayer: Prayer,
    val atMillis: Long,
    val previousPrayer: Prayer?,
    val previousAtMillis: Long?,
)

/**
 * «Növbəti namaz» seçimi — saf. Ana ekran kartı və tam ekran eyni funksiyanı çağırır ki, iki
 * səthin cavabı bir-birindən sürüşməsin.
 */
object NextPrayer {

    /**
     * [days] içindən [nowMillis]-dən sonrakı ilk vaxt.
     *
     * [include] adətən istifadəçinin seçdiyi bildiriş dəstidir; Günəş default olaraq kənardadır
     * (ibadət vaxtı deyil), amma cədvəldə görünür. [days] kifayət qədər irəli getməlidir —
     * gecə yarısından sonra növbəti vaxt **sabahkı** Fəcrdir.
     */
    fun after(nowMillis: Long, days: List<PrayerDayTimes>, include: Set<Prayer>): Upcoming? {
        if (include.isEmpty()) return null

        val ordered = days
            .flatMap { it.times }
            .filter { it.prayer in include }
            .sortedBy { it.atMillis }

        val index = ordered.indexOfFirst { it.atMillis > nowMillis }
        if (index < 0) return null

        val next = ordered[index]
        val previous = ordered.getOrNull(index - 1)

        return Upcoming(
            prayer = next.prayer,
            atMillis = next.atMillis,
            previousPrayer = previous?.prayer,
            previousAtMillis = previous?.atMillis,
        )
    }

    /**
     * Əvvəlki vaxtdan növbətiyə qədər keçilmiş hissə (0f..1f) — dairəvi/xətti indikator üçün.
     * Əvvəlki vaxt yoxdursa 0f (siyahının başındayıq, doldurulacaq bir aralıq yoxdur).
     */
    fun progressFraction(nowMillis: Long, upcoming: Upcoming): Float {
        val start = upcoming.previousAtMillis ?: return 0f
        val span = upcoming.atMillis - start
        if (span <= 0L) return 0f

        return ((nowMillis - start).toDouble() / span.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
}
