package com.cafarovceyxun.anamuslim.utils.translation

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranslationImportParserTest {

    @Test
    fun numberedParagraphsMapToTheirVerseNumbers() {
        val raw = """
            1. Mərhəmətli, Rəhmli Allahın adı ilə!
            2. Həmd olsun aləmlərin Rəbbi Allaha,
            3. Mərhəmətli, Rəhmli Allaha,
        """.trimIndent()
        val result = parseTranslationImport(raw, verseCount = 3, mode = ImportParseMode.ByVerseNumber)

        assertEquals(listOf(1, 2, 3), result.verses.map { it.verseNo })
        assertEquals("Mərhəmətli, Rəhmli Allahın adı ilə!", result.verses[0].text)
        assertEquals(emptyList(), result.missing)
        assertTrue(result.verses.all { it.problem == null })
    }

    @Test
    fun differentNumberSeparatorsAreAccepted() {
        val raw = "1) Birinci\n\n(2) İkinci\n\n[3] Üçüncü\n\n4 - Dördüncü\n\n5: Beşinci"
        val result = parseTranslationImport(raw, verseCount = 5, mode = ImportParseMode.ByVerseNumber)

        assertEquals(listOf(1, 2, 3, 4, 5), result.verses.map { it.verseNo })
        assertEquals(listOf("Birinci", "İkinci", "Üçüncü", "Dördüncü", "Beşinci"), result.verses.map { it.text })
    }

    @Test
    fun unnumberedParagraphContinuesThePreviousVerse() {
        val raw = "1. Birinci hissə\n\nikinci hissə\n\n2. Növbəti ayə"
        val result = parseTranslationImport(raw, verseCount = 2, mode = ImportParseMode.ByVerseNumber)

        assertEquals(listOf(1, 2), result.verses.map { it.verseNo })
        assertEquals("Birinci hissə\n\nikinci hissə", result.verses[0].text)
    }

    @Test
    fun sequentialModeAssignsParagraphsInOrder() {
        val raw = "Birinci ayə\n\nİkinci ayə\n\nÜçüncü ayə"
        val result = parseTranslationImport(raw, verseCount = 3, mode = ImportParseMode.SequentialParagraphs)

        assertEquals(listOf(1, 2, 3), result.verses.map { it.verseNo })
        assertEquals("İkinci ayə", result.verses[1].text)
        assertEquals(emptyList(), result.warnings)
    }

    @Test
    fun sequentialModeWarnsWhenParagraphCountDiffers() {
        val raw = "Bir\n\nİki"
        val result = parseTranslationImport(raw, verseCount = 5, mode = ImportParseMode.SequentialParagraphs)

        assertTrue(result.warnings.any { it.contains("Abzas sayı") })
        assertEquals(listOf(3, 4, 5), result.missing)
    }

    @Test
    fun gapsAreReportedAsMissingVerses() {
        val raw = "1. Bir\n\n3. Üç"
        val result = parseTranslationImport(raw, verseCount = 4, mode = ImportParseMode.ByVerseNumber)

        assertEquals(listOf(2, 4), result.missing)
        assertTrue(result.warnings.any { it.contains("mətnsiz") })
    }

    @Test
    fun duplicateNumberKeepsTheLastTextAndIsFlagged() {
        val raw = "1. Köhnə mətn\n\n1. Yeni mətn\n\n2. İkinci"
        val result = parseTranslationImport(raw, verseCount = 2, mode = ImportParseMode.ByVerseNumber)

        assertEquals("Yeni mətn", result.verses.first { it.verseNo == 1 }.text)
        assertEquals(ImportProblem.Duplicate, result.verses.first { it.verseNo == 1 }.problem)
        assertTrue(result.warnings.any { it.contains("Təkrarlanan") })
    }

    @Test
    fun outOfRangeNumberIsFlaggedAndExcludedFromUsable() {
        val raw = "1. Bir\n\n99. Doxsan doqquz"
        val result = parseTranslationImport(raw, verseCount = 3, mode = ImportParseMode.ByVerseNumber)

        assertEquals(ImportProblem.OutOfRange, result.verses.first { it.verseNo == 99 }.problem)
        assertEquals(listOf(1), result.usable.map { it.verseNo })
        assertTrue(result.warnings.any { it.contains("kənardadır") })
    }

    @Test
    fun singleLineTextIsSplitByNewlineWhenThereAreNoBlankLines() {
        val raw = "1. Bir\n2. İki\n3. Üç"
        val result = parseTranslationImport(raw, verseCount = 3, mode = ImportParseMode.ByVerseNumber)

        assertEquals(3, result.verses.size)
        assertEquals("Üç", result.verses[2].text)
    }

    @Test
    fun crlfLineEndingsAreHandled() {
        val raw = "1. Bir\r\n\r\n2. İki"
        val result = parseTranslationImport(raw, verseCount = 2, mode = ImportParseMode.ByVerseNumber)

        assertEquals(listOf(1, 2), result.verses.map { it.verseNo })
        assertEquals("İki", result.verses[1].text)
    }

    @Test
    fun emptyInputReportsEveryVerseAsMissing() {
        val result = parseTranslationImport("   \n\n  ", verseCount = 3, mode = ImportParseMode.ByVerseNumber)

        assertEquals(emptyList(), result.verses)
        assertEquals(listOf(1, 2, 3), result.missing)
        assertContains(result.warnings, "Mətn boşdur.")
    }

    @Test
    fun leadingNumberInsideTheSentenceIsNotTreatedAsAVerseNumber() {
        // Nömrə yalnız abzasın ƏVVƏLİNDƏ axtarılır; ortadakı rəqəm mətnin bir hissəsidir.
        val raw = "1. Bu ayədə 40 gün qeyd olunur"
        val result = parseTranslationImport(raw, verseCount = 1, mode = ImportParseMode.ByVerseNumber)

        assertEquals(1, result.verses.single().verseNo)
        assertEquals("Bu ayədə 40 gün qeyd olunur", result.verses.single().text)
        assertNull(result.verses.single().problem)
    }
}
