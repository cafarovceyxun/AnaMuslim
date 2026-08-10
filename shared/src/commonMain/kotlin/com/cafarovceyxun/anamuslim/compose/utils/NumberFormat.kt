package com.cafarovceyxun.anamuslim.compose.utils

import kotlin.math.floor

/**
 * Locale-numeral-aware integer rendering for common UI code, replacing the Android-only
 * `String.format(appLocale.platformLocale, "%d", value)` calls at sites that only shape digits
 * (no grouping separators, no decimals).
 *
 * Digit shaping is driven purely by [NumeralSystem], which already lives in `commonMain`. Numeral
 * selection is currently disabled (`numeralSystemsForLanguage` is empty ⇒ [AppLocale.numeralSystem]
 * is always `null`), so this yields Western digits — byte-identical to the previous Latin-locale
 * `%d` behaviour on every platform, while giving the seam a single place to switch on once
 * Arabic-Indic numerals are re-enabled.
 *
 * Only integer digit shaping lives here on purpose; locale-sensitive decimal separators and
 * grouping stay on the Android `String.format` path until the full `AppLocale` seam lands.
 */
fun NumeralSystem?.shapeDigits(ascii: String): String {
    val zero: Char = when (this) {
        null, NumeralSystem.LATN -> return ascii
        NumeralSystem.ARAB -> '٠'    // ٠ Arabic-Indic
        NumeralSystem.ARABEXT -> '۰' // ۰ Extended Arabic-Indic (Persian/Urdu)
    }
    val sb = StringBuilder(ascii.length)
    for (c in ascii) {
        sb.append(if (c in '0'..'9') zero + (c - '0') else c)
    }
    return sb.toString()
}

/** Renders [value] with numeral shaping. Replaces `String.format(locale, "%d", value)`. */
fun NumeralSystem?.formatNumber(value: Int): String = shapeDigits(value.toString())

/** Renders [value] with numeral shaping. Replaces `String.format(locale, "%d", value)`. */
fun NumeralSystem?.formatNumber(value: Long): String = shapeDigits(value.toString())

/**
 * Decimal separator for [language]. Covers the languages the app ships (en/az/tr/ru); anything
 * else falls back to a dot. Narrow on purpose — this exists so the few decimal call sites can
 * leave the Android `String.format` path, not to reimplement CLDR.
 */
fun decimalSeparatorFor(language: String): Char = when (language) {
    "az", "tr", "ru" -> ','
    else -> '.'
}

/**
 * Renders [value] with one decimal place, the locale's decimal separator and numeral shaping.
 * Replaces `String.format(locale, "%.1f", value)`.
 */
fun AppLocale.formatOneDecimal(value: Float): String {
    // Ties round away from zero, matching the `String.format("%.1f")` (HALF_UP) this replaced —
    // kotlin.math.round would round half-to-even and render 1.25 as "1.2" instead of "1.3".
    val scaled = value.toDouble() * 10
    val rounded = (if (scaled < 0) -floor(-scaled + 0.5) else floor(scaled + 0.5)).toLong()

    val whole = rounded / 10
    val tenth = if (rounded < 0) -(rounded % 10) else rounded % 10
    val sign = if (rounded < 0 && whole == 0L) "-" else ""

    return numeralSystem.shapeDigits("$sign$whole${decimalSeparatorFor(language)}$tenth")
}
