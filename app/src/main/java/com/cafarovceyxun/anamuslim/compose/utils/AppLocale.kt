package com.cafarovceyxun.anamuslim.compose.utils

import android.content.Context
import android.content.res.Resources
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

fun readAppLocale(context: Context): AppLocale {
    val languageTag = SPAppConfigs.getLocale(context)

    val baseLocale = when {
        languageTag == SPAppConfigs.LOCALE_DEFAULT -> systemDisplayLocale(context)
        else -> {
            val appLocales = AppCompatDelegate.getApplicationLocales()

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

    if (locale.rawLanguageTag == SPAppConfigs.LOCALE_DEFAULT) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    } else {
        val normalized = locale.rawLanguageTag.normalizedLanguageTag()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
    }

    refreshAppLocale(context)
}

// `appLocale()` itself lives in shared `AppLocaleProvider` (same package); this file only adds the
// Android `platformLocale` extension used for number/date formatting.
fun appPlatformLocale(): Locale = appLocale().platformLocale
