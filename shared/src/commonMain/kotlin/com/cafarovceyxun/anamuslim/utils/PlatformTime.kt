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
