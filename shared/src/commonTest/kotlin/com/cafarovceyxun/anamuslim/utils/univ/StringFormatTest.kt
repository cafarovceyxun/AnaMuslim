package com.cafarovceyxun.anamuslim.utils.univ

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs on both Android (backed by `String.format`) and iOS (backed by the custom
 * formatter). Passing on both proves the iOS actual matches Java's behavior for
 * every format string actually used in the app.
 */
class StringFormatTest {

    @Test
    fun hexColorPadded() {
        // compose/theme/Color.kt: "#%06X"
        assertEquals("#FFFFFF", stringFormatInvariant("#%06X", 0xFFFFFF))
        assertEquals("#001A2B", stringFormatInvariant("#%06X", 0x1A2B))
    }

    @Test
    fun zeroPaddedIntegers() {
        // WbwAudioRepository: "%03d.webm" / "%03d_%03d_%03d.mp3"
        assertEquals("005.webm", stringFormatInvariant("%03d.webm", 5))
        assertEquals("001_002_003.mp3", stringFormatInvariant("%03d_%03d_%03d.mp3", 1, 2, 3))
    }

    @Test
    fun mixedIntAndString() {
        // TranslUtils: "translation_%d_%s_%s.json"
        assertEquals(
            "translation_1_en_sahih.json",
            stringFormatInvariant("translation_%d_%s_%s.json", 1, "en", "sahih"),
        )
        // ChapterInfoUtils: "chapter_info_%d-%s.json"
        assertEquals(
            "chapter_info_7-az.json",
            stringFormatInvariant("chapter_info_%d-%s.json", 7, "az"),
        )
    }

    @Test
    fun plainSpecifiers() {
        assertEquals("42", stringFormatInvariant("%d", 42))
        assertEquals("ff", stringFormatInvariant("%x", 255))
        assertEquals("no specifiers", stringFormatInvariant("no specifiers"))
    }

    @Test
    fun escapedPercent() {
        assertEquals("100%", stringFormatInvariant("100%%"))
    }
}
