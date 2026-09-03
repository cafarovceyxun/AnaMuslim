package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageContent
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageEditorScreen
import com.cafarovceyxun.anamuslim.compose.components.share.ShareImageSegment
import com.cafarovceyxun.anamuslim.compose.theme.uthmaniFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.quran_image_editor_title
import com.cafarovceyxun.anamuslim.resources.strTitleShareVerse
import org.jetbrains.compose.resources.stringResource

/**
 * Ayənin şəkil redaktoru — ortaq [ShareImageEditorScreen]-in ayə üçün adapteri: mətn bloklarını
 * yığır və Uthmani üzünü verir, qalan hər şey (fon dəsti, format, düzülüş, önizləmə, paylaşma)
 * ortaqdır.
 *
 * [segments] vərəqdəki «hər ayəni tərcüməsi ilə cütlə» seçimini şəklə daşıyır: cütlənmiş rejimdə
 * hər ayə ayrıca seqmentdir, bloklu rejimdə isə bütün aralıq tək seqment kimi gəlir. [reference]
 * («Fatihə 1:1-7») tərcümədən ayrı ötürülür — kartda öz sətrində, vurğu rəngi ilə yazılır.
 */
@Composable
fun QuranImageEditorScreen(
    segments: List<ShareImageSegment>,
    reference: String,
    /** Əlfəcin qeydi — varsa kartda ayrıca blok kimi görünür. */
    note: String?,
    includeArabic: Boolean,
    includeAzerbaijani: Boolean,
    onBack: () -> Unit,
) {
    ShareImageEditorScreen(
        title = stringResource(Res.string.quran_image_editor_title),
        chooserTitle = stringResource(Res.string.strTitleShareVerse),
        content = ShareImageContent(
            segments = segments,
            reference = reference,
            note = note,
        ),
        // Şrift seçimi QƏSDƏN təklif olunmur: ayə mətni Uthmani kodlaşdırılmışdır və başqa üzdə
        // Uthmani-yə xas işarələr itir — yəni səhv Quran mətni paylaşılar. Boş siyahı Şrift alətini
        // redaktorun alət sırasından tamamilə çıxarır.
        arabicFonts = emptyList(),
        initialArabicFont = null,
        arabicFontFamily = { uthmaniFontFamily() },
        initialShowArabic = includeArabic,
        initialShowTranslation = includeAzerbaijani,
        onBack = onBack,
    )
}
