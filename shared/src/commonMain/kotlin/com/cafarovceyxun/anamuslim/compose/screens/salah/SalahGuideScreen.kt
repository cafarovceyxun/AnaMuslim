package com.cafarovceyxun.anamuslim.compose.screens.salah

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.reference.ReferencePeek
import com.cafarovceyxun.anamuslim.compose.components.reference.ReferencePeekItem
import com.cafarovceyxun.anamuslim.compose.screens.dua.DuaActions
import com.cafarovceyxun.anamuslim.compose.screens.dua.HajjEvidenceEditDialog
import com.cafarovceyxun.anamuslim.compose.screens.dua.LocalDuaActions
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.screens.hajj.EvidenceActions
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuideCard
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuidePage
import com.cafarovceyxun.anamuslim.compose.screens.hajj.GuideSectionTitle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.HajjEvidenceSection
import com.cafarovceyxun.anamuslim.compose.screens.hajj.SegmentedTabs
import com.cafarovceyxun.anamuslim.compose.screens.hajj.arabicStyle
import com.cafarovceyxun.anamuslim.compose.screens.hajj.contentStyle
import com.cafarovceyxun.anamuslim.compose.screens.salah.art.ArtImage
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_hands
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_mouth
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_nose
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_face
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_arm_right
import com.cafarovceyxun.anamuslim.resources.dr_salah_ghusl_body
import com.cafarovceyxun.anamuslim.resources.dr_salah_ghusl_hair
import com.cafarovceyxun.anamuslim.resources.dr_salah_ghusl_hands
import com.cafarovceyxun.anamuslim.resources.dr_salah_ghusl_sides
import com.cafarovceyxun.anamuslim.resources.dr_salah_ghusl_three
import com.cafarovceyxun.anamuslim.resources.dr_salah_ghusl_wudu
import com.cafarovceyxun.anamuslim.resources.dr_salah_tayammum_blow
import com.cafarovceyxun.anamuslim.resources.dr_salah_tayammum_face
import com.cafarovceyxun.anamuslim.resources.dr_salah_tayammum_hands
import com.cafarovceyxun.anamuslim.resources.dr_salah_tayammum_strike
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_arm_left
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_head
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_head_back
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_foot_right
import com.cafarovceyxun.anamuslim.resources.dr_salah_wudu_foot_left
import com.cafarovceyxun.anamuslim.resources.duaDeleteConfirmTitle
import com.cafarovceyxun.anamuslim.resources.duaDeleteEvidenceConfirm
import com.cafarovceyxun.anamuslim.resources.salahComingSoon
import com.cafarovceyxun.anamuslim.resources.salahDraftBadge
import com.cafarovceyxun.anamuslim.resources.salahFinish
import com.cafarovceyxun.anamuslim.resources.salahNext
import com.cafarovceyxun.anamuslim.resources.salahNoCount
import com.cafarovceyxun.anamuslim.resources.salahPrevious
import com.cafarovceyxun.anamuslim.resources.salahSectionTitle
import com.cafarovceyxun.anamuslim.resources.salahStartSteps
import com.cafarovceyxun.anamuslim.resources.salahTimesUnit
import com.cafarovceyxun.anamuslim.resources.salahWomenNote
import com.cafarovceyxun.anamuslim.resources.salahWuduFull
import com.cafarovceyxun.anamuslim.resources.salahWuduOnce
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.salah.NajasaRow
import com.cafarovceyxun.anamuslim.utils.salah.RuleRow
import com.cafarovceyxun.anamuslim.utils.salah.SalahGuideContent
import com.cafarovceyxun.anamuslim.utils.salah.SalahTopic
import com.cafarovceyxun.anamuslim.utils.salah.TaharahPicture
import com.cafarovceyxun.anamuslim.utils.salah.TaharahStep
import com.cafarovceyxun.anamuslim.utils.salah.TaharahDrawing
import com.cafarovceyxun.anamuslim.utils.salah.WuduForm
import com.cafarovceyxun.anamuslim.utils.supabase.GuideEvidence
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.SalahViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal enum class SalahPage {
    ROOT, TAHARAH, WUDU, WUDU_STEP, WUDU_KNOW, GHUSL, TAYAMMUM, KHUFF, TOILET, NAJASA, HAYD,
    // Mərhələ 2 — `SalahPrayerScreens.kt`
    PRAYER, PRAYER_HOW, PRAYER_STEP, TIMES, QIBLA, ADHAN, AFTER, KNOW,
    // Mərhələ 3 — `SalahGroupScreens.kt`
    GROUP, JAMAAH, ROWS, JUMUAH, SAFAR, EID, ISTISQA, KUSUF, KHAWF,
    // Mərhələ 4 — `SalahNaflScreens.kt`
    NAFL, SUNNAH, DUHA, NIGHT, SAHW, TILAWAH, JANAZAH,
}

/**
 * Bələdçinin vəziyyəti — **ekrandan kənarda**, Həcc bələdçisi ilə eyni səbəbdən: mənbəni oxucuda
 * açanda bələdçi (tam ekran `Dialog`) bağlanır, qayıdan istifadəçi yerini itirməməlidir.
 */
@Stable
class SalahGuideState {
    internal var stack by mutableStateOf(listOf(SalahPage.ROOT))
    internal var wuduForm by mutableStateOf(WuduForm.ONCE)
    internal var wuduStep by mutableIntStateOf(0)
    internal var prayerStep by mutableIntStateOf(0)

    /** Rükət xəritəsində seçilmiş namaz — `SalahPrayerContent.prayers` indeksi (defolt Zöhr). */
    internal var prayerIndex by mutableIntStateOf(1)

    /** Səflər sxemində seçilmiş hal — `SalahGroupContent.seats` indeksi. */
    internal var seatIndex by mutableIntStateOf(0)

    /** İstisqa: musallədə (`true`) və ya Cümə xütbəsində. */
    internal var istisqaMusalla by mutableStateOf(true)

    /** Qorxu namazının seçilmiş forması — `SalahGroupContent.khawfForms` indeksi. */
    internal var khawfIndex by mutableIntStateOf(0)

    /** Gecə namazının seçilmiş forması — `SalahNaflContent.nightForms` indeksi. */
    internal var nightIndex by mutableIntStateOf(0)

    /** Səhv səcdəsində seçilmiş hal — `SalahNaflContent.sahwCases` indeksi. */
    internal var sahwIndex by mutableIntStateOf(0)

    /** Namazdan sonrakı təsbih sayğacı — yalnız bu açılışda yaşayır, yadda saxlanmır. */
    internal var counts by mutableStateOf(listOf(0, 0, 0))

    internal val page: SalahPage get() = stack.last()

    internal fun push(page: SalahPage) {
        stack = stack + page
    }

    internal fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    internal fun openWuduStep(index: Int) {
        wuduStep = index
        push(SalahPage.WUDU_STEP)
    }

    internal fun openPrayerStep(index: Int) {
        prayerStep = index
        push(SalahPage.PRAYER_STEP)
    }
}

@Composable
fun rememberSalahGuideState(): SalahGuideState = remember { SalahGuideState() }

/**
 * Namaz bələdçisi — ana ekrandakı kartdan tam ekran səthdə açılır (Həcc bələdçisi ilə eyni qurğu, yeni
 * `AppDestination` yoxdur). Dörd mərhələnin hamısı açıqdır: **Təharət**, **Əzan və namaz**, **Camaat və xüsusi namazlar**,
 * **Nafilə və cənazə**.
 *
 * Addımlar [SalahGuideContent]-dən, hədis çıxarışları `salah_evidence`-dən ([SalahViewModel]) gəlir.
 * Giriş etmiş admin çıxarışı burada redaktə edir və silir; yenisini hədis oxucusundan əlavə edir.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SalahGuideScreen(state: SalahGuideState, onBack: () -> Unit) {
    // ⚠️ `viewModel { … }` — factory-siz `viewModel<T>()` iOS-da çökür (CLAUDE.md).
    val salahViewModel = viewModel { SalahViewModel() }
    val authViewModel = viewModel { AuthViewModel() }

    val evidence by salahViewModel.evidence.collectAsStateWithLifecycle()
    val isSaving by salahViewModel.isLoading.collectAsStateWithLifecycle()
    val revision by salahViewModel.revision.collectAsStateWithLifecycle()
    val session by authViewModel.session.collectAsStateWithLifecycle()

    LaunchedEffect(revision) { if (revision > 0) salahViewModel.refresh() }

    val byTopic = remember(evidence) { evidence.groupBy { it.topic } }

    var peekItems by remember { mutableStateOf<List<ReferencePeekItem>>(emptyList()) }
    var peekIndex by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<GuideEvidence?>(null) }
    var deleting by remember { mutableStateOf<GuideEvidence?>(null) }

    val actions = remember(session != null) {
        EvidenceActions(
            isAuthorized = session != null,
            onOpenSource = { item, siblings ->
                peekItems = siblings.map { ReferencePeekItem.Source(it, isEvidence = !it.isDhikr) }
                peekIndex = siblings.indexOf(item).takeIf { it >= 0 }
            },
            onEdit = { editing = it },
            onDelete = { deleting = it },
        )
    }

    // Mənbəni oxucuda açmaq bələdçini bağlayır — bax `HajjGuideScreen`.
    val outer = LocalDuaActions.current
    val guideActions = remember(outer, onBack) {
        DuaActions(
            onOpenHadith = outer.onOpenHadith?.let { open -> { id: Long -> onBack(); open(id) } },
            onOpenVerse = outer.onOpenVerse?.let { open -> { chapter: Int, verse: Int -> onBack(); open(chapter, verse) } },
        )
    }

    BackHandler(enabled = state.stack.size > 1) { state.pop() }
    val back: () -> Unit = { if (state.stack.size > 1) state.pop() else onBack() }

    CompositionLocalProvider(LocalDuaActions provides guideActions) {
        when (state.page) {
            SalahPage.ROOT -> RootPage(state, back)
            SalahPage.TAHARAH -> TaharahPage(state, back)
            SalahPage.WUDU -> WuduPage(state, byTopic, actions, back)
            SalahPage.WUDU_STEP -> WuduStepPager(state, byTopic, actions, back)
            SalahPage.WUDU_KNOW -> WuduKnowPage(byTopic, actions, back)
            SalahPage.GHUSL -> GhuslPage(state, byTopic, actions, back)
            SalahPage.TAYAMMUM -> TayammumPage(byTopic, actions, back)
            SalahPage.KHUFF -> KhuffPage(byTopic, actions, back)
            SalahPage.TOILET -> ToiletPage(byTopic, actions, back)
            SalahPage.NAJASA -> NajasaPage(byTopic, actions, back)
            SalahPage.HAYD -> HaydPage(byTopic, actions, back)
            SalahPage.PRAYER -> PrayerHubPage(state, back)
            SalahPage.PRAYER_HOW -> PrayerHowPage(state, byTopic, actions, back)
            SalahPage.PRAYER_STEP -> PrayerStepPager(state, byTopic, actions, back)
            SalahPage.TIMES -> TimesPage(byTopic, actions, back, onCloseGuide = onBack)
            SalahPage.QIBLA -> QiblaPage(byTopic, actions, back, onCloseGuide = onBack)
            SalahPage.ADHAN -> AdhanPage(byTopic, actions, back)
            SalahPage.AFTER -> AfterPage(state, byTopic, actions, back)
            SalahPage.KNOW -> KnowPage(byTopic, actions, back)
            SalahPage.GROUP -> GroupHubPage(state, back)
            SalahPage.JAMAAH -> JamaahPage(byTopic, actions, back)
            SalahPage.ROWS -> RowsPage(state, byTopic, actions, back)
            SalahPage.JUMUAH -> JumuahPage(byTopic, actions, back)
            SalahPage.SAFAR -> SafarPage(byTopic, actions, back)
            SalahPage.EID -> EidPage(byTopic, actions, back)
            SalahPage.ISTISQA -> IstisqaPage(state, byTopic, actions, back)
            SalahPage.KUSUF -> KusufPage(byTopic, actions, back)
            SalahPage.KHAWF -> KhawfPage(state, byTopic, actions, back)
            SalahPage.NAFL -> NaflHubPage(state, back)
            SalahPage.SUNNAH -> SunnahPage(byTopic, actions, back)
            SalahPage.DUHA -> DuhaPage(byTopic, actions, back)
            SalahPage.NIGHT -> NightPage(state, byTopic, actions, back)
            SalahPage.SAHW -> SahwPage(state, byTopic, actions, back)
            SalahPage.TILAWAH -> TilawahPage(byTopic, actions, back)
            SalahPage.JANAZAH -> JanazahPage(byTopic, actions, back)
        }

        ReferencePeek(
            items = peekItems,
            index = peekIndex,
            onIndexChange = { peekIndex = it },
            hasMore = false,
            onOpenVerse = { chapterNo, range ->
                peekIndex = null
                ReaderUiHooks.openVerseRange?.invoke(chapterNo, range.first, range.last)
            },
            onOpenHadith = null,
            onClose = { peekIndex = null },
        )
    }

    editing?.let { item ->
        HajjEvidenceEditDialog(
            evidence = item,
            isSaving = isSaving,
            onSave = { updated -> salahViewModel.updateEvidence(updated) { editing = null } },
            onDismiss = { editing = null },
        )
    }

    val pendingDelete = deleting
    AlertDialog(
        isOpen = pendingDelete != null,
        onClose = { deleting = null },
        title = stringResource(Res.string.duaDeleteConfirmTitle),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = { pendingDelete?.id?.let { id -> salahViewModel.deleteEvidence(id) } },
            ),
        ),
    ) {
        Text(
            text = stringResource(Res.string.duaDeleteEvidenceConfirm),
            style = typography.bodyMedium.withScriptDirection(arabic = false),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Kök: dörd mərhələ
// ---------------------------------------------------------------------------------------------

private class Phase(val title: String, val items: String, val open: SalahPage?)

private val phases = listOf(
    Phase("Təharət", "Dəstəmaz, qüsl, təyəmmüm, xuff, ayaqyolu ədəbi, nəcasət", SalahPage.TAHARAH),
    Phase("Əzan və namaz", "Vaxtlar, qiblə, əzan və iqamə, namazın qılınışı, namazdan sonra", SalahPage.PRAYER),
    Phase("Camaat və xüsusi namazlar", "Səflər, Cümə, səfər, bayram, istisqa, küsuf, qorxu namazı", SalahPage.GROUP),
    Phase("Nafilə və cənazə", "Sünnət, Duha, gecə namazı və vitr, səhv və tilavət səcdəsi, cənazə", SalahPage.NAFL),
)

@Composable
private fun RootPage(state: SalahGuideState, onBack: () -> Unit) {
    GuidePage(title = stringResource(Res.string.salahSectionTitle), onBack = onBack) {
        phases.forEachIndexed { index, phase ->
            val enabled = phase.open != null
            Surface(
                onClick = { phase.open?.let(state::push) },
                enabled = enabled,
                shape = RoundedCornerShape(18.dp),
                color = if (enabled) colorScheme.primaryContainer.alpha(0.3f) else colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(if (enabled) colorScheme.primary else colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (enabled) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(phase.title, style = contentStyle(typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)),
                            color = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant)
                        Text(phase.items, style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
                    }
                    if (enabled) {
                        Icon(painterResource(Res.drawable.dr_icon_chevron_right), null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            stringResource(Res.string.salahComingSoon),
                            style = typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
        DraftNote()
    }
}

@Composable
internal fun DraftNote() {
    Text(
        text = stringResource(Res.string.salahDraftBadge),
        style = typography.labelSmall,
        color = colorScheme.tertiary,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colorScheme.tertiary.alpha(0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// ---------------------------------------------------------------------------------------------
// Təharət bölməsi
// ---------------------------------------------------------------------------------------------

/**
 * Təharət bölməsinin kartı. Şəkil bölmənin içindəki kadrdandır ([drawing]); qiblə sxemi çertyoj olaraq
 * qalır ([art]). Xuff və nəcasətin şəkli yoxdur (istifadəçi, 2026-10-04) — ona görə ikisi bir sətirdədir,
 * şəkilli kartın yanında boş qalmasınlar.
 */
private class Tile(
    val page: SalahPage,
    val title: String,
    val sub: String,
    val drawing: TaharahDrawing? = null,
    val art: TaharahPicture? = null,
)

private val tiles = listOf(
    Tile(SalahPage.WUDU, "Dəstəmaz", "iki forma · № 109–125", drawing = TaharahDrawing.WUDU_FACE),
    Tile(SalahPage.GHUSL, "Qüsl", "7 addım · № 134–158", drawing = TaharahDrawing.GHUSL_THREE),
    Tile(SalahPage.TAYAMMUM, "Təyəmmüm", "4 hərəkət · № 177–182", drawing = TaharahDrawing.TAYAMMUM_STRIKE),
    Tile(SalahPage.TOILET, "Ayaqyolu ədəbi", "dua və qaydalar · № 91–99", art = TaharahPicture.QIBLA),
    Tile(SalahPage.KHUFF, "Xufflara məsh", "1 və 3 gün · № 130–133"),
    Tile(SalahPage.NAJASA, "Nəcasət", "5 hal · № 100–108"),
)

@Composable
private fun TaharahPage(state: SalahGuideState, onBack: () -> Unit) {
    GuidePage(title = "Təharət", onBack = onBack) {
        GuideCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("الطَّهَارَة", style = arabicStyle(34), color = colorScheme.primary)
                Text("Muheymin 1-ci cild · Təharət kitabı", style = contentStyle(typography.titleSmall), color = colorScheme.onSurface)
                Text("13 bab · 93 hədis · № 91–182", style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
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
                            when {
                                tile.drawing != null -> TaharahDrawingImage(
                                    tile.drawing, contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(240f / 170f).padding(top = 12.dp, start = 10.dp, end = 10.dp),
                                )
                                tile.art != null -> ArtImage(tile.art.art, contentDescription = null, corner = 0)
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
        val women = womenColor()
        Surface(
            onClick = { state.push(SalahPage.HAYD) },
            shape = RoundedCornerShape(16.dp),
            color = women.alpha(0.1f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.salahWomenNote), style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = women)
                    Text("Heyz, istihazə, qüsldə hörüklər · № 145, 159–176", style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant)
                }
                Icon(painterResource(Res.drawable.dr_icon_chevron_right), null, tint = women, modifier = Modifier.size(18.dp))
            }
        }
        DraftNote()
    }
}

// ---------------------------------------------------------------------------------------------
// Dəstəmaz
// ---------------------------------------------------------------------------------------------

private val TaharahDrawing.drawable: DrawableResource
    get() = when (this) {
        TaharahDrawing.WUDU_HANDS -> Res.drawable.dr_salah_wudu_hands
        TaharahDrawing.WUDU_MOUTH -> Res.drawable.dr_salah_wudu_mouth
        TaharahDrawing.WUDU_NOSE -> Res.drawable.dr_salah_wudu_nose
        TaharahDrawing.WUDU_FACE -> Res.drawable.dr_salah_wudu_face
        TaharahDrawing.WUDU_ARM_RIGHT -> Res.drawable.dr_salah_wudu_arm_right
        TaharahDrawing.WUDU_ARM_LEFT -> Res.drawable.dr_salah_wudu_arm_left
        TaharahDrawing.WUDU_HEAD -> Res.drawable.dr_salah_wudu_head
        TaharahDrawing.WUDU_HEAD_BACK -> Res.drawable.dr_salah_wudu_head_back
        TaharahDrawing.WUDU_FOOT_RIGHT -> Res.drawable.dr_salah_wudu_foot_right
        TaharahDrawing.WUDU_FOOT_LEFT -> Res.drawable.dr_salah_wudu_foot_left
        TaharahDrawing.TAYAMMUM_STRIKE -> Res.drawable.dr_salah_tayammum_strike
        TaharahDrawing.TAYAMMUM_BLOW -> Res.drawable.dr_salah_tayammum_blow
        TaharahDrawing.TAYAMMUM_FACE -> Res.drawable.dr_salah_tayammum_face
        TaharahDrawing.TAYAMMUM_HANDS -> Res.drawable.dr_salah_tayammum_hands
        TaharahDrawing.GHUSL_HANDS -> Res.drawable.dr_salah_ghusl_hands
        TaharahDrawing.GHUSL_WUDU -> Res.drawable.dr_salah_ghusl_wudu
        TaharahDrawing.GHUSL_HAIR -> Res.drawable.dr_salah_ghusl_hair
        TaharahDrawing.GHUSL_THREE -> Res.drawable.dr_salah_ghusl_three
        TaharahDrawing.GHUSL_SIDES -> Res.drawable.dr_salah_ghusl_sides
        TaharahDrawing.GHUSL_BODY -> Res.drawable.dr_salah_ghusl_body
    }

/** İstifadəçinin kadrı (dəstəmaz, təyəmmüm, qüsl) — vektor, xəttin rəngi mövzudan gəlir (tünd rejimdə açıq olur). */
@Composable
private fun TaharahDrawingImage(drawing: TaharahDrawing, contentDescription: String?, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(drawing.drawable),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(colorScheme.onSurface),
        modifier = modifier,
    )
}

private fun WuduForm.intro(): String = when (this) {
    WuduForm.ONCE -> "İbn Abbas dəstəmazı üzlə başladı, hər üzvü bir ovuc su ilə bir dəfə yudu və dedi: «Rəsulullahı belə dəstəmaz alarkən gördüm»."
    WuduForm.FULL -> "Abdullah ibn Zeyd Nəbinin dəstəmazını göstərmək üçün su gətirtdi: əllər iki, ağız və burun üç, üz üç, qollar iki dəfə, başa məsh, sonra ayaqlar."
}

@Composable
private fun WuduPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    val steps = SalahGuideContent.wudu(state.wuduForm)
    GuidePage(title = "Dəstəmaz", onBack = onBack) {
        SegmentedTabs(
            labels = listOf(stringResource(Res.string.salahWuduOnce) + " · № 114", stringResource(Res.string.salahWuduFull) + " · № 112"),
            selectedIndex = state.wuduForm.ordinal,
            onSelect = { state.wuduForm = WuduForm.entries[it] },
        )
        GuideCard {
            Text(state.wuduForm.intro(), style = contentStyle(typography.bodyMedium), color = colorScheme.onSurface, modifier = Modifier.padding(14.dp))
        }
        GuideCard {
            Column {
                steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { state.openWuduStep(index) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("${index + 1}", style = typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary,
                            modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
                        val thumb = step.drawings.firstOrNull()
                        if (thumb != null) {
                            Box(
                                Modifier.size(width = 72.dp, height = 60.dp).clip(RoundedCornerShape(10.dp))
                                    .background(colorScheme.surfaceVariant.alpha(0.5f)).padding(4.dp),
                            ) { TaharahDrawingImage(thumb, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                        } else {
                            step.picture?.let { Box(Modifier.width(72.dp)) { ArtImage(it.art, contentDescription = null, corner = 10) } }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(step.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                            step.side?.let { Text("${it.label} tərəf", style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant) }
                        }
                        TimesPill(step.times)
                    }
                }
            }
        }
        Button(onClick = { state.openWuduStep(0) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(stringResource(Res.string.salahStartSteps), style = typography.labelLarge)
        }
        TopicQuotes(byTopic.of(SalahTopic.WUDU_FORMS), actions)
        Surface(
            onClick = { state.push(SalahPage.WUDU_KNOW) },
            shape = RoundedCornerShape(16.dp),
            color = colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bilmək lazımdır", style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                    Text("Nə pozur, nə pozmur, şübhə qaydası, sünnətlər", style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
                }
                Icon(painterResource(Res.drawable.dr_icon_chevron_right), null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TimesPill(times: Int?) {
    Text(
        text = times?.let { "$it×" } ?: "—",
        style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(colorScheme.primary.alpha(0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Dəstəmaz addımları — sürüşdürülən səhifələr, aşağıda «əvvəlki / növbəti». */
@Composable
private fun WuduStepPager(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    val steps = SalahGuideContent.wudu(state.wuduForm)
    val pager = rememberPagerState(initialPage = state.wuduStep.coerceIn(0, steps.lastIndex)) { steps.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { state.wuduStep = it } }
    val formLabel = stringResource(if (state.wuduForm == WuduForm.ONCE) Res.string.salahWuduOnce else Res.string.salahWuduFull)

    Scaffold(
        topBar = {
            AppBar(title = "Dəstəmaz · $formLabel", onBack = onBack) {
                Text(
                    "${pager.currentPage + 1} / ${steps.size}",
                    style = typography.labelLarge,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 14.dp),
                )
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                steps.indices.forEach { i ->
                    Box(
                        Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (i <= pager.currentPage) colorScheme.primary else colorScheme.surfaceVariant),
                    )
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                StepContent(steps[page], byTopic.of(steps[page].topic), actions)
            }
        }
    }
}

@Composable
private fun StepContent(step: TaharahStep, items: List<GuideEvidence>, actions: EvidenceActions) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ReadableWidthColumn {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (step.drawings.isNotEmpty() || step.picture != null) {
                    Box {
                        if (step.drawings.isNotEmpty()) {
                            // Üstdəki boşluq sağ/sol və «neçə dəfə» nişanları üçündür — kadrın üstünə düşməsin.
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colorScheme.surface)
                                    .padding(start = 14.dp, end = 14.dp, top = 64.dp, bottom = 16.dp).height(200.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                step.drawings.forEach { TaharahDrawingImage(it, step.title, Modifier.weight(1f).fillMaxHeight()) }
                            }
                        } else {
                            step.picture?.let { ArtImage(it.art, contentDescription = step.title, corner = 20) }
                        }
                        step.side?.let {
                            Text(
                                it.label,
                                style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(999.dp))
                                    .background(colorScheme.primary).padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                        Column(
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surface).padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(step.times?.let { "$it×" } ?: "—", style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                            Text(
                                stringResource(if (step.times != null) Res.string.salahTimesUnit else Res.string.salahNoCount),
                                style = typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(step.title, style = contentStyle(typography.headlineSmall.copy(fontWeight = FontWeight.Bold)), color = colorScheme.onSurface)
                Text(step.text, style = contentStyle(typography.bodyLarge), color = colorScheme.onSurface.alpha(0.9f))
                TopicQuotes(items, actions)
            }
        }
    }
}

@Composable
private fun WuduKnowPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Dəstəmaz: bilmək lazımdır", onBack = onBack) {
        val doubt = byTopic.of(SalahTopic.WUDU_DOUBT)
        Callout("Şübhə olanda", doubt.firstOrNull(), doubt, actions)
        GuideSectionTitle("Dəstəmazı pozur")
        Rules(SalahGuideContent.wuduBreaks, byTopic, actions)
        GuideSectionTitle("Pozmur")
        Rules(SalahGuideContent.wuduKeeps, byTopic, actions)
        GuideSectionTitle("Sünnətlər")
        Rules(SalahGuideContent.wuduSunnah, byTopic, actions)
    }
}

@Composable
private fun Rules(rows: List<RuleRow>, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions) {
    rows.forEach { RuleRowView(it, byTopic.of(it.topic), actions) }
}

// ---------------------------------------------------------------------------------------------
// Qüsl
// ---------------------------------------------------------------------------------------------

@Composable
private fun GhuslPage(state: SalahGuideState, byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Qüsl", onBack = onBack) {
        GuideSectionTitle("Nə vaxt")
        Rules(SalahGuideContent.ghuslWhen, byTopic, actions)
        GuideSectionTitle("Necə · № 140, 141, 145")
        SalahGuideContent.ghuslSteps.forEachIndexed { index, step ->
            TimelineStep(index + 1, isLast = index == SalahGuideContent.ghuslSteps.lastIndex) {
                Text(step.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                val drawing = step.drawings.firstOrNull()
                if (drawing != null) {
                    TaharahDrawingImage(
                        drawing, step.title,
                        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)).background(colorScheme.surface).padding(12.dp),
                    )
                } else {
                    step.picture?.let { ArtImage(it.art, contentDescription = step.title, corner = 14) }
                }
                TopicQuotes(byTopic.of(step.topic), actions, compact = true)
            }
        }
        Surface(onClick = { state.push(SalahPage.WUDU) }, shape = RoundedCornerShape(14.dp), color = colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Text("Dəstəmaz: iki forma →", style = contentStyle(typography.labelLarge), color = colorScheme.primary, modifier = Modifier.padding(12.dp))
        }
        GuideSectionTitle("Bilmək lazımdır")
        Rules(SalahGuideContent.ghuslExtra, byTopic, actions)
    }
}

/** Nömrəli zaman xətti addımı — sol tərəfdə dairə və xətt. */
@Composable
internal fun TimelineStep(n: Int, isLast: Boolean, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(modifier = Modifier.width(32.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(colorScheme.primary), contentAlignment = Alignment.Center) {
                Text("$n", style = typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colorScheme.onPrimary)
            }
            if (!isLast) Box(Modifier.padding(top = 4.dp).width(2.dp).weight(1f).background(colorScheme.outlineVariant.alpha(0.6f)))
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = 10.dp, top = 4.dp, bottom = if (isLast) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

// ---------------------------------------------------------------------------------------------
// Təyəmmüm
// ---------------------------------------------------------------------------------------------

@Composable
private fun TayammumPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Təyəmmüm", onBack = onBack) {
        val enough = byTopic.of(SalahTopic.TAYAMMUM_ENOUGH)
        Callout("Ammar ibn Yasir danışır", enough.firstOrNull(), enough, actions)
        GuideSectionTitle("Dörd hərəkət · № 180")
        SalahGuideContent.tayammumSteps.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                pair.forEachIndexed { col, step ->
                    val items = byTopic.of(step.topic)
                    Surface(
                        onClick = { items.firstOrNull()?.let { actions.onOpenSource(it, items) } },
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.surface,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        Column {
                            Box {
                                val drawing = step.drawings.firstOrNull()
                                if (drawing != null) {
                                    TaharahDrawingImage(drawing, step.title, Modifier.fillMaxWidth().aspectRatio(1f).padding(start = 14.dp, end = 14.dp, top = 14.dp))
                                } else {
                                    step.picture?.let { ArtImage(it.art, contentDescription = step.title, corner = 0) }
                                }
                                Box(
                                    Modifier.padding(8.dp).size(24.dp).clip(CircleShape).background(colorScheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("${row * 2 + col + 1}", style = typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.onPrimary)
                                }
                            }
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(step.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
                                items.firstOrNull()?.let {
                                    Text("«${it.text_az.displayText()}»", style = contentStyle(typography.bodySmall), color = colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        GuideSectionTitle("Nə vaxt")
        Rules(SalahGuideContent.tayammumWhen, byTopic, actions)
    }
}

// ---------------------------------------------------------------------------------------------
// Xuff, ayaqyolu, nəcasət, heyz
// ---------------------------------------------------------------------------------------------

@Composable
private fun KhuffPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Xufflara məsh", onBack = onBack) {
        ArtImage(TaharahPicture.KHUFF.art, contentDescription = "Xufflara məsh", corner = 20)
        TopicQuotes(byTopic.of(SalahTopic.KHUFF_WIPE), actions)
        GuideSectionTitle("Müddət")
        GuideCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DurationBar("Muqim", days = 1)
                DurationBar("Müsafir", days = 3)
            }
        }
        TopicQuotes(byTopic.of(SalahTopic.KHUFF_TIME), actions, compact = true)
    }
}

@Composable
private fun DurationBar(label: String, days: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurface, modifier = Modifier.width(70.dp))
        Row(modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(colorScheme.surfaceVariant)) {
            repeat(days) {
                Box(Modifier.weight(1f).fillMaxHeight().padding(end = 2.dp).background(colorScheme.primary))
            }
            if (days < 3) Spacer(Modifier.weight((3 - days).toFloat()))
        }
        Text("${days * 24} saat", style = typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface,
            modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun ToiletPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Ayaqyolu ədəbi", onBack = onBack) {
        GuideSectionTitle("Ayaqyoluna girəndə")
        HajjEvidenceSection(byTopic.of(SalahTopic.TOILET_DUA), actions, showTitles = false)
        ArtImage(TaharahPicture.QIBLA.art, contentDescription = "Qiblə sxemi", corner = 20)
        Rules(SalahGuideContent.toiletRules, byTopic, actions)
    }
}

@Composable
private fun NajasaPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = "Nəcasət", onBack = onBack) {
        GuideSectionTitle("Nə necə təmizlənir")
        SalahGuideContent.najasa.forEach { NajasaCard(it, byTopic.of(it.topic), actions) }
    }
}

@Composable
private fun NajasaCard(row: NajasaRow, items: List<GuideEvidence>, actions: EvidenceActions) {
    GuideCard {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.title, style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)), color = colorScheme.onSurface)
            Text(row.how, style = contentStyle(typography.labelLarge.copy(fontWeight = FontWeight.Bold)), color = colorScheme.secondary)
            if (row.sevenWashes) SevenWashes()
            TopicQuotes(items, actions, compact = true)
        }
    }
}

/** Yeddi yuma, birincisi torpaqla (№ 107) — sayı göz önündə tutmaq üçün. */
@Composable
private fun SevenWashes() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..7).forEach { i ->
            val earth = i == 1
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (earth) colorScheme.tertiary.alpha(0.75f) else colorScheme.secondary.alpha(0.18f))
                    .border(1.5.dp, if (earth) colorScheme.tertiary else colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$i", style = typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (earth) colorScheme.onTertiary else colorScheme.secondary)
            }
        }
        Text("1-ci torpaqla", style = contentStyle(typography.labelSmall), color = colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HaydPage(byTopic: Map<String, List<GuideEvidence>>, actions: EvidenceActions, onBack: () -> Unit) {
    GuidePage(title = stringResource(Res.string.salahWomenNote), onBack = onBack) {
        Rules(SalahGuideContent.hayd, byTopic, actions)
        GuideSectionTitle("Qüsldə")
        // № 145-də qadın soruşur («hörükləri açımmı?») — sual cavabsız qalmasın, ardınca Rəsulullahın
        // cavabı (eyni mövzunun dəlil sətri) gəlir.
        val ghuslBody = byTopic.of(SalahTopic.GHUSL_BODY)
        TopicQuotes(
            ghuslBody.filter { it.isWomenNote } + ghuslBody.filterNot { it.isWomenNote } + byTopic.of(SalahTopic.GHUSL_HAYD),
            actions,
            compact = true,
        )
    }
}
