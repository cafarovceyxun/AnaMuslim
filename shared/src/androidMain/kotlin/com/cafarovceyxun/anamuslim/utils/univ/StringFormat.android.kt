package com.cafarovceyxun.anamuslim.utils.univ

import java.util.Locale

actual fun stringFormatInvariant(format: String, vararg args: Any?): String =
    String.format(Locale.ENGLISH, format, *args)
