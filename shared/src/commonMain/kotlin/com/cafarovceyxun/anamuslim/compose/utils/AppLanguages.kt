package com.cafarovceyxun.anamuslim.compose.utils

/**
 * The languages the app ships in.
 *
 * This replaces `R.array.availableLocalesValues` / `availableLocalesNames`. It is a plain Kotlin
 * list rather than a Compose-Resources string array on purpose: only one entry ("system default")
 * was ever localized — the rest are endonyms, which by definition do not vary by app language.
 * So the array carried no localization and only cost a platform resource lookup.
 */
data class AppLanguage(
    /** Raw tag as persisted: [APP_LOCALE_DEFAULT] or a BCP-47 tag. */
    val rawLanguageTag: String,
    /** The language's own name for itself, or null for the "follow the system" entry. */
    val endonym: String?,
)

/** Sentinel meaning "follow the system language" — mirrors `SPAppConfigs.LOCALE_DEFAULT`. */
const val APP_LOCALE_DEFAULT = "default"

val appLanguages: List<AppLanguage> = listOf(
    AppLanguage(APP_LOCALE_DEFAULT, null),
    AppLanguage("ar", "العربية"),
    AppLanguage("az", "Azərbaycan"),
    AppLanguage("en", "English"),
    AppLanguage("ru", "Русский"),
    AppLanguage("tr", "Türkçe"),
)

/**
 * The primary language subtag of [rawLanguageTag] ("az-Latn" → "az"), or "" for the system-default
 * entry. The common-code stand-in for `Locale.forLanguageTag(tag).language`; the app only ships
 * simple tags, so splitting on the separator is exact here.
 */
fun languageSubtagOf(rawLanguageTag: String): String {
    if (rawLanguageTag == APP_LOCALE_DEFAULT) return ""
    return rawLanguageTag.normalizedLanguageTag()
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()
}
