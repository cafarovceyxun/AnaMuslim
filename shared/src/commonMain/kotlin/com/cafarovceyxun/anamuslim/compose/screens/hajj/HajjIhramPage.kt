package com.cafarovceyxun.anamuslim.compose.screens.hajj

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.hajjEvidence
import com.cafarovceyxun.anamuslim.resources.hajjAllowed
import com.cafarovceyxun.anamuslim.resources.hajjForbidden
import com.cafarovceyxun.anamuslim.resources.hajjIhramTitle
import com.cafarovceyxun.anamuslim.resources.hajjMiqatNote
import com.cafarovceyxun.anamuslim.resources.hajjMiqatTitle
import com.cafarovceyxun.anamuslim.resources.hajjTalbiyah
import com.cafarovceyxun.anamuslim.resources.hajjWhatToDo
import com.cafarovceyxun.anamuslim.utils.hajj.HajjGuideContent
import com.cafarovceyxun.anamuslim.utils.hajj.HajjTopic
import com.cafarovceyxun.anamuslim.utils.supabase.HajjEvidence
import org.jetbrains.compose.resources.stringResource

/** İhram addımı: görüləcək işlər, Təlbiyə (bazadan), qadağalar, kitabdan dəlillər. */
@Composable
internal fun HajjIhramPage(
    byTopic: Map<String, List<HajjEvidence>>,
    actions: EvidenceActions,
    onBack: () -> Unit,
    onOpenMiqat: () -> Unit,
) {
    GuidePage(title = stringResource(Res.string.hajjIhramTitle), onBack = onBack) {
        DraftBadge()

        GuideSectionTitle(stringResource(Res.string.hajjWhatToDo))
        GuideCard {
            Column {
                HajjGuideContent.ihramTodo.forEachIndexed { index, text ->
                    if (index > 0) HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        NumberBadge(index + 1)
                        Text(text, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(top = 4.dp))
                    }
                }
            }
        }
        TextButton(onClick = onOpenMiqat) {
            Text(stringResource(Res.string.hajjMiqatTitle))
        }

        byTopic[HajjTopic.TALBIYAH.key]?.takeIf { it.isNotEmpty() }?.let { talbiyah ->
            GuideSectionTitle(stringResource(Res.string.hajjTalbiyah))
            HajjEvidenceSection(talbiyah, actions, showTitles = false)
        }

        GuideSectionTitle(stringResource(Res.string.hajjForbidden))
        GuideCard {
            Column {
                HajjGuideContent.ihramForbidden.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(item.title, style = contentStyle(typography.titleSmall), color = colorScheme.onSurface)
                        Text(item.text, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        GuideSectionTitle(stringResource(Res.string.hajjAllowed))
        GuideCard {
            Column {
                HajjGuideContent.ihramAllowed.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(item.title, style = contentStyle(typography.titleSmall), color = colorScheme.onSurface)
                        Text(item.text, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        val proofs = evidenceFor(listOf(HajjTopic.IHRAM, HajjTopic.TYPES), byTopic)
        if (proofs.isNotEmpty()) {
            GuideSectionTitle(stringResource(Res.string.hajjEvidence))
            HajjEvidenceSection(proofs, actions, showTitles = false)
        }
    }
}

/** Miqatlar: şərti sxem, kitabdakı dörd miqat, 807-ci hədisdəki ümumi qayda. */
@Composable
internal fun HajjMiqatPage(
    byTopic: Map<String, List<HajjEvidence>>,
    actions: EvidenceActions,
    onBack: () -> Unit,
) {
    GuidePage(title = stringResource(Res.string.hajjMiqatTitle), onBack = onBack) {
        GuideCard { MiqatSchematic(Modifier.fillMaxWidth().aspectRatio(1.4f).padding(8.dp)) }
        Text(
            stringResource(Res.string.hajjMiqatNote),
            style = typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
        )
        GuideCard {
            Column {
                HajjGuideContent.miqats.forEachIndexed { index, miqat ->
                    if (index > 0) HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(miqat.name, style = contentStyle(typography.titleSmall), color = colorScheme.onSurface)
                            Text(miqat.forWhom, style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
                        }
                        Text(miqat.nameAr, style = arabicStyle(19), color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        HajjEvidenceSection(byTopic[HajjTopic.MIQAT.key].orEmpty(), actions)
    }
}

/**
 * Məkkə ətrafında dörd miqatın **şərti** sxemi — istiqamətlər təxminidir, məsafə miqyasda deyil
 * (ekranda da yazılır). Xəritə kitabxanası gətirməmək üçün Canvas-dır.
 */
@Composable
private fun MiqatSchematic(modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val label = TextStyle(fontSize = 12.sp, color = colorScheme.onSurface)
    val center = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
    val line = colorScheme.outlineVariant
    val dot = colorScheme.primary
    val ray = colorScheme.primary.alpha(0.35f)
    val kaaba = colorScheme.onSurface
    val band = colorScheme.tertiary

    // Nisbi mövqelər (0..1): şimal yuxarıdadır.
    val points = listOf(
        Triple("Zul-Huleyfə", 0.62f, 0.08f),
        Triple("Cuhfə", 0.18f, 0.32f),
        Triple("Qarn", 0.84f, 0.52f),
        Triple("Yələmləm", 0.58f, 0.92f),
    )

    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height * 0.56f)
        val r = size.minDimension * 0.30f
        drawCircle(
            color = line,
            radius = r,
            center = c,
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))),
        )
        points.forEach { (name, fx, fy) ->
            val p = Offset(size.width * fx, size.height * fy)
            drawLine(ray, p, c, strokeWidth = 1.5.dp.toPx())
            drawCircle(dot, radius = 6.dp.toPx(), center = p)
            val layout = measurer.measure(name, label)
            val x = (p.x + 10.dp.toPx()).coerceAtMost(size.width - layout.size.width)
            drawText(layout, topLeft = Offset(x, p.y - layout.size.height / 2f))
        }
        val s = 18.dp.toPx()
        drawRoundRect(kaaba, topLeft = Offset(c.x - s / 2, c.y - s / 2), size = Size(s, s), cornerRadius = CornerRadius(2.dp.toPx()))
        drawRect(band, topLeft = Offset(c.x - s / 2, c.y - s / 4), size = Size(s, 3.dp.toPx()))
        val mecca = measurer.measure("Məkkə", center)
        drawText(mecca, topLeft = Offset(c.x - mecca.size.width / 2f, c.y + s / 2 + 4.dp.toPx()))
    }
}
