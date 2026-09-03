package com.cafarovceyxun.anamuslim.compose.utils.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Ana ekran düzəninin bir sətirdə saxlanması — oxuma tərəfi burada qorunur. */
class HomePreferencesTest {

    @Test
    fun `empty layout falls back to the default order with everything visible`() {
        val parsed = HomePreferences.parse("")

        assertEquals(HomePreferences.DEFAULT_ORDER, parsed.map { it.section })
        assertTrue(parsed.all { it.visible })
    }

    @Test
    fun `hidden sections carry a bang prefix through a round trip`() {
        val states = listOf(
            HomeSectionState(HomeSection.BOOKMARKS, visible = true),
            HomeSectionState(HomeSection.STORIES, visible = false),
        )

        val parsed = HomePreferences.parse(HomePreferences.serialize(states))

        assertEquals(HomeSection.BOOKMARKS, parsed[0].section)
        assertTrue(parsed[0].visible)
        assertEquals(HomeSection.STORIES, parsed[1].section)
        assertTrue(!parsed[1].visible)
    }

    /**
     * Tətbiqə yeni bölmə gələndə köhnə düzən qorunmalı, yenilik isə gözdən qaçmamalıdır — ona görə
     * sətirdə olmayan bölmə **sona, görünən** halda düşür.
     */
    @Test
    fun `sections missing from the stored string are appended visible`() {
        val parsed = HomePreferences.parse("bookmarks")

        assertEquals(HomeSection.BOOKMARKS, parsed.first().section)
        assertEquals(HomePreferences.DEFAULT_ORDER.size, parsed.size)
        assertTrue(parsed.all { it.visible })
    }

    /** Açar silinəndə və ya sətir zədələnəndə ekran boş qalmamalıdır. */
    /**
     * Yeni quraşdırmada **hekayə zolağı birinci, namaz ikinci**dir; saxlanılan sətirdə olmayan
     * bölmə isə sona, **görünən** halda düşür. Parseri bunun üçün dəyişmək lazım deyildi; test
     * həm defolt sıranı, həm də həmin davranışın qorunduğunu təsbit edir.
     */
    @Test
    fun `stories lead the default order and missing sections are appended`() {
        val fresh = HomePreferences.parse("").map { it.section }
        assertEquals(
            listOf(HomeSection.STORIES, HomeSection.PRAYER),
            fresh.take(2),
            "yeni quraşdırmada hekayə üstdə, namaz altda olmalıdır",
        )

        val existing = HomePreferences.parse("stories,read_history,bookmarks")
        val prayerIndex = existing.indexOfFirst { it.section == HomeSection.PRAYER }

        // Saxlanılan üç bölmə sıranı saxlayır; çatışmayanlar (namaz da daxil) onlardan SONRA gəlir.
        assertTrue(prayerIndex >= 3, "köhnə düzəndə saxlanılanlardan sonra gəlməlidir: $existing")
        assertTrue(existing[prayerIndex].visible, "yeni bölmə görünən gəlməlidir")
        assertEquals(
            listOf(HomeSection.STORIES, HomeSection.READ_HISTORY, HomeSection.BOOKMARKS),
            existing.take(3).map { it.section },
            "mövcud düzən pozulmamalıdır",
        )
    }

    @Test
    fun `unknown keys are dropped rather than breaking the layout`() {
        val parsed = HomePreferences.parse("bookmarks,qeyri_movcud,!stories")

        assertEquals(HomePreferences.DEFAULT_ORDER.size, parsed.size)
        assertEquals(HomeSection.BOOKMARKS, parsed[0].section)
        assertEquals(HomeSection.STORIES, parsed[1].section)
        assertTrue(!parsed[1].visible)
    }
}
