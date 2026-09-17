package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.supabase.DuaCategory
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSubcategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Yastı vərəqləyici siyahısının **sırası**.
 *
 * Sıra səhv olsa ekranda heç nə sınmır — sadəcə sürüşdürəndə dualar gözlənilməz ardıcıllıqla gəlir,
 * istifadəçi isə bunu «dua itdi» kimi başa düşür. Ona görə hər qaydanın öz testi var.
 */
class DuaFlatListTest {

    private fun category(slug: String) = DuaCategory(slug = slug, name = "Kateqoriya $slug")

    private fun subcategory(slug: String, categorySlug: String) =
        DuaSubcategory(slug = slug, category_slug = categorySlug, name = "Alt $slug")

    private fun dua(id: Long, categorySlug: String, subcategorySlug: String? = null) = Dua(
        id = id,
        category_slug = categorySlug,
        subcategory_slug = subcategorySlug,
        source_type = "hadith",
        text_ar = "ع$id",
        text_az = "tərcümə $id",
    )

    private fun idsOf(entries: List<DuaFlatEntry>) = entries.map { it.dua.id }

    @Test
    fun directDuasComeBeforeSubcategoryDuas() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = listOf(subcategory("a1", "a")),
            duas = listOf(
                dua(1, "a", "a1"),
                dua(2, "a"),
            ),
        )

        // Siyahı ekranı da birbaşa duaları yuxarıda göstərir — iki yerdə sıra eyni olmalıdır.
        assertEquals(listOf(2L, 1L), idsOf(entries))
    }

    @Test
    fun categoriesKeepTheOrderTheyArriveIn() {
        val entries = flattenDuas(
            categories = listOf(category("b"), category("a")),
            subcategories = emptyList(),
            duas = listOf(dua(1, "a"), dua(2, "b")),
        )

        // Repozitoriya onsuz da `sort_no` üzrə düzülmüş qaytarır; burada yenidən sıralanmamalıdır.
        assertEquals(listOf(2L, 1L), idsOf(entries))
    }

    @Test
    fun subcategoriesKeepTheirIncomingOrder() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = listOf(subcategory("a2", "a"), subcategory("a1", "a")),
            duas = listOf(dua(1, "a", "a1"), dua(2, "a", "a2")),
        )

        assertEquals(listOf(2L, 1L), idsOf(entries))
    }

    @Test
    fun firstDuaOfEachGroupIsMarkedAsGroupStart() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = listOf(subcategory("a1", "a")),
            duas = listOf(dua(1, "a"), dua(2, "a"), dua(3, "a", "a1")),
        )

        assertEquals(listOf(true, false, true), entries.map { it.isGroupStart })
    }

    @Test
    fun groupTitleAndKeyPreferTheSubcategory() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = listOf(subcategory("a1", "a")),
            duas = listOf(dua(1, "a"), dua(2, "a", "a1")),
        )

        assertEquals("Kateqoriya a", entries[0].groupTitle)
        assertEquals("a", entries[0].groupKey)
        assertEquals("Alt a1", entries[1].groupTitle)
        assertEquals("a1", entries[1].groupKey)
    }

    /** Başlığı silinmiş dua səhifədə başlıqsız qalardı — ona görə siyahıya düşmür. */
    @Test
    fun duasWithoutAKnownCategoryAreDropped() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = emptyList(),
            duas = listOf(dua(1, "a"), dua(2, "silinmiş")),
        )

        assertEquals(listOf(1L), idsOf(entries))
    }

    /** Alt başlıq silinəndə dua sətri qalır — o, başlığın birbaşa duası kimi göstərilməlidir. */
    @Test
    fun duasPointingAtAMissingSubcategoryFallBackToTheCategory() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = emptyList(),
            duas = listOf(dua(1, "a", "silinmiş-alt")),
        )

        assertEquals(listOf(1L), idsOf(entries))
        assertEquals("a", entries.single().groupKey)
        assertTrue(entries.single().isGroupStart)
    }

    @Test
    fun emptyInputsGiveAnEmptyList() {
        assertTrue(flattenDuas(emptyList(), emptyList(), listOf(dua(1, "a"))).isEmpty())
        assertTrue(flattenDuas(listOf(category("a")), emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun indexOfGroupFindsTheFirstPageOfThatGroup() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = listOf(subcategory("a1", "a")),
            duas = listOf(dua(1, "a"), dua(2, "a"), dua(3, "a", "a1")),
        )

        assertEquals(0, indexOfGroup(entries, "a"))
        assertEquals(2, indexOfGroup(entries, "a1"))
    }

    /** Başqa cihazdan silinmiş qrup — ekran boş yox, siyahının başından açılmalıdır. */
    @Test
    fun indexOfGroupFallsBackToZeroForUnknownOrNullKeys() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = emptyList(),
            duas = listOf(dua(1, "a")),
        )

        assertEquals(0, indexOfGroup(entries, null))
        assertEquals(0, indexOfGroup(entries, "yoxdur"))
    }

    @Test
    fun groupDuaIdsReturnsOnlyThatGroup() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = listOf(subcategory("a1", "a")),
            duas = listOf(dua(1, "a"), dua(2, "a"), dua(3, "a", "a1")),
        )

        assertEquals(listOf(1L, 2L), groupDuaIds(entries, "a"))
        assertEquals(listOf(3L), groupDuaIds(entries, "a1"))
    }

    @Test
    fun groupPositionIsRelativeToTheGroupNotTheWholeList() {
        val entries = flattenDuas(
            categories = listOf(category("a"), category("b")),
            subcategories = emptyList(),
            duas = listOf(dua(1, "a"), dua(2, "a"), dua(3, "b")),
        )

        assertEquals(1 to 2, groupPositionOf(entries, 0))
        assertEquals(2 to 2, groupPositionOf(entries, 1))
        // Yeni mövzu — sayğac sıfırlanır, «3 / 3» olmur.
        assertEquals(1 to 1, groupPositionOf(entries, 2))
    }

    @Test
    fun groupPositionSurvivesAnOutOfRangePage() {
        val entries = flattenDuas(
            categories = listOf(category("a")),
            subcategories = emptyList(),
            duas = listOf(dua(1, "a")),
        )

        assertEquals(1 to 1, groupPositionOf(entries, 99))
        assertEquals(1 to 1, groupPositionOf(emptyList(), 0))
    }
}
