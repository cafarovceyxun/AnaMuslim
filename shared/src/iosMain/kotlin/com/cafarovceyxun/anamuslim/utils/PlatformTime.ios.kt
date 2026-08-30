package com.cafarovceyxun.anamuslim.utils

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970

actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun currentLocalDateIsoString(): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd"
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        timeZone = NSTimeZone.localTimeZone
    }
    return formatter.stringFromDate(NSDate())
}

/**
 * `en_US_POSIX` is required: with the user's own locale the formatter may substitute a non-
 * Gregorian calendar or non-Latin digits, which would make the exported text unreadable by the
 * Android build and by this parser.
 */
private fun systemDateTimeFormatter() = NSDateFormatter().apply {
    dateFormat = DATETIME_FORMAT_SYSTEM
    locale = NSLocale(localeIdentifier = "en_US_POSIX")
    timeZone = NSTimeZone.localTimeZone
}

actual fun formatLocalDateTime(epochMillis: Long): String =
    systemDateTimeFormatter().stringFromDate(
        NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    )

actual fun parseLocalDateTime(text: String): Long? {
    val date = systemDateTimeFormatter().dateFromString(text) ?: return null
    return (date.timeIntervalSince1970 * 1000.0).toLong()
}

actual fun epochMillisAtLocalTime(isoDate: String, hour: Int, minute: Int): Long? {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd HH:mm"
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
        timeZone = NSTimeZone.localTimeZone
    }

    val text = "$isoDate ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    val date = formatter.dateFromString(text) ?: return null

    return (date.timeIntervalSince1970 * 1000.0).toLong()
}
