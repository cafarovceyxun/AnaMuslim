package com.cafarovceyxun.anamuslim.compose.utils

/**
 * The name of [languageTag] as written in [inLanguageTag] — e.g. ("az", "en") → "Azerbaijani".
 * Returns null when the platform cannot resolve it, so callers can fall back to the endonym.
 *
 * Backed by the platform's locale database (`Locale.getDisplayName` / `NSLocale`); there is no
 * common-code equivalent short of shipping a CLDR table.
 */
expect fun localizedLanguageName(languageTag: String, inLanguageTag: String): String?
