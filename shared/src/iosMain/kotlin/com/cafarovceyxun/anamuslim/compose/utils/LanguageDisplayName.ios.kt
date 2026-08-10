package com.cafarovceyxun.anamuslim.compose.utils

import platform.Foundation.NSLocale
import platform.Foundation.localizedStringForLanguageCode

actual fun localizedLanguageName(languageTag: String, inLanguageTag: String): String? {
    val subtag = languageSubtagOf(languageTag)
    if (subtag.isEmpty()) return null
    return NSLocale(localeIdentifier = inLanguageTag)
        .localizedStringForLanguageCode(subtag)
        ?.takeIf { it.isNotBlank() }
}
