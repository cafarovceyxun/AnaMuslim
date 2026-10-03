package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.produceState
import com.cafarovceyxun.anamuslim.compose.components.reference.ReferencePeek
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strLabelOpen
import com.cafarovceyxun.anamuslim.resources.strLabelQuranOnly
import com.cafarovceyxun.anamuslim.resources.strLabelQuranPrefix
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderLayoutItem
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderTextZoom
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomFeedback
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomFeedbackOverlay
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderZoomTarget
import com.cafarovceyxun.anamuslim.compose.components.reader.readerTextZoom
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderPreparedData
import com.cafarovceyxun.anamuslim.compose.components.reader.TextStyleProvider
import com.cafarovceyxun.anamuslim.compose.components.reader.VerseView
import com.cafarovceyxun.anamuslim.compose.extensions.bottomBorder
import com.cafarovceyxun.anamuslim.compose.theme.LegacyColors
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.repository.UserRepository
import com.cafarovceyxun.anamuslim.utils.reader.ComposeUiConfig
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderProvider
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.ReaderItemsBuilder
import com.cafarovceyxun.anamuslim.utils.reader.TextBuilderParams
import com.cafarovceyxun.anamuslim.utils.univ.RegexPattern
import com.cafarovceyxun.anamuslim.viewModels.ReaderProviderViewModel
import com.cafarovceyxun.anamuslim.utils.reader.VerseHighlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

data class QuickReferenceData(
    val slugs: Set<String>,
    val chapterNo: Int,
    val verses: String? = null,
    val parsedVerses: QuickReferenceVerses? = null,
    /**
     * Axtarışdan gəlirsə sorğu: tərcümədə tapılan sözlər vərəqdə də sarı fonla işarələnir — hədis
     * nəticəsindəki davranışın eynisi ([com.cafarovceyxun.anamuslim.compose.components.search.HadithQuickReferenceContent]).
     */
    val query: String? = null,
    /**
     * Ərəbcə mətndə işarələnəcək hissə, sorğudan fərqli olanda — Əsmada adın özü və ya dəlilin
     * çıxarışı. Null olanda ərəbcə [query] işlənir (axtarış).
     */
    val arabicHighlight: VerseHighlight? = null,
)

sealed class QuickReferenceVerses(open val chapterNo: Int) {
    data class Range(override val chapterNo: Int, val range: IntRange) :
        QuickReferenceVerses(chapterNo)

    data class Discrete(override val chapterNo: Int, val verseNos: List<Int>) :
        QuickReferenceVerses(chapterNo)

    data class ChapterOnly(override val chapterNo: Int) : QuickReferenceVerses(chapterNo)
}

fun parseVerses(chapterNo: Int, versesStr: String): QuickReferenceVerses {
    if (versesStr.isBlank()) return QuickReferenceVerses.ChapterOnly(chapterNo)

    val parts = versesStr.split(",")
    if (parts.size > 1) {
        val ints = parts.mapNotNull { it.trim().toIntOrNull() }.sorted()
        return QuickReferenceVerses.Discrete(chapterNo, ints)
    }

    val matcher = RegexPattern.VERSE_RANGE_PATTERN.find(versesStr)
    if (matcher != null && matcher.groupValues.size >= 3) {
        val from = matcher.groupValues[1].toInt()
        val to = matcher.groupValues[2].toInt()
        return QuickReferenceVerses.Range(chapterNo, from..to)
    }

    val single = versesStr.trim().toIntOrNull()

    return if (single != null) QuickReferenceVerses.Range(chapterNo, single..single)
    else QuickReferenceVerses.ChapterOnly(chapterNo)
}

/**
 * Qonşu istinada keçmək üçün barmağın neçə dp getməli olduğu.
 *
 * Toxunuş sürüşmə həddindən (~ 8–16dp) xeyli böyükdür: vərəqin içi şaquli sürüşür və mətn seçimi
 * var, yəni kiçik hədd oxuyarkən təsadüfən ayəni dəyişərdi.
 */
private val QuickReferenceSwipeThreshold = 64.dp

private fun parsedVersesToList(parsed: QuickReferenceVerses): List<Int> = when (parsed) {
    is QuickReferenceVerses.Range -> parsed.range.toList()
    is QuickReferenceVerses.Discrete -> parsed.verseNos
    is QuickReferenceVerses.ChapterOnly -> emptyList()
}

private fun parsedVersesToIntRange(parsed: QuickReferenceVerses): IntRange? = when (parsed) {
    is QuickReferenceVerses.Range -> parsed.range
    is QuickReferenceVerses.Discrete -> {
        if (parsed.verseNos.isNotEmpty()) parsed.verseNos.min()..parsed.verseNos.max()
        else null
    }

    is QuickReferenceVerses.ChapterOnly -> null
}

private fun formatTitle(
    prefix: String,
    quranOnlyLabel: String,
    chapterNo: Int,
    parsed: QuickReferenceVerses,
): String {
    return when (parsed) {
        is QuickReferenceVerses.Range -> {
            if (parsed.range.first == parsed.range.last) "$prefix${parsed.range.first}"
            else "$prefix${parsed.range.first}-${parsed.range.last}"
        }

        is QuickReferenceVerses.Discrete -> {
            prefix + parsed.verseNos.joinToString(", ")
        }

        is QuickReferenceVerses.ChapterOnly -> "$quranOnlyLabel $chapterNo"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReference(
    data: QuickReferenceData?,
    onOpenInReader: (chapterNo: Int, range: IntRange) -> Unit,
    onClose: () -> Unit,
) {
    if (data == null) return

    val parsed = remember(data) {
        if (data.parsedVerses != null) {
            data.parsedVerses
        } else if (data.verses != null) {
            parseVerses(data.chapterNo, data.verses)
        } else {
            null
        }
    }

    if (parsed == null) {
        return
    }

    if (parsed is QuickReferenceVerses.ChapterOnly) {
        ChapterOnlyDialog(
            chapterNo = data.chapterNo,
            slugs = data.slugs,
            onOpen = {
                onOpenInReader(data.chapterNo, 1..Int.MAX_VALUE)
                onClose()
            },
            onClose = onClose,
        )
        return
    }

    // Vərəq **yarıda** açılır: ayə qısadırsa ekranın hamısını tutmasının mənası yoxdur, arxadakı
    // siyahı da görünür. Yuxarı çəkəndə tam hündürlüyə (85%) qalxır — ona görə `skipPartiallyExpanded`
    // söndürülüb, `fillMaxHeight` isə yenə 85%-dir (o, sheet-in **maksimumudur**, açılış hündürlüyü deyil).
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Sağa-sola sürüşdürmə **surənin qonşu ayəsinə** keçir (1 → 2, 2 → 1): vərəq oxucudan, surə
    // məlumatından və axtarışın sürətli keçidindən açılır, orada «növbəti» elə növbəti ayədir.
    // Siyahı boyu keçid (axtarış nəticələri, Əsma dəlilləri) ortaq vərəqdədir — [ReferencePeek].
    var shown by remember(parsed) { mutableStateOf(parsed) }
    val verseCount by produceState<Int?>(null, parsed.chapterNo) {
        value = RepositoryProvider.quranRepository.getSurah(parsed.chapterNo)?.ayahCount
    }
    val step: (Int) -> Unit = { direction ->
        neighbourVerse(shown, direction, verseCount)?.let { shown = it }
    }

    ReaderProvider {
        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = sheetState,
            scrimColor = colorScheme.scrim.alpha(0.5f),
            containerColor = colorScheme.surface,
            contentColor = colorScheme.onSurface,
            dragHandle = null,
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Top) },
        ) {
            // Barmağın istiqaməti ilə yeni ayənin gəldiyi tərəf üst-üstə düşsün — [ReferencePeek]-in
            // keçidi ilə eyni.
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    val forward = targetState.firstVerse() > initialState.firstVerse()
                    (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it else it } + fadeOut()) using
                        SizeTransform(clip = true)
                },
                label = "quickReferenceVerse",
            ) { verses ->
                QuickReferenceContent(
                    data = data,
                    parsed = verses,
                    onStep = step,
                    onOpenInReader = onOpenInReader,
                    onClose = onClose,
                    position = verseCount?.let { count -> "${verses.firstVerse()} / $count" },
                )
            }
        }
    }
}

/** İstinadın ilk ayəsi — sayğac və keçidin istiqaməti üçün. */
private fun QuickReferenceVerses.firstVerse(): Int = when (this) {
    is QuickReferenceVerses.Range -> range.first
    is QuickReferenceVerses.Discrete -> verseNos.firstOrNull() ?: 0
    is QuickReferenceVerses.ChapterOnly -> 0
}

/**
 * [current]-dən [direction] tərəfdəki tək ayə; surənin sərhədində null.
 *
 * Aralıq istinadından (2:255-257) irəli addım aralığın **sonrakı** ayəsinə (258), geri addım
 * əvvəlkinə (254) düşür — aralığın ortasına qayıtmaq mənasızdır. [verseCount] hələ oxunmayıbsa
 * irəli addım atılmır: surənin sonundan kənara çıxmaq olmaz.
 */
internal fun neighbourVerse(current: QuickReferenceVerses, direction: Int, verseCount: Int?): QuickReferenceVerses? {
    val (first, last) = when (current) {
        is QuickReferenceVerses.Range -> current.range.first to current.range.last
        is QuickReferenceVerses.Discrete -> (current.verseNos.minOrNull() ?: return null) to current.verseNos.max()
        is QuickReferenceVerses.ChapterOnly -> return null
    }
    val next = if (direction > 0) last + 1 else first - 1
    if (next < 1) return null
    if (direction > 0 && (verseCount == null || next > verseCount)) return null
    return QuickReferenceVerses.Range(current.chapterNo, next..next)
}

/**
 * Vərəqin içi, `ModalBottomSheet`-siz — axtarış nəticələrinin ortaq vərəqi
 * ([com.cafarovceyxun.anamuslim.compose.components.reference.ReferencePeek]) ayə ilə hədis arasında
 * keçəndə bunu öz vərəqinin içində göstərir. [ReaderProvider]-in içində çağırılmalıdır.
 *
 * @param position başlığın altındakı «4 / 25» — siyahıda yeri; null = sayğac yoxdur.
 */
@Composable
internal fun QuickReferenceContent(
    data: QuickReferenceData,
    parsed: QuickReferenceVerses,
    /** Üfüqi sürüşdürmə: `+1` sonrakı, `-1` əvvəlki ayəyə. Sərhəddə çağırış heç nə etmir. */
    onStep: (Int) -> Unit,
    onOpenInReader: (Int, IntRange) -> Unit,
    onClose: () -> Unit,
    position: String? = null,
) {
    val viewModel = viewModel { ReaderProviderViewModel() }

    val textMeasurer = rememberTextMeasurer()
    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val type by rememberUpdatedState(MaterialTheme.typography)
    val density = LocalDensity.current
    val isDark = ThemeUtils.observeDarkTheme()

    val verseActions = LocalVerseActions.current

    // ⚠️ Surə nömrəsi **göstərilən istinaddan** oxunur, [data]-dan yox: sürüşdürmə jesti qonşu
    // elementə keçir və qonşu **başqa surədən** ola bilər (Əsmada avtomatik ayələr bütün Quran
    // boyu yayılır). `data.chapterNo` yalnız vərəqin açıldığı ilk elementi bildirir.
    val chapterNo = parsed.chapterNo
    val verseNos = remember(parsed) { parsedVersesToList(parsed) }
    val verseRange = remember(parsed) { parsedVersesToIntRange(parsed) }
    val quranPrefix = stringResource(Res.string.strLabelQuranPrefix, chapterNo.toString())
    val quranOnlyLabel = stringResource(Res.string.strLabelQuranOnly)
    val title = remember(quranPrefix, quranOnlyLabel, chapterNo, parsed) {
        formatTitle(quranPrefix, quranOnlyLabel, chapterNo, parsed)
    }

    var prepared by remember { mutableStateOf<ReaderPreparedData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val isBookmarked by if (verseRange != null) {
        viewModel.userRepository
            .isBookmarkedFlow(chapterNo, verseRange)
            .collectAsStateWithLifecycle(false)
    } else {
        remember { mutableStateOf(false) }
    }

    // ⚠️ Ölçü çarpanları **müşahidə olunur**, `get` ilə bir dəfə oxunmur: zoom jesti ayarı dəyişir,
    // vərəq isə hazır qurulmuş sətirləri göstərir — açar siyahısında olmasaydı barmaq açılırdı,
    // ekranda heç nə olmurdu (kompilyator da, testlər də susurdu).
    val arabicSizeMultiplier = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translationSizeMultiplier = ReaderPreferences.observeTranlationTextSizeMultiplier()

    // ⚠️ `parsed` də açardır: sürüşdürmə jesti `data`-nı deyil, göstərilən aralığı dəyişir — o,
    // açar siyahısında olmasaydı başlıq yeni ayəni yazar, mətn isə köhnəsində qalardı.
    LaunchedEffect(
        data, parsed, verseActions, colors, type, density, isDark,
        arabicSizeMultiplier, translationSizeMultiplier,
    ) {
        isLoading = true

        withContext(Dispatchers.IO) {
            val params = TextBuilderParams(
                uiConfig = ComposeUiConfig(
                    colors = colors,
                    type = type,
                    density = density,
                    textMeasurer = textMeasurer,
                    isDark = isDark,
                ),
                fontResolver = viewModel.fontResolver,
                verseActions = verseActions,
                arabicEnabled = ReaderPreferences.getArabicTextEnabled(),
                script = ReaderPreferences.getQuranScript(),
                arabicSizeMultiplier = arabicSizeMultiplier,
                translationSizeMultiplier = translationSizeMultiplier,
                slugs = data.slugs.takeIf { it.isNotEmpty() }
                    ?: ReaderPreferences.getTranslations(),
                searchQuery = data.query?.takeIf { it.isNotBlank() },
            )

            prepared = ReaderItemsBuilder.buildQuickReferenceItems(
                params, chapterNo, verseNos,
                arabicHighlight = data.arabicHighlight
                    ?: data.query?.takeIf { it.isNotBlank() }?.let { VerseHighlight.Query(it) },
            )
        }
        isLoading = false
    }

    var zoomFeedback by remember { mutableStateOf<ReaderZoomFeedback?>(null) }
    val zoomScope = rememberCoroutineScope()
    val zoomModifier = Modifier.readerTextZoom(
        enabled = AppPreferences.observeReaderPinchZoomEnabled(),
        arabicMultiplier = arabicSizeMultiplier,
        translationMultiplier = translationSizeMultiplier,
        minMultiplier = ReaderTextZoom.QURAN_MIN,
        maxMultiplier = ReaderTextZoom.QURAN_MAX,
        onZoom = { target, value ->
            zoomFeedback = ReaderZoomFeedback(target, value)
            zoomScope.launch {
                when (target) {
                    ReaderZoomTarget.Arabic -> ReaderPreferences.setArabicTextSizeMultiplier(value)
                    ReaderZoomTarget.Translation ->
                        ReaderPreferences.setTranslationTextSizeMultiplier(value)
                }
            }
        },
    )

    Box {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .quickReferenceSwipe(onStep),
    ) {
        QuickReferenceHeader(
            title = title,
            position = position,
            isBookmarked = isBookmarked,
            showActions = verseRange != null && !isLoading,
            onBookmark = {
                if (verseRange == null) return@QuickReferenceHeader
                verseActions.onBookmarkRequest?.invoke(chapterNo, verseRange)
            },
            onOpen = {
                if (verseRange != null) {
                    onOpenInReader(chapterNo, verseRange)
                    onClose()
                }
            },
            onClose = onClose,
        )


        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val verseRows = prepared?.items.orEmpty().filterIsInstance<ReaderLayoutItem.VerseUI>()

            TextStyleProvider(prepared?.textStyles ?: emptyMap()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        // İki barmaq tərcüməni, üç barmaq ərəbcəni ölçüləndirir — oxucudakı jest
                        // lüğətinin eynisi. Vərəq oxucunun öz mətnini göstərdiyi üçün ayar da
                        // oxucununkudur (`ReaderPreferences`), duanınkı deyil.
                        .then(zoomModifier),
                ) {
                    itemsIndexed(
                        verseRows,
                        key = { _, item -> item.key }
                    ) { index, verseUi ->
                        VerseViewWrapped(
                            viewModel.userRepository,
                            verseUi = verseUi,
                            showDivier = index < verseRows.lastIndex,
                        )
                    }
                }
            }
        }
    }

        // «Ərəbcə · 120%» yazısı — jestin nəyi dəyişdiyini deyir.
        ReaderZoomFeedbackOverlay(zoomFeedback) { zoomFeedback = null }
    }
}

/**
 * Üfüqi sürüşdürmə → qonşu element (`+1` sonrakı, `-1` əvvəlki). Ayə vərəqi və axtarış nəticələrinin
 * ortaq vərəqi eyni jesti işlədir.
 *
 * ⚠️ Jest **xarici sütuna** qoyulur, siyahının özünə yox: siyahı şaquli sürüşür və ölçüləndirmə
 * jestini ([readerTextZoom]) daşıyır, ikisi də üfüqi hərəkəti udmur, ona görə valideyn onu sərbəst
 * tutur. Addım `onDragEnd`-dədir, sürükləmə boyu yox — element hər 60dp-də bir dəyişsəydi bir jestlə
 * bir neçə element keçilərdi.
 */
@Composable
internal fun Modifier.quickReferenceSwipe(onStep: (Int) -> Unit): Modifier {
    val stepThresholdPx = with(LocalDensity.current) { QuickReferenceSwipeThreshold.toPx() }
    val currentStep by rememberUpdatedState(onStep)

    return pointerInput(stepThresholdPx) {
        var dragged = 0f

        detectHorizontalDragGestures(
            onDragStart = { dragged = 0f },
            onDragEnd = {
                // Sola çəkmək = irəli (növbəti element) — vərəqləyicilərdəki eyni istiqamət.
                if (dragged <= -stepThresholdPx) currentStep(1)
                else if (dragged >= stepThresholdPx) currentStep(-1)
            },
            onDragCancel = { dragged = 0f },
        ) { _, dragAmount -> dragged += dragAmount }
    }
}

/**
 * Başlıq altındakı «4 / 25» — vərəqin sürüşdürülə bilən siyahının bir elementi olduğunu göstərir.
 * Hədis vərəqinin başlığı da bunu işlədir.
 */
@Composable
internal fun QuickReferencePosition(position: String) {
    Text(
        text = position,
        style = typography.labelSmall,
        color = colorScheme.onSurfaceVariant.alpha(0.75f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun VerseViewWrapped(
    bookmarksRepo: UserRepository,
    verseUi: ReaderLayoutItem.VerseUI,
    showDivier: Boolean
) {
    val verse = verseUi.verse

    val isBookmarked by bookmarksRepo
        .isBookmarkedFlow(verse.chapterNo, verse.verseNo..verse.verseNo)
        .collectAsStateWithLifecycle(false)

    VerseView(
        verseUi = verseUi,
        isBookmarked = isBookmarked,
        showDivider = showDivier,
    )
}

@Composable
private fun QuickReferenceHeader(
    title: String,
    position: String?,
    isBookmarked: Boolean,
    showActions: Boolean,
    onBookmark: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val iconTint = colorScheme.onSurface.alpha(0.7f)
    val bookmarkTint = if (isBookmarked) LegacyColors.brandPrimary
    else iconTint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bottomBorder()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelClose),
                tint = colorScheme.onSurface,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            position?.let { QuickReferencePosition(it) }
        }

        if (showActions) {
            IconButton(onClick = onBookmark) {
                Icon(
                    painter = painterResource(
                        if (isBookmarked) Res.drawable.ic_bookmark_added
                        else Res.drawable.ic_bookmark
                    ),
                    contentDescription = stringResource(Res.string.strLabelBookmark),
                    tint = bookmarkTint,
                )
            }

            IconButton(onClick = onOpen) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_open),
                    contentDescription = stringResource(Res.string.strLabelOpen),
                    tint = iconTint,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ChapterOnlyDialog(
    chapterNo: Int,
    slugs: Set<String>,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val repository = remember { RepositoryProvider.quranRepository }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            repository.getChapterName(chapterNo)
        }
    }

    AlertDialog(
        isOpen = true,
        onClose = onClose,
        title = stringResource(Res.string.strLabelOpen),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = onClose,
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelOpen),
                style = AlertDialogActionStyle.Primary,
                onClick = onOpen,
            ),
        ),
    ) {
        Text(
            text = chapterName.ifEmpty { "Surah $chapterNo" },
            style = typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
