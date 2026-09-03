package com.cafarovceyxun.anamuslim.utils

import com.cafarovceyxun.anamuslim.compose.utils.appLocale

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

actual fun currentLocalDateIsoString(): String = LocalDate.now().toString()

private val systemDateTimeFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern(DATETIME_FORMAT_SYSTEM)

actual fun formatLocalDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(systemDateTimeFormatter)

actual fun parseLocalDateTime(text: String): Long? = try {
    LocalDateTime.parse(text, systemDateTimeFormatter)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
} catch (e: Exception) {
    null
}

actual fun epochMillisAtLocalTime(isoDate: String, hour: Int, minute: Int): Long? = try {
    LocalDate.parse(isoDate)
        .atTime(hour, minute)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
} catch (e: Exception) {
    null
}

actual fun formatLocalDateLong(epochMillis: Long): String {
    val date = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()

    return date.format(
        java.time.format.DateTimeFormatter
            .ofPattern("EEE, d MMM yyyy")
            .withLocale(java.util.Locale.forLanguageTag(appLocale().languageTag))
    )
}

actual fun formatLocalDateMedium(epochMillis: Long): String {
    val date = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()

    return date.format(
        java.time.format.DateTimeFormatter
            .ofPattern("d MMM yyyy")
            .withLocale(java.util.Locale.forLanguageTag(appLocale().languageTag))
    )
}

actual fun hijriDate(epochMillis: Long): Triple<Int, Int, Int>? {
    // `HijrahChronology` API 26+-dadır; minSdk 24 olduğu üçün aşağıda sətir göstərilmir.
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return null

    return runCatching {
        val date = java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val hijri = java.time.chrono.HijrahDate.from(date)

        Triple(
            hijri.get(java.time.temporal.ChronoField.DAY_OF_MONTH),
            hijri.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
            hijri.get(java.time.temporal.ChronoField.YEAR),
        )
    }.getOrNull()
}
