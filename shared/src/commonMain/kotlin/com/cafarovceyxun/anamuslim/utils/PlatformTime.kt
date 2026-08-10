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
