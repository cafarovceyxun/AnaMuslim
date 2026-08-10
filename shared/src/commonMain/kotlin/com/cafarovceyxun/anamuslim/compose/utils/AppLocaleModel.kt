package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Platform-neutral snapshot of the user's effective locale, held as BCP-47 strings so it lives in
 * commonMain. The `java.util.Locale` reconstruction (`platformLocale`) plus all resolution and
 * persistence — SPAppConfigs, AppCompatDelegate, system-locale lookup — stay in the Android `app`
 * module (AppLocale.kt there), which builds these snapshots.
 */
data class AppLocale(
    val rawLanguageTag: String,
    val languageTag: String,
    val language: String,
    val numeralSystem: NumeralSystem?,
) {
    /** Language codes to try, most specific first, when resolving localized content. */
    fun fallbackLanguageCodes(default: String = "en"): Sequence<String> =
        sequenceOf(languageTag, language, default)
}

/** Provided by `QuranAppTheme` (Android). Consumers read `LocalAppLocale.current`. */
val LocalAppLocale = staticCompositionLocalOf<AppLocale> {
    error("LocalAppLocale not provided; wrap content with QuranAppTheme")
}
