package com.cafarovceyxun.anamuslim.compose.utils

/**
 * Applying a language change is startup-critical and deeply platform-bound — on Android it writes
 * SharedPreferences *and* calls `AppCompatDelegate.setApplicationLocales`, which recreates
 * activities. So the resolution/persistence stays in the platform module and common UI reaches it
 * through this settable sink (`AppLogger`/`ReaderUiHooks` convention). Registered at startup
 * (Android `QuranApp.onCreate()`).
 *
 * Reading the current selection needs no hook: [appLocale] already exposes `rawLanguageTag` and
 * `numeralSystem`, and the platform keeps [appLocaleFlow] refreshed.
 */
object AppLocaleHooks {

    /**
     * Persists and applies the chosen language, then refreshes [appLocaleFlow]. [languageTag] is a
     * raw tag — [APP_LOCALE_DEFAULT] or BCP-47. No-op while unset.
     */
    var applyLanguage: ((languageTag: String, numeral: NumeralSystem?) -> Unit)? = null
}

/** Applies a language selection through [AppLocaleHooks.applyLanguage]. */
fun applyAppLanguage(languageTag: String, numeral: NumeralSystem?) {
    AppLocaleHooks.applyLanguage?.invoke(languageTag, numeral)
}
