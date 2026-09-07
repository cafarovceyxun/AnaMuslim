package com.cafarovceyxun.anamuslim.utils

/**
 * `yyyy-MM-dd` mətnləri üzərində təqvim arifmetikası — sırf Kotlin, asılılıqsız.
 *
 * `kotlinx-datetime` qəsdən işlədilmir: layihədə yalnız transitiv olaraq var və birbaşa versiya
 * yazmaq CLAUDE.md-dəki «asılılıq versiya sürüşməsi» tələsini açır (yalnız iOS-da, runtime-da
 * partlayır). Burada lazım olan hər şey saat qurşağından **asılı olmayan** mülki tarix hesabıdır:
 * növbədəki elementin hansı gündə olduğunu saymaq üçün qurşaq bilgisi lazım deyil, yalnız
 * bildirişin **anını** hesablamaq üçün lazımdır — o da [epochMillisAtLocalTime] seam-indədir.
 *
 * Alqoritm Howard Hinnant-ın `days_from_civil` / `civil_from_days` çevirmələridir (proleptik
 * Qreqorian təqvimi).
 */
object IsoDate {

    /** `yyyy-MM-dd` → epoxa günü, format pozulubsa null. */
    fun toEpochDay(iso: String): Long? {
        val parts = iso.split('-')
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        if (month !in 1..12 || day !in 1..31) return null

        return daysFromCivil(year, month, day)
    }

    /** Epoxa günü → `yyyy-MM-dd`. */
    fun fromEpochDay(epochDay: Long): String {
        val (year, month, day) = civilFromDays(epochDay)

        return buildString {
            append(year.toString().padStart(4, '0'))
            append('-')
            append(month.toString().padStart(2, '0'))
            append('-')
            append(day.toString().padStart(2, '0'))
        }
    }

    /** [iso] tarixinə [days] gün əlavə edir; [iso] pozulubsa null. */
    fun plusDays(iso: String, days: Int): String? =
        toEpochDay(iso)?.let { fromEpochDay(it + days) }

    /** ISO həftə günü nömrələri — [dayOfWeek] bunları qaytarır. */
    const val FRIDAY = 5

    /**
     * ISO həftə günü: 1 = bazar ertəsi … 7 = bazar. [iso] pozulubsa null.
     *
     * Sabit nöqtə 1970-01-01-dir və o, **cümə axşamıdır** (ISO 4) — düstur buradan çıxır. Platforma
     * təqviminə müraciət olunmur: fayl qəsdən asılılıqsızdır (bax sinfin KDoc-u) və həftə günü də
     * mülki tarixdən çıxır, qurşaq bilgisi tələb etmir.
     */
    fun dayOfWeek(iso: String): Int? = toEpochDay(iso)?.let { dayOfWeek(it) }

    /** Epoxa günündən ISO həftə günü. */
    fun dayOfWeek(epochDay: Long): Int = ((epochDay + 3L).mod(7L) + 1L).toInt()

    /** `to - from` gün fərqi; hər hansı biri pozulubsa null. */
    fun daysBetween(from: String, to: String): Long? {
        val a = toEpochDay(from) ?: return null
        val b = toEpochDay(to) ?: return null
        return b - a
    }

    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy

        return era.toLong() * 146097L + doe.toLong() - 719468L
    }

    private fun civilFromDays(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468L
        val era = (if (z >= 0) z else z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()

        return Triple((if (month <= 2) y + 1 else y).toInt(), month, day)
    }
}
