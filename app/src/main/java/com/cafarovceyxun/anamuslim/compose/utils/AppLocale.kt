package com.cafarovceyxun.anamuslim.compose.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import com.cafarovceyxun.anamuslim.compose.utils.setAppLocale as setSharedAppLocale
import com.cafarovceyxun.anamuslim.utils.sharedPrefs.SPAppConfigs
import java.util.Locale

// `AppLocale` (data class) and `LocalAppLocale` now live in `shared/commonMain`; this file keeps the
// Android-only resolution/persistence and the `java.util.Locale` reconstruction below.

/** The Android `java.util.Locale` used for number/date formatting and display names. */
val AppLocale.platformLocale: Locale
    get() {
        val numeral = numeralSystem
        return Locale.Builder()
            .setLocale(Locale.forLanguageTag(languageTag))
            .apply {
                if (numeral != null) {
                    setUnicodeLocaleKeyword("nu", numeral.nuKeyword)
                }
            }
            .build()
    }

/** Builds an [AppLocale] snapshot from a resolved [base] locale, coercing the numeral system. */
private fun buildAppLocale(
    rawLanguageTag: String,
    base: Locale,
    numeral: NumeralSystem?,
): AppLocale = AppLocale(
    rawLanguageTag = rawLanguageTag,
    languageTag = base.toLanguageTag(),
    language = base.language,
    numeralSystem = coerceNumeralForLanguage(base.language, numeral),
)

private fun systemDisplayLocale(context: Context? = null): Locale {
    if (context != null) {
        val system = LocaleManagerCompat.getSystemLocales(context.applicationContext)
        if (!system.isEmpty) return system[0]!!
    }

    val config = Resources.getSystem().configuration
    return config.locales[0] ?: Locale.getDefault()
}

/**
 * The per-app language the platform is actually honouring.
 *
 * Read straight off the framework on API 33+ rather than through `AppCompatDelegate`: AppCompat
 * only redirects to `LocaleManager` once it can find an app `Context`, and it looks for one by
 * scanning its *live `AppCompatActivity` delegates*. The language picker now lives inside
 * `MainActivity`, a plain `ComponentActivity`, so that scan comes up empty and both the getter and
 * the setter turn into silent no-ops — the stored preference moved while every resource kept
 * resolving against the system locale.
 */
private fun applicationLocales(context: Context): LocaleListCompat {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return AppCompatDelegate.getApplicationLocales()
    }

    val manager = context.applicationContext.getSystemService(LocaleManager::class.java)
    val locales = manager?.applicationLocales ?: return LocaleListCompat.getEmptyLocaleList()

    return if (locales.isEmpty) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.wrap(locales)
}

/**
 * Applies the stored language to a base context, for hosts the platform does not cover: below API
 * 33 there is no per-app language at all, and AppCompat's backport only reaches `AppCompatActivity`.
 * `MainActivity` calls this from `attachBaseContext`. A no-op on API 33+, where the framework has
 * already applied the language to every context the app is handed.
 */
fun wrapContextWithAppLocale(base: Context): Context {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base

    val languageTag = SPAppConfigs.getLocale(base)
    if (languageTag == SPAppConfigs.LOCALE_DEFAULT) return base

    val locale = Locale.forLanguageTag(languageTag.normalizedLanguageTag())
    val config = Configuration(base.resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }

    return base.createConfigurationContext(config)
}

fun readAppLocale(context: Context): AppLocale {
    val languageTag = SPAppConfigs.getLocale(context)

    val baseLocale = when {
        languageTag == SPAppConfigs.LOCALE_DEFAULT -> systemDisplayLocale(context)
        else -> {
            val appLocales = applicationLocales(context)

            if (!appLocales.isEmpty) appLocales[0]!!
            else Locale.forLanguageTag(languageTag.normalizedLanguageTag())
        }
    }

    val storedNumeral = NumeralSystem.fromStorage(SPAppConfigs.getNumeralSystem(context))

    return buildAppLocale(languageTag, baseLocale, storedNumeral)
}

fun appLocaleForLanguageChange(
    context: Context,
    languageTag: String,
    numberSystem: NumeralSystem?
): AppLocale {
    val baseLocale = if (languageTag == SPAppConfigs.LOCALE_DEFAULT) {
        systemDisplayLocale(context)
    } else {
        Locale.forLanguageTag(languageTag.normalizedLanguageTag())
    }

    return buildAppLocale(languageTag, baseLocale, numberSystem)
}

fun refreshAppLocale(context: Context) {
    setSharedAppLocale(readAppLocale(context.applicationContext))
}

fun setAppLocale(context: Context, locale: AppLocale) {
    SPAppConfigs.setLocale(context, locale.rawLanguageTag)
    SPAppConfigs.setNumeralSystem(context, locale.numeralSystem?.storageKey)

    val normalized = locale.rawLanguageTag
        .takeIf { it != SPAppConfigs.LOCALE_DEFAULT }
        ?.normalizedLanguageTag()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Set on the framework directly — see [applicationLocales] for why the AppCompat wrapper
        // cannot be trusted here. The platform then recreates the visible activities itself.
        context.applicationContext.getSystemService(LocaleManager::class.java)?.applicationLocales =
            if (normalized == null) LocaleList.getEmptyLocaleList()
            else LocaleList.forLanguageTags(normalized)
    } else {
        // Still worth doing below 33: it is what carries the language into the legacy
        // `BaseActivity` screens, which *are* AppCompat. `MainActivity` covers itself through
        // [wrapContextWithAppLocale].
        AppCompatDelegate.setApplicationLocales(
            if (normalized == null) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(normalized)
        )
    }

    refreshAppLocale(context)
}

// `appLocale()` itself lives in shared `AppLocaleProvider` (same package); this file only adds the
// Android `platformLocale` extension used for number/date formatting.
fun appPlatformLocale(): Locale = appLocale().platformLocale
