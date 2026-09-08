package com.cafarovceyxun.anamuslim.utils.prayer

import com.cafarovceyxun.anamuslim.compose.components.prayer.PrayerUiFormat
import com.cafarovceyxun.anamuslim.compose.utils.preferences.PrayerPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.hijriDateFormat
import com.cafarovceyxun.anamuslim.resources.prayerLocationNotSet
import com.cafarovceyxun.anamuslim.resources.prayerTimesTitle
import com.cafarovceyxun.anamuslim.resources.prayerWidgetDateLine
import com.cafarovceyxun.anamuslim.resources.prayerWidgetRemaining
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.hijriDate
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString

/**
 * Ana ekran vidcetinin oxuduğu hazır məzmun.
 *
 * ### Niyə snapshot, niyə hesablama deyil
 * iOS vidceti ayrıca prosesdir (WidgetKit uzantısı) — nə DataStore-u, nə də paylaşılan Kotlin
 * qatını görür. İki yol var idi: çərçivəni uzantıya da bağlayıb hər şeyi orada hesablamaq, ya da
 * tətbiqin **hazır nəticəni** App Group-a yazması. İkincisi seçildi, çünki:
 *
 * - **Tərcümə bir yerdə qalır.** Sətirlər burada `getString` ilə oxunur, yəni uzantıda ikinci bir
 *   `Localizable.strings` dəsti saxlanmır. (Android tərəfdə məhz bu bölünmə var — vidcet
 *   `R.string`, ekranlar Compose Resources — və namaz adları iki dəfə tərcümə olunur.)
 * - **Uzantı yüngül qalır.** WidgetKit uzantısının yaddaş büdcəsi kiçikdir (~30 MB); ora Room,
 *   Ktor və atlas yükləyicisi ilə birlikdə gələn çərçivəni bağlamaq gərəksiz risk idi.
 *
 * ### Niyə bir neçə gün
 * Vidcet tətbiq işləmədən də düzgün qalmalıdır. Snapshot [DAY_COUNT] günü daşıyır, uzantı isə
 * hər namaz vaxtında dəyişən timeline qurur — yəni tətbiq üç gün açılmasa belə vidcet doğru vaxtı
 * vurğulayır.
 */
@Serializable
data class PrayerWidgetSnapshot(
    val generatedAtMillis: Long,
    /** Boş sətir = yer seçilməyib; uzantı bu halda [noLocationLabel] göstərir. */
    val placeName: String,
    val title: String,
    val noLocationLabel: String,
    /** Geri sayımın yanındakı söz («qaldı»). Uzantı özü tərcümə saxlamır. */
    val remainingLabel: String,
    val days: List<Day>,
) {
    @Serializable
    data class Day(
        val dateIso: String,
        /** «Bazar ertəsi, 24 Rəbiül-əvvəl 1448» — həftənin günü və qəməri tarix. */
        val dateLine: String,
        val items: List<Item>,
    )

    @Serializable
    data class Item(
        val label: String,
        val clock: String,
        val atMillis: Long,
        /** [Prayer] adının kiçik hərflə yazılışı — uzantı ikonu buna görə seçir. */
        val icon: String,
        /**
         * Bu vaxt «növbəti namaz» sayıla bilərmi.
         *
         * Günəş çıxması ibadət vaxtı deyil, üstəlik istifadəçi ayrı-ayrı vaxtları söndürə bilir —
         * vurğu və geri sayım yalnız bu bayraqlı elementlərə aiddir, sıra isə hamısını göstərir.
         */
        val countsAsNext: Boolean,
    )
}

object PrayerWidgetSnapshotBuilder {

    /** Bugün + iki gün. Uzantının timeline üfüqü bundan uzun ola bilməz. */
    const val DAY_COUNT = 3

    /**
     * [weekdayName] platformadan gəlir (iOS: `NSDateFormatter("EEEE")`) — həftə günü adlarını
     * ayrıca tərcümə etməyə dəyməz, hər iki platformada sistemdə var.
     *
     * Yer seçilməyibsə **yenə də** snapshot qaytarılır, sadəcə [PrayerWidgetSnapshot.days] boş olur:
     * uzantı «Yer seçin» mətnini də buradan oxuyur, yoxsa onu ikinci dəfə tərcümə etmək lazım gələrdi.
     */
    suspend fun build(weekdayName: (Long) -> String): PrayerWidgetSnapshot {
        val settings = PrayerPreferences.getSettings()
        val now = currentEpochMillis()

        val header = PrayerWidgetSnapshot(
            generatedAtMillis = now,
            placeName = settings.placeName,
            title = getString(Res.string.prayerTimesTitle),
            noLocationLabel = getString(Res.string.prayerLocationNotSet),
            remainingLabel = getString(Res.string.prayerWidgetRemaining),
            days = emptyList(),
        )

        val point = settings.point ?: return header

        val todayIso = PrayerUiFormat.localDate(now)
        val days = PrayerDay.forLocalDates(
            todayIso,
            count = DAY_COUNT,
            at = point,
            params = settings.params,
        )

        val include = settings.notify.ifEmpty { Prayer.entries.filter { it.isPrayer }.toSet() }

        return header.copy(
            days = days.map { day ->
                PrayerWidgetSnapshot.Day(
                    dateIso = day.dateIso,
                    dateLine = dateLine(day, weekdayName, settings.lunarOffsetDays),
                    items = Prayer.entries.mapNotNull { prayer ->
                        val time = day[prayer] ?: return@mapNotNull null

                        PrayerWidgetSnapshot.Item(
                            label = getString(PrayerUiFormat.labelOf(prayer, day.dateIso)),
                            clock = PrayerUiFormat.clock(time.atMillis).let {
                                // Yüksək enliklərdə hesablanmayan vaxt təxmindir — ekranlarda da
                                // eyni işarə ilə göstərilir.
                                if (time.source == TimeSource.ASTRONOMICAL) it else "≈$it"
                            },
                            atMillis = time.atMillis,
                            icon = prayer.name.lowercase(),
                            countsAsNext = prayer in include,
                        )
                    },
                )
            }
        )
    }

    private suspend fun dateLine(
        day: PrayerDayTimes,
        weekdayName: (Long) -> String,
        lunarOffsetDays: Int,
    ): String {
        // Günün lövbəri kimi ilk vaxt götürülür; yoxdursa günorta təxmini kifayətdir — sətir yalnız
        // tarix göstərir, dəqiqə dəqiqliyi lazım deyil.
        val anchor = Prayer.entries.firstNotNullOfOrNull { day[it]?.atMillis } ?: return ""
        val weekday = weekdayName(anchor)

        val hijri = hijriDate(anchor + lunarOffsetDays * MILLIS_PER_DAY)
            ?: return weekday

        val (dayOfMonth, month, year) = hijri
        val hijriText = getString(
            Res.string.hijriDateFormat,
            dayOfMonth,
            getString(PrayerUiFormat.hijriMonthName(month)),
            year,
        )

        return getString(Res.string.prayerWidgetDateLine, weekday, hijriText)
    }

    private const val MILLIS_PER_DAY = 86_400_000L
}
