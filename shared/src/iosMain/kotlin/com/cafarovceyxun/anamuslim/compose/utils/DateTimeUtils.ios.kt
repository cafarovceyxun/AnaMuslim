package com.cafarovceyxun.anamuslim.compose.utils

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDayOfYear
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

private fun nsDate(timeMillis: Long): NSDate =
    NSDate.dateWithTimeIntervalSince1970(timeMillis / 1000.0)

internal actual fun dateProximity(timeMillis: Long): DateProximity {
    val calendar = NSCalendar.currentCalendar
    val units = NSCalendarUnitYear or NSCalendarUnitDayOfYear
    val now = calendar.components(units, fromDate = NSDate())
    val target = calendar.components(units, fromDate = nsDate(timeMillis))

    val sameYear = now.year == target.year
    return when {
        sameYear && now.dayOfYear == target.dayOfYear -> DateProximity.TODAY
        sameYear -> DateProximity.THIS_YEAR
        else -> DateProximity.OLDER
    }
}

internal actual fun formatDateTime(timeMillis: Long, pattern: String): String {
    val formatter = NSDateFormatter().apply { setDateFormat(pattern) }
    return formatter.stringFromDate(nsDate(timeMillis))
}
