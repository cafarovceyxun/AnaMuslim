package com.cafarovceyxun.anamuslim.compose.utils

import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

/**
 * iOS side of the app-language seam.
 *
 * Two things have to happen for a language change to be visible, and neither is shared:
 *
 * 1. **The selection is persisted** — Android uses `SPAppConfigs`; here it is DataStore, which is
 *    already the shared preference store.
 * 2. **Compose Resources has to follow it.** Their string lookup resolves against
 *    `androidx.compose.ui.text.intl.Locale.current`, which on iOS is the first entry of
 *    `NSLocale.preferredLanguages` — and that list is backed by the `AppleLanguages` user default.
 *    Overriding that key is therefore the iOS equivalent of Android's
 *    `AppCompatDelegate.setApplicationLocales`.
 *
 * `Locale.current` is read (uncached) during composition, so the composition root additionally keys
 * its content on the locale to force a rebuild — Android gets the same effect from the activity
 * recreation `AppCompatDelegate` triggers.
 */
private val KEY_APP_LOCALE = stringPreferencesKey("key.app.locale")
private val KEY_APP_NUMERAL = stringPreferencesKey("key.app.numeral_system")

/** The user default iOS resolves `NSLocale.preferredLanguages` from. */
private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

/**
 * Registers [AppLocaleHooks.applyLanguage] and applies the stored selection. Call once at startup,
 * before the first composition — the language must be in place before any resource is read.
 */
fun installIosAppLanguage() {
    AppLocaleHooks.applyLanguage = { languageTag, numeral ->
        applyLanguage(languageTag, numeral, persist = true)
    }

    val storedTag = DataStoreManager.read(KEY_APP_LOCALE, APP_LOCALE_DEFAULT)
    val storedNumeral = DataStoreManager.read(KEY_APP_NUMERAL, "")
        .takeIf { it.isNotEmpty() }
        ?.let { NumeralSystem.fromStorage(it) }

    applyLanguage(storedTag, storedNumeral, persist = false)
}

private fun applyLanguage(rawLanguageTag: String, numeral: NumeralSystem?, persist: Boolean) {
    val defaults = NSUserDefaults.standardUserDefaults

    if (rawLanguageTag == APP_LOCALE_DEFAULT) {
        // Drop the override so iOS falls back to the device's own language order.
        defaults.removeObjectForKey(APPLE_LANGUAGES_KEY)
    } else {
        defaults.setObject(listOf(rawLanguageTag), APPLE_LANGUAGES_KEY)
    }

    setAppLocale(buildIosAppLocale(rawLanguageTag, numeral))

    if (persist) {
        // Written synchronously, like Android's SharedPreferences write: a language change is rare
        // and must not be lost if the app is killed right after it.
        runBlocking {
            DataStoreManager.write(KEY_APP_LOCALE, rawLanguageTag)
            DataStoreManager.write(KEY_APP_NUMERAL, numeral?.storageKey.orEmpty())
        }
    }
}

/**
 * Builds the shared locale snapshot. For [APP_LOCALE_DEFAULT] the effective tag is read back from
 * the system (after the override was cleared), so `appLocale()` and Compose Resources always agree
 * on which language is actually in effect.
 */
private fun buildIosAppLocale(rawLanguageTag: String, numeral: NumeralSystem?): AppLocale {
    val effectiveTag = if (rawLanguageTag == APP_LOCALE_DEFAULT) {
        systemPreferredLanguageTag()
    } else {
        rawLanguageTag
    }

    val language = languageSubtagOf(effectiveTag).ifEmpty { "en" }

    return AppLocale(
        rawLanguageTag = rawLanguageTag,
        languageTag = effectiveTag,
        language = language,
        numeralSystem = coerceNumeralForLanguage(language, numeral),
    )
}

private fun systemPreferredLanguageTag(): String =
    NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
