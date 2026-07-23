package com.cafarovceyxun.anamuslim.compose.navigation

object MainRoutes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val HADITH = "hadith"
    const val HADITH_ITEMS = "hadith_items/{volumeSlug}/{bookSlug}/{chapterSlug}/{subChapterSlug}/{title}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val READER = "reader"

    /** The five bottom-nav tabs, in bar order — indices line up with `rememberMainNavItems()`. */
    val BOTTOM_NAV_ROUTES = listOf(HOME, QURAN, HADITH, SEARCH, SETTINGS)
}
