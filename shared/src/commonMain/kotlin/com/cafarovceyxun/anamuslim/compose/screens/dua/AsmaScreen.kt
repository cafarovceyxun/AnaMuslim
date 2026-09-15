package com.cafarovceyxun.anamuslim.compose.screens.dua

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.screens.hadith.withScriptDirection
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.asmaEmpty
import com.cafarovceyxun.anamuslim.resources.asmaEvidenceCount
import com.cafarovceyxun.anamuslim.resources.asmaEvidenceEmpty
import com.cafarovceyxun.anamuslim.resources.asmaEvidenceTitle
import com.cafarovceyxun.anamuslim.resources.asmaHiddenBadge
import com.cafarovceyxun.anamuslim.resources.asmaMeaningLabel
import com.cafarovceyxun.anamuslim.resources.asmaSectionTitle
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.duaDeleteConfirmTitle
import com.cafarovceyxun.anamuslim.resources.duaDeleteEvidenceConfirm
import com.cafarovceyxun.anamuslim.resources.duaOpenSource
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaEvidence
import com.cafarovceyxun.anamuslim.utils.supabase.AsmaName
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSourceRef
import com.cafarovceyxun.anamuslim.viewModels.AsmaViewModel
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
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
fun AsmaScreen(onBack: () -> Unit) {
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

    LaunchedEffect(revision) { if (revision > 0) asmaViewModel.refresh() }

    var openedNo by remember { mutableStateOf<Int?>(null) }
    var query by remember { mutableStateOf("") }

    BackHandler(enabled = openedNo != null) { openedNo = null }

    // Gizlədilmiş adlar yalnız girişi olan istifadəçiyə görünür — bax [AsmaNameEditDialog].
    // Süzgəc həm siyahıda, həm də vərəqləyicidə eyni olmalıdır, yoxsa «növbəti» düyməsi
    // oxucunun görmədiyi ada aparardı.
    val available = remember(names, isAuthorized) {
        if (isAuthorized) names else names.filter { it.is_visible }
    }

    val opened = openedNo
    if (opened != null && available.isNotEmpty()) {
        AsmaDetailPager(
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
                            evidenceCount = counts[name.no] ?: 0,
                            onClick = { openedNo = name.no },
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
    evidenceCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.5f)),
        // Gizlədilmiş ad solğun çəkilir: admin siyahıda onu adi adlardan bir baxışda ayırsın.
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (name.is_visible) 1f else 0.55f)
            .clickable(onClick = onClick),
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
                    text = name.no.toString(),
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

/**
 * Adın detalı — yuxarıda ad, altında mənası, daha aşağıda ona dəlil olan ayə və hədislər.
 *
 * Səhifələr **bütün 99 ad** üzərindədir, yalnız seçilmiş ad üzərində deyil: istifadəçi siyahıya
 * qayıtmadan qonşu ada keçə bilsin.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AsmaDetailPager(
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

    var sourceRef by remember { mutableStateOf<DuaSourceRef?>(null) }
    var pendingDelete by remember { mutableStateOf<AsmaEvidence?>(null) }
    var editingEvidence by remember { mutableStateOf<AsmaEvidence?>(null) }
    var editingName by remember { mutableStateOf<AsmaName?>(null) }

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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { page ->
                val name = names[page]

                // Səhifə görünəndə həmin adın dəlilləri yüklənir (artıq yüklənibsə heç nə etmir).
                LaunchedEffect(name.no) { onEnsure(name.no) }

                AsmaDetailPage(
                    name = name,
                    evidence = evidenceOf(name.no),
                    isLoading = isLoadingEvidence(name.no),
                    isAuthorized = isAuthorized,
                    onOpenSource = { sourceRef = it },
                    onEdit = { editingEvidence = it },
                    onDelete = { pendingDelete = it },
                )
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
    evidence: List<AsmaEvidence>,
    isLoading: Boolean,
    isAuthorized: Boolean,
    onOpenSource: (AsmaEvidence) -> Unit,
    onEdit: (AsmaEvidence) -> Unit,
    onDelete: (AsmaEvidence) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                        text = asmaNumberLabel(name.no),
                        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }

                Text(
                    text = name.name_ar,
                    style = typography.displaySmall.copy(
                        fontSize = 40.sp,
                        lineHeight = 40.sp * 1.5,
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
                    onOpenSource = { onOpenSource(item) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) },
                )
            }
        }
    }
}

/** Bir dəlil — ərəbcə çıxarış, tərcüməsi, istinadı və qaynağa keçid. */
@Composable
private fun AsmaEvidenceCard(
    evidence: AsmaEvidence,
    isAuthorized: Boolean,
    onOpenSource: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = colorScheme.surfaceContainerLow.alpha(0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = evidence.text_ar,
                style = typography.titleMedium.copy(
                    fontSize = 20.sp,
                    lineHeight = 20.sp * 1.9,
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
            evidence.text_az.takeIf { it.isNotBlank() }?.let { translation ->
                Text(
                    text = translation,
                    style = typography.bodyMedium.copy(lineHeight = 15.sp * 1.65)
                        .withScriptDirection(arabic = false),
                    color = colorScheme.onSurface.alpha(0.9f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                evidence.source?.takeIf { it.isNotBlank() }?.let { source ->
                    Text(
                        text = "— $source",
                        style = typography.labelSmall.withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.alpha(0.7f),
                        modifier = Modifier.weight(1f),
                    )
                } ?: Spacer(Modifier.weight(1f))

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
