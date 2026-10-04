package com.cafarovceyxun.anamuslim.compose.screens.hajj

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.screens.dua.TRANSLATION_LINE_HEIGHT_RATIO
import com.cafarovceyxun.anamuslim.compose.screens.dua.TRANSLATION_SUBTEXT_DROP_SP
import com.cafarovceyxun.anamuslim.compose.screens.dua.withLineHeightRatio
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_menu
import com.cafarovceyxun.anamuslim.resources.duaOpenSource
import com.cafarovceyxun.anamuslim.resources.duaPartActions
import com.cafarovceyxun.anamuslim.resources.hajjDhikrSection
import com.cafarovceyxun.anamuslim.resources.hajjDraftBadge
import com.cafarovceyxun.anamuslim.resources.hajjEvidence
import com.cafarovceyxun.anamuslim.resources.hajjMarkDone
import com.cafarovceyxun.anamuslim.resources.icon_copy
import com.cafarovceyxun.anamuslim.resources.strLabelCopy
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.utils.supabase.HajjEvidence
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Bələdçi məzmunu azərbaycancadır — ərəbcə UI-də də LTR qalır (bax `HajjGuideContent`). */
@Composable
internal fun contentStyle(base: TextStyle): TextStyle = base.withScriptDirection(arabic = false)

/** Ərəbcə mətn — Dua zaman xəttinin nisbəti ilə (sətir 1.85). */
@Composable
internal fun arabicStyle(size: Int = 22): TextStyle = typography.headlineSmall.copy(
    fontSize = size.sp,
    lineHeight = (size * 1.85f).sp,
).withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily())

@Composable
internal fun translationStyle(): TextStyle =
    contentStyle(typography.bodyLarge.withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO))

@Composable
internal fun GuideSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = colorScheme.primary,
        modifier = modifier,
    )
}

/**
 * «Qaralama» nişanı — izah mətnləri alim yoxlamasından keçənə qədər ekranda qalır.
 * Rəng `tertiary`-dir: xəta (qırmızı) deyil, amma vurğu yaşılından da ayrılmalıdır.
 */
@Composable
internal fun DraftBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.hajjDraftBadge),
        style = typography.labelSmall,
        color = colorScheme.tertiary,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colorScheme.tertiary.alpha(0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Kart fonu — ana ekranın neytral kartları ilə eyni (`surface`, 16dp). */
@Composable
internal fun GuideCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = colorScheme.surface, modifier = modifier.fillMaxWidth(),
            content = content)
    } else {
        Surface(shape = shape, color = colorScheme.surface, modifier = modifier.fillMaxWidth(), content = content)
    }
}

/**
 * Ümrə/Həcc keçidi — iki bərabər seqment. Seçilmiş seqment
 * [ModeTabStrip][com.cafarovceyxun.anamuslim.compose.components.common.ModeTabStrip] kimi `primary`
 * doludur; fərqi odur ki, hər iki etiket görünür (cəmi iki qısa söz).
 */
@Composable
internal fun SegmentedTabs(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) colorScheme.primary else colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label,
                    style = typography.labelLarge,
                    color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 9.dp),
                )
            }
        }
    }
}

/** Nömrəli dairə — sıra informasiyadır (ihram hazırlığının addımları). */
@Composable
internal fun NumberBadge(n: Int) {
    Box(
        modifier = Modifier.size(28.dp).background(colorScheme.primary.alpha(0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = n.toString(), style = typography.labelLarge, color = colorScheme.primary)
    }
}

/**
 * Addımın ✓ işarəsi — **hər** addımın qabağında görünür (istifadəçi istəyi, 2026-10-03).
 *
 * Tamamlanmamış addımda da ✓ çəkilir, sönük haşiyəli dairədə: boş nöqtə «bura basıb
 * işarələyəcəksən» demirdi. Tamamlananda dairə dolur.
 */
@Composable
internal fun StepCheck(done: Boolean, onToggle: (() -> Unit)?, size: Dp = 28.dp) {
    val label = stringResource(Res.string.hajjMarkDone)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (done) colorScheme.primary else colorScheme.surface, CircleShape)
            .border(1.5.dp, if (done) colorScheme.primary else colorScheme.primary.alpha(0.45f), CircleShape)
            .then(
                if (onToggle != null) {
                    Modifier.clickable(role = Role.Checkbox, onClickLabel = label, onClick = onToggle)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(Res.drawable.dr_icon_check),
            contentDescription = label,
            tint = if (done) colorScheme.onPrimary else colorScheme.primary.alpha(0.35f),
            modifier = Modifier.size(size * 0.57f),
        )
    }
}

/** Dəlil/zikr üzərində admin əməliyyatları; giriş etməyənə **görünmür** (yazma RLS-də də bağlıdır). */
internal class EvidenceActions(
    val isAuthorized: Boolean,
    /**
     * Mənbəni istinad vərəqində açır. [siblings] bölmənin ekrandakı siyahısıdır (zikrlər, sonra
     * dəlillər) — vərəqi sağa-sola sürüşdürmək onu gəzir.
     */
    val onOpenSource: (item: HajjEvidence, siblings: List<HajjEvidence>) -> Unit,
    val onEdit: (HajjEvidence) -> Unit,
    val onDelete: (HajjEvidence) -> Unit,
)

/**
 * Mövzunun dəlilləri — **Dua ekranındakı kimi**: zikrlər tam blok (ərəbcə, oxunuş, məna), dəlillər
 * kart; toxunuş mənbəni istinad vərəqində (`ReferencePeek`) açır və çıxarış orada sarı ilə vurğulanır.
 */
@Composable
internal fun HajjEvidenceSection(
    items: List<HajjEvidence>,
    actions: EvidenceActions,
    showTitles: Boolean = true,
) {
    val dhikr = items.filter { it.isDhikr }
    val evidence = items.filterNot { it.isDhikr }
    // Ekrandakı sıra — vərəqdəki sürüşdürmə də bu sıra ilə gedir.
    val ordered = dhikr + evidence

    if (dhikr.isNotEmpty()) {
        if (showTitles) GuideSectionTitle(stringResource(Res.string.hajjDhikrSection))
        dhikr.forEach { DhikrBlock(it, ordered, actions) }
    }
    if (evidence.isNotEmpty()) {
        if (showTitles) GuideSectionTitle(stringResource(Res.string.hajjEvidence))
        evidence.forEach { HajjEvidenceCard(it, ordered, actions) }
    }
}

/** Bir zikr — `DhikrTimeline`-in addımı ilə eyni qat düzülüşü, kopyala düyməsi ilə. */
@Composable
private fun DhikrBlock(item: HajjEvidence, siblings: List<HajjEvidence>, actions: EvidenceActions) {
    val copiedMsg = stringResource(Res.string.copiedToClipboard)

    GuideCard(onClick = { actions.onOpenSource(item, siblings) }) {
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                item.text_ar,
                style = arabicStyle(24),
                color = colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            )
            item.transliteration?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = contentStyle(
                        typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                            .withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO),
                    ),
                    color = colorScheme.primary.alpha(0.9f),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
            if (item.text_az.isNotBlank()) {
                if (item.transliteration.isNullOrBlank()) Spacer(Modifier.height(8.dp))
                Text(
                    item.text_az,
                    style = translationStyle(),
                    color = colorScheme.onSurface.alpha(0.92f),
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
            item.note?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = contentStyle(
                        typography.bodySmall.copy(
                            fontSize = (typography.bodyLarge.fontSize.value - TRANSLATION_SUBTEXT_DROP_SP).sp,
                        ).withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO),
                    ),
                    color = colorScheme.onSurfaceVariant.alpha(0.85f),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                )
            }
            HorizontalDivider(
                color = colorScheme.outlineVariant.alpha(0.5f),
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    item.source.orEmpty(),
                    style = contentStyle(typography.labelSmall),
                    color = colorScheme.onSurfaceVariant.alpha(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    val text = listOfNotNull(item.text_ar, item.transliteration, item.text_az.takeIf { it.isNotBlank() })
                        .joinToString("\n\n")
                    PlatformUtils.copyToClipboard(text)
                    PlatformUtils.showClipboardMessage(copiedMsg)
                }) {
                    Icon(painterResource(Res.drawable.icon_copy), contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource(Res.string.strLabelCopy), modifier = Modifier.padding(start = 6.dp))
                }
                EvidenceMenu(item, siblings, actions)
            }
        }
    }
}

/** Bir dəlil — Əsma dəlil kartı kimi: ərəbcə çıxarış, tərcümə, istinad. */
@Composable
private fun HajjEvidenceCard(item: HajjEvidence, siblings: List<HajjEvidence>, actions: EvidenceActions) {
    Surface(
        onClick = { actions.onOpenSource(item, siblings) },
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surface,
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                item.text_ar,
                style = arabicStyle(20),
                color = colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
            )
            if (item.text_az.isNotBlank()) {
                Text(
                    item.text_az,
                    style = translationStyle(),
                    color = colorScheme.onSurface.alpha(0.92f),
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.source.orEmpty(),
                    style = contentStyle(typography.labelSmall),
                    color = colorScheme.onSurfaceVariant.alpha(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                EvidenceMenu(item, siblings, actions)
            }
        }
    }
}

@Composable
internal fun EvidenceMenu(item: HajjEvidence, siblings: List<HajjEvidence>, actions: EvidenceActions) {
    var open by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_menu),
                contentDescription = stringResource(Res.string.duaPartActions),
                tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.duaOpenSource)) },
                onClick = {
                    open = false
                    actions.onOpenSource(item, siblings)
                },
            )
            if (actions.isAuthorized) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.strLabelEdit)) },
                    onClick = {
                        open = false
                        actions.onEdit(item)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.strLabelDelete), color = colorScheme.error) },
                    onClick = {
                        open = false
                        actions.onDelete(item)
                    },
                )
            }
        }
    }
}

/** Addım sətrindəki kiçik say — «2 dəlil · 1 zikr». */
@Composable
internal fun CountLine(text: String) {
    Text(
        text,
        style = typography.labelSmall,
        color = colorScheme.primary.alpha(0.8f),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, colorScheme.primary.alpha(0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
