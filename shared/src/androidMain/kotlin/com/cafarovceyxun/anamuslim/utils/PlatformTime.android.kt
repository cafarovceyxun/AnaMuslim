package com.cafarovceyxun.anamuslim.utils

import java.time.LocalDate

actual fun currentEpochMillis(): Long = System.currentTimeMillis()

actual fun currentLocalDateIsoString(): String = LocalDate.now().toString()
