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
    @Test
    fun `unknown keys are dropped rather than breaking the layout`() {
        val parsed = HomePreferences.parse("bookmarks,qeyri_movcud,!stories")

        assertEquals(HomePreferences.DEFAULT_ORDER.size, parsed.size)
        assertEquals(HomeSection.BOOKMARKS, parsed[0].section)
        assertEquals(HomeSection.STORIES, parsed[1].section)
        assertTrue(!parsed[1].visible)
    }
}
