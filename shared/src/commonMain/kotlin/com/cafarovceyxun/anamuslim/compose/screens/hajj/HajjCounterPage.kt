package com.cafarovceyxun.anamuslim.compose.screens.hajj

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.hajjCompleted
import com.cafarovceyxun.anamuslim.resources.hajjLapDone
import com.cafarovceyxun.anamuslim.resources.hajjLapN
import com.cafarovceyxun.anamuslim.resources.hajjLapsOf
import com.cafarovceyxun.anamuslim.resources.hajjSay
import com.cafarovceyxun.anamuslim.resources.hajjTawaf
import com.cafarovceyxun.anamuslim.resources.reset
import com.cafarovceyxun.anamuslim.resources.strActionUndo
import com.cafarovceyxun.anamuslim.utils.hajj.HajjGuideContent
import com.cafarovceyxun.anamuslim.utils.hajj.HajjTopic
import com.cafarovceyxun.anamuslim.utils.supabase.HajjEvidence
import com.cafarovceyxun.anamuslim.resources.hajjSafaMarwaDhikr
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val LAPS = 7

/**
 * Tavaf/səy sayğacı — izdihamda ekrana baxmadan basılsın deyə düymə enli və hündürdür.
 * Səydə növbəti gedişin istiqaməti göstərilir (tək dövrələr Səfadan, cütlər Mərvədən).
 */
@Composable
internal fun HajjCounterPage(
    state: HajjGuideState,
    byTopic: Map<String, List<HajjEvidence>>,
    actions: EvidenceActions,
    onBack: () -> Unit,
) {
    val say = state.counterSay
    val title = stringResource(if (say) Res.string.hajjSay else Res.string.hajjTawaf)
    val finished = state.laps >= LAPS
    val next = state.laps + 1

    GuidePage(title = title, onBack = onBack) {
        SegmentedTabs(
            labels = listOf(stringResource(Res.string.hajjTawaf), stringResource(Res.string.hajjSay)),
            selectedIndex = if (say) 1 else 0,
            onSelect = { index ->
                val toSay = index == 1
                if (toSay != state.counterSay) {
                    state.counterSay = toSay
                    state.laps = 0
                }
            },
        )

        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            LapRing(laps = state.laps, modifier = Modifier.size(240.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.laps.coerceAtMost(LAPS).toString(),
                    style = typography.displayLarge.copy(fontSize = 56.sp, fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                )
                Text(stringResource(Res.string.hajjLapsOf), style = typography.bodyMedium, color = colorScheme.onSurfaceVariant)
            }
        }

        GuideCard(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (finished) {
                    Text(
                        if (say) HajjGuideContent.SAY_DONE else HajjGuideContent.TAWAF_DONE,
                        style = contentStyle(typography.titleSmall),
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(stringResource(Res.string.hajjLapN, next), style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    Text(
                        if (say) HajjGuideContent.sayDirection(next) else HajjGuideContent.TAWAF_DIRECTION,
                        style = contentStyle(typography.titleMedium),
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Button(
            onClick = { if (state.laps < LAPS) state.laps++ },
            enabled = !finished,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Text(
                stringResource(if (finished) Res.string.hajjCompleted else Res.string.hajjLapDone),
                style = typography.titleMedium,
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { if (state.laps > 0) state.laps-- }, enabled = state.laps > 0) {
                Text(stringResource(Res.string.strActionUndo))
            }
            TextButton(onClick = { state.laps = 0 }, enabled = state.laps > 0) {
                Icon(painterResource(Res.drawable.dr_icon_refresh), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(stringResource(Res.string.reset), modifier = Modifier.padding(start = 6.dp))
            }
        }

        // Səfa və Mərvə zikri sayğacın altında: hacı təpədə ekranı açıb dərhal oxuya bilsin.
        if (say) {
            val dhikr = byTopic[HajjTopic.SAY.key].orEmpty().filter { it.isDhikr }
            if (dhikr.isNotEmpty()) {
                GuideSectionTitle(stringResource(Res.string.hajjSafaMarwaDhikr))
                HajjEvidenceSection(dhikr, actions, showTitles = false)
            }
        }
    }
}

/** Yeddi seqmentli halqa: keçilmiş dövrələr `primary`, növbəti `secondary`, qalanlar sönük. */
@Composable
private fun LapRing(laps: Int, modifier: Modifier) {
    val track = colorScheme.surfaceVariant
    val doneColor = colorScheme.primary
    val nextColor = colorScheme.secondary.alpha(0.55f)
    Canvas(modifier = modifier) {
        val stroke = 16.dp.toPx()
        // Dairəvi uclar hər tərəfə yarım qalınlıq (~4°) çıxır — boşluq ondan geniş olmalıdır.
        val gapDeg = 14f
        val sweep = 360f / LAPS - gapDeg
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        repeat(LAPS) { i ->
            val start = -90f + i * (360f / LAPS) + gapDeg / 2
            val color = when {
                i < laps -> doneColor
                i == laps -> nextColor
                else -> track
            }
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
