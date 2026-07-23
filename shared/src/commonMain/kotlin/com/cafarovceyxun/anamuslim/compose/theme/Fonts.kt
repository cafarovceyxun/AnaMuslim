package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.quran_common
import com.cafarovceyxun.anamuslim.resources.scheherazadenew_regular
import com.cafarovceyxun.anamuslim.resources.suracon
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import org.jetbrains.compose.resources.Font

/**
 * Multiplatform font families (Compose MP `Res.font`), replacing the Android `Font(R.font.*)` vals.
 *
 * Unlike Android's `Font(resId)`, the resources `Font(...)` builder is `@Composable`, so these are
 * exposed as composable accessors rather than top-level vals. Reader/Arabic UI switches to these as
 * it moves to `commonMain`; until then the app keeps its `R.font`-based [fontArabic] etc.
 */
@Composable
fun arabicFontFamily(): FontFamily =
    FontFamily(Font(Res.font.scheherazadenew_regular, FontWeight.Normal))

@Composable
fun commonFontFamily(): FontFamily =
    FontFamily(Font(Res.font.quran_common, FontWeight.Normal))

@Composable
fun surahFontFamily(): FontFamily =
    FontFamily(Font(Res.font.suracon, FontWeight.Normal))

/**
 * Arabic font for hadith text, selected by [script].
 *
 * Multiplatform replacement for `String.getHadithArabicFontRes(isDark)`, which routes through
 * `QuranScriptPlatformHooks` to return an Android `R.font` id. Both fonts it can return already
 * exist as `Res.font`, so this closes that Int-boundary outright for hadith text. The hook's
 * `isDark` parameter is intentionally dropped — both of its branches ignore it.
 */
@Composable
fun hadithArabicFontFamily(script: String): FontFamily =
    FontFamily(
        Font(
            if (script == QuranScriptUtils.SCRIPT_PDMS_ISLAMIC) Res.font.quran_common
            else Res.font.scheherazadenew_regular,
            FontWeight.Normal,
        ),
    )
