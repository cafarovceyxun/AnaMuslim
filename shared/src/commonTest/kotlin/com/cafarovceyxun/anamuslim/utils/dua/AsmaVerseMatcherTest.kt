package com.cafarovceyxun.anamuslim.utils.dua

import com.cafarovceyxun.anamuslim.search.FtsQueryBuilder
import com.cafarovceyxun.anamuslim.search.SearchNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Əsmaül Hüsnə ↔ ayə uyğunlaşdırmasının **sorğu tərəfi**.
 *
 * Bu, gözlə tutulmayan sinifdəndir: səhv variant siyahısı ekranda «nəticə yoxdur» və ya əksinə,
 * onlarla əlaqəsiz ayə kimi görünür — nə kompilyator, nə də ekran görüntüsü xəbər verir. Ona görə
 * adların **həqiqi** yazılışları ilə yoxlanılır.
 */
class AsmaVerseMatcherTest {

    @Test
    fun harakatAndAlifVariantsAreStripped() {
        // `asma_name.name_ar` mushaf yazılışındadır — hərəkəli, bəzən alif wasla ilə.
        assertEquals(listOf("الرحمن", "رحمن"), AsmaVerseMatcher.arabicNameVariants("الرَّحْمَٰن"))
        assertEquals(listOf("الرحيم", "رحيم"), AsmaVerseMatcher.arabicNameVariants("الرَّحِيم"))
    }

    @Test
    fun theDefiniteArticleIsStrippedAsASecondVariant() {
        // Quran hər iki formanı işlədir: «الْمَلِك» ↔ «مَلِكِ النَّاسِ».
        assertEquals(listOf("الملك", "ملك"), AsmaVerseMatcher.arabicNameVariants("الْمَلِك"))
        assertEquals(listOf("السلام", "سلام"), AsmaVerseMatcher.arabicNameVariants("السَّلَام"))
    }

    /**
     * Ən vacib qayda: iki hərflik qalıq **atılır**.
     *
     * «الله» → «له» Quranda əvəzlik kimi yüzlərlə yerdə, «الحي» → «حي» isə adi sifət kimi keçir.
     * Onları axtarmaq siyahını istifadəsiz edərdi.
     */
    @Test
    fun shortStemsAreNotUsedAsASeparateVariant() {
        assertEquals(listOf("الله"), AsmaVerseMatcher.arabicNameVariants("اللَّه"))
        assertEquals(listOf("الحي"), AsmaVerseMatcher.arabicNameVariants("الْحَيّ"))
    }

    @Test
    fun onlyTheLeadingArticleIsStrippedInMultiWordNames() {
        val variants = AsmaVerseMatcher.arabicNameVariants("مَالِكُ الْمُلْك")

        // İkinci sözün artiklı ifadənin bir hissəsidir — atılsa ad tanınmaz olardı.
        assertEquals(listOf("مالك الملك"), variants)
    }

    @Test
    fun nonArabicOrEmptyNamesGiveNoVariants() {
        assertTrue(AsmaVerseMatcher.arabicNameVariants("").isEmpty())
        assertTrue(AsmaVerseMatcher.arabicNameVariants("   ").isEmpty())
        assertTrue(AsmaVerseMatcher.arabicNameVariants("ar-Rahman").isEmpty())
    }

    @Test
    fun matchingIsWholeWordOnly() {
        val ayah = SearchNormalizer.quranNormalize("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ")

        assertTrue(AsmaVerseMatcher.containsAsWord(ayah, "الرحمن"))
        assertTrue(AsmaVerseMatcher.containsAsWord(ayah, "الله"))
        // «رحمن» ayədə yalnız «الرحمن»-in içindədir, ayrıca söz kimi yoxdur.
        assertFalse(AsmaVerseMatcher.containsAsWord(ayah, "رحمن"))
    }

    @Test
    fun wordMatchingRejectsSubstringsOfLongerWords() {
        val ayah = SearchNormalizer.quranNormalize("وَهُوَ الْحَيُّ الْقَيُّومُ")

        assertTrue(AsmaVerseMatcher.containsAsWord(ayah, "الحي"))
        // «الحياة» kimi uzun sözün içindəki «الحي» uyğunluq sayılmamalıdır.
        assertFalse(AsmaVerseMatcher.containsAsWord("الحياة الدنيا", "الحي"))
    }

    @Test
    fun emptyVariantNeverMatches() {
        assertFalse(AsmaVerseMatcher.containsAsWord("الرحمن", ""))
    }

    // ---- FTS sorğusu ----

    @Test
    fun exactOrQueryJoinsVariantsWithoutPrefixWildcards() {
        val query = FtsQueryBuilder.toExactOrQuery(listOf("الرحمن", "رحمن"))

        assertEquals("الرحمن OR رحمن", query)
        // Prefiks `*` olsaydı «الحي*» «الحياة»-ni də tutardı — bax [toExactOrQuery].
        assertFalse(query!!.contains("*"))
    }

    @Test
    fun multiWordVariantsBecomeAPhrase() {
        assertEquals("\"مالك الملك\"", FtsQueryBuilder.toExactOrQuery(listOf("مالك الملك")))
    }

    @Test
    fun exactOrQueryDropsUnusableVariants() {
        assertNull(FtsQueryBuilder.toExactOrQuery(emptyList()))
        assertNull(FtsQueryBuilder.toExactOrQuery(listOf("", "   ")))
        // Tək hərflik token FTS üçün mənasızdır.
        assertNull(FtsQueryBuilder.toExactOrQuery(listOf("ا")))
    }

    @Test
    fun duplicateVariantsAreCollapsed() {
        assertEquals("الرحمن", FtsQueryBuilder.toExactOrQuery(listOf("الرحمن", "الرحمن")))
    }

    /** Mövcud Quran/tərcümə axtarışı prefiks davranışını saxlamalıdır — reqressiya. */
    @Test
    fun prefixQueryBehaviourIsUnchanged() {
        assertEquals("الرحمن*", FtsQueryBuilder.toPrefixAndQuery("الرحمن"))
        assertEquals("بسم* الله*", FtsQueryBuilder.toPrefixAndQuery("بسم الله"))
    }
}
