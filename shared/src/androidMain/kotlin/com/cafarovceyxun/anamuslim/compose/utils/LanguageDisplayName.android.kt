package com.cafarovceyxun.anamuslim.compose.utils

import java.util.Locale

actual fun localizedLanguageName(languageTag: String, inLanguageTag: String): String? {
    val target = Locale.forLanguageTag(languageTag.normalizedLanguageTag())
    if (target.language.isEmpty()) return null
    return target.getDisplayName(Locale.forLanguageTag(inLanguageTag))
        .takeIf { it.isNotBlank() }
}
