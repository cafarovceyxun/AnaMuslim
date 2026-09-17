package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.supabase.DuaCategory
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSubcategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Dua vərəqləyicisindəki axtarış.
 *
 * Səhv nəticə ekranda «axtarış işləmir» kimi görünür, kompilyator isə susur — xüsusən ərəbcə
 * tərəfdə, çünki mushafdan kopyalanan mətn hərəkəlidir, indeksdəki forma isə deyil.
 */
class DuaSearchTest {

    private fun entries(vararg duas: Dua): List<DuaFlatEntry> = flattenDuas(
        categories = listOf(DuaCategory(slug = "a", name = "Namazdan sonra")),
        subcategories = listOf(DuaSubcategory(slug = "a1", category_slug = "a", name = "Səhər zikri")),
        duas = duas.toList(),
    )

    private fun dua(
        id: Long,
        textAr: String = "",
        translit: String = "",
        textAz: String = "",
        source: String? = null,
        subcategorySlug: String? = null,
    ) = Dua(
        id = id,
        category_slug = "a",
        subcategory_slug = subcategorySlug,
        source_type = "hadith",
        text_ar = textAr,
        text_az = textAz,
        transliteration = translit,
        source = source,
    )

    @Test
    fun findsMatchesInTheTranslation() {
        val list = entries(dua(1, textAz = "Allahım, Sənə sığınıram"), dua(2, textAz = "Həmd olsun"))

        assertEquals(listOf(0), duaSearchMatches(list, "sığınıram"))
        assertEquals(listOf(1), duaSearchMatches(list, "həmd"))
    }

    @Test
    fun findsMatchesInTransliterationAndSource() {
        val list = entries(
            dua(1, translit = "Allahummə əntəs-sələəmu"),
            dua(2, source = "Buxari və Muslim"),
        )

        assertEquals(listOf(0), duaSearchMatches(list, "sələəmu"))
        assertEquals(listOf(1), duaSearchMatches(list, "buxari"))
    }

    /** Mushafdan kopyalanan mətn hərəkəlidir — axtarış onu hərəkəsiz sorğu ilə də tapmalıdır. */
    @Test
    fun arabicMatchingIgnoresHarakat() {
        val list = entries(dua(1, textAr = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"))

        assertEquals(listOf(0), duaSearchMatches(list, "الرحمن"))
        assertEquals(listOf(0), duaSearchMatches(list, "الرَّحْمَٰنِ"))
    }

    /** Mövzu adı da axtarılır: söz duanın öz mətnində olmaya bilər. */
    @Test
    fun groupTitlesAreSearchable() {
        val list = entries(dua(1, textAz = "mətn"), dua(2, textAz = "mətn", subcategorySlug = "a1"))

        assertEquals(listOf(0, 1), duaSearchMatches(list, "namazdan"))
        assertEquals(listOf(1), duaSearchMatches(list, "səhər"))
    }

    @Test
    fun emptyOrTooShortQueriesMatchNothing() {
        val list = entries(dua(1, textAz = "Allahım"))

        assertTrue(duaSearchMatches(list, "").isEmpty())
        assertTrue(duaSearchMatches(list, "   ").isEmpty())
        // Bir hərflik sorğu hər səhifəni tutardı — axtarışın öz qaydası ilə eyni.
        assertTrue(duaSearchMatches(list, "a").isEmpty())
    }

    @Test
    fun positionIsOneBasedAndZeroOffMatches() {
        val matches = listOf(1, 4, 7)

        assertEquals(1, duaMatchPosition(matches, 1))
        assertEquals(3, duaMatchPosition(matches, 7))
        assertEquals(0, duaMatchPosition(matches, 2))
    }

    @Test
    fun nextAndPreviousWalkTheMatches() {
        val matches = listOf(1, 4, 7)

        assertEquals(4, duaNextMatch(matches, 1))
        assertEquals(4, duaPreviousMatch(matches, 7))
        assertEquals(1, duaNextMatch(matches, 0))
    }

    /** Sonuncudan «növbəti» başa qayıdır — «səhifədə tap» ilə eyni davranış. */
    @Test
    fun navigationWrapsAround() {
        val matches = listOf(1, 4, 7)

        assertEquals(1, duaNextMatch(matches, 7))
        assertEquals(7, duaPreviousMatch(matches, 1))
    }

    @Test
    fun navigationOnAnEmptyMatchListGivesNull() {
        assertNull(duaNextMatch(emptyList(), 0))
        assertNull(duaPreviousMatch(emptyList(), 0))
    }
}
