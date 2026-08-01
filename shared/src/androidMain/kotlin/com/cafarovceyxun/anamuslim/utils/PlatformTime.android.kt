package com.cafarovceyxun.anamuslim.utils

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
