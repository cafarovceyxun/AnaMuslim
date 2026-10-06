package com.cafarovceyxun.anamuslim.compose.screens.salah

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.homepage.LocalHomeActions
import com.cafarovceyxun.anamuslim.compose.screens.hajj.EvidenceActions
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuideCard
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuidePage
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuideSectionTitle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.HajjEvidenceSection
import com.cafarovceyxun.anamuslim.compose.screens.hajj.arabicStyle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.contentStyle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.translationStyle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_feet
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_feetc
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_frontgaze
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_hand
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_qiyam
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_ruku
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_sajda
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_salam
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_sitfront
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_sitgaze
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_stand
import com.cafarovceyxun.anamuslim.resources.dr_salah_pose_takbir
import com.cafarovceyxun.anamuslim.resources.salahFinish
import com.cafarovceyxun.anamuslim.resources.salahNext
import com.cafarovceyxun.anamuslim.resources.salahPrevious
import com.cafarovceyxun.anamuslim.resources.salahStartSteps
import com.cafarovceyxun.anamuslim.resources.salahWomenNote
import com.cafarovceyxun.anamuslim.utils.salah.FixedDhikr
import com.cafarovceyxun.anamuslim.utils.salah.PrayerPose
import com.cafarovceyxun.anamuslim.utils.salah.PrayerReading
import com.cafarovceyxun.anamuslim.utils.salah.PrayerStep
import com.cafarovceyxun.anamuslim.utils.salah.RakatPart
import com.cafarovceyxun.anamuslim.utils.salah.RuleMark
import com.cafarovceyxun.anamuslim.utils.salah.RuleRow
import com.cafarovceyxun.anamuslim.utils.salah.SalahPrayerContent
import com.cafarovceyxun.anamuslim.utils.salah.SalahTopic
import com.cafarovceyxun.anamuslim.utils.supabase.GuideEvidence
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/*
 * Namaz bələdçisi — Mərhələ 2 (Əzan və namaz) ekranları. Məzmun [SalahPrayerContent]-dən, hədislər və
 * zikrlər `salah_evidence`-dən. Səhifə keçidi və vəziyyət [SalahGuideState]-dədir (Mərhələ 1 ilə ortaq).
 */

// ---------------------------------------------------------------------------------------------
// Şəkillər — istifadəçinin kadrları, vektor (xəttin rəngi mövzudan gəlir, tünd rejimdə açıq olur)
// ---------------------------------------------------------------------------------------------

private val PrayerPose.drawable: DrawableResource
    get() = when (this) {
        PrayerPose.STAND -> Res.drawable.dr_salah_pose_stand
        PrayerPose.TAKBIR -> Res.drawable.dr_salah_pose_takbir
        PrayerPose.QIYAM -> Res.drawable.dr_salah_pose_qiyam
        PrayerPose.RUKU -> Res.drawable.dr_salah_pose_ruku
        PrayerPose.SAJDA -> Res.drawable.dr_salah_pose_sajda
        PrayerPose.SALAM -> Res.drawable.dr_salah_pose_salam
        PrayerPose.SIT_FRONT -> Res.drawable.dr_salah_pose_sitfront
        PrayerPose.SIT_GAZE -> Res.drawable.dr_salah_pose_sitgaze
        PrayerPose.FRONT_GAZE -> Res.drawable.dr_salah_pose_frontgaze
        PrayerPose.HAND -> Res.drawable.dr_salah_pose_hand
        PrayerPose.FEET -> Res.drawable.dr_salah_pose_feet
        PrayerPose.FEET_CIRCLE -> Res.drawable.dr_salah_pose_feetc
    }

/** Kadr verilən sahəyə tam sığır (`Fit`) — kəsilmir. [mirror] — güzgü əksi (sola salam). */
@Composable
private fun PoseImage(pose: PrayerPose, contentDescription: String?, modifier: Modifier = Modifier, mirror: Boolean = false) {
    Image(
        painter = painterResource(pose.drawable),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(colorScheme.onSurface),
        modifier = modifier.graphicsLayer { if (mirror) scaleX = -1f },
    )
}

/** Böyük kadr kartı — addımda və səhifələrdə. [pair] — sağa və güzgü ilə sola (salam). */
@Composable
private fun PoseStage(pose: PrayerPose, title: String, pair: Boolean = false, height: Int = 260) {
    Surface(shape = RoundedCornerShape(20.dp), color = colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height.dp).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pair) {
                PoseImage(pose, "$title: sağa", Modifier.weight(1f).fillMaxHeight())
                PoseImage(pose, "$title: sola", Modifier.weight(1f).fillMaxHeight(), mirror = true)
            } else {
                PoseImage(pose, title, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PoseInsets(poses: List<PrayerPose>) {
    if (poses.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        poses.forEach { pose ->
            Surface(shape = RoundedCornerShape(16.dp), color = colorScheme.surface, modifier = Modifier.weight(1f).aspectRatio(1f)) {
                PoseImage(pose, null, Modifier.fillMaxSize().padding(10.dp))
            }
        }
    }
}

/** Bazada olmayan zikr kartı — salamın sözləri (istifadəçidən), «Amin». Mənbə sətri yoxdur; tərcümə boşdursa göstərilmir. */
@Composable
internal fun FixedDhikrCard(dhikr: FixedDhikr, caption: String) {
    GuideCard {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Text(caption.uppercase(), style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 18.dp))
            Text(dhikr.arabic, style = arabicStyle(26), color = colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp))
            Text(dhikr.transliteration, style = contentStyle(typography.bodyMedium.copy(fontStyle = FontStyle.Italic)),
                color = colorScheme.primary.alpha(0.9f), modifier = Modifier.padding(horizontal = 18.dp))
            if (dhikr.meaning.isNotBlank()) {
                Text(dhikr.meaning, style = translationStyle(), color = colorScheme.onSurface.alpha(0.92f),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
            }
        }
    }
}

/** Mövzunun zikrləri (tam blok) və dəlilləri (sitat) — addımın altında. */
@Composable
internal fun TopicBlock(items: List<GuideEvidence>, actions: EvidenceActions) {
    val dhikr = items.filter { it.isDhikr }
    val evidence = items.filterNot { it.isDhikr }
    if (dhikr.isNotEmpty()) HajjEvidenceSection(dhikr, actions, showTitles = false)
    if (evidence.isNotEmpty() || dhikr.isEmpty()) TopicQuotes(evidence, actions)
}

@Composable
internal fun LinkRow(title: String, sub: String?, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.primary)
                sub?.let { Text(it, style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant) }
            }
            Icon(painterResource(Res.drawable.dr_icon_chevron_right), null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun RulesOf(rows: List<RuleRow>, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions) {
    rows.forEach { RuleRowView(it, byTopic.of(it.topic), actions) }
}

// ---------------------------------------------------------------------------------------------
// Bölmə
// ---------------------------------------------------------------------------------------------

private class PrayerTile(val page: SalahPage, val pose: PrayerPose?, val arabic: String?, val title: String, val sub: String)

private val prayerTiles = listOf(
    PrayerTile(SalahPage.PRAYER_HOW, PrayerPose.RUKU, null, "Namazın qılınışı", "16 addım · № 205–364"),
    // Şəkil istifadəçinin istəyi ilə götürülüb (2026-10-04) — digər yazılı kartlar kimi ərəbcə ad.
    PrayerTile(SalahPage.ADHAN, null, "الأَذَان", "Əzan və iqamə", "8 cümlə · № 302–314"),
    PrayerTile(SalahPage.TIMES, null, "الْمَوَاقِيت", "Vaxtlar", "və rükət sayı · № 248–289"),
    PrayerTile(SalahPage.QIBLA, null, "الْقِبْلَة", "Qiblə", "№ 290–301"),
    PrayerTile(SalahPage.AFTER, null, "الأَذْكَار", "Namazdan sonra", "zikrlər · № 365–370"),
    PrayerTile(SalahPage.KNOW, null, "آدَاب", "Bilmək lazımdır", "nə olmaz, sütrə · № 189–326"),
)

@Composable
internal fun PrayerHubPage(state: SalahGuideState, onBack: () -> Unit) {
    GuidePage(title = "Əzan və namaz", onBack = onBack) {
        GuideCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("الصَّلَاة", style = arabicStyle(34), color = colorScheme.primary)
                Text("Muheymin 1-ci cild · Namaz Kitabı", style = contentStyle(typography.titleSmall), color = colorScheme.onSurface)
                Text("№ 183–370", style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
            }
        }
        prayerTiles.chunked(2).forEach { pair ->
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
                                modifier = Modifier.fillMaxWidth().height(116.dp)
                                    .background(if (tile.pose == null) colorScheme.primary.alpha(0.08f) else colorScheme.surface),
                                contentAlignment = Alignment.Center,
                            ) {
                                tile.pose?.let { PoseImage(it, null, Modifier.fillMaxSize().padding(10.dp)) }
                                tile.arabic?.let { Text(it, style = arabicStyle(28), color = colorScheme.primary, textAlign = TextAlign.Center) }
                            }
                            Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                                Text(tile.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                                Text(tile.sub, style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        DraftNote()
    }
}

// ---------------------------------------------------------------------------------------------
// Vaxtlar
// ---------------------------------------------------------------------------------------------

@Composable
internal fun TimesPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit, onCloseGuide: () -> Unit) {
    val home = LocalHomeActions.current
    GuidePage(title = "Vaxtlar", onBack = onBack) {
        LinkRow("Bu günün namaz vaxtları", "Tətbiqin namaz vaxtları ekranı") { onCloseGuide(); home.onOpenPrayerTimes() }
        GuideSectionTitle("Cibril iki gün göstərdi · № 255")
        GuideCard {
            Column {
                TimesRow("Namaz", "1-ci gün", "2-ci gün", header = true)
                SalahPrayerContent.jibrilTimes.forEach { (name, first, second) ->
                    HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                    TimesRow(name, first, second)
                }
            }
        }
        TopicQuotes(byTopic.of(SalahTopic.TIMES_BETWEEN), actions, compact = true)
        GuideSectionTitle("Rükət sayı")
        SalahPrayerContent.prayers.distinctBy { it.countTopic }.forEach { p ->
            val names = SalahPrayerContent.prayers.filter { it.countTopic == p.countTopic }
            val title = names.joinToString(", ") { "${it.name} · ${it.rakats}" }
            val note = if (p.countTopic == SalahTopic.RAKAT_FOUR) "Namaz əvvəl iki rükət fərz edildi, sonra muqim üçün dörd edildi. Mötərizədəki adlar tərcüməçinindir." else null
            RuleRowView(RuleRow(p.countTopic, RuleMark.INFO, title, note), byTopic.of(p.countTopic), actions)
        }
        GuideSectionTitle("Fəzilət və qaydalar")
        RulesOf(SalahPrayerContent.timesRules, byTopic, actions)
        GuideSectionTitle("Namaz qılınmayan vaxtlar")
        RulesOf(SalahPrayerContent.timesForbidden, byTopic, actions)
        GuideSectionTitle(stringResource(Res.string.salahWomenNote))
        TopicQuotes(byTopic.of(SalahTopic.TIMES_WOMEN), actions, compact = true)
    }
}

@Composable
private fun TimesRow(name: String, first: String, second: String, header: Boolean = false) {
    val style = if (header) typography.labelSmall.copy(fontWeight = FontWeight.Bold) else typography.bodySmall
    val color = if (header) colorScheme.onSurfaceVariant else colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().background(if (header) colorScheme.primary.alpha(0.08f) else colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(name, style = contentStyle(if (header) style else typography.labelLarge.copy(fontWeight = FontWeight.Bold)), color = color, modifier = Modifier.width(62.dp))
        Text(first, style = contentStyle(style), color = color, modifier = Modifier.weight(1f))
        Text(second, style = contentStyle(style), color = color, modifier = Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------------------------
// Qiblə
// ---------------------------------------------------------------------------------------------

@Composable
internal fun QiblaPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit, onCloseGuide: () -> Unit) {
    val home = LocalHomeActions.current
    GuidePage(title = "Qiblə", onBack = onBack) {
        val ayah = byTopic.of(SalahTopic.QIBLA_AYAH)
        Callout("Bəqərə 144 · № 291", ayah.firstOrNull(), ayah, actions)
        LinkRow("Qiblə istiqaməti", "Tətbiqin qiblə kompası") { onCloseGuide(); home.onOpenQibla() }
        RulesOf(SalahPrayerContent.qiblaRules, byTopic, actions)
    }
}

// ---------------------------------------------------------------------------------------------
// Əzan
// ---------------------------------------------------------------------------------------------

@Composable
internal fun AdhanPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Əzan və iqamə", onBack = onBack) {
        val pairs = byTopic.of(SalahTopic.ADHAN_PAIRS)
        Callout("№ 307", pairs.firstOrNull(), pairs, actions)
        GuideCard {
            Column {
                SalahPrayerContent.adhan.forEachIndexed { i, line ->
                    if (i > 0) HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(line.arabic, style = arabicStyle(24), color = colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(line.transliteration, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                                Text(line.meaning, style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
                            }
                            CountChip(line.adhan, "ƏZAN")
                            CountChip(line.iqama, "İQAMƏ")
                        }
                    }
                }
            }
        }
        GuideSectionTitle("Əzanı eşidən")
        TopicQuotes(byTopic.of(SalahTopic.ADHAN_REPEAT), actions)
        GuideSectionTitle("Bilmək lazımdır")
        RulesOf(SalahPrayerContent.adhanRules, byTopic, actions)
    }
}

@Composable
private fun CountChip(count: Int, label: String) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(colorScheme.background).padding(horizontal = 8.dp, vertical = 3.dp)
            .graphicsLayer { alpha = if (count == 0) 0.35f else 1f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (count == 0) "—" else "$count", style = typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
        Text(label, style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------------------------------------
// Namazın qılınışı
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PrayerHowPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Namazın qılınışı", onBack = onBack) {
        val teach = byTopic.of(SalahTopic.PRAYER_TEACH)
        Callout("№ 205 · Nəbi bir kişiyə üç dəfə dedi", teach.firstOrNull(), teach, actions)
        TopicQuotes(teach.drop(1), actions, compact = true)

        GuideSectionTitle("Rükət xəritəsi")
        val prayer = SalahPrayerContent.prayers[state.prayerIndex.coerceIn(0, SalahPrayerContent.prayers.lastIndex)]
        GuideCard {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SalahPrayerContent.prayers.forEachIndexed { i, p ->
                        val selected = p == prayer
                        Text(
                            "${p.name} · ${p.rakats}",
                            style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(999.dp))
                                .background(if (selected) colorScheme.primary else colorScheme.surfaceVariant)
                                .clickable { state.prayerIndex = i }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                (1..prayer.rakats).forEach { rakat ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(colorScheme.primary.alpha(0.16f)), contentAlignment = Alignment.Center) {
                            Text("$rakat", style = typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                        }
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            prayer.parts(rakat).forEach { part -> RakatChip(part) }
                        }
                    }
                }
                Text(
                    "«Fatihə + surə» və «yalnız Fatihə» bölgüsü № 336-da Zöhr və Əsr üçün gəlir. Təşəhhüd oturuşları: № 358.",
                    style = contentStyle(typography.bodySmall),
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
        TopicQuotes(byTopic.of(prayer.countTopic), actions, compact = true)
        GuideSectionTitle(if (prayer.reading == PrayerReading.SILENT) "Qiraət səssizdir" else "Qiraət eşidilirdi")
        TopicQuotes(byTopic.of(prayer.readingTopic), actions, compact = true)

        GuideSectionTitle("Addımlar · ${SalahPrayerContent.steps.size}")
        GuideCard {
            Column {
                SalahPrayerContent.steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { state.openPrayerStep(index) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("${index + 1}", style = typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary,
                            modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                        PoseImage(step.pose, null, Modifier.size(width = 64.dp, height = 56.dp))
                        Text(step.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)),
                            color = colorScheme.onSurface, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Button(onClick = { state.openPrayerStep(0) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(stringResource(Res.string.salahStartSteps), style = typography.labelLarge)
        }
    }
}

@Composable
private fun RakatChip(part: RakatPart) {
    val (bg, fg) = when (part) {
        RakatPart.FATIHA_SURAH, RakatPart.FATIHA_ONLY -> colorScheme.primary.alpha(0.12f) to colorScheme.primary
        RakatPart.FIRST_TASHAHHUD, RakatPart.LAST_TASHAHHUD, RakatPart.SALAM -> colorScheme.secondary.alpha(0.14f) to colorScheme.secondary
        else -> colorScheme.background to colorScheme.onSurface
    }
    Text(
        part.label,
        style = contentStyle(typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)),
        color = fg,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Namaz addımları — dəstəmaz pageri ilə eyni quruluş. */
@Composable
internal fun PrayerStepPager(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    val steps = SalahPrayerContent.steps
    val pager = rememberPagerState(initialPage = state.prayerStep.coerceIn(0, steps.lastIndex)) { steps.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { state.prayerStep = it } }

    Scaffold(
        topBar = {
            AppBar(title = "Namazın qılınışı", onBack = onBack) {
                Text("${pager.currentPage + 1} / ${steps.size}", style = typography.labelLarge, color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 14.dp))
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(colorScheme.background).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    enabled = pager.currentPage > 0,
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text(stringResource(Res.string.salahPrevious)) }
                val last = pager.currentPage == steps.lastIndex
                Button(
                    onClick = { if (last) onBack() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text(stringResource(if (last) Res.string.salahFinish else Res.string.salahNext)) }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                steps.indices.forEach { i ->
                    Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i <= pager.currentPage) colorScheme.primary else colorScheme.surfaceVariant))
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                PrayerStepContent(steps[page], byTopic.of(steps[page].topic), actions)
            }
        }
    }
}

@Composable
private fun PrayerStepContent(step: PrayerStep, items: List<GuideEvidence>, actions: EvidenceActions) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ReadableWidthColumn {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PoseStage(step.pose, step.title, pair = step.pair)
                PoseInsets(step.insets)
                Text(step.title, style = contentStyle(typography.headlineSmall.copy(fontWeight = FontWeight.Bold)), color = colorScheme.onSurface)
                Text(step.text, style = contentStyle(typography.bodyLarge), color = colorScheme.onSurface.alpha(0.9f))
                if (step.showsSalamWords) FixedDhikrCard(SalahPrayerContent.salamWords, "Salamın sözləri")
                TopicBlock(items, actions)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Namazdan sonra
// ---------------------------------------------------------------------------------------------

@Composable
internal fun AfterPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Namazdan sonra", onBack = onBack) {
        val takbir = byTopic.of(SalahTopic.AFTER_TAKBIR)
        Callout("İbn Abbas · № 366", takbir.firstOrNull(), takbir, actions)
        GuideSectionTitle("Salamdan dərhal sonra")
        TopicBlock(byTopic.of(SalahTopic.AFTER_SALAM), actions)
        GuideSectionTitle("Hər namazdan sonra · № 369")
        TopicQuotes(byTopic.of(SalahTopic.AFTER_33), actions, compact = true)
        val counter = SalahPrayerContent.counter
        counter.forEachIndexed { i, dhikr ->
            val count = state.counts.getOrElse(i) { 0 }
            Surface(
                onClick = { if (count < dhikr.times) state.counts = state.counts.toMutableList().also { it[i] = count + 1 } },
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(dhikr.arabic, style = arabicStyle(28), color = colorScheme.onSurface, textAlign = TextAlign.Center)
                    Text(dhikr.transliteration, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant)
                    Text("$count", style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colorScheme.surfaceVariant)) {
                        Box(Modifier.fillMaxWidth(count / dhikr.times.toFloat()).fillMaxHeight().background(colorScheme.primary))
                    }
                    Text("${dhikr.times} dəfə · toxunun", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
                }
            }
        }
        val total = state.counts.sum()
        if (total >= counter.sumOf { it.times }) {
            GuideSectionTitle("Sonda bir dəfə")
            TopicBlock(byTopic.of(SalahTopic.AFTER_TAHLIL), actions)
        } else {
            Text("99-dan sonra təhlil ilə bitirilir. Sayğac bitəndə mətn burada açılır.",
                style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
        }
        if (total > 0) {
            FilledTonalButton(onClick = { state.counts = List(counter.size) { 0 } }, modifier = Modifier.fillMaxWidth()) { Text("Sıfırla") }
        }
        GuideSectionTitle("Başqa")
        RuleRowView(RuleRow(SalahTopic.AFTER_LEFT, RuleMark.INFO, "Hansı tərəfdən getmək", null), byTopic.of(SalahTopic.AFTER_LEFT), actions)
    }
}

// ---------------------------------------------------------------------------------------------
// Namazda bilmək lazımdır
// ---------------------------------------------------------------------------------------------

@Composable
internal fun KnowPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Namazda bilmək lazımdır", onBack = onBack) {
        val light = byTopic.of(SalahTopic.KNOW_LIGHT)
        Callout("Hüzeyfə · № 206", light.firstOrNull(), light, actions)
        GuideSectionTitle("Olmaz")
        RulesOf(SalahPrayerContent.knowNo, byTopic, actions)
        GuideSectionTitle("Olar")
        RulesOf(SalahPrayerContent.knowYes, byTopic, actions)
        GuideSectionTitle("Sütrə")
        RulesOf(SalahPrayerContent.sutra, byTopic, actions)
        GuideSectionTitle(stringResource(Res.string.salahWomenNote))
        RulesOf(SalahPrayerContent.knowWomen, byTopic, actions)
    }
}
