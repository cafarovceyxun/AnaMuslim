package com.cafarovceyxun.anamuslim.compose.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic access to the user's fallback language codes (most-specific first),
 * used to pick a localized value from a translations map.
 *
 * The Android app registers the real provider (backed by `AppLocale`) at startup via
 * [setAppLocale]. Until then, and on platforms that don't register
 * one, it falls back to just the supplied default code.
 */
private val _appLocaleFlow = MutableStateFlow(
    AppLocale(
        rawLanguageTag = "app.locale.default",
        languageTag = "en",
        language = "en",
        numeralSystem = null
    )
)

/** The current effective app locale flow. */
val appLocaleFlow: StateFlow<AppLocale> = _appLocaleFlow

fun setAppLocale(locale: AppLocale) {
    _appLocaleFlow.value = locale
}

fun appLocale(): AppLocale = _appLocaleFlow.value

fun appFallbackLanguageCodes(default: String = "en"): Sequence<String> =
    appLocale().fallbackLanguageCodes(default)
