package com.cafarovceyxun.anamuslim.compose.components.prayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.resources.hijriDateFormat
import com.cafarovceyxun.anamuslim.resources.ic_prayer_asr
import com.cafarovceyxun.anamuslim.resources.ic_prayer_dhuhr
import com.cafarovceyxun.anamuslim.resources.ic_prayer_fajr
import com.cafarovceyxun.anamuslim.resources.ic_prayer_isha
import com.cafarovceyxun.anamuslim.resources.ic_prayer_maghrib
import com.cafarovceyxun.anamuslim.resources.ic_prayer_sunrise
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
import com.cafarovceyxun.anamuslim.resources.prayerJumuah
import com.cafarovceyxun.anamuslim.resources.prayerJumuahNotification
import com.cafarovceyxun.anamuslim.resources.prayerMaghrib
import com.cafarovceyxun.anamuslim.resources.prayerRemainingHm
import com.cafarovceyxun.anamuslim.resources.prayerRemainingM
import com.cafarovceyxun.anamuslim.resources.prayerRemainingSoon
import com.cafarovceyxun.anamuslim.resources.prayerSunrise
import com.cafarovceyxun.anamuslim.utils.formatLocalDateTime
import com.cafarovceyxun.anamuslim.utils.IsoDate
import com.cafarovceyxun.anamuslim.utils.hijriDate
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import org.jetbrains.compose.resources.DrawableResource
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

    /**
     * Vaxtın nişanı — fəcrdən işaya günün gedişini göstərən altı vektor.
     *
     * Eyni fayllar Android vidcetində də işlənir (`PrayerWidgetReceiver` → `R.drawable`), ona görə
     * ekran və vidcet eyni dili danışır. Nişanlar ağ konturla çəkilib və rəngi çağırış yerindəki
     * `Icon(tint = …)`-dən alır — burada rəng seçmə.
     */
    fun iconOf(prayer: Prayer): DrawableResource = when (prayer) {
        Prayer.FAJR -> Res.drawable.ic_prayer_fajr
        Prayer.SUNRISE -> Res.drawable.ic_prayer_sunrise
        Prayer.DHUHR -> Res.drawable.ic_prayer_dhuhr
        Prayer.ASR -> Res.drawable.ic_prayer_asr
        Prayer.MAGHRIB -> Res.drawable.ic_prayer_maghrib
        Prayer.ISHA -> Res.drawable.ic_prayer_isha
    }

    /**
     * Vaxtın adı **həmin günə görə**: cümə günü zöhr «Cümə» olur, qalan hər şey dəyişmir.
     *
     * Qısa formadır, çünki bu ad Fəcr/Zöhr/Əsr sırasında dayanır — ana səhifədəki və vidcetdəki
     * altı sütunlu sətir bərabər paylanır və «Cümə namazı» qonşularını sıxardı. Bildiriş cümləsi
     * ([notificationLabelOf]) tam formanı işlədir.
     *
     * [dateIso] **yerli** mülki gündür ([localDate]), UTC açarı deyil. Fərq real haldır: UTC+14-də
     * cümə günorta yerli cümədir, amma UTC-yə görə hələ cümə axşamıdır — bildiriş planı günləri UTC
     * ilə açarlayır, istifadəçi isə öz təqvimini görür.
     */
    fun labelOf(prayer: Prayer, dateIso: String): StringResource =
        if (isJumuah(prayer, dateIso)) Res.string.prayerJumuah else labelOf(prayer)

    /**
     * Bildiriş üçün tam ad: «Cümə namazı» → «Cümə namazı vaxtıdır».
     *
     * Ekran etiketindən ([labelOf]) ayrıdır: orada ad sütun başlığı kimi tək dayanır, burada isə
     * cümlənin içinə düşür və «Cümə vaxtıdır» yarımçıq səslənərdi.
     */
    fun notificationLabelOf(prayer: Prayer, dateIso: String): StringResource =
        if (isJumuah(prayer, dateIso)) Res.string.prayerJumuahNotification else labelOf(prayer)

    private fun isJumuah(prayer: Prayer, dateIso: String): Boolean =
        prayer == Prayer.DHUHR && IsoDate.dayOfWeek(dateIso) == IsoDate.FRIDAY

    @Composable
    fun label(prayer: Prayer): String = stringResource(labelOf(prayer))

    /** [labelOf]-un günə həssas variantı. */
    @Composable
    fun label(prayer: Prayer, dateIso: String): String = stringResource(labelOf(prayer, dateIso))

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
     * Qəməri tarix — «19 Rəbiüləvvəl 1448», və ya platforma çevirməni dəstəkləmirsə null.
     *
     * İstifadəçinin gün düzəlişi ([PrayerPreferences.KEY_LUNAR_OFFSET]) burada oxunur, ona görə
     * ekranda göstərilən hər qəməri tarix onu **avtomatik** alır. Düzəlişi özü bilən çağırış
     * yerləri (paylaşılan şəkil) `hijri(atMillis, offsetDays)` overload-unu işlədir.
     *
     * ⚠️ Ay adları **bizim** resurslarımızdandır, sistemin deyil: `java.time`-ın CLDR datasında
     * azərbaycanca islam ay adları yoxdur və `MMMM` ayı rəqəm kimi yazırdı («19 3 1448»).
     */
    @Composable
    fun hijri(atMillis: Long): String? = hijri(atMillis, PrayerPreferences.observeLunarOffset())

    /**
     * Düzəlişi **kənardan** alan variant.
     *
     * Düzəliş millisə əlavə olunur, çevirmənin nəticəsinə yox: gün/ay/il sərhədləri belə özü-özünə
     * düzgün keçir (29 Zilhiccə + 1 → növbəti ilin 1 Məhərrəmi), əks halda hər sərhədi əl ilə
     * saymaq lazım gələrdi.
     */
    @Composable
    fun hijri(atMillis: Long, offsetDays: Int): String? {
        val (day, month, year) = hijriDate(atMillis + offsetDays * MILLIS_PER_DAY) ?: return null
        val name = stringResource(hijriMonthName(month))

        return stringResource(Res.string.hijriDateFormat, day, name, year)
    }

    private const val MILLIS_PER_DAY = 86_400_000L

    /** Qəməri ayın adı (1–12). Paylaşılan təqvim kartı başlıq üçün bunu oxuyur. */
    fun hijriMonthName(month: Int): StringResource = when (month) {
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
