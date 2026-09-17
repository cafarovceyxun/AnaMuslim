package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.supabase.DuaCategory
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSubcategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Naviqator vərəqinin sətirləri.
 *
 * Səhv nəticə ekranda «yanlış yerə tulladı» kimi görünür: indeks bir səhifə sürüşsə də istifadəçi
 * gözlədiyi mövzunun ortasına düşür, sayğac isə («12 dua») sakitcə yalan danışır.
 */
class DuaNavigatorTest {

    private val categories = listOf(
        DuaCategory(slug = "namaz", name = "Namaz"),
        DuaCategory(slug = "sefer", name = "Səfər"),
    )

    private val subcategories = listOf(
        DuaSubcategory(slug = "sonra", category_slug = "namaz", name = "Namazdan sonra"),
        DuaSubcategory(slug = "seher", category_slug = "sefer", name = "Səhər zikri"),
    )

    private fun dua(id: Long, category: String, subcategory: String? = null) = Dua(
        id = id,
        category_slug = category,
        subcategory_slug = subcategory,
        source_type = "hadith",
        text_ar = "ع",
        text_az = "tərcümə",
    )

    private fun entries(vararg duas: Dua) = flattenDuas(categories, subcategories, duas.toList())

    @Test
    fun groupsFollowThePagerOrderAndCountTheirPages() {
        val list = entries(
            dua(1, "namaz"),
            dua(2, "namaz"),
            dua(3, "namaz", "sonra"),
            dua(4, "sefer", "seher"),
        )

        val groups = duaNavigatorGroups(list)

        assertEquals(listOf("namaz", "sonra", "seher"), groups.map { it.key })
        assertEquals(listOf(2, 1, 1), groups.map { it.count })
        // İndeks vərəqləyicinin səhifəsidir: «Namazdan sonra» üçüncü səhifədən başlayır.
        assertEquals(listOf(0, 2, 3), groups.map { it.firstIndex })
    }

    /** Başlığın adı yalnız **alt başlıq** sətirlərində kontekst kimi durur. */
    @Test
    fun onlySubcategoryRowsCarryTheParentTitle() {
        val groups = duaNavigatorGroups(entries(dua(1, "namaz"), dua(2, "namaz", "sonra")))

        assertEquals(null, groups.first { it.key == "namaz" }.parentTitle)
        assertEquals("Namaz", groups.first { it.key == "sonra" }.parentTitle)
    }

    /**
     * Süzgəc başlığın adına da baxır: «Namaz» yazan adam onun alt mövzularını gözləyir, halbuki
     * «Namazdan sonra» sətrinin **öz** adında da söz keçdiyi üçün bu, ikinci bir nümunə ilə
     * yoxlanılır — alt mövzunun adında ana başlığın sözü olmaya bilər.
     */
    @Test
    fun theFilterMatchesTheParentTitleToo() {
        val groups = duaNavigatorGroups(entries(dua(1, "sefer", "seher"), dua(2, "namaz")))

        assertEquals(listOf("seher"), filterDuaNavigatorGroups(groups, "səfər").map { it.key })
        assertEquals(listOf("seher"), filterDuaNavigatorGroups(groups, "SƏHƏR").map { it.key })
        assertTrue(filterDuaNavigatorGroups(groups, "yoxdur").isEmpty())
        // Boş sorğu siyahını olduğu kimi qaytarır — süzgəc yazılmayıb.
        assertEquals(groups, filterDuaNavigatorGroups(groups, "   "))
    }

    /**
     * Başlıq yalnız **bütün** mövzuları bitəndə ✓ alır.
     *
     * Səhv olsa istifadəçi bitirmədiyi başlığı bitmiş sanır — ekranda heç bir xəbərdarlıq yoxdur,
     * sadəcə yanlış nişan.
     */
    @Test
    fun aCategoryIsDoneOnlyWhenEveryOneOfItsTopicsIsDone() {
        val list = entries(
            dua(1, "namaz"),
            dua(2, "namaz", "sonra"),
            dua(3, "sefer", "seher"),
        )

        assertEquals(emptySet(), completedDuaCategories(list, setOf("namaz")))
        assertEquals(setOf("namaz"), completedDuaCategories(list, setOf("namaz", "sonra")))
        assertEquals(
            setOf("namaz", "sefer"),
            completedDuaCategories(list, setOf("namaz", "sonra", "seher")),
        )
        // Boş dəst heç nəyi bitmiş etmir.
        assertEquals(emptySet(), completedDuaCategories(list, emptySet()))
    }

    @Test
    fun anEmptyPagerHasNoGroups() {
        assertTrue(duaNavigatorGroups(emptyList()).isEmpty())
    }
}
