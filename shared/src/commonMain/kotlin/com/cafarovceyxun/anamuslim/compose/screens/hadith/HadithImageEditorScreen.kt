package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageContent
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageEditorScreen
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageSegment
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.hadithShareTitle
import com.cafarovceyxun.anamuslim.resources.imageEditorTitle
import org.jetbrains.compose.resources.stringResource

/**
 * Hədisin şəkil redaktoru — ortaq [ShareImageEditorScreen]-in hədis üçün adapteri. Ərəb üzü
 * istifadəçinin hədis şrift seçimindən gəlir ki, şəkil ekranda oxuduğu mətnlə eyni görünsün.
 *
 * Mətnlər hazır sətir kimi ötürülür (hədis obyekti yox): mötərizə təmizləməsi kimi qərarlar
 * [HadithShareSheet]-də bir dəfə verilir və həm mətn, həm şəkil paylaşımı eyni nəticəni alır.
 */
@Composable
fun HadithImageEditorScreen(
    eyebrow: String,
    arabicText: String,
    translationText: String,
    reference: String,
    /** Hədisin `note` sahəsi — varsa kartda ayrıca blok kimi görünür. */
    note: String?,
    includeArabic: Boolean,
    includeAzerbaijani: Boolean,
    onBack: () -> Unit,
) {
    val selectedFont = HadithPreferences.observeArabicFont()

    ShareImageEditorScreen(
        title = stringResource(Res.string.imageEditorTitle),
        chooserTitle = stringResource(Res.string.hadithShareTitle),
        content = ShareImageContent(
            segments = listOf(ShareImageSegment(arabic = arabicText, translation = translationText)),
            reference = reference,
            eyebrow = eyebrow,
            note = note,
        ),
        // Redaktorda üz dəyişdirmək olar, amma seçim `HadithPreferences`-ə YAZILMIR — şəkil üçün
        // birdəfəlikdir, oxuma ekranının şriftini arxadan dəyişməməlidir.
        arabicFonts = QuranScriptUtils.HADITH_ARABIC_FONTS,
        initialArabicFont = selectedFont,
        arabicFontFamily = { font ->
            hadithArabicFontFamily(font ?: QuranScriptUtils.HADITH_ARABIC_FONT_DEFAULT)
        },
        initialShowArabic = includeArabic,
        initialShowTranslation = includeAzerbaijani,
        onBack = onBack,
    )
}
