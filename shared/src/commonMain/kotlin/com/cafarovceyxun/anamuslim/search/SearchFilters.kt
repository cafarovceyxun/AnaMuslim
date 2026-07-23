package com.cafarovceyxun.anamuslim.search

data class TranslationOption(
    val slug: String,
    val displayName: String,
)

data class SearchFilters(
    val selectedSlugs: Set<String>? = null,
    val searchQuran: Boolean = true,
    val searchHadith: Boolean = true,
) {
    val isEmpty: Boolean
        get() = selectedSlugs.isNullOrEmpty() && searchQuran && searchHadith
}
