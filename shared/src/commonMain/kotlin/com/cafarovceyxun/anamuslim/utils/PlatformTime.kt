package com.cafarovceyxun.anamuslim.utils

/**
 * Current wall-clock time in milliseconds since the Unix epoch.
 * Platform-backed: Android uses [System.currentTimeMillis], iOS uses `NSDate`.
 */
expect fun currentEpochMillis(): Long

/**
 * Today's date in the device's local calendar, formatted `yyyy-MM-dd`.
 * Platform-backed: Android uses `java.time.LocalDate`, iOS uses `NSDateFormatter`/`NSCalendar`.
 */
expect fun currentLocalDateIsoString(): String

/** The wire format shared by the export file and the legacy `DateUtils.DATETIME_FORMAT_SYSTEM`. */
const val DATETIME_FORMAT_SYSTEM = "yyyy-MM-dd HH:mm:ss"

/**
 * Formats [epochMillis] in the device's local calendar as [DATETIME_FORMAT_SYSTEM].
 *
 * Exists because the export file stores bookmark timestamps as local wall-clock text (the format
 * the Android app has written since v1), while the entity holds epoch millis. Kept as a seam rather
 * than pulling in `kotlinx-datetime`: that library is only present transitively here, and pinning a
 * direct version would re-open the drift trap documented in `CLAUDE.md`.
 */
expect fun formatLocalDateTime(epochMillis: Long): String

/**
 * Parses [text] written as [DATETIME_FORMAT_SYSTEM] in the device's local calendar back to epoch
 * millis, or `null` when it is malformed — imported files are user-supplied and may be anything.
 */
expect fun parseLocalDateTime(text: String): Long?

/**
 * [isoDate] (`yyyy-MM-dd`) gününün [hour]:[minute] anını cihazın **yerli** qurşağında epoxa
 * millisaniyəsinə çevirir; tarix pozulubsa null.
 *
 * Günün ayəsi bildirişləri sabit yerli saatlarda çalınır ([com.cafarovceyxun.anamuslim.utils.verse.DailyContentSchedule]),
 * planlaşdırma isə hər iki platformada mütləq zaman tələb edir — Android `WorkManager`-ə gecikmə,
 * iOS `UNCalendarNotificationTrigger`-ə tarix verir. Qurşaq hesabı platformanındır: [IsoDate]
 * qəsdən yalnız təqvim arifmetikası edir.
 */
expect fun epochMillisAtLocalTime(isoDate: String, hour: Int, minute: Int): Long?

/**
 * [epochMillis]-i cihazın yerli təqvimində **uzun, lokallaşdırılmış** tarix kimi verir —
 * «Sal, 1 sentyabr 2026» kimi.
 *
 * Platformanın öz formatlayıcısı işlədilir (Android `DateTimeFormatter.ofLocalizedDate`, iOS
 * `NSDateFormatter`), ona görə həftə günü və ay adları üçün ayrıca tərcümə sətri saxlamağa ehtiyac
 * yoxdur — beş dil × 12 ay + 7 gün yazmaq həm baxımsız, həm də səhvə açıq olardı.
 */
expect fun formatLocalDateLong(epochMillis: Long): String

/**
 * [epochMillis]-in hicri qarşılığı: `(gün, ay 1–12, il)`.
 *
 * Çevirmə platformanın Ümmül-Qüra təqvimindəndir (Android `HijrahChronology`, iOS
 * `NSCalendarIdentifierIslamicUmmAlQura`), **ad isə yox**.
 *
 * ⚠️ Sistemin öz formatlayıcısından ad almaq sınandı və işləmədi: `java.time`-ın CLDR datasında
 * azərbaycanca islam ay adları yoxdur, ona görə `MMMM` ayı **rəqəm** kimi yazırdı («19 3 1448»).
 * Adlar `Res.string.hijriMonth1…12`-dədir və beş dilə yazılıb.
 *
 * **null** qaytara bilər: Android-də `HijrahChronology` yalnız API 26+-dadır, `minSdk` isə 24.
 */
expect fun hijriDate(epochMillis: Long): Triple<Int, Int, Int>?

/**
 * [epochMillis]-in **qısa** yerli tarixi — «17 iyn 2026». [formatLocalDateLong]-un həftə günüsüz
 * variantı: qəməri təqvim cədvəlində hər sətrin yanında miladi qarşılıq durur, həftə günü isə
 * sütunu lazımsız enli edərdi.
 */
expect fun formatLocalDateMedium(epochMillis: Long): String
