package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
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
    val arabic = nameAr?.trimTitlePunctuation()?.takeIf { it.isNotBlank() }

    return if (arabicUi && arabic != null) {
        HadithDisplayName(text = arabic, isArabic = true, secondaryArabic = null)
    } else {
        HadithDisplayName(text = name, isArabic = false, secondaryArabic = arabic)
    }
}

/**
 * [hadithDisplayName] against the current app language.
 *
 * [arabicMode] is for the surfaces that already show Arabic text whatever the app language is —
 * the reader's Arabic tab — so their headings lead with the Arabic name too, the way the context
 * card has always done. Everywhere else it stays false and the app language decides alone.
 */
@Composable
fun rememberHadithDisplayName(
    name: String,
    nameAr: String?,
    arabicMode: Boolean = false,
): HadithDisplayName {
    val arabicUi = arabicMode || isArabicAppLanguage()
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
 * Sondakı cümlə nöqtəsini atır — ad başlıqdır, cümlə deyil.
 *
 * Bazadakı ərəbcə adların çoxu «كتاب الإيمان.» kimi nöqtə ilə bitir. Ərəb yazısı sağdan-sola
 * düzüldüyü üçün cümlə sonundakı nöqtə **sətrin sol ucuna** düşür: soldan oxunan indeks siyahısında
 * hər sətir nöqtə ilə başlayırmış kimi görünürdü («.كتاب الإيمان»). Bu, bidi qaydasına görə
 * doğrudur — düzəliş mətnin özündədir, düzülüşdə yox.
 *
 * Azərbaycanca ada toxunulmur: orada nöqtə sətrin sonunda qalır və heç nəyi pozmur.
 */
private fun String.trimTitlePunctuation(): String =
    trimEnd().trimEnd('.', '\u06D4', '\u060C', ',', ';', '\u061B').trimEnd()

/**
 * Whether [query] matches this level, checking both names regardless of the app language: someone
 * reading the Arabic index still types Latin book names, and vice versa.
 */
fun hadithNameMatches(query: String, name: String, nameAr: String?): Boolean =
    name.contains(query, ignoreCase = true) || nameAr?.contains(query, ignoreCase = true) == true

/** Ərəb yazısı eyni sp-də latından kiçik oxunur — ada verilən ölçü çarpanı. */
const val ARABIC_NAME_SCALE = 1.2f

/**
 * Ərəbcə ad üçün şrift ölçüsü və sətir hündürlüyü.
 *
 * Sətir hündürlüyü **böyüdülmüş** ölçüdən hesablanmalıdır. Əvvəl nisbət xam ölçüyə vurulurdu —
 * 16sp × 1.7 = 27sp, şrift isə 19.2sp idi, yəni faktiki nisbət 1.42 çıxırdı və çox sətirli ərəbcə
 * ad sıxılırdı (hərəkələr yuxarıdakı sətrin quyruğuna girirdi). Hədisin ərəbcə mətni eyni yerdə
 * 1.9 nisbətlə nəfəs alır; ad daha qısa olduğu üçün 1.7 kifayətdir.
 */
fun TextStyle.withArabicNameSize(base: TextUnit, lineHeightRatio: Float = 1.7f): TextStyle {
    val size = base * ARABIC_NAME_SCALE
    return copy(fontSize = size, lineHeight = size * lineHeightRatio)
}

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
