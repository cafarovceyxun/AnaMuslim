package com.cafarovceyxun.anamuslim.utils.univ

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Runs on both Android and iOS. These patterns were `java.util.regex.Pattern`s read through
 * `matcher(q).find()` / `toMatchResult().group(n)`; they are now Kotlin [Regex] read through
 * `find(q)` / `groupValues[n]`. The search quick-links and the reference dialog depend on the
 * exact match/group behaviour, so it is pinned here.
 */
class RegexPatternTest {

    @Test
    fun verseJumpMatchesChapterAndVerse() {
        val m = RegexPattern.VERSE_JUMP_PATTERN.find("2:255")
        assertEquals(listOf("2:255", "2", "255"), m?.groupValues)
    }

    @Test
    fun verseJumpToleratesSpacesAroundColon() {
        // The pattern allows optional whitespace on either side of the colon.
        assertEquals("18", RegexPattern.VERSE_JUMP_PATTERN.find("18 : 10")?.groupValues?.get(1))
        assertEquals("10", RegexPattern.VERSE_JUMP_PATTERN.find("18 : 10")?.groupValues?.get(2))
    }

    @Test
    fun verseRangeJumpMatchesAllThreeGroups() {
        val m = RegexPattern.VERSE_RANGE_JUMP_PATTERN.find("2:255-260")
        assertEquals(listOf("2:255-260", "2", "255", "260"), m?.groupValues)
    }

    @Test
    fun verseRangeJumpDoesNotMatchPlainVerseJump() {
        assertNull(RegexPattern.VERSE_RANGE_JUMP_PATTERN.find("2:255"))
    }

    @Test
    fun verseRangeMatchesFromAndTo() {
        val m = RegexPattern.VERSE_RANGE_PATTERN.find("255-260")
        assertEquals(listOf("255-260", "255", "260"), m?.groupValues)
    }

    @Test
    fun chapterOrJuzMatchesFirstNumberOnly() {
        // `find` scans for the first match rather than anchoring, as `matcher().find()` did.
        assertEquals("30", RegexPattern.CHAPTER_OR_JUZ_PATTERN.find("30")?.groupValues?.get(1))
        assertEquals("2", RegexPattern.CHAPTER_OR_JUZ_PATTERN.find("2:255")?.groupValues?.get(1))
    }

    @Test
    fun nonNumericQueryMatchesNothing() {
        assertNull(RegexPattern.CHAPTER_OR_JUZ_PATTERN.find("Fatihə"))
        assertNull(RegexPattern.VERSE_JUMP_PATTERN.find("Fatihə"))
    }
}
