package com.cafarovceyxun.anamuslim.compose.screens.salah

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.screens.hajj.EvidenceActions
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuideCard
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuidePage
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuideSectionTitle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.arabicStyle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.contentStyle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.salahWomenNote
import com.cafarovceyxun.anamuslim.utils.salah.GuideStep
import com.cafarovceyxun.anamuslim.utils.salah.KhawfForm
import com.cafarovceyxun.anamuslim.utils.salah.LaneKind
import com.cafarovceyxun.anamuslim.utils.salah.SalahGroupContent
import com.cafarovceyxun.anamuslim.utils.salah.SalahTopic
import com.cafarovceyxun.anamuslim.utils.salah.SeatKind
import com.cafarovceyxun.anamuslim.utils.salah.SeatLayout
import com.cafarovceyxun.anamuslim.utils.supabase.GuideEvidence
import org.jetbrains.compose.resources.stringResource

/*
 * Namaz bələdçisi — Mərhələ 3 (Camaat və xüsusi namazlar) ekranları. Məzmun [SalahGroupContent]-dən, hədislər
 * `salah_evidence`-dən. Yeni poza şəkli yoxdur: səf sxemi və qorxu namazının dəstə cədvəli Compose-da çəkilir,
 * rəng mövzudan gəlir. Vəziyyət (seçilmiş sxem, istisqa yolu, qorxu forması) [SalahGuideState]-dədir.
 */

// ---------------------------------------------------------------------------------------------
// Ortaq hissələr
// ---------------------------------------------------------------------------------------------

/** Seçim düymələri — rükət xəritəsindəki kimi. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceChips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label,
                style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (on) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    .background(if (on) colorScheme.primary else colorScheme.surfaceVariant)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Ardıcıllıq: nömrəli zaman xətti, hər addımın altında öz çıxarışları. */
@Composable
internal fun StepsTimeline(steps: List<GuideStep>, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions) {
    Column {
        steps.forEachIndexed { index, step ->
            TimelineStep(index + 1, isLast = index == steps.lastIndex) {
                Text(step.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                step.text?.let { Text(it, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant) }
                TopicQuotes(byTopic.of(step.topic), actions, compact = true)
            }
        }
    }
}

/** Mövzunun ilk sətri vurğulu blokda, qalanları altında. */
@Composable
internal fun CalloutWithRest(caption: String, topic: SalahTopic, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions) {
    val items = byTopic.of(topic)
    Callout(caption, items.firstOrNull(), items, actions)
    if (items.size > 1) TopicQuotes(items.drop(1), actions, compact = true)
}

/** Kitabda olmayan məlumatın açıq qeydi (uydurmaq əvəzinə). */
@Composable
internal fun NotInBook(text: String) {
    Text(
        text,
        style = contentStyle(typography.bodySmall),
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, colorScheme.outlineVariant), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

// ---------------------------------------------------------------------------------------------
// Bölmə
// ---------------------------------------------------------------------------------------------

internal class ArabicTile(val page: SalahPage, val arabic: String, val title: String, val sub: String)

private val groupTiles = listOf(
    ArabicTile(SalahPage.JAMAAH, "الْجَمَاعَة", "Camaat namazı", "imam, tabe olmaq, qunut · № 371–427"),
    ArabicTile(SalahPage.ROWS, "الصُّفُوف", "Səflər", "kim harada durur · № 398–410"),
    ArabicTile(SalahPage.JUMUAH, "الْجُمُعَة", "Cümə", "qüsl, xütbə, iki rükət · № 428–464"),
    ArabicTile(SalahPage.SAFAR, "السَّفَر", "Səfər namazı", "qəsr və cəm · № 465–480"),
    ArabicTile(SalahPage.EID, "الْعِيدَيْن", "Bayram namazı", "Fitr və Ədha · № 481–490"),
    ArabicTile(SalahPage.ISTISQA, "الِاسْتِسْقَاء", "İstisqa", "yağış duası · № 491–498"),
    ArabicTile(SalahPage.KUSUF, "الْكُسُوف", "Küsuf", "Günəş və Ay tutulanda · № 499–504"),
    ArabicTile(SalahPage.KHAWF, "الْخَوْف", "Qorxu namazı", "beş forma · № 505–510"),
)

@Composable
internal fun GroupHubPage(state: SalahGuideState, onBack: () -> Unit) {
    ArabicHubPage("Camaat və xüsusi namazlar", "الْجَمَاعَة", "№ 371–510", groupTiles, state, onBack)
}

/** Mərhələ bölməsi: başlıq kartı və ərəbcə adlı kafel cütləri (Mərhələ 3 və 4). */
@Composable
internal fun ArabicHubPage(title: String, arabic: String, range: String, tiles: List<ArabicTile>, state: SalahGuideState, onBack: () -> Unit) {
    GuidePage(title = title, onBack = onBack) {
        GuideCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(arabic, style = arabicStyle(34), color = colorScheme.primary)
                Text("Muheymin 1-ci cild · Namaz Kitabı", style = contentStyle(typography.titleSmall), color = colorScheme.onSurface)
                Text(range, style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
            }
        }
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                pair.forEach { tile ->
                    Surface(
                        onClick = { state.push(tile.page) },
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.surface,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Column {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(96.dp).background(colorScheme.primary.alpha(0.08f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(tile.arabic, style = arabicStyle(26), color = colorScheme.primary, textAlign = TextAlign.Center)
                            }
                            Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                                Text(tile.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                                Text(tile.sub, style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        DraftNote()
    }
}

// ---------------------------------------------------------------------------------------------
// Camaat namazı
// ---------------------------------------------------------------------------------------------

@Composable
internal fun JamaahPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Camaat namazı", onBack = onBack) {
        CalloutWithRest("Əbu Hureyrə · № 373", SalahTopic.JAMAAH_25, byTopic, actions)
        GuideSectionTitle("Fəzilət")
        RulesOf(SalahGroupContent.jamaahVirtue, byTopic, actions)
        GuideSectionTitle("Kim imam olur")
        RulesOf(SalahGroupContent.imamWho, byTopic, actions)
        GuideSectionTitle("İmama tabe olmaq")
        TopicQuotes(byTopic.of(SalahTopic.FOLLOW_IMAM), actions)
        FixedDhikrCard(SalahGroupContent.amin, "İmam «Valəd-daalliin» deyəndə")
        TopicQuotes(byTopic.of(SalahTopic.FOLLOW_AMIN), actions, compact = true)
        TopicBlock(byTopic.of(SalahTopic.FOLLOW_RABBANA), actions)
        RulesOf(SalahGroupContent.follow, byTopic, actions)
        GuideSectionTitle("Gec gələn")
        RulesOf(SalahGroupContent.late, byTopic, actions)
        TopicBlock(byTopic.of(SalahTopic.LATE_HAMD), actions)
        GuideSectionTitle("İmam yüngül qıldırsın")
        RulesOf(SalahGroupContent.light, byTopic, actions)
        GuideSectionTitle("Qunut")
        RulesOf(SalahGroupContent.qunut, byTopic, actions)
    }
}

// ---------------------------------------------------------------------------------------------
// Səflər
// ---------------------------------------------------------------------------------------------

@Composable
internal fun RowsPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Səflər", onBack = onBack) {
        CalloutWithRest("Nu'man ibn Bəşir · № 399", SalahTopic.ROWS_OR, byTopic, actions)
        GuideSectionTitle("Kim harada durur")
        val seats = SalahGroupContent.seats
        val layout = seats[state.seatIndex.coerceIn(0, seats.lastIndex)]
        ChoiceChips(seats.map { it.title }, seats.indexOf(layout)) { state.seatIndex = it }
        GuideCard {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SeatDiagram(layout)
                SeatLegend()
                Text(layout.caption, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurface)
            }
        }
        TopicQuotes(byTopic.of(layout.topic), actions)
        GuideSectionTitle("Səfləri düzəltmək")
        RulesOf(SalahGroupContent.rows, byTopic, actions)
        GuideSectionTitle(stringResource(Res.string.salahWomenNote))
        TopicQuotes(byTopic.of(SalahTopic.ROWS_WOMEN), actions, compact = true)
    }
}

private val SEAT_WIDTH = 280f

/** Yuxarıdan baxış: qiblə yuxarıda. Sahə 280 enli koordinatlarla qurulur və ekran eninə miqyaslanır. */
@Composable
private fun SeatDiagram(layout: SeatLayout) {
    val women = womenColor()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val scale = maxWidth / SEAT_WIDTH
        Box(modifier = Modifier.fillMaxWidth().height(scale * layout.height)) {
            Text(
                "↑ QİBLƏ",
                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.primary,
                modifier = Modifier.offset(x = scale * 8, y = scale * 6),
            )
            layout.marks.forEach { mark ->
                val radius = if (mark.kind == SeatKind.IMAM) 15 else 13
                val (fill, edge, ink) = when (mark.kind) {
                    SeatKind.IMAM -> Triple(colorScheme.primary, colorScheme.primary, colorScheme.onPrimary)
                    SeatKind.MAN -> Triple(colorScheme.surface, colorScheme.onSurface, colorScheme.onSurface)
                    SeatKind.WOMAN -> Triple(women.alpha(0.12f), women, women)
                }
                Box(
                    modifier = Modifier
                        .offset(x = scale * (mark.x - radius), y = scale * (mark.y - radius))
                        .size(scale * radius * 2)
                        .clip(CircleShape)
                        .background(fill)
                        .border(BorderStroke(2.dp, edge), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (mark.label.isNotEmpty()) {
                        Text(mark.label, style = typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ink)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeatLegend() {
    val women = womenColor()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LegendDot("İmam", colorScheme.primary, colorScheme.primary)
        LegendDot("Kişi (U: uşaq)", colorScheme.surface, colorScheme.onSurface)
        LegendDot("Qadın", women.alpha(0.12f), women)
    }
}

@Composable
private fun LegendDot(label: String, fill: Color, edge: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(fill).border(BorderStroke(2.dp, edge), CircleShape))
        Text(label, style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------
// Cümə
// ---------------------------------------------------------------------------------------------

@Composable
internal fun JumuahPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Cümə", onBack = onBack) {
        CalloutWithRest("Əbu Hureyrə · № 428", SalahTopic.JUMUAH_DAY, byTopic, actions)
        GuideSectionTitle("Cümə günü addım-addım")
        StepsTimeline(SalahGroupContent.jumuahDay, byTopic, actions)
        GuideSectionTitle("Erkən gedənin savabı · № 442")
        GuideCard {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val hours = SalahGroupContent.earlyHours
                hours.forEachIndexed { i, (hour, reward) ->
                    LadderRow(hour, reward, 1f - i * 0.18f, colorScheme.primary)
                }
                LadderRow("imam çıxır", "səhifələr bükülür", 1f, colorScheme.tertiary)
            }
        }
        GuideSectionTitle("Bilmək lazımdır")
        RulesOf(SalahGroupContent.jumuahRules, byTopic, actions)
    }
}

@Composable
private fun LadderRow(label: String, value: String, fraction: Float, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = contentStyle(typography.labelSmall.copy(fontWeight = FontWeight.Bold)), color = colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp))
        Box(modifier = Modifier.weight(1f)) {
            Text(
                value,
                style = contentStyle(typography.labelLarge.copy(fontWeight = FontWeight.Bold)),
                color = tint,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(fraction).clip(RoundedCornerShape(8.dp)).background(tint.alpha(0.16f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Səfər
// ---------------------------------------------------------------------------------------------

@Composable
internal fun SafarPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Səfər namazı", onBack = onBack) {
        CalloutWithRest("Ömər Nəbidən soruşdu · № 465", SalahTopic.SAFAR_SADAQA, byTopic, actions)
        GuideSectionTitle("Rükət sayı")
        GuideCard {
            Column {
                TravelRow("Namaz", "Evdə", "Səfərdə", header = true)
                SalahGroupContent.travelRakats.forEach { r ->
                    HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                    TravelRow(r.name, "${r.home}", "${r.travel}", changed = r.home != r.travel)
                }
            }
        }
        TopicQuotes(byTopic.of(SalahTopic.SAFAR_TWO), actions, compact = true)
        Text(
            "Məğribi üç, İşanı iki rükət Nəbi Muzdəlifədə belə qıldı (2-ci cild № 872), İbn Ömər də birləşdirəndə belə qılırdı (№ 478). Sübh hər yerdə ikidir.",
            style = contentStyle(typography.bodySmall),
            color = colorScheme.onSurfaceVariant,
        )
        GuideSectionTitle("Nə vaxt başlayır")
        RulesOf(SalahGroupContent.safarStart, byTopic, actions)
        GuideSectionTitle("İki namazı birləşdirmək")
        SalahGroupContent.safarCombine.forEach { card ->
            GuideCard {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(card.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))) {
                        Text(card.first, style = contentStyle(typography.labelSmall.copy(fontWeight = FontWeight.Bold)), color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).background(colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 6.dp))
                        Text(card.second, style = contentStyle(typography.labelSmall.copy(fontWeight = FontWeight.Bold)), color = colorScheme.onPrimary,
                            modifier = Modifier.weight(1f).background(colorScheme.primary).padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                    Text(card.text, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant)
                    TopicQuotes(byTopic.of(card.topic), actions, compact = true)
                }
            }
        }
        RulesOf(SalahGroupContent.safarOther, byTopic, actions)
        NotInBook(SalahGroupContent.SAFAR_GAP)
    }
}

@Composable
private fun TravelRow(name: String, home: String, travel: String, header: Boolean = false, changed: Boolean = false) {
    val style = if (header) typography.labelSmall.copy(fontWeight = FontWeight.Bold) else typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth().background(if (header) colorScheme.primary.alpha(0.08f) else colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = contentStyle(style), color = if (header) colorScheme.onSurfaceVariant else colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(home, style = contentStyle(style.copy(fontWeight = FontWeight.Bold)), color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
        Text(travel, style = contentStyle(style.copy(fontWeight = FontWeight.Bold)),
            color = if (header) colorScheme.onSurfaceVariant else if (changed) colorScheme.primary else colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Bayram
// ---------------------------------------------------------------------------------------------

@Composable
internal fun EidPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Bayram namazı", onBack = onBack) {
        CalloutWithRest("Ənəs · № 481", SalahTopic.EID_DAYS, byTopic, actions)
        GuideSectionTitle("Ardıcıllıq")
        StepsTimeline(SalahGroupContent.eidSteps, byTopic, actions)
        NotInBook(SalahGroupContent.EID_GAP)
        RulesOf(SalahGroupContent.eidRules, byTopic, actions)
        GuideSectionTitle(stringResource(Res.string.salahWomenNote))
        RulesOf(SalahGroupContent.eidWomen, byTopic, actions)
    }
}

// ---------------------------------------------------------------------------------------------
// İstisqa
// ---------------------------------------------------------------------------------------------

@Composable
internal fun IstisqaPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "İstisqa", onBack = onBack) {
        CalloutWithRest("Quraqlıq ili · № 492", SalahTopic.ISTISQA_ASK, byTopic, actions)
        ChoiceChips(listOf("Musallədə", "Cümə xütbəsində"), if (state.istisqaMusalla) 0 else 1) { state.istisqaMusalla = it == 0 }
        StepsTimeline(if (state.istisqaMusalla) SalahGroupContent.istisqaMusalla else SalahGroupContent.istisqaJumuah, byTopic, actions)
        GuideSectionTitle("Dualar")
        TopicBlock(byTopic.of(SalahTopic.ISTISQA_DUA), actions)
        GuideSectionTitle("Duada əlləri qaldırmaq")
        RulesOf(SalahGroupContent.raisingHands, byTopic, actions)
    }
}

// ---------------------------------------------------------------------------------------------
// Küsuf
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KusufPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Küsuf", onBack = onBack) {
        CalloutWithRest("İbrahim vəfat edən gün · № 499", SalahTopic.KUSUF_SIGNS, byTopic, actions)
        GuideSectionTitle("Qılınışı · iki rükət, dörd rüku")
        GuideCard {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SalahGroupContent.kusufRakats.forEachIndexed { i, parts ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(colorScheme.primary.alpha(0.16f)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                        }
                        FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            parts.forEach { part ->
                                val (bg, fg) = when {
                                    part.sajda -> colorScheme.secondary.alpha(0.14f) to colorScheme.secondary
                                    part.long -> colorScheme.primary.alpha(0.12f) to colorScheme.primary
                                    else -> colorScheme.background to colorScheme.onSurface
                                }
                                Text(part.label, style = contentStyle(typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)), color = fg, maxLines = 1,
                                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
                Text(
                    "Təkbirlə başlanır, insanlar arxada səfə düzülür. Birinci qiyam Bəqərə surəsini oxuyacaq qədər uzundur (№ 503).",
                    style = contentStyle(typography.bodySmall),
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
        TopicQuotes(byTopic.of(SalahTopic.KUSUF_HOW), actions)
        GuideSectionTitle("Tutulanda nə edilir")
        RulesOf(SalahGroupContent.kusufDo, byTopic, actions)
        GuideSectionTitle(stringResource(Res.string.salahWomenNote))
        TopicQuotes(byTopic.of(SalahTopic.KUSUF_WOMEN), actions, compact = true)
    }
}

// ---------------------------------------------------------------------------------------------
// Qorxu namazı
// ---------------------------------------------------------------------------------------------

@Composable
internal fun KhawfPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Qorxu namazı", onBack = onBack) {
        val forms = SalahGroupContent.khawfForms
        val form = forms[state.khawfIndex.coerceIn(0, forms.lastIndex)]
        Text(
            "Döyüşdə Nəbi camaatı ikiyə böldü: bir dəstə onunla namaz qıldı, o biri düşmənin qarşısında durdu (Nisə, 102). Kitabda beş forma var.",
            style = contentStyle(typography.bodyMedium),
            color = colorScheme.onSurface,
        )
        ChoiceChips(forms.map { it.title }, forms.indexOf(form)) { state.khawfIndex = it }
        Text(form.summary, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant)
        LanesTable(form)
        TopicQuotes(byTopic.of(form.topic), actions)
        GuideSectionTitle("Qorxu çox şiddətli olanda")
        TopicQuotes(byTopic.of(SalahTopic.KHAWF_SEVERE), actions)
        Text(
            "Nəfi: «Abdullah ibn Ömərin bunu yalnız Rəsulullahdan danışdığını hesab edirəm» (№ 506).",
            style = contentStyle(typography.bodySmall),
            color = colorScheme.onSurfaceVariant,
        )
    }
}

/** Dəstələrin növbəsi: hər sətir bir mərhələ, sütunlar imam və iki dəstə. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanesTable(form: KhawfForm) {
    GuideCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(colorScheme.primary.alpha(0.08f)).padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.width(18.dp))
                form.lanes.forEach { lane ->
                    Text(lane.uppercase(), style = typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                }
            }
            form.rows.forEachIndexed { i, cells ->
                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${i + 1}", style = typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.width(18.dp))
                    cells.forEach { cell ->
                        val bg = when (cell.kind) {
                            LaneKind.PRAYER -> colorScheme.primary.alpha(0.16f)
                            LaneKind.GUARD -> colorScheme.tertiary.alpha(0.12f)
                            LaneKind.SALAM -> colorScheme.secondary.alpha(0.16f)
                            LaneKind.NONE -> Color.Transparent
                        }
                        Text(
                            cell.text,
                            style = contentStyle(typography.labelSmall),
                            color = if (cell.kind == LaneKind.NONE) colorScheme.onSurfaceVariant else colorScheme.onSurface,
                            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(bg)
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LaneKey("namaz", colorScheme.primary.alpha(0.16f))
                LaneKey("keşik", colorScheme.tertiary.alpha(0.12f))
                LaneKey("salam", colorScheme.secondary.alpha(0.16f))
            }
        }
    }
}

@Composable
private fun LaneKey(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(width = 14.dp, height = 10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant)
    }
}
