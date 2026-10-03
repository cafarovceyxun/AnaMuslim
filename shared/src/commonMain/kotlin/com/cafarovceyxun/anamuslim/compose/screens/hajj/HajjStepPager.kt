package com.cafarovceyxun.anamuslim.compose.screens.hajj

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_qibla
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.hajjCompleted
import com.cafarovceyxun.anamuslim.resources.hajjComplete
import com.cafarovceyxun.anamuslim.resources.hajjCounterTitle
import com.cafarovceyxun.anamuslim.resources.hajjDaysTitle
import com.cafarovceyxun.anamuslim.resources.hajjEvidenceAdminHint
import com.cafarovceyxun.anamuslim.resources.hajjEvidenceEmpty
import com.cafarovceyxun.anamuslim.resources.hajjIhramRules
import com.cafarovceyxun.anamuslim.resources.hajjMiqatTitle
import com.cafarovceyxun.anamuslim.resources.hajjModeUmrah
import com.cafarovceyxun.anamuslim.resources.hajjSayCounter
import com.cafarovceyxun.anamuslim.resources.hajjStepPosition
import com.cafarovceyxun.anamuslim.utils.hajj.HajjStep
import com.cafarovceyxun.anamuslim.utils.hajj.HajjTool
import com.cafarovceyxun.anamuslim.utils.supabase.HajjEvidence
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Addımlar arasında **sağa-sola sürüşdürmə** (istifadəçi istəyi, 2026-10-03) — Dua vərəqləyicisi kimi.
 *
 * Siyahı seçilmiş növün Həcc addımlarıdır və ya ümrənin dörd addımı ([HajjGuideState.pagerSteps]).
 * Alt zolaqdakı **«Tamamla»** addımı işarələyib növbətisinə keçir; tamamlanmış addımda düymə
 * «Tamamlandı ✓» olur və basanda işarəni geri alır.
 */
@Composable
internal fun HajjStepPager(
    state: HajjGuideState,
    byTopic: Map<String, List<HajjEvidence>>,
    actions: EvidenceActions,
    onBack: () -> Unit,
) {
    val steps = state.pagerSteps
    if (steps.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = state.stepIndex.coerceIn(0, steps.lastIndex)) { steps.size }
    val scope = rememberCoroutineScope()

    // Geri qayıdıb yenidən açanda eyni addım açılsın.
    LaunchedEffect(pagerState.currentPage) { state.stepIndex = pagerState.currentPage }

    val current = steps[pagerState.currentPage.coerceIn(0, steps.lastIndex)]
    val currentDone = current.id in state.done

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(if (state.pagerUmrah) Res.string.hajjModeUmrah else Res.string.hajjDaysTitle),
                onBack = onBack,
            )
        },
        bottomBar = {
            Surface(color = colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                ReadableWidthColumn {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(Res.string.hajjStepPosition, pagerState.currentPage + 1, steps.size),
                            style = typography.titleSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                        if (currentDone) {
                            OutlinedButton(
                                onClick = { state.toggleDone(current.id) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(painterResource(Res.drawable.dr_icon_check), contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(stringResource(Res.string.hajjCompleted), modifier = Modifier.padding(start = 8.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    state.toggleDone(current.id)
                                    val next = pagerState.currentPage + 1
                                    if (next < steps.size) scope.launch { pagerState.animateScrollToPage(next) }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Text(stringResource(Res.string.hajjComplete), style = typography.labelLarge)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1f) / steps.size },
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant,
            )
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val step = steps[page]
                StepPage(
                    step = step,
                    showDay = !state.pagerUmrah,
                    done = step.id in state.done,
                    items = evidenceFor(step, byTopic),
                    actions = actions,
                    onToggle = { state.toggleDone(step.id) },
                    onOpenTool = { tool -> state.openTool(tool) },
                )
            }
        }
    }
}

@Composable
private fun StepPage(
    step: HajjStep,
    showDay: Boolean,
    done: Boolean,
    items: List<HajjEvidence>,
    actions: EvidenceActions,
    onToggle: () -> Unit,
    onOpenTool: (HajjTool) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ReadableWidthColumn {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (showDay) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(step.day.heading, style = contentStyle(typography.labelLarge), color = colorScheme.primary)
                        if (step.day.titleAr.isNotEmpty()) {
                            Text(step.day.titleAr, style = arabicStyle(16), color = colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepCheck(done = done, onToggle = onToggle, size = 32.dp)
                    Text(
                        step.title,
                        style = contentStyle(typography.titleLarge.copy(fontWeight = FontWeight.Bold)),
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(step.text, style = translationStyle(), color = colorScheme.onSurface.alpha(0.9f))
                DraftBadge()

                step.tool?.let { tool -> ToolLink(tool) { onOpenTool(tool) } }

                if (items.isEmpty()) {
                    Text(
                        stringResource(Res.string.hajjEvidenceEmpty),
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                    if (actions.isAuthorized) {
                        Text(
                            stringResource(Res.string.hajjEvidenceAdminHint),
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant.alpha(0.8f),
                        )
                    }
                } else {
                    HajjEvidenceSection(items, actions)
                }
            }
        }
    }
}

@Composable
private fun ToolLink(tool: HajjTool, onClick: () -> Unit) {
    val (icon, label) = when (tool) {
        HajjTool.IHRAM -> Res.drawable.dr_icon_check to Res.string.hajjIhramRules
        HajjTool.MIQAT -> Res.drawable.dr_icon_qibla to Res.string.hajjMiqatTitle
        HajjTool.COUNTER_TAWAF -> Res.drawable.dr_icon_refresh to Res.string.hajjCounterTitle
        HajjTool.COUNTER_SAY -> Res.drawable.dr_icon_refresh to Res.string.hajjSayCounter
    }
    GuideCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(stringResource(label), style = typography.titleSmall, color = colorScheme.onSurface, modifier = Modifier.weight(1f))
        }
    }
}
