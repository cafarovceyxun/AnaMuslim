package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.resources.hijriDateFormat
import com.cafarovceyxun.anamuslim.resources.hijriMonth1
import com.cafarovceyxun.anamuslim.resources.hijriMonth2
import com.cafarovceyxun.anamuslim.resources.hijriMonth3
import com.cafarovceyxun.anamuslim.resources.hijriMonth4
import com.cafarovceyxun.anamuslim.resources.hijriMonth5
import com.cafarovceyxun.anamuslim.resources.hijriMonth6
import com.cafarovceyxun.anamuslim.resources.hijriMonth7
import com.cafarovceyxun.anamuslim.resources.hijriMonth8
import com.cafarovceyxun.anamuslim.resources.hijriMonth9
import com.cafarovceyxun.anamuslim.resources.hijriMonth10
import com.cafarovceyxun.anamuslim.resources.hijriMonth11
import com.cafarovceyxun.anamuslim.resources.hijriMonth12
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.prayerAsr
import com.cafarovceyxun.anamuslim.resources.prayerDhuhr
import com.cafarovceyxun.anamuslim.resources.prayerFajr
import com.cafarovceyxun.anamuslim.resources.prayerIsha
import com.cafarovceyxun.anamuslim.resources.prayerMaghrib
import com.cafarovceyxun.anamuslim.resources.prayerRemainingHm
import com.cafarovceyxun.anamuslim.resources.prayerRemainingM
import com.cafarovceyxun.anamuslim.resources.prayerRemainingSoon
import com.cafarovceyxun.anamuslim.resources.prayerSunrise
import com.cafarovceyxun.anamuslim.utils.formatLocalDateTime
import com.cafarovceyxun.anamuslim.utils.hijriDate
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Namaz UI-nin formatlama qatı.
 *
 * ⚠️ Vaxt mətnləri **həmişə LTR-dir** ([ltrDigits]). `QuranAppTheme` ərəb interfeysində
 * `LocalLayoutDirection`-ı RTL edir və istiqamətini özü təyin etməyən hər mətn çevrilir: `05:42`
 * güzgülənib `42:05` kimi oxuna bilər. Kompilyator da, testlər də susur (CLAUDE.md, 2026-08-20).
 */
object PrayerUiFormat {

    fun labelOf(prayer: Prayer): StringResource = when (prayer) {
        Prayer.FAJR -> Res.string.prayerFajr
        Prayer.SUNRISE -> Res.string.prayerSunrise
        Prayer.DHUHR -> Res.string.prayerDhuhr
        Prayer.ASR -> Res.string.prayerAsr
        Prayer.MAGHRIB -> Res.string.prayerMaghrib
        Prayer.ISHA -> Res.string.prayerIsha
    }

    @Composable
    fun label(prayer: Prayer): String = stringResource(labelOf(prayer))

    /**
     * `HH:mm`, cihazın yerli qurşağında.
     *
     * Mövcud [formatLocalDateTime] seam-indən kəsilir (`"yyyy-MM-dd HH:mm:ss"`), yeni `expect/actual`
     * əlavə edilmir — namaz qatı qəsdən UTC anı ilə işləyir və qurşaq hesabı yalnız burada, bir
     * dəfə baş verir.
     */
    fun clock(atMillis: Long): String {
        val text = formatLocalDateTime(atMillis)
        return if (text.length >= 16) text.substring(11, 16) else text
    }

    /**
     * Hicri tarix — «19 Rəbiüləvvəl 1448», və ya platforma çevirməni dəstəkləmirsə null.
     *
     * ⚠️ Ay adları **bizim** resurslarımızdandır, sistemin deyil: `java.time`-ın CLDR datasında
     * azərbaycanca islam ay adları yoxdur və `MMMM` ayı rəqəm kimi yazırdı («19 3 1448»).
     */
    @Composable
    fun hijri(atMillis: Long): String? {
        val (day, month, year) = hijriDate(atMillis) ?: return null
        val name = stringResource(hijriMonthName(month))

        return stringResource(Res.string.hijriDateFormat, day, name, year)
    }

    private fun hijriMonthName(month: Int): StringResource = when (month) {
        1 -> Res.string.hijriMonth1
        2 -> Res.string.hijriMonth2
        3 -> Res.string.hijriMonth3
        4 -> Res.string.hijriMonth4
        5 -> Res.string.hijriMonth5
        6 -> Res.string.hijriMonth6
        7 -> Res.string.hijriMonth7
        8 -> Res.string.hijriMonth8
        9 -> Res.string.hijriMonth9
        10 -> Res.string.hijriMonth10
        11 -> Res.string.hijriMonth11
        else -> if (month == 12) Res.string.hijriMonth12 else Res.string.hijriMonth1
    }

    /** Vaxtın düşdüyü yerli tarix (`yyyy-MM-dd`). */
    fun localDate(atMillis: Long): String = formatLocalDateTime(atMillis).substringBefore(' ')

    /** «2 saat 15 dəqiqə» / «15 dəqiqə» / «bir dəqiqədən az». */
    @Composable
    fun remaining(millis: Long): String {
        val totalMinutes = (millis / 60_000L).toInt()

        return when {
            totalMinutes < 1 -> stringResource(Res.string.prayerRemainingSoon)
            totalMinutes < 60 -> stringResource(Res.string.prayerRemainingM, totalMinutes)
            else -> stringResource(Res.string.prayerRemainingHm, totalMinutes / 60, totalMinutes % 60)
        }
    }

    /** Rəqəm və latın mətnini interfeys dilindən asılı olmayaraq soldan-sağa bağlayır. */
    fun TextStyle.ltrDigits(): TextStyle = withScriptDirection(arabic = false)
}
