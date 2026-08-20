package com.cafarovceyxun.anamuslim.compose.screens.hadith

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Which of a hadith level's two names leads, and what is left underneath it.
 *
 * The rule is easy to get subtly wrong in a way no screen makes obvious: a row whose `name_ar` was
 * never filled in must keep its translated name even in Arabic, or switching the app language would
 * blank out that row. These cases pin both directions.
 */
class HadithDisplayNamesTest {

    @Test
    fun arabicUiLeadsWithTheArabicNameAndRepeatsNothing() {
        val name = hadithDisplayName("İman kitabı", "كتاب الإيمان", arabicUi = true)

        assertEquals("كتاب الإيمان", name.text)
        assertTrue(name.isArabic)
        assertNull(name.secondaryArabic)
    }

    @Test
    fun otherLanguagesLeadWithTheTranslationAndKeepArabicUnderneath() {
        val name = hadithDisplayName("İman kitabı", "كتاب الإيمان", arabicUi = false)

        assertEquals("İman kitabı", name.text)
        assertFalse(name.isArabic)
        assertEquals("كتاب الإيمان", name.secondaryArabic)
    }

    @Test
    fun arabicUiFallsBackToTheTranslationWhenThereIsNoArabicName() {
        for (missing in listOf(null, "", "   ")) {
            val name = hadithDisplayName("İman kitabı", missing, arabicUi = true)

            assertEquals("İman kitabı", name.text, "name_ar = ${missing?.let { "\"$it\"" }}")
            assertFalse(name.isArabic)
            assertNull(name.secondaryArabic)
        }
    }

    @Test
    fun searchMatchesEitherNameWhicheverIsOnScreen() {
        assertTrue(hadithNameMatches("iman", "İman kitabı", "كتاب الإيمان"))
        assertTrue(hadithNameMatches("الإيمان", "İman kitabı", "كتاب الإيمان"))
        // Case-insensitive, but only on plain ASCII: the dotted İ lowercases differently per
        // platform, so nothing here leans on it.
        assertTrue(hadithNameMatches("KITABI", "İman kitabi", "كتاب الإيمان"))
        assertFalse(hadithNameMatches("namaz", "İman kitabı", "كتاب الإيمان"))
        assertFalse(hadithNameMatches("الصلاة", "İman kitabı", null))
    }
}
