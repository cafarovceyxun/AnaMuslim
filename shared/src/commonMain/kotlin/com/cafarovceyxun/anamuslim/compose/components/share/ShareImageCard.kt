package com.cafarovceyxun.anamuslim.compose.components.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.app_name
import com.cafarovceyxun.anamuslim.resources.dr_logo_android
import com.cafarovceyxun.anamuslim.resources.dr_logo_apple
import com.cafarovceyxun.anamuslim.resources.dr_qr_app_store
import com.cafarovceyxun.anamuslim.resources.dr_qr_play_store
import com.cafarovceyxun.anamuslim.resources.ic_launcher_foreground
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Mətn ölçüləri 1080px enli kətan üçündür (`Density(1f)` altında `1.sp == 1px`).
 *
 * Ərəbcə/tərcümə üçün bunlar **tavandır**: mətn kətana sığdırılır, «yazı ölçüsü» xətkeşi isə
 * nəticəni 0.5–1.0 aralığında miqyaslayır (bax [FittedBlock]). Sabit ölçülü sətirlər (etiket,
 * istinad) elə həmin əmsalla kiçilir ki, kart bütövlükdə eyni nisbətdə qalsın.
 */
private const val ArabicCeilingSize = 132f
private const val TranslationCeilingSize = 78f
private const val EyebrowBaseSize = 31f
private const val ReferenceBaseSize = 34f

/** Qeyd bloku — tərcümədən kiçik, mənbədən böyük: izahatdır, başlıq deyil. */
private const val NoteBaseSize = 38f

/** Alt sətirdəki mağaza QR-lərinin əsas ölçüsü (piksel); loqo miqyası ilə birlikdə dəyişir. */
private const val BrandQrSize = 96f

/**
 * Çox seqmentli (ayə-ayə cütlənmiş) kartda tavanlar aşağıdır: uzun ayə öz böyük payını sonuna qədər
 * doldurub qonşu qısa ayədən qat-qat iri çıxırdı.
 */
private const val ArabicCeilingMultiSegment = 74f
private const val TranslationCeilingMultiSegment = 46f

/**
 * Hər bloka verilən **minimum** pay. Pay yalnız simvol sayına bağlı olanda «الٓمٓ» kimi üç hərflik
 * ayə kadrın 5%-ini alıb oxunmaz dərəcədə kiçilirdi; sabit əlavə nisbəti yumşaldır.
 */
private const val ShareFloorUnits = 25f

/**
 * Paylaşılacaq şəklin özü — ayə və hədis üçün **eyni** kətan.
 *
 * İki qayda bu komponentin bütün quruluşunu izah edir:
 *
 *  1. **Sabit sıxlıq.** Kart `Density(1f, 1f)` altında qurulur, ona görə buradakı hər `dp`/`sp` bir
 *     pikselə bərabərdir və kətan hər cihazda tam olaraq [ShareImageRatio.widthPx] × [heightPx]
 *     olur. Ekran sıxlığından asılı ölçü = fərqli telefonlarda fərqli şəkil deməkdir.
 *  2. **Mətn kəsilmir, kiçilir.** Ərəb və tərcümə blokları [TextAutoSize.StepBased] ilə öz
 *     qutularına sığdırılır, «yazı ölçüsü» xətkeşi isə sığmış nəticəni miqyaslayır
 *     (bax [FittedBlock]). Uzun ayə aralığı seçiləndə şəkil özü kiçik şriftə keçir, kadrdan kənara
 *     çıxmır.
 *
 * [modifier] xarici tərəfdən verilir (önizləmə miqyası + `graphicsLayer` yazısı) və ölçü
 * modifikatorlarından **əvvəl** tətbiq olunur, yəni miqyas kartın öz koordinat sistemini dəyişmir —
 * yazılan qat həmişə tam ölçüdə qalır.
 */
@Composable
fun ShareImageCard(
    content: ShareImageContent,
    style: ShareImageStyle,
    arabicFontFamily: FontFamily?,
    modifier: Modifier = Modifier,
) {
    val theme = style.theme
    val ratio = style.ratio
    val appName = stringResource(Res.string.app_name)

    // Görünən seqmentlər: söndürülmüş bloklar və tamamilə boş ayələr burada süzülür ki, aşağıdakı
    // render dövrü ayırıcıları «ola bilər boşdur» halına görə yoxlamasın.
    val segments = remember(content.segments, style.showArabic, style.showTranslation) {
        content.segments.mapNotNull { segment ->
            val arabic = if (style.showArabic) segment.arabic.trim() else ""
            val translation = if (style.showTranslation) segment.translation.trim() else ""
            if (arabic.isEmpty() && translation.isEmpty()) null
            else ShareImageSegment(arabic, translation)
        }
    }
    val hasReference = style.showReference && content.reference.isNotBlank()
    val note = content.note?.trim().orEmpty()
    val hasNote = style.showNote && note.isNotEmpty()

    // Çərçivə **sabit** yerdə qalır, xətkeş yalnız mətnin ondan içəri məsafəsini dəyişir. Əks halda
    // çərçivə də mətnlə birlikdə sürüşürdü və xətkeş «heç nə etmirmiş» kimi görünürdü.
    val frameInset = ratio.widthPx * 0.032f
    val contentPad = frameInset + ratio.widthPx * 0.062f * style.margin
    val gap = ratio.widthPx * 0.028f

    val textAlign = when (style.align) {
        ShareImageAlign.Left -> TextAlign.Left
        ShareImageAlign.Center -> TextAlign.Center
        ShareImageAlign.Right -> TextAlign.Right
    }
    // Ərəbcənin düzülüşü artıq **ayrıca** verilir və güzgülənmir: «sağ» sağ deməkdir. Güzgü
    // əvvəllər burada hesablanırdı (ərəbcənin oxu kənarı sağ olduğu üçün), indi həmin default
    // redaktorda qurulur — istifadəçi istəsə ikisini ayıra bilir.
    val arabicTextAlign = when (style.arabicAlign) {
        ShareImageAlign.Left -> TextAlign.Left
        ShareImageAlign.Center -> TextAlign.Center
        ShareImageAlign.Right -> TextAlign.Right
    }
    val noteTextAlign = when (style.noteAlign) {
        ShareImageAlign.Left -> TextAlign.Left
        ShareImageAlign.Center -> TextAlign.Center
        ShareImageAlign.Right -> TextAlign.Right
    }
    val translationFontFamily = when (style.translationFamily) {
        ShareTextFamily.Sans -> FontFamily.SansSerif
        ShareTextFamily.Serif -> FontFamily.Serif
        ShareTextFamily.Mono -> FontFamily.Monospace
    }
    val columnAlign = when (style.align) {
        ShareImageAlign.Left -> Alignment.Start
        ShareImageAlign.Center -> Alignment.CenterHorizontally
        ShareImageAlign.Right -> Alignment.End
    }

    // Kətan həmişə LTR qurulur ki, «sol»/«sağ» seçimi hərfi olsun — tətbiqin dili dəyişəndə
    // düzülüş düymələri yerini dəyişməsin.
    CompositionLocalProvider(
        LocalDensity provides Density(density = 1f, fontScale = 1f),
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        Box(
            modifier = modifier
                .requiredSize(ratio.widthPx.dp, ratio.heightPx.dp)
                .background(Brush.verticalGradient(theme.gradient)),
        ) {
            val customBackground = style.customBackground
            if (customBackground != null) {
                Image(
                    bitmap = customBackground,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Scrim(style.scrim)
            } else theme.photo?.let { photo ->
                Image(
                    painter = painterResource(photo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Scrim(style.scrim)
            }

            // İncə çərçivə — kart kəsilmiş ekran şəkli kimi yox, hazır poster kimi görünsün.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(frameInset.dp)
                    .border(
                        width = 2.dp,
                        color = theme.accent.copy(alpha = 0.22f),
                        shape = RoundedCornerShape((ratio.widthPx * 0.02f).dp),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentPad.dp, vertical = (contentPad * 1.05f).dp),
                horizontalAlignment = columnAlign,
            ) {
                content.eyebrow?.takeIf { it.isNotBlank() }?.let { eyebrow ->
                    // Böyük hərfə çevirmirik: `uppercase()` lokaldan asılı olmadan `i → I` verir,
                    // Azərbaycan dilində isə doğru forma `İ`-dir («Hədis» → «HƏDIS» olurdu).
                    BasicText(
                        text = eyebrow,
                        style = TextStyle(
                            color = theme.accent,
                            fontSize = (EyebrowBaseSize * style.translationScale).sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (EyebrowBaseSize * 0.15f * style.translationScale).sp,
                            textAlign = textAlign,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height((gap * 1.2f).dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = columnAlign,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val multiSegment = segments.size > 1

                    segments.forEachIndexed { index, segment ->
                        val hasArabic = segment.arabic.isNotEmpty()
                        val hasTranslation = segment.translation.isNotEmpty()

                        // Ornament tək seqmentdə ərəbcə ilə tərcüməni, çox seqmentdə isə ayələri
                        // ayırır — hər iki halda «bir vahid bitdi» işarəsidir.
                        if (index > 0) {
                            Spacer(Modifier.height(gap.dp))
                            Ornament(theme.accent, (ratio.widthPx * 0.16f).dp, style.align)
                            Spacer(Modifier.height(gap.dp))
                        }

                        if (hasArabic) {
                            FittedBlock(
                                text = segment.arabic,
                                // Bölgü mətn uzunluğuna görədir: bərabər bölgü uzun tərcüməni
                                // lazımsız yerə kiçildirdi. Ərəb hərfləri simvol başına daha çox
                                // yer tutduğu üçün 1.5 əmsalı ilə ölçülür.
                                share = segment.arabic.length * 1.5f + ShareFloorUnits,
                                fill = style.arabicScale,
                                style = TextStyle(
                                    color = theme.text,
                                    fontFamily = arabicFontFamily,
                                    textAlign = arabicTextAlign,
                                    textDirection = TextDirection.Rtl,
                                    lineHeight = 1.9.em,
                                ),
                                ceilingFontSize =
                                    if (multiSegment) ArabicCeilingMultiSegment.sp
                                    else ArabicCeilingSize.sp,
                                minFontSize = 16.sp,
                            )
                        }

                        if (hasArabic && hasTranslation) {
                            if (segments.size == 1) {
                                Spacer(Modifier.height(gap.dp))
                                Ornament(theme.accent, (ratio.widthPx * 0.16f).dp, style.align)
                                Spacer(Modifier.height(gap.dp))
                            } else {
                                Spacer(Modifier.height((gap * 0.55f).dp))
                            }
                        }

                        if (hasTranslation) {
                            FittedBlock(
                                text = segment.translation,
                                share = segment.translation.length + ShareFloorUnits,
                                fill = style.translationScale,
                                style = TextStyle(
                                    color = theme.text.copy(alpha = 0.94f),
                                    fontFamily = translationFontFamily,
                                    fontWeight = if (style.translationBold) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = textAlign,
                                    lineHeight = 1.5.em,
                                ),
                                ceilingFontSize =
                                    if (multiSegment) TranslationCeilingMultiSegment.sp
                                    else TranslationCeilingSize.sp,
                                minFontSize = 12.sp,
                            )
                        }
                    }
                }

                // Qeyd — mətnlə mənbə arasında, öz ölçüsü və düzülüşü ilə. Sığdırma blokundan
                // (`FittedBlock`) kənardadır: qeyd izahatdır, ayənin/hədisin özü ilə yer üstündə
                // yarışmamalıdır — uzun qeyd ana mətni kiçiltmək əvəzinə öz sətirlərində kəsilir.
                if (hasNote) {
                    Spacer(Modifier.height((gap * 1.3f).dp))
                    BasicText(
                        text = note,
                        style = TextStyle(
                            color = theme.secondaryText,
                            fontFamily = translationFontFamily,
                            fontSize = (NoteBaseSize * style.noteScale).sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = noteTextAlign,
                            lineHeight = 1.45.em,
                        ),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (hasReference) {
                    Spacer(Modifier.height((gap * 1.6f).dp))
                    Box(
                        modifier = Modifier
                            .width((ratio.widthPx * 0.09f).dp)
                            .height(2.dp)
                            .background(theme.accent.copy(alpha = 0.55f)),
                    )
                    Spacer(Modifier.height(gap.dp))
                    BasicText(
                        text = content.reference,
                        style = TextStyle(
                            color = theme.accent,
                            fontSize = (ReferenceBaseSize * style.translationScale).sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            textAlign = textAlign,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (style.showBranding || style.showQr) {
                    Spacer(Modifier.height((gap * 1.6f).dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        // Loqo **rənglənmir**: `ic_launcher_foreground` qızılı halqalı, qırmızı
                        // xəttatlıqlı tam nişandır — `ColorFilter.tint` onu bir rəngli disk edir
                        // (əvvəlki qurğuda altda boz ləkə kimi görünürdü, ona görə görünmür sanılırdı).
                        if (style.showBranding) {
                            Image(
                                painter = painterResource(Res.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size((52f * style.brandingScale).dp),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicText(
                                text = appName,
                                style = TextStyle(
                                    color = theme.secondaryText,
                                    fontSize = (26f * style.brandingScale).sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (2f * style.brandingScale).sp,
                                ),
                                maxLines = 1,
                            )
                        }

                        // Namaz cədvəli kartındakı ilə **eyni** QR cütü və eyni ortaq komponent
                        // (`ShareEditorParts.QrCode`) — kodlar `error="h"` ilə generasiya olunub,
                        // loğo mərkəzi örtdüyü üçün başqa cür oxunmurdu.
                        if (style.showQr) {
                            if (style.showBranding) Spacer(Modifier.width(20.dp))
                            QrCode(
                                image = Res.drawable.dr_qr_app_store,
                                logo = Res.drawable.dr_logo_apple,
                                sizePx = (BrandQrSize * style.brandingScale).toInt(),
                            )
                            Spacer(Modifier.width(10.dp))
                            QrCode(
                                image = Res.drawable.dr_qr_play_store,
                                logo = Res.drawable.dr_logo_android,
                                sizePx = (BrandQrSize * style.brandingScale).toInt(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mətn bloku: əvvəlcə qutusuna **tam sığdırılır**, sonra [fill] əmsalı ilə çəkiliş miqyaslanır.
 *
 * Bu iki addımın ardıcıllığı vacibdir və hər ikisi əvvəlki iki cəhdin qüsurunu aradan qaldırır:
 *
 *  - **Şrift həddini xətkeşə bağlamaq işləmirdi.** `StepBased` sığan ən böyük ölçünü seçir; uzun
 *    ayə/hədis onsuz da hədddən kiçik yazılırdı, yəni həddi dəyişmək **heç nə etmirdi** (yalnız
 *    sabit ölçülü istinad sətri reaksiya verirdi).
 *  - **Sığdırma qutusunu daraltmaq mətni kəsirdi.** Qutu kiçiləndə sığdırma aşağı hədddə dayanır və
 *    mətnin sonu «…» ilə itirdi.
 *
 * Miqyas isə sığdırmadan **sonra** gəlir: kadr həmişə sığan mətnlə hesablanır, `fill ≤ 1` olduğu
 * üçün miqyaslanmış nəticə heç vaxt qutudan böyük olmur. Qısa da, uzun da mətn eyni əmsalla
 * dəyişir — ölü zona yoxdur, kəsilmə də yoxdur. (Yalnız kiçildirik, ona görə qatın yenidən
 * ölçülənməsi kəskinliyi itirmir.)
 *
 * [share] mətn uzunluğuna görə verilir və `fill = false` ilə yalnız yuxarı hədddir — qısa mətn öz
 * hündürlüyünə yığılır, ayırıcının ətrafında boşluq qalmır.
 */
@Composable
private fun ColumnScope.FittedBlock(
    text: String,
    share: Float,
    fill: Float,
    style: TextStyle,
    ceilingFontSize: TextUnit,
    minFontSize: TextUnit,
) {
    Box(
        modifier = Modifier
            .weight(share.coerceAtLeast(0.001f), fill = false)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = style,
            autoSize = TextAutoSize.StepBased(
                minFontSize = minFontSize,
                maxFontSize = ceilingFontSize,
                stepSize = 1.sp,
            ),
            // `Ellipsis` **olmamalıdır**: o, sığmayan mətni qısaldır, `StepBased` isə qısalmış
            // nəticəni «sığdı» sayıb axtarışı dayandırır — mətnin sonu «…» ilə itirdi. `Clip` ilə
            // hündürlük aşımı düzgün bildirilir və şrift həqiqətən sığana qədər kiçilir.
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = fill
                    scaleY = fill
                },
        )
    }
}

/**
 * Ərəbcə ilə tərcümə arasındakı ayırıcı: solğun xətt(lər) və romb.
 *
 * Düzülüş kənara verildikdə ayırıcı da simmetrik qalmır — mətn hansı kənara söykənirsə, ornament
 * də oradan başlayır, əks halda mərkəzdə asılı qalıb düzülüşü pozurdu.
 */
@Composable
private fun Ornament(color: Color, ruleWidth: Dp, align: ShareImageAlign) {
    val rule = @Composable { fadeToRight: Boolean ->
        val stops = listOf(color.copy(alpha = 0.6f), Color.Transparent)
        Box(
            modifier = Modifier
                .width(ruleWidth)
                .height(2.dp)
                .background(Brush.horizontalGradient(if (fadeToRight) stops else stops.reversed())),
        )
    }
    val diamond = @Composable {
        Box(
            modifier = Modifier
                .size(14.dp)
                .rotate(45f)
                .background(color.copy(alpha = 0.85f)),
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (align != ShareImageAlign.Left) {
            rule(false)
            Spacer(Modifier.width(18.dp))
        }
        diamond()
        if (align != ShareImageAlign.Right) {
            Spacer(Modifier.width(18.dp))
            rule(true)
        }
    }
}

/** Fon şəklinin üstündəki qaraltma — mətn hər fotoda oxunaqlı qalsın deyə. */
@Composable
private fun BoxScope.Scrim(strength: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = strength),
                        Color.Black.copy(alpha = (strength - 0.12f).coerceAtLeast(0f)),
                        Color.Black.copy(alpha = (strength + 0.1f).coerceAtMost(1f)),
                    )
                )
            ),
    )
}

