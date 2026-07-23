package com.cafarovceyxun.anamuslim.compose.utils

/**
 * The curated "featured reading" entries shown on the homepage, as `chapter`, `chapter:verse` or
 * `chapter:from–to` references.
 *
 * Deliberately a plain Kotlin list rather than a Compose Resources string-array: these are verse
 * references, so they carry no localization (the Android `arrFeaturedQuranItems` array had no
 * translated variant either — same reasoning as [appLanguages]). Their display text is built at
 * runtime from localized format strings plus chapter names read from the database.
 *
 * The raw string form is kept because the renderer distinguishes "whole chapter" entries by the
 * absence of a `:` — see `HomeSectionFeaturedReading`.
 */
val featuredQuranItems: List<String> = listOf(
    "1",
    "2:255",
    "18",
    "36",
    "55",
    "59",
    "59:22–24",
    "62",
    "67",
)
