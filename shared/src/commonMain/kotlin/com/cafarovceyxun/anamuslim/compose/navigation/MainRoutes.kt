package com.cafarovceyxun.anamuslim.compose.navigation

object MainRoutes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val HADITH = "hadith"
    /**
     * ⚠️ Sonundakı iki parametr **opsionaldır** (`?hadithId=…&q=…`) — yalnız axtarışdan gələn keçid
     * onları doldurur: babın içində hansı hədisə eniləcəyi və orada işarələnəcək sorğu.
     * URL-i əl ilə yığmayın, [hadithItems] işlədin: naviqasiya pattern-i yalnız parametrlər tam
     * yazılanda (ya da heç yazılmayanda) uyğun gəlir.
     */
    const val HADITH_ITEMS =
        "hadith_items/{volumeSlug}/{bookSlug}/{chapterSlug}/{subChapterSlug}/{title}?hadithId={hadithId}&q={q}"

    /**
     * [HADITH_ITEMS] üçün konkret ünvan. [title] və [query] **artıq kodlanmış** gəlməlidir
     * (Android-də `Uri.encode`) — burada kodlama yoxdur, çünki fayl ortaq koddadır.
     */
    fun hadithItems(
        volumeSlug: String?,
        bookSlug: String?,
        chapterSlug: String?,
        subChapterSlug: String?,
        encodedTitle: String,
        hadithId: Long? = null,
        encodedQuery: String? = null,
    ): String {
        val base = "hadith_items/${volumeSlug ?: "null"}/${bookSlug ?: "null"}/" +
            "${chapterSlug ?: "null"}/${subChapterSlug ?: "null"}/$encodedTitle"

        val params = buildList {
            if (hadithId != null) add("hadithId=$hadithId")
            if (!encodedQuery.isNullOrBlank()) add("q=$encodedQuery")
        }

        return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
    }
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val READER = "reader"

    /** The five bottom-nav tabs, in bar order — indices line up with `rememberMainNavItems()`. */
    val BOTTOM_NAV_ROUTES = listOf(HOME, QURAN, HADITH, SEARCH, SETTINGS)
}
