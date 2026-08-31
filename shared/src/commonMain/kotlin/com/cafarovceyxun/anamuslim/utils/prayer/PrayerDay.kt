package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.formatLocalDateTime

/**
 * [PrayerTimes] UTC mülki günü ilə işləyir; istifadəçi isə **cihazın yerli gününü** görür.
 * Bu obyekt aradakı körpüdür.
 *
 * Üç UTC günü (dünən/bugün/sabah) hesablanır və yalnız yerli tarixi [localDateIso] olan anlar
 * saxlanılır. Bu bir mexanizm üç problemi eyni anda həll edir:
 *
 * 1. **Qurşaq sürüşməsi** — Bakıda oturub Londonu seçəndə Londonun düzgün anları Bakı saatı ilə
 *    göstərilir; heç bir IANA bazası lazım deyil.
 * 2. **Tarix sərhədi** — UTC+13 və UTC−11-də «bugünkü» vaxtlar iki fərqli UTC gününə düşür.
 * 3. **DST** — keçid günündə saat irəli/geri sürüşəndə xüsusi kod olmadan düzgün sayda vaxt qalır,
 *    çünki filtr **anlara** baxır, divar saatına yox.
 */
object PrayerDay {

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * [localDateIso] yerli gününə düşən vaxtlar.
     *
     * [localDateOf] inyeksiya olunur ki, testlər saxta qurşaqla işləyə bilsin — default cihazın
     * öz qurşağıdır (`formatLocalDateTime` → `"yyyy-MM-dd HH:mm:ss"` mətninin tarix hissəsi).
     *
     * ⚠️ Nəticə **altı vaxtdan az** ola bilər: ekstremal qurşaq/enlik birləşmələrində bir namaz
     * yerli günə heç düşmür. UI `get(prayer)`-in null qaytardığını gözləməlidir.
     */
    fun forLocalDate(
        localDateIso: String,
        at: GeoPoint,
        params: PrayerParams,
        localDateOf: (Long) -> String = ::localDateOfDevice,
    ): PrayerDayTimes {
        val collected = HashMap<Prayer, PrayerTime>(Prayer.entries.size)

        for (offset in -1..1) {
            val dateIso = IsoDate.plusDays(localDateIso, offset) ?: continue
            val day = PrayerTimes.calculate(dateIso, at, params) ?: continue

            for (time in day.times) {
                if (localDateOf(time.atMillis) != localDateIso) continue

                // Eyni namaz iki UTC günündən gələ bilər (sərhəd halları) — ən erkəni doğrudur.
                val existing = collected[time.prayer]
                if (existing == null || time.atMillis < existing.atMillis) {
                    collected[time.prayer] = time
                }
            }
        }

        return PrayerDayTimes(
            dateIso = localDateIso,
            times = collected.values.sortedBy { it.prayer.ordinal },
        )
    }

    /** Ardıcıl [count] yerli gün — geri sayım və bildiriş planı üçün. */
    fun forLocalDates(
        startLocalDateIso: String,
        count: Int,
        at: GeoPoint,
        params: PrayerParams,
        localDateOf: (Long) -> String = ::localDateOfDevice,
    ): List<PrayerDayTimes> = (0 until count).mapNotNull { index ->
        IsoDate.plusDays(startLocalDateIso, index)?.let { forLocalDate(it, at, params, localDateOf) }
    }

    /**
     * Cihazın UTC ofseti (saniyə) — yeni `expect/actual` olmadan, mövcud [formatLocalDateTime]
     * seam-indən çıxarılır: yerli divar saatını «sanki UTC-dir» kimi oxuyub anla fərqini götürür.
     *
     * Tam ekrandakı «bu vaxtlar {şəhər} üçündür» zolağı bunu seçilmiş yerin günəş ofseti
     * (`lng / 15`) ilə müqayisə edir.
     */
    fun deviceUtcOffsetSeconds(
        nowMillis: Long,
        format: (Long) -> String = ::formatLocalDateTime,
    ): Int {
        val text = format(nowMillis)
        val asUtcSeconds = parseAsUtcSeconds(text) ?: return 0

        return (asUtcSeconds - floorDiv(nowMillis, 1000L)).toInt()
    }

    /** `"yyyy-MM-dd HH:mm:ss"` → sanki UTC olsaydı hansı epoxa saniyəsi olardı. */
    private fun parseAsUtcSeconds(text: String): Long? {
        val datePart = text.substringBefore(' ')
        val timePart = text.substringAfter(' ', missingDelimiterValue = "")

        val epochDay = IsoDate.toEpochDay(datePart) ?: return null
        val pieces = timePart.split(':')
        if (pieces.size < 2) return null

        val hour = pieces[0].toIntOrNull() ?: return null
        val minute = pieces[1].toIntOrNull() ?: return null
        val second = pieces.getOrNull(2)?.toIntOrNull() ?: 0

        return epochDay * 86_400L + hour * 3_600L + minute * 60L + second
    }

    /** Cihazın yerli tarixi (`yyyy-MM-dd`) — [forLocalDate]-in default [localDateOf]-u. */
    fun localDateOfDevice(epochMillis: Long): String =
        formatLocalDateTime(epochMillis).substringBefore(' ')

    /** `Math.floorDiv` commonMain-də yoxdur; mənfi anlar üçün doğru davranış lazımdır. */
    internal fun floorDiv(value: Long, divisor: Long): Long {
        var quotient = value / divisor
        if (value % divisor != 0L && (value xor divisor) < 0L) quotient--
        return quotient
    }

    /** [epochMillis]-in düşdüyü UTC mülki günü. */
    internal fun utcEpochDay(epochMillis: Long): Long = floorDiv(epochMillis, MILLIS_PER_DAY)
}
