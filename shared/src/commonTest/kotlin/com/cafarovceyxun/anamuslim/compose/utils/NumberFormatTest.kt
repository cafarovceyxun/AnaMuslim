package com.cafarovceyxun.anamuslim.compose.utils

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs on both Android and iOS. Proves the numeral-shaping seam that replaces
 * `String.format(appLocale.platformLocale, "%d", value)` produces identical output on every
 * platform. Numeral selection is disabled today ([NumeralSystem] is always resolved to `null`),
 * so the `null`/LATN cases are the ones the live app hits.
 */
class NumberFormatTest {

    @Test
    fun nullNumeralYieldsWesternDigits() {
        val ns: NumeralSystem? = null
        assertEquals("1", ns.formatNumber(1))
        assertEquals("114", ns.formatNumber(114))
        assertEquals("0", ns.formatNumber(0))
        assertEquals("-7", ns.formatNumber(-7))
        assertEquals("2147483648", ns.formatNumber(2147483648L))
    }

    @Test
    fun latnMatchesNull() {
        assertEquals("42", NumeralSystem.LATN.formatNumber(42))
    }

    @Test
    fun arabicIndicShaping() {
        // "١٢٣" — Arabic-Indic digits for 123
        assertEquals("١٢٣", NumeralSystem.ARAB.formatNumber(123))
        // "۴۵۶" — Extended Arabic-Indic digits for 456
        assertEquals("۴۵۶", NumeralSystem.ARABEXT.formatNumber(456))
    }

    @Test
    fun shapeDigitsPreservesNonDigits() {
        // Mirrors the multi-arg "%1$d:%2$d" verse-reference sites once interpolated.
        val ns: NumeralSystem? = null
        assertEquals("2:5", "${ns.formatNumber(2)}:${ns.formatNumber(5)}")
        // Non-digit characters (separators, sign) pass through under shaping.
        assertEquals("١:٥", NumeralSystem.ARAB.shapeDigits("1:5"))
        assertEquals("٪١٠٠", NumeralSystem.ARAB.shapeDigits("٪100"))
    }

    @Test
    fun oneDecimalMatchesPlaybackSpeeds() {
        // The playback-speed menu's full option set, which is what this replaced `%.1f` for.
        val en = appLocale("en")
        assertEquals("0.5", en.formatOneDecimal(0.5f))
        assertEquals("0.8", en.formatOneDecimal(0.75f))
        assertEquals("1.0", en.formatOneDecimal(1.0f))
        assertEquals("1.3", en.formatOneDecimal(1.25f))
        assertEquals("1.5", en.formatOneDecimal(1.5f))
        assertEquals("2.0", en.formatOneDecimal(2.0f))
    }

    @Test
    fun oneDecimalUsesLocaleSeparator() {
        assertEquals("1,5", appLocale("az").formatOneDecimal(1.5f))
        assertEquals("1,5", appLocale("tr").formatOneDecimal(1.5f))
        assertEquals("1,5", appLocale("ru").formatOneDecimal(1.5f))
        assertEquals("1.5", appLocale("de").formatOneDecimal(1.5f))
    }

    @Test
    fun oneDecimalHandlesZeroAndNegatives() {
        val en = appLocale("en")
        assertEquals("0.0", en.formatOneDecimal(0f))
        assertEquals("-0.5", en.formatOneDecimal(-0.5f))
        assertEquals("-1.5", en.formatOneDecimal(-1.5f))
    }

    private fun appLocale(language: String) = AppLocale(
        rawLanguageTag = language,
        languageTag = language,
        language = language,
        numeralSystem = null,
    )
}
