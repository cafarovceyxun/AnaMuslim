package com.cafarovceyxun.anamuslim.utils.univ

/**
 * Locale-invariant printf-style formatting.
 * Android delegates to `String.format(Locale.ENGLISH, ...)`; iOS uses a focused
 * formatter covering the conversions used in this app (`%d %i %x %X %s`, `0`/width).
 */
expect fun stringFormatInvariant(format: String, vararg args: Any?): String
