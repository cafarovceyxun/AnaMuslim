package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageContent
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageEditorScreen
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageSegment
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.imageEditorTitle
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import org.jetbrains.compose.resources.stringResource

/**
 * Duanın/dəlilin şəkil redaktoru — ortaq [ShareImageEditorScreen]-in dua üçün adapteri.
 *
 * Kartın quruluşu **bir seqmentdir**: yuxarıda ərəbcə, ornamentdən sonra latın mətni. Oxunuş ayrıca
 * seqment kimi verilsəydi, aralarına ikinci ornament düşərdi və kart «iki ayrı sitat» kimi
 * görünərdi; ona görə oxunuş və tərcümə boş sətirlə eyni bloka yığılır.
 *
 * Şrift seçimi **təklif olunur** (ayə redaktorundan fərqli olaraq): dua mətni Uthmani
 * kodlaşdırılmış deyil, adi ərəb mətnidir — başqa üzdə işarələr itmir. Başlanğıc üz `null`-dur,
 * yəni ekranda oxunan üz ([arabicFontFamily]) ilə eyni.
 *
 * @param parts paylaşma vərəqindəki çekbokslar — şəkil də **eyni seçimi** daşıyır, yoxsa vərəqdə
 *   söndürülən blok şəkildə yenidən peyda olurdu.
 */
@Composable
internal fun DuaImageEditorScreen(
    ref: DuaSourceRef,
    parts: DuaShareParts,
    /** Kartın ən üstündəki kiçik etiket — mövzunun adı; boşdursa etiket çəkilmir. */
    eyebrow: String?,
    onBack: () -> Unit,
) {
    val arabic = ref.text_ar.takeIf { parts.arabic && it.isNotBlank() }.orEmpty()

    val latin = listOfNotNull(
        ref.transliteration?.takeIf { parts.transliteration && it.isNotBlank() },
        ref.text_az.takeIf { parts.translation && it.isNotBlank() },
    ).joinToString("\n\n")

    ShareImageEditorScreen(
        title = stringResource(Res.string.imageEditorTitle),
        chooserTitle = stringResource(Res.string.strLabelShare),
        content = ShareImageContent(
            segments = listOf(ShareImageSegment(arabic = arabic, translation = latin)),
            reference = ref.source?.takeIf { parts.source && it.isNotBlank() }.orEmpty(),
            eyebrow = eyebrow?.takeIf { it.isNotBlank() },
            note = ref.note?.takeIf { parts.note && it.isNotBlank() },
        ),
        arabicFonts = QuranScriptUtils.HADITH_ARABIC_FONTS,
        initialArabicFont = null,
        arabicFontFamily = { font ->
            font?.let { hadithArabicFontFamily(it) } ?: arabicFontFamily()
        },
        // Blok seçimi artıq mətnə tətbiq olunub (yuxarıdakı `arabic`/`latin`), ona görə redaktorun
        // öz açarları açıq başlayır — bağlı başlasaydı, seçdiyi bloku yenidən yandırmaq lazım gələrdi.
        initialShowArabic = arabic.isNotBlank(),
        initialShowTranslation = latin.isNotBlank(),
        onBack = onBack,
    )
}
