package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkDate
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkDateToday
import org.jetbrains.compose.resources.stringResource

/** How a timestamp relates to now, deciding which bookmark-date label is shown. */
enum class DateProximity { TODAY, THIS_YEAR, OLDER }

internal expect fun dateProximity(timeMillis: Long): DateProximity

/** Formats [timeMillis] using a platform date/time [pattern] such as `hh:mm a` or `dd MMM`. */
internal expect fun formatDateTime(timeMillis: Long, pattern: String): String

@Composable
fun formatBookmarkDate(timeMillis: Long): String {
    val timeText = formatDateTime(timeMillis, "hh:mm a")
    return when (dateProximity(timeMillis)) {
        DateProximity.TODAY -> stringResource(Res.string.strMsgBookmarkDateToday, timeText)
        DateProximity.THIS_YEAR ->
            stringResource(Res.string.strMsgBookmarkDate, formatDateTime(timeMillis, "dd MMM"), timeText)
        DateProximity.OLDER ->
            stringResource(Res.string.strMsgBookmarkDate, formatDateTime(timeMillis, "dd MMM yyyy"), timeText)
    }
}
