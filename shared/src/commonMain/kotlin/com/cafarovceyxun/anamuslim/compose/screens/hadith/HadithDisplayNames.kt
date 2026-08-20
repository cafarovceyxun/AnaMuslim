package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.appLocale

/** The app-language code the hadith index treats as "show the Arabic names first". */
private const val ARABIC_LANGUAGE = "ar"

/**
 * True when the app UI language is Arabic.
 *
 * Read from [LocalAppLocale] rather than `appLocale()` so a language change recomposes the callers:
 * on Android the activity is recreated anyway, but on iOS the composition root only re-keys itself,
 * and a plain `appLocale()` read would leave already-composed rows on the old language.
 */
@Composable
fun isArabicAppLanguage(): Boolean = LocalAppLocale.current.language == ARABIC_LANGUAGE

/** Non-composable twin of [isArabicAppLanguage], for callbacks and view-model-side code. */
fun isArabicAppLanguageNow(): Boolean = appLocale().language == ARABIC_LANGUAGE

/**
 * How one hadith level (volume, book, bab or sub-bab) is titled on screen.
 *
 * The index rows carry two names: the translated one (`name`) and the original Arabic (`name_ar`).
 * Which one leads depends on the app language, so both the leading text and its script travel
 * together — a caller that only got a `String` could not know whether to reach for the Arabic font.
 */
data class HadithDisplayName(
    /** The name to show as the title. */
    val text: String,
    /** True when [text] is the Arabic name, i.e. it needs the hadith Arabic font and RTL. */
    val isArabic: Boolean,
    /**
     * The Arabic name to show as a second line under the title, or null when there is nothing to
     * add — either the title is already the Arabic name, or the level has no Arabic name at all.
     */
    val secondaryArabic: String?,
)

/**
 * Picks the display name for a hadith level.
 *
 * In Arabic the original name leads and nothing is repeated underneath; in every other language the
 * translated name leads with the Arabic underneath it, which is what the index has always shown.
 * A level whose `name_ar` was never filled in keeps its translated name in both cases, so switching
 * the app to Arabic can never blank out a row.
 */
fun hadithDisplayName(name: String, nameAr: String?, arabicUi: Boolean): HadithDisplayName {
    val arabic = nameAr?.takeIf { it.isNotBlank() }

    return if (arabicUi && arabic != null) {
        HadithDisplayName(text = arabic, isArabic = true, secondaryArabic = null)
    } else {
        HadithDisplayName(text = name, isArabic = false, secondaryArabic = arabic)
    }
}

/** [hadithDisplayName] against the current app language. */
@Composable
fun rememberHadithDisplayName(name: String, nameAr: String?): HadithDisplayName {
    val arabicUi = isArabicAppLanguage()
    return remember(name, nameAr, arabicUi) { hadithDisplayName(name, nameAr, arabicUi) }
}

/**
 * The plain title string for a level, for the places that take a `String` and cannot show a second
 * line — app bars and the title carried through navigation.
 */
@Composable
fun hadithTitleText(name: String, nameAr: String?): String =
    rememberHadithDisplayName(name, nameAr).text

/** [hadithTitleText] outside composition — navigation callbacks build their title this way. */
fun hadithTitleTextNow(name: String, nameAr: String?): String =
    hadithDisplayName(name, nameAr, isArabicAppLanguageNow()).text

/**
 * Whether [query] matches this level, checking both names regardless of the app language: someone
 * reading the Arabic index still types Latin book names, and vice versa.
 */
fun hadithNameMatches(query: String, name: String, nameAr: String?): Boolean =
    name.contains(query, ignoreCase = true) || nameAr?.contains(query, ignoreCase = true) == true

/**
 * Mətn üslubunu göstərdiyi yazının öz istiqamətinə bağlayır.
 *
 * Tətbiq dili ərəbcə olanda bütün düzülüş RTL-ə keçir və istiqamətini özü təyin etməyən hər mətn
 * onunla birlikdə çevrilir: latın hərfli azərbaycanca abzas sağa dayanır, sətrin əvvəlindəki nömrə
 * ilə sonundakı durğu işarəsi yer dəyişir («596. Bizə…» → «:Bizə… .596») — mətn güzgülənmiş kimi
 * görünür. Hədis məzmunu (tərcümə, qeyd, mənbə) və tərcümə adları isə hansı interfeys dilində
 * olursa-olsun azərbaycancadır, ona görə istiqamətləri düzülüşdən yox, öz yazısından gəlməlidir.
 *
 * [arabicFontFamily] yalnız ərəbcə halda tətbiq olunur — azərbaycanca mətn öz üzündə qalır.
 */
fun TextStyle.withScriptDirection(
    arabic: Boolean,
    arabicFontFamily: FontFamily? = null,
): TextStyle = copy(
    textDirection = if (arabic) TextDirection.Rtl else TextDirection.Ltr,
    fontFamily = if (arabic) arabicFontFamily ?: fontFamily else fontFamily,
)
