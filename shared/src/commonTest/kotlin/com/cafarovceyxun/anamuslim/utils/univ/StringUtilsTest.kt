package com.cafarovceyxun.anamuslim.utils.univ

import com.cafarovceyxun.anamuslim.utils.nfkcNormalize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringUtilsTest {

    @Test
    fun removeHtmlPreservesContent() {
        assertEquals("hello", StringUtils.removeHTML("<b>hello</b>", preserveContent = true))
        assertEquals("", StringUtils.removeHTML("<b>hello</b>", preserveContent = false))
    }

    @Test
    fun isRtlLanguage() {
        assertTrue(StringUtils.isRtlLanguage("ar"))
        assertTrue(StringUtils.isRtlLanguage("ar-SA"))
        assertTrue(StringUtils.isRtlLanguage("ar_EG"))
        assertFalse(StringUtils.isRtlLanguage("en"))
        assertFalse(StringUtils.isRtlLanguage(""))
        assertFalse(StringUtils.isRtlLanguage(null))
    }

    @Test
    fun escapeRegexMakesLiteral() {
        val escaped = StringUtils.escapeRegex("a.b*c")
        // The escaped form must match the literal string and nothing else.
        assertTrue(Regex(escaped).matches("a.b*c"))
        assertFalse(Regex(escaped).matches("axbxc"))
    }
}

class PlatformTextTest {

    @Test
    fun nfkcNormalizeComposesCompatibilityForms() {
        // U+FB01 (ﬁ ligature) → "fi" under NFKC on both platforms.
        assertEquals("fi", nfkcNormalize("ﬁ"))
        // Plain ASCII is unchanged.
        assertEquals("salam", nfkcNormalize("salam"))
    }
}
