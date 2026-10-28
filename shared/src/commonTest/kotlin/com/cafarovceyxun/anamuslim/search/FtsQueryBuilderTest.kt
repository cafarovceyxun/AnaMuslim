package com.cafarovceyxun.anamuslim.search

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FtsQueryBuilderTest {

    private fun terms(raw: String) = FtsQueryBuilder.translationTerms(raw)

    @Test
    fun punctuationNeverReachesFtsAsAnOperator() {
        // Əvvəl `kitabdır,*`, `(c.c)*`, `-deməyin*` FTS-ə xam gedirdi: mötərizə qrup, öndəki `-` NOT idi.
        assertEquals(
            listOf("bu", "kitabdır", "şəkk-şübhə", "yoxdur"),
            terms("Bu, kitabdır  şəkk-şübhə yoxdur."),
        )
        assertEquals(listOf("allah", "c.c"), terms("Allah (c.c)"))
        assertEquals(listOf("deməyin"), terms("-deməyin"))
        assertEquals(listOf("rainə"), terms("“Rainə”"))
    }

    @Test
    fun everyTermIsQuotedWithTheStarInside() {
        // FTS4-də `"iman"*` prefiks DEYİL — ulduz dırnağın içində olmalıdır.
        assertEquals("\"iman*\" \"or*\"", FtsQueryBuilder.toMatchQuery(listOf("iman", "or")))
        assertEquals("text:\"iman*\"", FtsQueryBuilder.toMatchQuery(listOf("iman"), matchColumn = "text"))
    }

    @Test
    fun numbersAreKeptEvenWhenShortAndMatchedExactly() {
        assertEquals(listOf("7"), terms("7"))
        assertEquals("\"40\"", FtsQueryBuilder.toTranslationTextQuery("40"))
        assertEquals("\"40\" \"gün*\"", FtsQueryBuilder.toTranslationTextQuery("40 gün"))
        // Tək hərf hələ də atılır — «o» bütün bazanı qaytarardı.
        assertNull(FtsQueryBuilder.toTranslationTextQuery("o"))
    }

    @Test
    fun copiedVerseLabelAndFootnoteDigitsAreDropped() {
        // Oxucudan kopyalanan ayə: başlıq və üst-indeks haşiyə (`möminlər¹` → NFKC → `möminlər1`).
        assertEquals(listOf("onlar", "möminlər", "deyillər"), terms("-8: Onlar möminlər¹ deyillər"))
        assertEquals(listOf("edən"), terms("edən2"))
    }

    @Test
    fun capitalDottedIStaysOneWord() {
        // «İ».lowercase() iki simvoldur (i + U+0307) — söz parçalanmamalıdır.
        assertEquals(1, terms("İnsanlardan").size)
    }

    @Test
    fun longQueryDropsWordsTheIndexHasNowhere() = runTest {
        val known = setOf("\"ey*\"", "\"iman*\"", "\"gətirənlər*\"", "\"səbr*\"")
        val query = FtsQueryBuilder.toMatchQueryWithFallback(
            listOf("ey", "iman", "gətirənlər", "səbrr", "səbr"),
        ) { candidate -> candidate in known }

        assertEquals("\"ey*\" \"iman*\" \"gətirənlər*\" \"səbr*\"", query)
    }

    @Test
    fun strictQueryIsKeptWhenItMatchesOrWhenTooLittleSurvives() = runTest {
        val strict = FtsQueryBuilder.toMatchQueryWithFallback(listOf("ey", "iman", "səbr")) { true }
        assertEquals("\"ey*\" \"iman*\" \"səbr*\"", strict)

        // Beş sözdən yalnız biri tanınır — «təxmini» nəticə artıq sorğunun özü deyil.
        val onlyOne = FtsQueryBuilder.toMatchQueryWithFallback(
            listOf("aaa", "bbb", "ccc", "ddd", "iman"),
        ) { it == "\"iman*\"" }
        assertEquals("\"aaa*\" \"bbb*\" \"ccc*\" \"ddd*\" \"iman*\"", onlyOne)
    }

    @Test
    fun indexedTranslationTextLosesVerseLabelAndFootnotes() {
        assertEquals(
            "Onlar möminlər deyillər.",
            indexableTranslationText("-8: Onlar möminlər (1) deyillər."),
        )
        assertEquals(
            "onlar yalnız parçalanma (ixtilaf) içində",
            indexableTranslationText("-137: onlar yalnız parçalanma1 (ixtilaf) içində"),
        )
        // Adi say haşiyə deyil.
        assertEquals("40 gecə vəd etdik", indexableTranslationText("-51: 40 gecə vəd etdik"))
    }
}
