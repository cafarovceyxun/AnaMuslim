package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.em
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.utils.reader.footnoteInlineId

/**
 * Tərcümə mətnindəki haşiyə nömrələrinin nişanları: əsas rəngdə (yaşıl) halqa, içi boş, içində ağ rəqəm.
 * `buildTranslationAnnotatedString`-in nəticəsini çəkən hər `Text`-ə `inlineContent` kimi verilir.
 *
 * Ölçülər `em` ilədir — nişan ətrafındakı mətnin şrift ölçüsü ilə böyüyüb-kiçilir, ona görə çağıran
 * yer ölçü ötürmür (oxucuda ölçü span-ın içindədir, `Text`-in öz üslubunda yox). Yer sətrin
 * yuxarısına düzlənir (`TextTop`) və sətir hündürlüyündən kiçikdir: nişan üst-indeks kimi durur,
 * sətirlərin arasını açmır.
 */
@Composable
fun rememberFootnoteInlineContent(): Map<String, InlineTextContent> {
    val ring = colorScheme.primary
    // Qaranlıq mövzuda ağ rəqəm; işıqlı mövzunun ağ fonunda ağ rəqəm görünməzdi, ona görə orada
    // mətn rəngi işlənir.
    val digit = if (ThemeUtils.observeDarkTheme()) Color.White else colorScheme.onSurface
    return remember(ring, digit) {
        (0..99).associate { number ->
            footnoteInlineId(number.toString()) to InlineTextContent(
                placeholder = Placeholder(
                    width = if (number < 10) BADGE_SLOT_EM.em else WIDE_BADGE_SLOT_EM.em,
                    height = BADGE_EM.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextTop,
                ),
            ) { digits -> FootnoteBadge(digits, ring, digit) }
        }
    }
}

/** Nişanın hündürlüyü və bir rəqəmli/iki rəqəmli yerin eni — ətraf mətnin şrift ölçüsünə nisbətlə. */
private const val BADGE_EM = 0.78f
private const val BADGE_SLOT_EM = 0.9f
private const val WIDE_BADGE_SLOT_EM = 1.25f

/**
 * Rəqəm `drawText` ilə çəkilir, alt-`Text` ilə yox: ölçüsü yerin **faktiki** hündürlüyündən
 * hesablanır, yerin özü isə `em`-dir — kompozisiya vaxtı piksel ölçüsü hələ məlum deyil.
 */
@Composable
private fun FootnoteBadge(digits: String, ring: Color, digit: Color) {
    val measurer = rememberTextMeasurer(cacheSize = 2)
    Spacer(
        Modifier.fillMaxSize().drawBehind {
            // Yer nişandan bir az enlidir ki, dairə əvvəlki hərfə dəyməsin.
            val gap = size.height * 0.08f
            val body = Size(size.width - gap * 2, size.height)
            val origin = Offset(gap, 0f)
            val radius = CornerRadius(body.height / 2)
            val stroke = (body.height * 0.08f).coerceAtLeast(1f)

            // Yalnız halqa: içi şəffafdır, rəqəm fonun üstündə görünür.
            drawRoundRect(
                color = ring,
                topLeft = origin + Offset(stroke / 2, stroke / 2),
                size = Size(body.width - stroke, body.height - stroke),
                cornerRadius = radius,
                style = Stroke(stroke),
            )

            val layout = measurer.measure(
                text = digits,
                style = TextStyle(
                    color = digit,
                    fontSize = (body.height * 0.68f).toSp(),
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            // Sətir qutusu rəqəmdən hündürdür (ascent/descent) — rəqəmin özünü mərkəzləmək üçün
            // ilk sətrin əsas xətti ilə böyük hərf hündürlüyünün ortası götürülür.
            val baseline = layout.firstBaseline
            val capHeight = body.height * 0.68f * 0.72f
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = origin.x + (body.width - layout.size.width) / 2f,
                    y = (body.height + capHeight) / 2f - baseline,
                ),
            )
        }
    )
}
