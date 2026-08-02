package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.aayat_quraan_031
import com.cafarovceyxun.anamuslim.resources.kfgqpc_uthman_taha_naskh
import com.cafarovceyxun.anamuslim.resources.noto_naskh_arabic
import com.cafarovceyxun.anamuslim.resources.quran_common
import com.cafarovceyxun.anamuslim.resources.scheherazadenew_regular
import com.cafarovceyxun.anamuslim.resources.suracon
import com.cafarovceyxun.anamuslim.resources.uthmanic_hafs
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
 * Uthmani Hafs — ayə mətninin standart üzü. Reader eyni faylı `FontResolver` vasitəsilə yükləyir
 * (`SCRIPT_UTHMANI` → `uthmanic_hafs.ttf`); paylaşma şəkli isə səhifə nömrəsindən asılı olmadığı
 * üçün onu birbaşa Compose Resources-dan götürür.
 */
@Composable
fun uthmaniFontFamily(): FontFamily =
    FontFamily(Font(Res.font.uthmanic_hafs, FontWeight.Normal))

/**
 * Arabic font for hadith text, selected by [script] — one of the faces the hadith font picker
 * offers ([QuranScriptUtils.HADITH_ARABIC_FONTS]). The `else` returns the default
 * ([QuranScriptUtils.HADITH_ARABIC_FONT_DEFAULT], Uthman Taha) so any unrecognised or legacy stored
 * value still renders with a valid face.
 */
@Composable
fun hadithArabicFontFamily(script: String): FontFamily {
    val font = when (script) {
        QuranScriptUtils.SCRIPT_NOTO_NASKH -> Res.font.noto_naskh_arabic
        QuranScriptUtils.SCRIPT_AYAT_QURAAN -> Res.font.aayat_quraan_031
        else -> Res.font.kfgqpc_uthman_taha_naskh
    }
    return FontFamily(Font(font, FontWeight.Normal))
}
