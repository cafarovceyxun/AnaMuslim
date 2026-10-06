package com.cafarovceyxun.anamuslim.compose.screens.hajj

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ReadableWidthColumn
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.screens.dua.DuaActions
import com.cafarovceyxun.anamuslim.compose.components.reference.ReferencePeek
import com.cafarovceyxun.anamuslim.compose.components.reference.ReferencePeekItem
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.compose.screens.dua.HajjEvidenceEditDialog
import com.cafarovceyxun.anamuslim.compose.screens.dua.LocalDuaActions
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_qibla
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.duaDeleteConfirmTitle
import com.cafarovceyxun.anamuslim.resources.duaDeleteEvidenceConfirm
import com.cafarovceyxun.anamuslim.resources.hajjChooseType
import com.cafarovceyxun.anamuslim.resources.hajjCounterTitle
import com.cafarovceyxun.anamuslim.resources.hajjDaysTitle
import com.cafarovceyxun.anamuslim.resources.hajjDhikrCount
import com.cafarovceyxun.anamuslim.resources.hajjEvidenceCount
import com.cafarovceyxun.anamuslim.resources.hajjFactIhram
import com.cafarovceyxun.anamuslim.resources.hajjFactSacrifice
import com.cafarovceyxun.anamuslim.resources.hajjMiqatTitle
import com.cafarovceyxun.anamuslim.resources.hajjModeHajj
import com.cafarovceyxun.anamuslim.resources.hajjModeUmrah
import com.cafarovceyxun.anamuslim.resources.hajjSectionTitle
import com.cafarovceyxun.anamuslim.resources.hajjStartWith
import com.cafarovceyxun.anamuslim.resources.hajjStepsDone
import com.cafarovceyxun.anamuslim.resources.hajjTools
import com.cafarovceyxun.anamuslim.resources.hajjTypeLabel
import com.cafarovceyxun.anamuslim.resources.hajjUmrahSteps
import com.cafarovceyxun.anamuslim.resources.hajjVirtue
import com.cafarovceyxun.anamuslim.resources.hajjWho
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.utils.hajj.HajjDay
import com.cafarovceyxun.anamuslim.utils.hajj.HajjGuideContent
import com.cafarovceyxun.anamuslim.utils.hajj.HajjStep
import com.cafarovceyxun.anamuslim.utils.hajj.HajjTool
import com.cafarovceyxun.anamuslim.utils.hajj.HajjTopic
import com.cafarovceyxun.anamuslim.utils.hajj.HajjType
import com.cafarovceyxun.anamuslim.utils.supabase.HajjEvidence
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HajjViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal enum class HajjPage { START, DAYS, STEP, IHRAM, MIQAT, COUNTER }

/**
 * Bələdçinin vəziyyəti — **ekrandan kənarda** saxlanır.
 *
 * Bələdçi `FullScreenSurface` (Dialog) içində açılır və mənbəni oxucuda açanda bağlanır (bax
 * [HajjGuideScreen]). Vəziyyət dialoqun içində olsaydı, hədisə baxıb qayıdan hacı seçdiyi növü və
 * işarələdiyi addımları itirərdi — ona görə onu açan yer ([rememberHajjGuideState]) saxlayır.
 *
 * Prototipdə yaddaşdadır (proses ölənə qədər); real istifadə üçün DataStore-a köçməlidir — onda
 * açarlar ehtiyat nüsxəyə öz-özünə düşür (CLAUDE.md → «Ehtiyat nüsxə ayarları əl ilə sadalanmır»).
 */
@Stable
class HajjGuideState {
    internal var stack by mutableStateOf(listOf(HajjPage.START))
    internal var isHajj by mutableStateOf(true)
    internal var type by mutableStateOf(HajjType.TAMATTU)
    internal var done by mutableStateOf(emptySet<String>())
    internal var counterSay by mutableStateOf(false)
    internal var laps by mutableIntStateOf(0)

    /** Səhifələyicinin siyahısı: `true` → ümrənin dörd addımı, `false` → seçilmiş növün Həcc addımları. */
    internal var pagerUmrah by mutableStateOf(false)
    internal var stepIndex by mutableIntStateOf(0)

    internal val page: HajjPage get() = stack.last()

    internal val pagerSteps: List<HajjStep>
        get() = if (pagerUmrah) HajjGuideContent.umrahSteps else HajjGuideContent.stepsFor(type)

    internal fun push(page: HajjPage) {
        stack = stack + page
    }

    internal fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    internal fun openStep(umrah: Boolean, index: Int) {
        pagerUmrah = umrah
        stepIndex = index
        push(HajjPage.STEP)
    }

    internal fun openTool(tool: HajjTool) {
        when (tool) {
            HajjTool.IHRAM -> push(HajjPage.IHRAM)
            HajjTool.MIQAT -> push(HajjPage.MIQAT)
            HajjTool.COUNTER_TAWAF, HajjTool.COUNTER_SAY -> {
                val say = tool == HajjTool.COUNTER_SAY
                if (say != counterSay) laps = 0
                counterSay = say
                push(HajjPage.COUNTER)
            }
        }
    }

    internal fun toggleDone(id: String) {
        done = if (id in done) done - id else done + id
    }
}

@Composable
fun rememberHajjGuideState(): HajjGuideState = remember { HajjGuideState() }

/**
 * Həcc və Ümrə bələdçisi — ana ekrandakı kartdan tam ekran səthdə açılır (Dua/Əsma ilə eyni qurğu,
 * yeni `AppDestination` yoxdur, ona görə tab yığını tələsinə düşmür).
 *
 * Daxili naviqasiya öz yığınıdır ([HajjGuideState.stack]); geri jesti əvvəlcə onu boşaldır, sonra
 * bələdçini bağlayır.
 *
 * **Mənbəni oxucuda açmaq bələdçini bağlayır.** Bələdçi `Dialog`-dur: oxucu onun **altında** açılır və
 * iOS-da route push olunsa da dialoq üstdə qalır — «heç nə olmadı» kimi görünür. Ona görə
 * qaynaq vərəqinə (`DuaSourcePeekContent`) verilən [DuaActions] əvvəlcə [onBack]-i çağırır; vəziyyət [HajjGuideState]-də
 * qaldığı üçün geri qayıdan hacı yerini itirmir.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HajjGuideScreen(state: HajjGuideState, onBack: () -> Unit) {
    // ⚠️ Ad qəsdən `viewModel` deyil — bax `DuaScreen`.
    val hajjViewModel = viewModel { HajjViewModel() }
    val authViewModel = viewModel { AuthViewModel() }

    val evidence by hajjViewModel.evidence.collectAsStateWithLifecycle()
    val isSaving by hajjViewModel.isLoading.collectAsStateWithLifecycle()
    val revision by hajjViewModel.revision.collectAsStateWithLifecycle()
    val session by authViewModel.session.collectAsStateWithLifecycle()

    // Oxucudakı seçim ekranı başqa instansiyadan yazır — bax `HajjViewModel`-in sayğacı.
    LaunchedEffect(revision) { if (revision > 0) hajjViewModel.refresh() }

    val byTopic = remember(evidence) { evidence.groupBy { it.topic } }

    // Qaynaq vərəqi bölmənin siyahısı ilə açılır — sürüşdürmə zikrlər və dəlillər boyu gedir.
    var peekItems by remember { mutableStateOf<List<ReferencePeekItem>>(emptyList()) }
    var peekIndex by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<HajjEvidence?>(null) }
    var deleting by remember { mutableStateOf<HajjEvidence?>(null) }

    val actions = remember(session != null) {
        EvidenceActions(
            isAuthorized = session != null,
            onOpenSource = { item, siblings ->
                // Zikrin qaynağı «Duanın qaynağı», qalanı «Dəlilin qaynağı» başlığı ilə açılır.
                peekItems = siblings.map { ReferencePeekItem.Source(it, isEvidence = !it.isDhikr) }
                peekIndex = siblings.indexOf(item).takeIf { it >= 0 }
            },
            onEdit = { editing = it },
            onDelete = { deleting = it },
        )
    }

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
            HajjPage.START -> HajjStartPage(state, byTopic, actions, back)
            HajjPage.DAYS -> HajjDaysPage(state, byTopic, back)
            HajjPage.STEP -> HajjStepPager(state, byTopic, actions, back)
            HajjPage.IHRAM -> HajjIhramPage(byTopic, actions, back, onOpenMiqat = { state.push(HajjPage.MIQAT) })
            HajjPage.MIQAT -> HajjMiqatPage(byTopic, actions, back)
            HajjPage.COUNTER -> HajjCounterPage(state, byTopic, actions, back)
        }

        // Dua ekranının öz qaynaq vərəqi: tam hədis cihazdakı bazadan, çıxarış sarı vurğu ilə.
        // Provider-in **içindədir**: «aç» düyməsi bələdçini bağlayan `guideActions`-ı oxuyur.
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
            onSave = { updated -> hajjViewModel.updateEvidence(updated) { editing = null } },
            onDismiss = { editing = null },
        )
    }

    // Düymənin lambdası vəziyyəti basılan anda oxuyur; dialoq bağlananda `deleting` sıfırlanır —
    // id əvvəlcədən götürülür ki, ardıcıllıqdan asılı olmasın.
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
                onClick = { pendingDelete?.id?.let { id -> hajjViewModel.deleteEvidence(id) } },
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

/** Addımın mövzularındakı dəlillər — zikrlər əvvəl, təkrarsız. */
internal fun evidenceFor(step: HajjStep, byTopic: Map<String, List<HajjEvidence>>): List<HajjEvidence> =
    evidenceFor(step.topics, byTopic)

internal fun evidenceFor(topics: List<HajjTopic>, byTopic: Map<String, List<HajjEvidence>>): List<HajjEvidence> =
    topics.flatMap { byTopic[it.key].orEmpty() }
        .distinctBy { it.id ?: it.hashCode().toLong() }
        .sortedBy { if (it.isDhikr) 0 else 1 }

/** Səhifə karkası: bar + oxunaqlı enli sürüşən sütun. */
@Composable
internal fun GuidePage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(topBar = { AppBar(title = title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ReadableWidthColumn {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    content()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Başlanğıc
// ---------------------------------------------------------------------------------------------

@Composable
private fun HajjStartPage(
    state: HajjGuideState,
    byTopic: Map<String, List<HajjEvidence>>,
    actions: EvidenceActions,
    onBack: () -> Unit,
) {
    GuidePage(title = stringResource(Res.string.hajjSectionTitle), onBack = onBack) {
        SegmentedTabs(
            labels = listOf(stringResource(Res.string.hajjModeUmrah), stringResource(Res.string.hajjModeHajj)),
            selectedIndex = if (state.isHajj) 1 else 0,
            onSelect = { state.isHajj = it == 1 },
        )

        if (state.isHajj) {
            GuideSectionTitle(stringResource(Res.string.hajjChooseType))
            HajjType.entries.forEach { type ->
                TypeCard(type = type, selected = state.type == type, onSelect = { state.type = type })
            }
            Button(
                onClick = { state.push(HajjPage.DAYS) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(stringResource(Res.string.hajjStartWith, state.type.title), style = typography.labelLarge)
            }
        } else {
            GuideSectionTitle(stringResource(Res.string.hajjUmrahSteps))
            HajjGuideContent.umrahSteps.forEachIndexed { index, step ->
                val done = step.id in state.done
                GuideCard(onClick = { state.openStep(umrah = true, index = index) }) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StepCheck(done = done, onToggle = { state.toggleDone(step.id) })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                step.title,
                                style = contentStyle(typography.titleSmall),
                                color = if (done) colorScheme.onSurfaceVariant else colorScheme.onSurface,
                            )
                            Text(step.text, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            HajjEvidenceSection(byTopic[HajjTopic.UMRAH.key].orEmpty(), actions, showTitles = false)
        }

        GuideSectionTitle(stringResource(Res.string.hajjTools))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            ToolTile(Res.drawable.dr_icon_qibla, stringResource(Res.string.hajjMiqatTitle)) {
                state.openTool(HajjTool.MIQAT)
            }
            ToolTile(Res.drawable.dr_icon_refresh, stringResource(Res.string.hajjCounterTitle)) {
                state.openTool(HajjTool.COUNTER_TAWAF)
            }
        }

        byTopic[HajjTopic.VIRTUE.key]?.takeIf { it.isNotEmpty() }?.let { items ->
            GuideSectionTitle(stringResource(Res.string.hajjVirtue))
            HajjEvidenceSection(items, actions, showTitles = false)
        }

        byTopic[HajjTopic.WHO.key]?.takeIf { it.isNotEmpty() }?.let { items ->
            GuideSectionTitle(stringResource(Res.string.hajjWho))
            HajjEvidenceSection(items, actions, showTitles = false)
        }
    }
}

@Composable
private fun TypeCard(type: HajjType, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colorScheme.primaryContainer.alpha(0.18f) else colorScheme.surface,
        border = BorderStroke(1.5.dp, if (selected) colorScheme.primary else colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RadioButton(selected = selected, onClick = onSelect, modifier = Modifier.size(20.dp))
                Text(
                    type.title,
                    style = contentStyle(typography.titleMedium),
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(type.titleAr, style = arabicStyle(19), color = colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.padding(start = 30.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Fact(stringResource(Res.string.hajjFactIhram), type.ihram)
                Fact(stringResource(Res.string.hajjFactSacrifice), type.sacrifice)
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = typography.bodySmall, color = colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(min = 52.dp))
        Text(value, style = contentStyle(typography.bodySmall), color = colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ToolTile(
    icon: DrawableResource,
    title: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        modifier = Modifier.weight(1f).fillMaxHeight(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(title, style = typography.titleSmall, color = colorScheme.onSurface)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Günlər xətti
// ---------------------------------------------------------------------------------------------

private sealed interface DaysRow {
    data class Header(val day: HajjDay) : DaysRow
    data class Step(val step: HajjStep, val index: Int, val isLastInDay: Boolean) : DaysRow
}

@Composable
private fun HajjDaysPage(state: HajjGuideState, byTopic: Map<String, List<HajjEvidence>>, onBack: () -> Unit) {
    val steps = remember(state.type) { HajjGuideContent.stepsFor(state.type) }
    val rows = remember(steps) {
        buildList {
            HajjDay.entries.forEach { day ->
                val list = steps.withIndex().filter { it.value.day == day }
                if (list.isNotEmpty()) {
                    add(DaysRow.Header(day))
                    list.forEachIndexed { i, (index, s) -> add(DaysRow.Step(s, index, i == list.lastIndex)) }
                }
            }
        }
    }
    val headerIndex = remember(rows) {
        rows.withIndex().filter { it.value is DaysRow.Header }.associate { (i, r) -> (r as DaysRow.Header).day to i }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    /** Görünən ilk sətrin günü — çip zolağı onu vurğulayır. */
    val currentDay by remember(rows) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            rows.take(first + 1).lastOrNull { it is DaysRow.Header }?.let { (it as DaysRow.Header).day } ?: HajjDay.ARRIVAL
        }
    }
    /** Növbəti görüləcək addım — Dua zaman xəttindəki kimi yüngül fonla seçilir. */
    val activeId = steps.firstOrNull { it.id !in state.done }?.id
    val doneCount = steps.count { it.id in state.done }
    val inset = readableWidthInset()

    Scaffold(topBar = { AppBar(title = stringResource(Res.string.hajjDaysTitle), onBack = onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp + inset, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HajjDay.entries.filter { it in headerIndex }.forEach { day ->
                    DayChip(day = day, selected = day == currentDay) {
                        headerIndex[day]?.let { scope.launch { listState.animateScrollToItem(it) } }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp + inset, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(Res.string.hajjTypeLabel, state.type.title),
                    style = typography.bodySmall,
                    color = colorScheme.primary,
                )
                Text(
                    stringResource(Res.string.hajjStepsDone, doneCount, steps.size),
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp + inset, end = 16.dp + inset, bottom = 32.dp),
            ) {
                items(rows, key = { row -> if (row is DaysRow.Header) "h-${row.day}" else "s-${(row as DaysRow.Step).step.id}" }) { row ->
                    when (row) {
                        is DaysRow.Header -> DayHeader(row.day)
                        is DaysRow.Step -> StepRow(
                            step = row.step,
                            items = evidenceFor(row.step, byTopic),
                            done = row.step.id in state.done,
                            active = row.step.id == activeId,
                            isLast = row.isLastInDay,
                            onToggle = { state.toggleDone(row.step.id) },
                            onOpen = { state.openStep(umrah = false, index = row.index) },
                        )
                    }
                }
                item { DraftBadge(Modifier.padding(start = 4.dp, top = 12.dp)) }
            }
        }
    }
}

@Composable
private fun DayChip(day: HajjDay, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colorScheme.primary else colorScheme.surface,
        border = if (selected) null else BorderStroke(1.dp, colorScheme.outlineVariant),
        modifier = Modifier.widthIn(min = 62.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = day.number.ifEmpty { day.title },
                style = contentStyle(typography.titleMedium),
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
            )
            if (day.number.isNotEmpty()) {
                Text(
                    text = day.title,
                    style = contentStyle(typography.labelSmall),
                    color = if (selected) colorScheme.onPrimary.alpha(0.85f) else colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayHeader(day: HajjDay) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(day.heading, style = contentStyle(typography.titleSmall), color = colorScheme.onSurface)
        if (day.titleAr.isNotEmpty()) {
            Text(day.titleAr, style = arabicStyle(17), color = colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Addım sətri — [com.cafarovceyxun.anamuslim.compose.screens.dua.DhikrTimeline]-in görünüşü: solda
 * ✓ və xətt, aktiv addımda yüngül fon. ✓ **ayrıca düymədir** (tamamlandı ↔ yox), sətrin özü isə
 * addımın səhifəsini açır — bir toxunuşun iki mənası olmasın.
 */
@Composable
private fun StepRow(
    step: HajjStep,
    items: List<HajjEvidence>,
    done: Boolean,
    active: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(
                if (active) Modifier.background(colorScheme.primaryContainer.alpha(0.18f), RoundedCornerShape(14.dp))
                else Modifier,
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(start = 4.dp, end = 8.dp, top = 6.dp),
    ) {
        Column(
            modifier = Modifier.width(32.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepCheck(done = done, onToggle = onToggle)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            if (done) colorScheme.primary.alpha(0.6f) else colorScheme.outlineVariant.alpha(0.6f),
                            RoundedCornerShape(1.dp),
                        ),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 3.dp, bottom = if (isLast) 10.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                step.title,
                style = contentStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)),
                color = if (done) colorScheme.onSurfaceVariant else colorScheme.onSurface,
            )
            Text(step.text, style = contentStyle(typography.bodyMedium), color = colorScheme.onSurfaceVariant)
            val dhikr = items.count { it.isDhikr }
            val proofs = items.size - dhikr
            if (items.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    if (dhikr > 0) CountLine(stringResource(Res.string.hajjDhikrCount, dhikr))
                    if (proofs > 0) CountLine(stringResource(Res.string.hajjEvidenceCount, proofs))
                }
            }
        }
    }
}
