package com.cafarovceyxun.anamuslim.compose.utils

/**
 * Digit shaping used when rendering numbers (Unicode `nu` locale keyword). Platform-neutral: the
 * enum and its storage keys live here so preferences and settings/onboarding UI share them across
 * platforms; the Android locale resolution that consumes it stays in `AppLocale` (androidMain).
 */
enum class NumeralSystem(val storageKey: String) {
    LATN("latn"),
    ARAB("arab"),
    ARABEXT("arabext"),
    ;

    val nuKeyword: String get() = storageKey

    companion object {
        fun fromStorage(key: String?): NumeralSystem {
            if (key == null) return LATN
            return entries.firstOrNull { it.storageKey == key } ?: LATN
        }
    }
}

/** Normalizes an Android resource-style language tag (e.g. `az-rAZ`) to a BCP-47 tag (`az-AZ`). */
fun String.normalizedLanguageTag(): String = replace("-r", "-")

/** Numeral systems offered for a given language. Currently none (numeral selection is disabled). */
fun numeralSystemsForLanguage(language: String): List<Pair<NumeralSystem, String>> =
    emptyList()

/** Keeps a numeral [preference] only if it is valid for [language]; otherwise falls back to null. */
fun coerceNumeralForLanguage(language: String, preference: NumeralSystem?): NumeralSystem? {
    if (preference == null) return null
    return if (numeralSystemsForLanguage(language).any { it.first == preference }) preference else null
}
