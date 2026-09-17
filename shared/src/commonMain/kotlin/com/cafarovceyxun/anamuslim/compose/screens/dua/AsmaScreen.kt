package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderTextZoom
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomFeedback
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomFeedbackOverlay
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomTarget
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReference
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceData
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceVerses
import com.cafarovceyxun.anamuslim.compose.components.reader.pageTurnEffect
import com.cafarovceyxun.anamuslim.compose.components.reader.readerTextZoom
import com.cafarovceyxun.anamuslim.repository.AutoVerseMatch
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.screens.hadith.quranReference
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.app.KeepScreenOnIfEnabled
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DuaPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.asmaEmpty
import com.cafarovceyxun.anamuslim.resources.asmaEvidenceCount
import com.cafarovceyxun.anamuslim.resources.asmaEvidenceEmpty
import com.cafarovceyxun.anamuslim.resources.asmaEvidenceTitle
import com.cafarovceyxun.anamuslim.resources.asmaHiddenBadge
import com.cafarovceyxun.anamuslim.resources.asmaMeaningLabel
import com.cafarovceyxun.anamuslim.resources.asmaAutoCount
import com.cafarovceyxun.anamuslim.resources.asmaAutoNote
import com.cafarovceyxun.anamuslim.resources.asmaAutoTitle
import com.cafarovceyxun.anamuslim.resources.asmaSectionTitle
import com.cafarovceyxun.anamuslim.resources.copiedToClipboard
import com.cafarovceyxun.anamuslim.resources.strLabelOrder
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_eye
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.duaDeleteConfirmTitle
import com.cafarovceyxun.anamuslim.resources.duaDeleteEvidenceConfirm
import com.cafarovceyxun.anamuslim.resources.duaOpenSource
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.topicsMoreVerses
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strTitleVerseRecitation
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaName
import com.cafarovceyxun.anamuslim.utils.dua.AsmaVerseMatcher
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.dua.DuaSourceContent
import com.cafarovceyxun.anamuslim.utils.dua.loadDuaSource
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayerProvider
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.utils.text.SearchHighlightStyle
import com.cafarovceyxun.anamuslim.utils.text.withExcerptHighlight
import com.cafarovceyxun.anamuslim.viewModels.AsmaViewModel
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Əsmaül Hüsnə — 99 adın siyahısı, adın üstünə basanda isə **kitab kimi** vərəqlənən detal.
 *
 * Quruluşu [DuaScreen] ilə eynidir (iki səviyyə, bir ekran, öz geri idarəsi), çünki hər ikisi Namaz
 * ekranından eyni formada açılır və istifadəçi eyni jest gözləyir.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AsmaScreen(
    onBack: () -> Unit,
    /**
     * Açılışda birbaşa göstəriləcək adın nömrəsi (1–99) — dərin linkin giriş nöqtəsi.
     *
     * `null` = adi giriş (siyahı). Ad tapılmasa ekran **sakitcə** siyahıda qalır: nömrə gizlədilmiş
     * ada düşə bilər, boş ekran isə linkin özündən pis olardı (bax [DuaScreen]-dəki eyni qayda).
     */
    initialNameNo: Int? = null,
) {
    val asmaViewModel = viewModel { AsmaViewModel() }
    val authViewModel = viewModel { AuthViewModel() }

    val names by asmaViewModel.names.collectAsStateWithLifecycle()
    val counts by asmaViewModel.counts.collectAsStateWithLifecycle()
    val evidenceByName by asmaViewModel.evidenceByName.collectAsStateWithLifecycle()
    val loadingNames by asmaViewModel.loadingNames.collectAsStateWithLifecycle()
    val isLoaded by asmaViewModel.isLoaded.collectAsStateWithLifecycle()
    val isSaving by asmaViewModel.isLoading.collectAsStateWithLifecycle()
    val revision by asmaViewModel.revision.collectAsStateWithLifecycle()

    val session by authViewModel.session.collectAsStateWithLifecycle()
    val isAuthorized = session != null

    val autoEnabled = DuaPreferences.observeAutoEvidenceEnabled()
    val autoMatches by asmaViewModel.autoMatches.collectAsStateWithLifecycle()
    val autoCounts by asmaViewModel.autoCounts.collectAsStateWithLifecycle()
    val loadingAuto by asmaViewModel.loadingAuto.collectAsStateWithLifecycle()

    // Sayğaclar 99 lokal FTS `COUNT(*)`-dur — arxa fonda, siyahını bloklamadan.
    LaunchedEffect(names, autoEnabled) {
        if (autoEnabled) asmaViewModel.ensureAutoCounts(names)
    }

    LaunchedEffect(revision) { if (revision > 0) asmaViewModel.refresh() }

    var openedNo by remember { mutableStateOf<Int?>(null) }

    // Adlar şəbəkədən/keşdən sonra gəlir, ona görə hədəf ilk kompozisiyada tapılmaya bilər.
    LaunchedEffect(names, initialNameNo) {
        val target = initialNameNo ?: return@LaunchedEffect
        if (names.any { it.no == target }) openedNo = target
    }
    var query by remember { mutableStateOf("") }
    var sorting by remember { mutableStateOf(false) }

    BackHandler(enabled = openedNo != null && !sorting) { openedNo = null }

    // Sıralama rejimində geri jesti **yalnız** rejimi bağlayır — `DuaScreen`-dəki eyni qayda.
    BackHandler(enabled = sorting) { sorting = false }

    // Gizlədilmiş adlar yalnız girişi olan istifadəçiyə görünür — bax [AsmaNameEditDialog].
    // Süzgəc həm siyahıda, həm də vərəqləyicidə eyni olmalıdır, yoxsa «növbəti» düyməsi
    // oxucunun görmədiyi ada aparardı.
    val available = remember(names, isAuthorized) {
        if (isAuthorized) names else names.filter { it.is_visible }
    }

    if (sorting) {
        // Sıralanan dəst **görünən** adlardır: gizli ad siyahıda yoxdur, onu sürükləmək də olmaz.
        DuaReorderScreen(
            title = stringResource(Res.string.strLabelOrder),
            rows = available.map { name ->
                ReorderRow(
                    key = name.no.toString(),
                    title = name.transliteration,
                    subtitle = name.meaning,
                    arabic = name.name_ar,
                )
            },
            isSaving = isSaving,
            onSave = { order ->
                asmaViewModel.saveNameOrder(order.mapNotNull { it.toIntOrNull() }) {
                    sorting = false
                }
            },
            onCancel = { sorting = false },
        )
        return
    }

    val opened = openedNo
    if (opened != null && available.isNotEmpty()) {
        AsmaDetailPager(
            autoEnabled = autoEnabled,
            autoMatches = autoMatches,
            autoCounts = autoCounts,
            loadingAuto = loadingAuto,
            onLoadMoreAuto = { name -> asmaViewModel.loadMoreAuto(name) },
            onEnsureAuto = { name -> asmaViewModel.ensureAutoEvidence(name) },
            onHideAuto = { nameNo, match ->
                asmaViewModel.toggleAutoHidden(nameNo, match.chapterNo, match.verseNo, hide = true)
            },
            names = available,
            initialNo = opened,
            // Dəlillər ada görə yüklənir — bax `AsmaViewModel`. Səhifə açılanda `onEnsure`
            // çağırılır; `HorizontalPager` qonşu səhifələri də kompozisiya etdiyi üçün növbəti ad
            // qabaqcadan gəlir.
            evidenceOf = { no -> evidenceByName[no].orEmpty() },
            isLoadingEvidence = { no -> no in loadingNames && !evidenceByName.containsKey(no) },
            onEnsure = { no -> asmaViewModel.ensureEvidence(no) },
            isAuthorized = isAuthorized,
            isSaving = isSaving,
            onDelete = { id, nameNo -> asmaViewModel.deleteEvidence(id, nameNo) },
            onEditEvidence = { updated -> asmaViewModel.updateEvidence(updated) },
            onEditName = { updated -> asmaViewModel.updateName(updated) },
            onBack = { openedNo = null },
        )
        return
    }

    // Siyahıdakı **sıra nömrəsi**: admin adları sürükləyib düzəndə nişan da onunla birlikdə
    // dəyişir. `no` (adın kanonik nömrəsi) toxunulmaz qalır — o, PK-dır və dəlillər ona bağlıdır
    // (`asma_evidence.name_no`), yəni sıraya görə dəyişsəydi dəlillər qoparddı.
    //
    // Mövqe **süzülməmiş** siyahıdan gəlir: axtarış sətri yazanda nömrələr sürüşməməlidir.
    val displayNumbers = remember(available) {
        available.withIndex().associate { (index, name) -> name.no to index + 1 }
    }

    val filtered = remember(available, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) available
        else available.filter { name ->
            name.transliteration.lowercase().contains(needle) ||
                name.meaning.lowercase().contains(needle) ||
                name.name_ar.contains(query.trim())
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(Res.string.asmaSectionTitle),
                onBack = onBack,
                searchQuery = query,
                onSearchQueryChange = { query = it },
                // Sıralama düyməsi bardan çıxdı: admin sətri **basılı saxlayıb** sıralamaya
                // keçir (dua siyahısındakı jestin eynisi), adi istifadəçidə isə bar təmiz qalır.
                actions = {},
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                names.isEmpty() && !isLoaded -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                    color = colorScheme.primary,
                )

                names.isEmpty() -> Text(
                    text = stringResource(Res.string.asmaEmpty),
                    style = typography.bodyMedium.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Lazy siyahını `ReadableWidthColumn` ilə sarmaq olmur (sürüşmə jesti və sətir
                    // fonu ekranın kənarından qopardı) — məhdudiyyət `contentPadding`-ə qatılır.
                    contentPadding = PaddingValues(
                        horizontal = 16.dp + readableWidthInset(),
                        vertical = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.no }) { name ->
                        AsmaNameRow(
                            name = name,
                            displayNo = displayNumbers[name.no] ?: name.no,
                            evidenceCount = counts[name.no] ?: 0,
                            onClick = { openedNo = name.no },
                            // Hər iki jest **yalnız admin üçün**: adi istifadəçidə uzun basma və
                            // sürüşdürmə boş vədə çevrilərdi.
                            onLongClick = if (isAuthorized && available.size > 1) {
                                { sorting = true }
                            } else {
                                null
                            },
                            onToggleVisibility = if (isAuthorized) {
                                { asmaViewModel.updateName(name.copy(is_visible = !name.is_visible)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Siyahının bir sətri — nömrə, ərəbcə ad, transliterasiya, məna və dəlil sayı. */
@Composable
private fun AsmaNameRow(
    name: AsmaName,
    /** Siyahıdakı sıra nömrəsi — adın kanonik `no`-su deyil (bax `displayNumbers`). */
    displayNo: Int,
    evidenceCount: Int,
    onClick: () -> Unit,
    /** Basılı saxlama — sıralama rejimi (yalnız admin); `null` = jest yoxdur. */
    onLongClick: (() -> Unit)? = null,
    /** Sola sürüşdürmə — adın siyahıda görünməsini dəyişir (yalnız admin). */
    onToggleVisibility: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(name.no) { Animatable(0f) }
    val swipeTrigger = with(LocalDensity.current) { SWIPE_TRIGGER.toPx() }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Sürüşdürmənin altından çıxan nişan — jestin nə edəcəyini deyir.
        if (onToggleVisibility != null) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_eye),
                    contentDescription = null,
                    tint = if (name.is_visible) colorScheme.error.alpha(0.8f)
                    else colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.5f)),
        // Gizlədilmiş ad solğun çəkilir: admin siyahıda onu adi adlardan bir baxışda ayırsın.
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .alpha(if (name.is_visible) 1f else 0.55f)
            .then(
                if (onToggleVisibility == null) {
                    Modifier
                } else {
                    // ⚠️ Jest **sola** məhduddur (`coerceIn(-max, 0)`): sağa sürüşdürmə siyahının
                    // öz üfüqi hərəkəti ilə qarışardı, üstəlik geri jesti də sağdan gəlir.
                    Modifier.draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                offsetX.snapTo(
                                    (offsetX.value + delta).coerceIn(-swipeTrigger * 1.6f, 0f),
                                )
                            }
                        },
                        onDragStopped = {
                            val passed = offsetX.value <= -swipeTrigger
                            // Sətir həmişə yerinə qayıdır: bu, silmə deyil, açar dəyişməsidir —
                            // nəticəni solğunluq və «Gizli» nişanı göstərir.
                            offsetX.animateTo(0f)
                            if (passed) onToggleVisibility()
                        },
                    )
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(colorScheme.primaryContainer.alpha(0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayNo.toString(),
                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.transliteration,
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurface,
                )
                Text(
                    text = name.meaning,
                    style = typography.bodySmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.8f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (evidenceCount > 0) {
                        Text(
                            text = stringResource(Res.string.asmaEvidenceCount, evidenceCount),
                            style = typography.labelSmall.withScriptDirection(arabic = false),
                            color = colorScheme.primary.alpha(0.85f),
                        )
                    }

                    // Sətir buraya yalnız admin üçün düşür — oxucu gizli adı ümumiyyətlə görmür.
                    if (!name.is_visible) {
                        Text(
                            text = stringResource(Res.string.asmaHiddenBadge),
                            style = typography.labelSmall.withScriptDirection(arabic = false),
                            color = colorScheme.error.alpha(0.85f),
                        )
                    }
                }
            }

            Text(
                text = name.name_ar,
                style = typography.titleLarge.withScriptDirection(
                    arabic = true,
                    arabicFontFamily = arabicFontFamily(),
                ),
                color = colorScheme.onSurface,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
    }
}

/** Sətri neçə piksel sola çəkəndə görünmə açarı dəyişir. */
private val SWIPE_TRIGGER = 72.dp

/**
 * Adın detalı — yuxarıda ad, altında mənası, daha aşağıda ona dəlil olan ayə və hədislər.
 *
 * Səhifələr **bütün 99 ad** üzərindədir, yalnız seçilmiş ad üzərində deyil: istifadəçi siyahıya
 * qayıtmadan qonşu ada keçə bilsin.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AsmaDetailPager(
    autoEnabled: Boolean,
    autoMatches: Map<Int, List<AutoVerseMatch>>,
    autoCounts: Map<Int, Int>,
    loadingAuto: Set<Int>,
    onLoadMoreAuto: (AsmaName) -> Unit,
    onEnsureAuto: (AsmaName) -> Unit,
    onHideAuto: (nameNo: Int, match: AutoVerseMatch) -> Unit,
    names: List<AsmaName>,
    initialNo: Int,
    evidenceOf: (Int) -> List<AsmaEvidence>,
    isLoadingEvidence: (Int) -> Boolean,
    onEnsure: (Int) -> Unit,
    isAuthorized: Boolean,
    isSaving: Boolean,
    onDelete: (id: Long, nameNo: Int) -> Unit,
    onEditEvidence: (AsmaEvidence) -> Unit,
    onEditName: (AsmaName) -> Unit,
    onBack: () -> Unit,
) {
    val initialPage = remember(names, initialNo) {
        names.indexOfFirst { it.no == initialNo }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { names.size }
    val scope = rememberCoroutineScope()

    // Oxuma səthi — bax [DuaPagerScreen]-dəki eyni çağırış.
    KeepScreenOnIfEnabled()

    var zoomFeedback by remember { mutableStateOf<ReaderZoomFeedback?>(null) }

    // Ölçü və animasiya ayarları dua ilə **ortaqdır**: istifadəçi üçün bu, bir bölmədir.
    val arabicMult = DuaPreferences.observeArabicSizeMultiplier()
    val translationMult = DuaPreferences.observeTranslationSizeMultiplier()
    val pageTurnAnimation = AppPreferences.observeReaderPageTurnAnimation()

    val zoomModifier = Modifier.readerTextZoom(
        enabled = AppPreferences.observeReaderPinchZoomEnabled(),
        arabicMultiplier = arabicMult,
        translationMultiplier = translationMult,
        minMultiplier = ReaderTextZoom.HADITH_MIN,
        maxMultiplier = ReaderTextZoom.HADITH_MAX,
        onZoom = { target, value ->
            zoomFeedback = ReaderZoomFeedback(target, value)
            scope.launch {
                when (target) {
                    ReaderZoomTarget.Arabic -> DuaPreferences.setArabicSizeMultiplier(value)
                    ReaderZoomTarget.Translation ->
                        DuaPreferences.setTranslationSizeMultiplier(value)
                }
            }
        },
    )

    var sourceRef by remember { mutableStateOf<DuaSourceRef?>(null) }
    var sharing by remember { mutableStateOf<DuaSourceRef?>(null) }
    var pendingDelete by remember { mutableStateOf<AsmaEvidence?>(null) }
    var editingEvidence by remember { mutableStateOf<AsmaEvidence?>(null) }
    var editingName by remember { mutableStateOf<AsmaName?>(null) }
    var quickRef by remember { mutableStateOf<QuickReferenceData?>(null) }

    // ▷ düymələri üçün pleyer bağlantısı ekran açıq olduğu müddətdədir.
    val versePlayer = rememberEvidenceVersePlayer()

    val current = names.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            AppBar(
                title = current?.transliteration ?: stringResource(Res.string.asmaSectionTitle),
                onBack = onBack,
                actions = {
                    // Adın mətni və görünüşü yalnız admin üçün; qapı bazadadır (RLS), düymə isə
                    // girişi olmayan istifadəçiyə heç göstərilmir.
                    if (isAuthorized && current != null) {
                        IconButton(onClick = { editingName = current }) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_edit),
                                contentDescription = stringResource(Res.string.strLabelEdit),
                                tint = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val name = names[page]

                    // Səhifə görünəndə həmin adın dəlilləri yüklənir (artıq yüklənibsə heç nə etmir).
                    LaunchedEffect(name.no) { onEnsure(name.no) }
                    LaunchedEffect(name.no, autoEnabled) {
                        if (autoEnabled) onEnsureAuto(name)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pageTurnEffect(
                                animation = pageTurnAnimation,
                                pagerState = pagerState,
                                page = page,
                                ground = colorScheme.background,
                            ),
                    ) {
                        AsmaDetailPage(
                            name = name,
                            // Vərəqləyici elə siyahının sırası ilə gedir, ona görə səhifə indeksi
                            // sıra nömrəsidir.
                            displayNo = page + 1,
                            evidence = evidenceOf(name.no),
                            isLoading = isLoadingEvidence(name.no),
                            isAuthorized = isAuthorized,
                            arabicSizeMult = arabicMult,
                            translationSizeMult = translationMult,
                            zoomModifier = zoomModifier,
                            auto = if (autoEnabled) {
                                AsmaAutoSection(
                                    matches = autoMatches[name.no].orEmpty(),
                                    total = autoCounts[name.no],
                                    isLoading = name.no in loadingAuto,
                                    onLoadMore = { onLoadMoreAuto(name) },
                                    onHide = { match -> onHideAuto(name.no, match) },
                                )
                            } else {
                                null
                            },
                            versePlayer = versePlayer,
                            // Ayə mənbəli dəlil/uyğunluq oxucunun sürətli baxış vərəqini açır
                            // (tərcümə, oxucuda açmaq, paylaşma — hamısı orada); hədis mənbəli
                            // dəlil isə mövcud qaynaq vərəqini açır, çünki ona kontekst lazımdır.
                            onOpenVerse = { chapterNo, verseNo, verseEnd ->
                                quickRef = QuickReferenceData(
                                    chapterNo = chapterNo,
                                    parsedVerses = QuickReferenceVerses.Range(
                                        chapterNo = chapterNo,
                                        range = verseNo..(verseEnd ?: verseNo),
                                    ),
                                    // Boş dəst = istifadəçinin öz seçdiyi tərcümələr.
                                    slugs = emptySet(),
                                )
                            },
                            onOpenSource = { sourceRef = it },
                            onShare = { sharing = it },
                            onEdit = { editingEvidence = it },
                            onDelete = { pendingDelete = it },
                        )
                    }
                }

                ReaderZoomFeedbackOverlay(zoomFeedback) { zoomFeedback = null }
            }

            DuaPagerControls(
                position = pagerState.currentPage,
                total = names.size,
                onPrevious = {
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(names.lastIndex),
                        )
                    }
                },
            )
        }
    }

    DuaSourceSheet(ref = sourceRef, isEvidence = true, onClose = { sourceRef = null })

    // Oxucunun öz sürətli baxış vərəqi — özünü `ReaderProvider`-ə sarır, ona görə Əsmadan
    // çağırmaq təhlükəsizdir (⚠️ `LocalRecitation`-a birbaşa toxunmaq olmaz: provider-siz çökür).
    QuickReference(
        data = quickRef,
        onOpenInReader = { chapterNo, range ->
            quickRef = null
            ReaderUiHooks.openVerseRange?.invoke(chapterNo, range.first, range.last)
        },
        onClose = { quickRef = null },
    )

    // Dua ekranındakı vərəqin eynisi. Burada [DuaShareParts.visible] yoxdur, çünki dəlil kartı
    // bloklarını gizlətmir — ekranda nə varsa, seçim də odur.
    DuaShareSheet(
        ref = sharing,
        initialParts = DuaShareParts(),
        // Şəkil kartının üst etiketi — adın özü («ər-Rahim»), çünki dəlil həmin ada aiddir.
        eyebrow = current?.transliteration,
        onDismiss = { sharing = null },
    )

    editingEvidence?.let { item ->
        AsmaEvidenceEditDialog(
            evidence = item,
            isSaving = isSaving,
            onSave = { updated ->
                editingEvidence = null
                onEditEvidence(updated)
            },
            onDismiss = { editingEvidence = null },
        )
    }

    editingName?.let { item ->
        AsmaNameEditDialog(
            name = item,
            isSaving = isSaving,
            onSave = { updated ->
                editingName = null
                onEditName(updated)
            },
            onDismiss = { editingName = null },
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            isOpen = true,
            onClose = { pendingDelete = null },
            title = stringResource(Res.string.duaDeleteConfirmTitle),
            actions = listOf(
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelCancel),
                    onClick = { pendingDelete = null },
                ),
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelDelete),
                    style = AlertDialogActionStyle.Danger,
                    onClick = {
                        pendingDelete = null
                        item.id?.let { id -> onDelete(id, item.name_no) }
                    },
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
}

/**
 * Bir adın səhifəsi: başlıq bloku + dəlillər.
 *
 * ⚠️ **`LazyColumn`**, adi `Column` + `verticalScroll` deyil: bir ada çox dəlil düşür və hamısını
 * birdən kompozisiya etmək səhifəni ağırlaşdırardı — üstəlik `HorizontalPager` qonşu səhifələri də
 * canlı saxlayır, yəni eyni anda üç adın bütün kartları qurulardı. Şaquli lazy siyahı üfüqi
 * vərəqləyicinin içində sərbəstdir (jest istiqamətləri fərqlidir).
 */
@Composable
private fun AsmaDetailPage(
    name: AsmaName,
    /** Siyahıdakı sıra nömrəsi — başlıqdakı «№N» bunu göstərir. */
    displayNo: Int,
    evidence: List<AsmaEvidence>,
    isLoading: Boolean,
    isAuthorized: Boolean,
    arabicSizeMult: Float,
    translationSizeMult: Float,
    /** İki/üç barmaqla ölçüləndirmə — sürüşən siyahıya zəncirlənir (bax `ReaderTextZoom`). */
    zoomModifier: Modifier,
    /** Avtomatik uyğunlaşdırma bloku; `null` → ayarda söndürülüb, blok çəkilmir. */
    auto: AsmaAutoSection?,
    versePlayer: EvidenceVersePlayer,
    /** Ayə mənbəli kart açılanda — sürətli baxış vərəqi. */
    onOpenVerse: (chapterNo: Int, verseNo: Int, verseEnd: Int?) -> Unit,
    onOpenSource: (AsmaEvidence) -> Unit,
    onShare: (AsmaEvidence) -> Unit,
    onEdit: (AsmaEvidence) -> Unit,
    onDelete: (AsmaEvidence) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().then(zoomModifier),
        contentPadding = PaddingValues(
            start = 20.dp + readableWidthInset(),
            end = 20.dp + readableWidthInset(),
            top = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
        // Başlıq bloku — ad, nömrə, transliterasiya və məna bir kartda: adın «kimliyi» ekranda
        // dağılmasın, dəlillər isə onun altında ayrıca oxunsun.
        Surface(
            color = colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = colorScheme.primaryContainer.alpha(0.45f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = asmaNumberLabel(displayNo),
                        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }

                Text(
                    text = name.name_ar,
                    style = typography.displaySmall.copy(
                        fontSize = 40.sp * arabicSizeMult,
                        lineHeight = (40.sp * arabicSizeMult) * 1.5f,
                        textAlign = TextAlign.Center,
                    ).withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily()),
                    color = colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = name.transliteration,
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                HorizontalDivider(
                    modifier = Modifier.width(64.dp),
                    color = colorScheme.primary.alpha(0.3f),
                )

                Text(
                    text = stringResource(Res.string.asmaMeaningLabel),
                    style = typography.labelSmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.7f),
                )

                Text(
                    text = name.meaning,
                    style = typography.bodyLarge.copy(textAlign = TextAlign.Center)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurface.alpha(0.92f),
                    modifier = Modifier.fillMaxWidth(),
                )

                name.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = typography.bodyMedium.copy(textAlign = TextAlign.Center)
                            .withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.85f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        }

        item(key = "evidence-header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.asmaEvidenceTitle),
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurface,
                )

                if (evidence.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.asmaEvidenceCount, evidence.size),
                        style = typography.labelSmall.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.75f),
                    )
                }
            }
        }

        when {
            // «Hələ gəlməyib» ilə «yoxdur» ayrılır: yükləmə vaxtı «dəlil yoxdur» yazmaq yanlış
            // məlumatdır, xüsusən zəif şəbəkədə.
            evidence.isEmpty() && isLoading -> item(key = "evidence-loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorScheme.primary,
                    )
                }
            }

            evidence.isEmpty() -> item(key = "evidence-empty") {
                Text(
                    text = stringResource(Res.string.asmaEvidenceEmpty),
                    style = typography.bodySmall.withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.75f),
                )
            }

            else -> items(evidence, key = { it.id ?: 0L }) { item ->
                AsmaEvidenceCard(
                    evidence = item,
                    isAuthorized = isAuthorized,
                    arabicSizeMult = arabicSizeMult,
                    translationSizeMult = translationSizeMult,
                    versePlayer = versePlayer,
                    onOpen = {
                        val chapterNo = item.chapter_no
                        val verseNo = item.verse_no
                        // Ayə → oxucunun sürətli baxışı; hədis → qaynaq vərəqi (kontekst lazımdır).
                        if (item.isQuran && chapterNo != null && verseNo != null) {
                            onOpenVerse(chapterNo, verseNo, item.verse_end)
                        } else {
                            onOpenSource(item)
                        }
                    },
                    onOpenSource = { onOpenSource(item) },
                    onShare = { onShare(item) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) },
                )
            }
        }

        // ---- Avtomatik tapılan ayələr ----
        //
        // Əl ilə əlavə edilmiş dəlillərlə **qarışdırılmır**: yuxarıdakılar redaktorun seçdiyi
        // çıxarışlardır, bunlar isə maşın uyğunluğudur. Qarışsaydı istifadəçi kurasiya olunmuş
        // məzmunla təxmini ayıra bilməzdi, admin gizlətmə düyməsi isə yalnız ikinci bloka aiddir.
        if (auto != null) {
            item(key = "auto-header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.asmaAutoTitle),
                            style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                .withScriptDirection(arabic = false),
                            color = colorScheme.onSurface,
                        )

                        auto.total?.takeIf { it > 0 }?.let { total ->
                            Text(
                                // «dəlil» yox, «ayə»: qeyd elə bunların dəlil **olmadığını** deyir,
                                // başlıqda «45 dəlil» yazmaq özü ilə ziddiyyət yaradırdı.
                                text = stringResource(Res.string.asmaAutoCount, total),
                                style = typography.labelSmall.withScriptDirection(arabic = false),
                                color = colorScheme.onSurfaceVariant.alpha(0.75f),
                            )
                        }
                    }

                    Text(
                        text = stringResource(Res.string.asmaAutoNote),
                        style = typography.labelSmall.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.7f),
                    )
                }
            }

            items(auto.matches, key = { "auto-${it.chapterNo}-${it.verseNo}" }) { match ->
                AsmaAutoMatchCard(
                    match = match,
                    isAuthorized = isAuthorized,
                    arabicSizeMult = arabicSizeMult,
                    nameAr = name.name_ar,
                    versePlayer = versePlayer,
                    onOpen = { onOpenVerse(match.chapterNo, match.verseNo, null) },
                    onHide = { auto.onHide(match) },
                )
            }

            if (auto.isLoading) {
                item(key = "auto-loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colorScheme.primary,
                        )
                    }
                }
            } else if (auto.hasMore) {
                item(key = "auto-more") {
                    TextButton(
                        onClick = auto.onLoadMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.topicsMoreVerses, auto.remaining))
                    }
                }
            }
        }
    }
}

/**
 * Avtomatik tapılmış bir ayə.
 *
 * `AsmaEvidenceCard`-dan qəsdən sadədir: burada tərcümə, oxunuş və qeyd yoxdur — bunlar seçilmiş
 * çıxarış deyil, indeksdən gələn tam ayədir. Admin üçün yeganə əlavə «gizlət» düyməsidir.
 */
@Composable
private fun AsmaAutoMatchCard(
    match: AutoVerseMatch,
    isAuthorized: Boolean,
    arabicSizeMult: Float,
    /** Adın müshəf yazılışı — ayədəki yeri bununla tapılır. */
    nameAr: String,
    versePlayer: EvidenceVersePlayer,
    onOpen: () -> Unit,
    onHide: () -> Unit,
) {
    // Adın ayənin **harasında** olduğu sarı ilə işarələnir: kart onsuz «bu ayə niyə buradadır»
    // sualını cavabsız qoyurdu, uzun ayədə isə adı gözlə tapmaq çətindir.
    val highlighted = remember(match.textAr, nameAr) {
        highlightNameInVerse(match.textAr, nameAr)
    }

    Surface(
        color = colorScheme.surfaceContainerLow.alpha(0.4f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.25f)),
        // Toxunuş ayəni oxucudakı sürətli baxış vərəqində açır (`QuickReference`) — kartın özü
        // yalnız ərəbcəni göstərir, tərcüməni isə istifadəçi elə orada görür.
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = highlighted,
                style = typography.titleMedium.copy(
                    fontSize = 20.sp * arabicSizeMult,
                    lineHeight = (20.sp * arabicSizeMult) * 1.9f,
                    textAlign = TextAlign.Right,
                ).withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily()),
                color = colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VerseReferenceRow(
                    chapterNo = match.chapterNo,
                    verseNo = match.verseNo,
                    verseEnd = null,
                    versePlayer = versePlayer,
                )

                Spacer(Modifier.weight(1f))

                if (isAuthorized) {
                    IconButton(onClick = onHide) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_delete),
                            contentDescription = stringResource(Res.string.asmaHiddenBadge),
                            tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ayə istinadı «Fatihə 1:5» formasında və **yalnız** yanındakı ▷ düyməsi.
 *
 * Format bütün tətbiqdə eynidir ([quranReference]) — əvvəl dəlil kartı bazadakı sərbəst mətni
 * («Fatihə 5», nömrəsiz), avtomatik kart isə quru «1:5» göstərirdi, yəni eyni ayə iki cür yazılırdı.
 * Surə adı cihazdan oxunur, ona görə kart ilk kadrda istinadsız çıxa bilər.
 *
 * ▷ **yalnız həmin ayəni** səsləndirir ([RecitationPlayer.playSingleVerse]) — ardınca gələn ayələrə
 * keçmir və mini pleyeri açmır.
 */
@Composable
private fun VerseReferenceRow(
    chapterNo: Int,
    verseNo: Int,
    verseEnd: Int?,
    versePlayer: EvidenceVersePlayer,
) {
    val chapterName by produceState("", chapterNo) {
        value = RepositoryProvider.quranRepository.getChapterName(chapterNo)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (chapterName.isBlank()) {
                "$chapterNo:$verseNo"
            } else {
                quranReference(chapterName, chapterNo, verseNo, verseEnd ?: verseNo)
            },
            style = typography.labelSmall.withScriptDirection(arabic = false),
            color = colorScheme.onSurfaceVariant.alpha(0.7f),
        )

        val label = stringResource(Res.string.strTitleVerseRecitation)
        val playing = versePlayer.isPlaying(chapterNo, verseNo)

        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .semantics { contentDescription = label }
                .size(28.dp)
                .clip(CircleShape)
                .clickable { versePlayer.onPlay(chapterNo, verseNo) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (playing) Res.drawable.ic_pause else Res.drawable.ic_play,
                ),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colorScheme.primary,
            )
        }
    }
}

/**
 * Avtomatik uyğunlaşdırma blokunun ekrana lazım olan hər şeyi.
 *
 * Bir data sinifdə yığılıb, çünki [AsmaDetailPage] onsuz da uzun parametr siyahısına malikdir və
 * bu altısı həmişə birlikdə gəlir. `null` → blok ümumiyyətlə çəkilmir (ayarda söndürülüb).
 */
internal data class AsmaAutoSection(
    val matches: List<AutoVerseMatch>,
    /** Ümumi uyğunluq sayı; hələ hesablanmayıbsa `null`. */
    val total: Int?,
    val isLoading: Boolean,
    val onLoadMore: () -> Unit,
    val onHide: (AutoVerseMatch) -> Unit,
) {
    /** Serverdən daha çox gətirmək mümkündürmü. */
    val hasMore: Boolean get() = total != null && matches.size < total

    /** «+N daha çox ayə» düyməsindəki rəqəm. */
    val remaining: Int get() = ((total ?: 0) - matches.size).coerceAtLeast(0)
}

/** Bir dəlil — ərəbcə çıxarış, tərcüməsi, istinadı və qaynağa keçid. */
@Composable
private fun AsmaEvidenceCard(
    evidence: AsmaEvidence,
    isAuthorized: Boolean,
    arabicSizeMult: Float,
    translationSizeMult: Float,
    versePlayer: EvidenceVersePlayer,
    onOpen: () -> Unit,
    onOpenSource: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // Dəlil kartı bloklarını gizlətmir, ona görə kopyalanan mətn həmişə tamdır (`DuaShareParts()`).
    val copyText = remember(evidence) { buildDuaShareText(evidence) }
    val clipboardMsg = stringResource(Res.string.copiedToClipboard)

    // Kart artıq **tam** ayəni/hədisi göstərir, seçilmiş çıxarışı yox — avtomatik tapılan ayələrlə
    // eyni forma. Çıxarış isə içində sarı ilə işarələnir, yəni «hansı hissə dəlildir» sualı da,
    // «ətrafında nə deyilir» sualı da eyni kartda cavablanır.
    //
    // ⚠️ Mənbə **cihazdan** oxunur (`loadDuaSource`): hədis bazası endirilməyibsə `null` gəlir və
    // kart köhnəsi kimi yalnız çıxarışı göstərir — boş kart göstərmək olmaz.
    val source by produceState<DuaSourceContent?>(null, evidence) {
        value = loadDuaSource(evidence)
    }

    val arabic = remember(source, evidence) {
        source?.arabic?.takeIf { it.isNotBlank() }?.withExcerptHighlight(evidence.text_ar)
            ?: AnnotatedString(evidence.text_ar)
    }

    val translation = remember(source, evidence) {
        source?.translation?.takeIf { it.isNotBlank() }?.withExcerptHighlight(evidence.text_az)
            ?: AnnotatedString(evidence.text_az)
    }

    Surface(
        color = colorScheme.surfaceContainerLow.alpha(0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            // Toxunuş sürətli baxışı açır (ayədə oxucunun `QuickReference` vərəqi, hədisdə qaynaq
            // vərəqi), uzun basmaq isə kartı kopyalayır — Dua səhifəsindəki jestin eynisi.
            .combinedClickable(
                onClick = onOpen,
                onLongClick = {
                    PlatformUtils.copyToClipboard(copyText)
                    PlatformUtils.showClipboardMessage(clipboardMsg)
                },
            ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = arabic,
                style = typography.titleMedium.copy(
                    fontSize = 20.sp * arabicSizeMult,
                    lineHeight = (20.sp * arabicSizeMult) * 1.9f,
                    textAlign = TextAlign.Right,
                ).withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily()),
                color = colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )

            evidence.transliteration?.takeIf { it.isNotBlank() }?.let { translit ->
                Text(
                    text = translit,
                    style = typography.bodySmall.copy(fontStyle = FontStyle.Italic)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurfaceVariant.alpha(0.9f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Tərcümə boş qala bilər — bax `DuaPage`.
            if (translation.isNotEmpty()) {
                Text(
                    text = translation,
                    style = typography.bodyMedium
                        .copy(fontSize = typography.bodyMedium.fontSize * translationSizeMult)
                        .withLineHeightRatio(TRANSLATION_LINE_HEIGHT_RATIO)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurface.alpha(0.9f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Ayə üçün kanonik istinad + ▷; hədis üçün bazadakı sərbəst mənbə sətri.
                val chapterNo = evidence.chapter_no
                val verseNo = evidence.verse_no

                if (evidence.isQuran && chapterNo != null && verseNo != null) {
                    VerseReferenceRow(
                        chapterNo = chapterNo,
                        verseNo = verseNo,
                        verseEnd = evidence.verse_end,
                        versePlayer = versePlayer,
                    )

                    Spacer(Modifier.weight(1f))
                } else {
                    evidence.source?.takeIf { it.isNotBlank() }?.let { sourceText ->
                        Text(
                            text = "— $sourceText",
                            style = typography.labelSmall.withScriptDirection(arabic = false),
                            // Mənbə sətri mətnin özü deyil, arxasındakı istinaddır — bir az daha boz.
                            color = colorScheme.onSurfaceVariant.alpha(0.55f),
                            modifier = Modifier.weight(1f),
                        )
                    } ?: Spacer(Modifier.weight(1f))
                }

                TextButton(onClick = onOpenSource) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_open),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.duaOpenSource),
                        style = typography.labelMedium,
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_share),
                        contentDescription = stringResource(Res.string.strLabelShare),
                        tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }

                if (isAuthorized) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_edit),
                            contentDescription = stringResource(Res.string.strLabelEdit),
                            tint = colorScheme.onSurfaceVariant.alpha(0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_delete),
                            contentDescription = stringResource(Res.string.strLabelDelete),
                            tint = colorScheme.error.alpha(0.75f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dəlil kartlarındakı ▷ düymələrinin pleyer vəziyyəti.
 *
 * Kartlar bunu **hazır** alır: hər kartın öz axınına abunə olması 50 kartlıq siyahıda əlli abunə
 * deməkdir, halbuki səsləndirilən ayə birdir.
 */
internal data class EvidenceVersePlayer(
    /** Hazırda **səsləndirilən** ayə; dayanıbsa `null`. */
    val playingVerse: ChapterVersePair?,
    val onPlay: (chapterNo: Int, verseNo: Int) -> Unit,
) {
    fun isPlaying(chapterNo: Int, verseNo: Int): Boolean =
        playingVerse?.chapterNo == chapterNo && playingVerse.verseNo == verseNo
}

/**
 * Ekran açıq olduğu müddətdə pleyer sessiyasına qoşulur.
 *
 * `ReaderProvider`-siz işləyir (⚠️ `LocalRecitation` burada **yoxdur** — ona toxunmaq çökmə
 * deməkdir), bağlantı isə `ReciterPreview`-dakı qurğunun eynisidir: `connect` sayğaclıdır, ona görə
 * oxucuda gedən səsləndirmə bundan zərər görmür.
 */
@Composable
private fun rememberEvidenceVersePlayer(): EvidenceVersePlayer {
    val player = remember { RecitationPlayerProvider.player }
    val state by player.state.collectAsStateWithLifecycle()
    val isPlaying by player.isPlayingState.collectAsStateWithLifecycle()

    DisposableEffect(player) {
        player.connect()
        onDispose { player.disconnect() }
    }

    return remember(state.currentVerse, isPlaying, player) {
        EvidenceVersePlayer(
            playingVerse = state.currentVerse.takeIf { isPlaying },
            onPlay = { chapterNo, verseNo ->
                player.playSingleVerse(ChapterVersePair(chapterNo, verseNo))
            },
        )
    }
}

/**
 * Ayə mətnində adın özünü sarı ilə işarələyir.
 *
 * Yeri [AsmaVerseMatcher.nameRangeIn] tapır (hərəkəsiz müqayisə + söz sərhədi + müshəfin xəncər
 * əlifi); tapılmasa mətn **vurğusuz** qaytarılır — səhv sözü işarələmək vurğusuzdan pisdir.
 */
private fun highlightNameInVerse(text: String, nameAr: String): AnnotatedString {
    val range = AsmaVerseMatcher.nameRangeIn(text, nameAr) ?: return AnnotatedString(text)

    return buildAnnotatedString {
        append(text)
        addStyle(SearchHighlightStyle, range.first, range.last + 1)
    }
}
