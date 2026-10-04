package com.cafarovceyxun.anamuslim.compose.screens.salah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.cafarovceyxun.anamuslim.compose.screens.dua.LocalDuaActions
import com.cafarovceyxun.anamuslim.compose.screens.hajj.EvidenceActions
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuideCard
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuidePage
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuideSectionTitle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.arabicStyle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.contentStyle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.salahWomenNote
import com.cafarovceyxun.anamuslim.utils.salah.NightBlockKind
import com.cafarovceyxun.anamuslim.utils.salah.NightForm
import com.cafarovceyxun.anamuslim.utils.salah.SahwPartKind
import com.cafarovceyxun.anamuslim.utils.salah.SahwWay
import com.cafarovceyxun.anamuslim.utils.salah.SalahNaflContent
import com.cafarovceyxun.anamuslim.utils.salah.SalahTopic
import com.cafarovceyxun.anamuslim.utils.salah.SunnahCell
import com.cafarovceyxun.anamuslim.utils.salah.SunnahKind
import com.cafarovceyxun.anamuslim.utils.supabase.GuideEvidence
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/*
 * Namaz bələdçisi — Mərhələ 4 (Nafilə və cənazə) ekranları. Məzmun [SalahNaflContent]-dən, hədislər və üç gecə
 * duası `salah_evidence`-dən. Yeni şəkil yoxdur: sünnət cədvəli, gecə namazı formaları və səhv səcdəsinin
 * ardıcıllığı Compose-da çəkilir. Vəziyyət (seçilmiş forma və hal) [SalahGuideState]-dədir.
 */

// ---------------------------------------------------------------------------------------------
// Ortaq hissələr
// ---------------------------------------------------------------------------------------------

/**
 * Quran oxucusunu açan düymə. Bələdçi tam ekran səthdir, ona görə [LocalDuaActions] əvvəl onu bağlayır
 * (`SalahGuideScreen`); host ayə açmağı vermirsə düymə görünmür.
 */
@Composable
private fun VerseButton(label: String, chapter: Int, verse: Int) {
    val open = LocalDuaActions.current.onOpenVerse ?: return
    Surface(onClick = { open(chapter, verse) }, shape = RoundedCornerShape(999.dp), color = colorScheme.primary.alpha(0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = contentStyle(typography.labelLarge.copy(fontWeight = FontWeight.Bold)), color = colorScheme.primary)
            Icon(painterResource(Res.drawable.dr_icon_chevron_right), null, tint = colorScheme.primary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun WomenSection(vararg topics: SalahTopic, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions) {
    GuideSectionTitle(stringResource(Res.string.salahWomenNote))
    TopicQuotes(topics.flatMap { byTopic.of(it) }, actions, compact = true)
}

@Composable
private fun SmallNote(text: String) {
    Text(text, style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyRow(vararg items: Pair<String, Color>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(width = 14.dp, height = 10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                Text(label, style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bölmə
// ---------------------------------------------------------------------------------------------

private val naflTiles = listOf(
    ArabicTile(SalahPage.SUNNAH, "السُّنَن", "Sünnət namazları", "fərzdən əvvəl və sonra · № 511–526"),
    ArabicTile(SalahPage.DUHA, "الضُّحَى", "Duha və nafilələr", "duha, evdə, davamlı · № 527–539"),
    ArabicTile(SalahPage.NIGHT, "قِيَامُ اللَّيْل", "Gecə namazı və vitr", "iki-iki, vitr, dualar · № 535–569"),
    ArabicTile(SalahPage.SAHW, "سُجُودُ السَّهْو", "Səhv səcdəsi", "şəkk, artıq, əskik · № 570–575"),
    ArabicTile(SalahPage.TILAWAH, "سُجُودُ التِّلَاوَة", "Tilavət səcdəsi", "Nəcm, İnşiqaq, Aləq · № 576–579"),
    ArabicTile(SalahPage.JANAZAH, "الْجَنَائِز", "Cənazə", "yumaq, namaz, dəfn · № 580–595"),
)

@Composable
internal fun NaflHubPage(state: SalahGuideState, onBack: () -> Unit) {
    ArabicHubPage("Nafilə və cənazə", "التَّطَوُّع", "№ 511–595", naflTiles, state, onBack)
}

// ---------------------------------------------------------------------------------------------
// Sünnət namazları
// ---------------------------------------------------------------------------------------------

@Composable
internal fun SunnahPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Sünnət namazları", onBack = onBack) {
        CalloutWithRest("Ummu Həbibə · № 511", SalahTopic.SUNNAH_12, byTopic, actions)
        GuideSectionTitle("Fərzdən əvvəl və sonra")
        SunnahTable()
        KeyRow(
            "Nəbi qılırdı" to colorScheme.primary.alpha(0.16f),
            "istəyən üçün" to colorScheme.secondary.alpha(0.16f),
            "qadağa" to colorScheme.tertiary.alpha(0.12f),
        )
        TopicQuotes(byTopic.of(SalahTopic.SUNNAH_TEN), actions)
        SmallNote(SalahNaflContent.SUNNAH_TWELVE_NOTE)
        GuideSectionTitle("Hər əzan ilə iqamə arası")
        TopicQuotes(byTopic.of(SalahTopic.SUNNAH_BETWEEN), actions)
        GuideSectionTitle("Fəcrin iki rükəti")
        RulesOf(SalahNaflContent.fajr, byTopic, actions)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VerseButton("Bəqərə, 136", 2, 136)
            VerseButton("Ali-İmran, 64", 3, 64)
        }
        GuideSectionTitle("Məğribdən əvvəl iki rükət")
        TopicQuotes(byTopic.of(SalahTopic.MAGHRIB_BEFORE), actions)
        GuideSectionTitle("Qadağan vaxtlar")
        RulesOf(SalahNaflContent.sunnahOther, byTopic, actions)
    }
}

@Composable
private fun SunnahTable() {
    GuideCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(colorScheme.primary.alpha(0.08f)).padding(horizontal = 9.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.width(58.dp))
                listOf("ƏVVƏL", "FƏRZ", "SONRA").forEach {
                    Text(it, style = typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            SalahNaflContent.sunnahMap.forEach { row ->
                HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 9.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.name, style = contentStyle(typography.labelLarge.copy(fontWeight = FontWeight.Bold)), color = colorScheme.onSurface,
                        modifier = Modifier.width(58.dp))
                    listOf(row.before, row.fard, row.after).forEach { SunnahCellView(it, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun SunnahCellView(cell: SunnahCell, modifier: Modifier) {
    val (bg, fg) = when (cell.kind) {
        SunnahKind.PROPHET -> colorScheme.primary.alpha(0.16f) to colorScheme.onSurface
        SunnahKind.OPTIONAL -> colorScheme.secondary.alpha(0.16f) to colorScheme.onSurface
        SunnahKind.FORBIDDEN -> colorScheme.tertiary.alpha(0.12f) to colorScheme.tertiary
        SunnahKind.FARD -> colorScheme.background to colorScheme.onSurface
        SunnahKind.NONE -> Color.Transparent to colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        cell.main?.let { Text(it, style = typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = fg, textAlign = TextAlign.Center) }
        cell.sub?.let {
            Text(it, style = contentStyle(typography.labelSmall), color = if (cell.kind == SunnahKind.FORBIDDEN) fg else colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Duha və nafilələr
// ---------------------------------------------------------------------------------------------

@Composable
internal fun DuhaPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Duha və nafilələr", onBack = onBack) {
        GuideSectionTitle("Duha namazı")
        RulesOf(SalahNaflContent.duha, byTopic, actions)
        NotInBook("Müəllifin qeydi (№ 532): «${SalahNaflContent.DUHA_NOTE}»")
        GuideSectionTitle("Başqa nafilələr")
        RulesOf(SalahNaflContent.nafl, byTopic, actions)
        WomenSection(SalahTopic.NAFL_WOMEN, byTopic = byTopic, actions = actions)
    }
}

// ---------------------------------------------------------------------------------------------
// Gecə namazı və vitr
// ---------------------------------------------------------------------------------------------

@Composable
internal fun NightPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Gecə namazı və vitr", onBack = onBack) {
        CalloutWithRest("Davudun namazı · № 543", SalahTopic.NIGHT_DAWUD, byTopic, actions)
        GuideSectionTitle("Niyə qalxmalı")
        RulesOf(SalahNaflContent.nightWhy, byTopic, actions)
        GuideSectionTitle("Nə vaxt")
        RulesOf(SalahNaflContent.nightWhen, byTopic, actions)
        GuideSectionTitle("Necə")
        TopicQuotes(byTopic.of(SalahTopic.NIGHT_TWO_TWO), actions)
        TopicQuotes(byTopic.of(SalahTopic.WITR_ONE), actions, compact = true)
        GuideSectionTitle("Neçə rükət · kitabdakı formalar")
        val forms = SalahNaflContent.nightForms
        val form = forms[state.nightIndex.coerceIn(0, forms.lastIndex)]
        ChoiceChips(forms.map { it.title }, forms.indexOf(form)) { state.nightIndex = it }
        NightBlocks(form)
        TopicQuotes(byTopic.of(form.topic), actions)
        GuideSectionTitle("Qalxanda")
        StepsTimeline(SalahNaflContent.nightWake, byTopic, actions)
        VerseButton("Ali-İmran, 190-dan sona", 3, 190)
        GuideSectionTitle("Gecə duaları")
        TopicBlock(byTopic.of(SalahTopic.NIGHT_DUA_TAHAJJUD), actions)
        SmallNote(SalahNaflContent.TAHAJJUD_NOTES)
        TopicBlock(byTopic.of(SalahTopic.NIGHT_DUA_NUR), actions)
        TopicBlock(byTopic.of(SalahTopic.NIGHT_DUA_SAJDA), actions)
        GuideSectionTitle("Bilmək lazımdır")
        RulesOf(SalahNaflContent.nightOther, byTopic, actions)
        WomenSection(SalahTopic.NIGHT_WOMEN, byTopic = byTopic, actions = actions)
    }
}

/** Gecə namazının bir forması: blokun eni rükət sayına mütənasibdir, vitr və sünnət ayrı rəngdə. */
@Composable
private fun NightBlocks(form: NightForm) {
    GuideCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(form.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                Text("${form.total}", style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                form.blocks.forEach { block ->
                    if (block.kind == NightBlockKind.SLEEP) {
                        Text(block.label, style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically))
                        return@forEach
                    }
                    val (bg, fg) = when (block.kind) {
                        NightBlockKind.WITR -> colorScheme.tertiary.alpha(0.14f) to colorScheme.tertiary
                        NightBlockKind.SUNNAH -> colorScheme.background to colorScheme.onSurfaceVariant
                        else -> colorScheme.primary.alpha(0.16f) to colorScheme.primary
                    }
                    Box(
                        modifier = Modifier.weight(block.rakats.toFloat()).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(bg)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // «Fəcr 2», «İşa 2» iki rükətlik dar blokda tək sətrə sığmır.
                        val numeric = block.label.all { it.isDigit() }
                        Text(
                            block.label,
                            style = (if (numeric) typography.labelLarge else typography.labelSmall).copy(fontWeight = FontWeight.Bold),
                            color = fg,
                            maxLines = if (numeric) 1 else 2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            SmallNote(form.summary)
            KeyRow(
                "gecə namazı" to colorScheme.primary.alpha(0.16f),
                "vitr" to colorScheme.tertiary.alpha(0.14f),
                "sünnət" to colorScheme.background,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Səhv səcdəsi
// ---------------------------------------------------------------------------------------------

@Composable
internal fun SahwPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Səhv səcdəsi", onBack = onBack) {
        CalloutWithRest("Şeytan namazda · № 570", SalahTopic.SAHW_SHAYTAN, byTopic, actions)
        TopicQuotes(byTopic.of(SalahTopic.SAHW_SIT), actions, compact = true)
        GuideSectionTitle("Nə oldu?")
        val cases = SalahNaflContent.sahwCases
        val case = cases[state.sahwIndex.coerceIn(0, cases.lastIndex)]
        ChoiceChips(cases.map { it.title }, cases.indexOf(case)) { state.sahwIndex = it }
        case.ways.forEach { way ->
            SahwWayCard(way)
            TopicQuotes(byTopic.of(way.topic), actions, compact = true)
        }
        KeyRow("səhv səcdəsi" to colorScheme.tertiary.alpha(0.14f), "salam" to colorScheme.primary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SahwWayCard(way: SahwWay) {
    GuideCard {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(way.title.uppercase(), style = typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                way.parts.forEachIndexed { i, part ->
                    if (i > 0) Text("→", style = typography.labelLarge, color = colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
                    val (bg, fg) = when (part.kind) {
                        SahwPartKind.SALAM -> colorScheme.primary to colorScheme.onPrimary
                        SahwPartKind.SAJDA -> colorScheme.tertiary.alpha(0.14f) to colorScheme.tertiary
                        SahwPartKind.STEP -> colorScheme.background to colorScheme.onSurface
                    }
                    Text(part.label, style = contentStyle(typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)), color = fg,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 9.dp, vertical = 4.dp))
                }
            }
            way.note?.let { SmallNote(it) }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tilavət səcdəsi
// ---------------------------------------------------------------------------------------------

@Composable
internal fun TilawahPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    val open = LocalDuaActions.current.onOpenVerse
    GuidePage(title = "Tilavət səcdəsi", onBack = onBack) {
        CalloutWithRest("İbn Ömər · № 576", SalahTopic.TILAWAH_OUT, byTopic, actions)
        GuideSectionTitle("Kitabda adı keçən surələr")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            SalahNaflContent.tilawahSurahs.forEach { surah ->
                Surface(
                    onClick = { open?.invoke(surah.chapter, 1) },
                    enabled = open != null,
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surface,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(surah.arabic, style = arabicStyle(22), color = colorScheme.primary, textAlign = TextAlign.Center)
                        Text(surah.name, style = contentStyle(typography.labelLarge.copy(fontWeight = FontWeight.Bold)), color = colorScheme.onSurface)
                        Text(surah.refs, style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        RulesOf(SalahNaflContent.tilawah, byTopic, actions)
        NotInBook(SalahNaflContent.TILAWAH_GAP)
    }
}

// ---------------------------------------------------------------------------------------------
// Cənazə
// ---------------------------------------------------------------------------------------------

@Composable
internal fun JanazahPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Cənazə", onBack = onBack) {
        CalloutWithRest("Əbu Hureyrə · № 583", SalahTopic.JANAZAH_QIRAT, byTopic, actions)
        GuideSectionTitle("Ardıcıllıq")
        StepsTimeline(SalahNaflContent.janazahSteps, byTopic, actions)
        GuideSectionTitle("Cənazə namazı")
        StepsTimeline(SalahNaflContent.janazahPrayer, byTopic, actions)
        AwaitingVolume(SalahNaflContent.JANAZAH_WAIT)
        TopicQuotes(byTopic.of(SalahTopic.JANAZAH_ISTIGHFAR), actions, compact = true)
        GuideSectionTitle("Kimə qılınır")
        RulesOf(SalahNaflContent.janazahWhom, byTopic, actions)
        GuideSectionTitle("Qəbirlər")
        RulesOf(SalahNaflContent.graves, byTopic, actions)
        WomenSection(SalahTopic.JANAZAH_HAIR, SalahTopic.JANAZAH_WOMEN, byTopic = byTopic, actions = actions)
    }
}

/** Kitabın hələ yüklənməmiş hissəsini gözləyən yer — boş qalmır, səbəbi deyilir. */
@Composable
private fun AwaitingVolume(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colorScheme.tertiary.alpha(0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("7-Cİ CİLD GÖZLƏNİLİR", style = typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.tertiary)
        Text(text, style = contentStyle(typography.bodySmall), color = colorScheme.onSurface)
    }
}
