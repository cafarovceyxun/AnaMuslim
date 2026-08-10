package com.cafarovceyxun.anamuslim.compose.utils

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Date

internal actual fun dateProximity(timeMillis: Long): DateProximity {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    return when {
        sameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> DateProximity.TODAY
        sameYear -> DateProximity.THIS_YEAR
        else -> DateProximity.OLDER
    }
}

internal actual fun formatDateTime(timeMillis: Long, pattern: String): String =
    DateFormat.format(pattern, Date(timeMillis)).toString()
