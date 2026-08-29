package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.utils.reader.ReaderScrollStep
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.reader.ComposeUiConfig
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.TextBuilderParams
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelPageNo
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerial
import org.jetbrains.compose.resources.stringResource

/*
 * Kitab rejimi — ayə-ayə oxucusunun səhifə-səhifə düzülüşü.
 *
 * Adi ayə-ayə rejimində hər ayə öz kartındadır: çərçivə, nömrə nişanı, üstündə oynat/paylaş/əlfəcin
 * sırası — uzun oxumada bunlar səhifəni doğrayır və mətn sonsuz bir siyahı kimi axır. Kitab rejimi
 * eyni məzmunu — **ərəbcəsi, tərcüməsi və qeydi** — çap olunmuş tərcümə kitabı kimi verir: müshəf
 * səhifəsi başına bir səhifə, kartsız, vərəqlənən.
 *
 * Səhifələmə tərcümə rejimindəki ilə eyni mənbədən gəlir (müshəf xəritəsi, eyni səhifə nömrələri),
 * məzmunu isə ayə-ayə rejimin öz elementləridir ([ReaderItemsBuilder.buildBookPages]) — ərəbcə,
 * atlas/təcvid və tərcümə boru xətti burada təkrarlanmır, [VerseView]-un çağırdığı eyni
 * [QuranTextWbw] və [TranslationText] işlədilir, sadəcə kart və əməllər sırası olmadan.
 */
/**
 * Kitab səhifəsinin sağ-sol kənar boşluğu.
 *
 * [QuranTextWbw] və [TranslationText] öz 8dp daxili boşluqlarını gətirir; bu, onların üstünə səhifə
 * kənarını əlavə edir. Kart rejimindəki 20dp-dən dardır: burada çərçivə yoxdur, ona görə mətn
 * kənara «yapışmır», geniş boşluq isə uzun ərəbcə sətri vaxtsız qırır.
 */
private val BookPageMargin = 6.dp

/**
 * Bir kitab səhifəsi.
 *
 * [isScrollable] üfüqi vərəqləmə üçündür: orada səhifə öz içində sürüşür (sürüşmə vəziyyəti
 * kənardan verilir ki, düymə ilə səhifələmə onu addımlaya bilsin). Şaquli düzülüşdə isə səhifələr
 * bir `LazyColumn`-un elementləridir və öz-özünə sürüşmür.
 */
@Composable
fun BookPageContent(
    pageItem: BookPageItem,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    externalScrollState: ScrollState? = null,
    bottomInset: Dp = 0.dp,
) {
    val scrollState = externalScrollState ?: rememberScrollState()
    val items = pageItem.prepared.items

    // Səhifənin sonundakı bölmə nişanı həmişə «Səhifə N sonu»-dur: burada səhifənin özü sərhəddir,
    // ona görə nişan başlıqdakı nömrəni təkrarlayır. İçəridəki ruku/rüb nişanları qalır.
    val visibleItems = remember(items) {
        if (items.lastOrNull() is ReaderLayoutItem.SectionMarker) items.dropLast(1) else items
    }

    TextStyleProvider(pageItem.prepared.textStyles) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier)
                // Üzən xrom (oxu zolağı, mini pleyer) səhifənin **üstündədir**: mətn onun altından
                // sürüşür, ona görə yer qısaltma ilə yox, boşluqla açılır.
                .padding(bottom = if (isScrollable) 100.dp + bottomInset else bottomInset),
        ) {
            BookPageHeader(pageItem)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = colorScheme.outlineVariant.alpha(0.5f),
            )

            visibleItems.forEach { item ->
                BookPageRow(item)
            }
        }
    }
}

/** Səhifə başlığı — kart rejimindəki ilə eyni məlumat: səhifə nömrəsi və səhifədəki surələr. */
@Composable
private fun BookPageHeader(pageItem: BookPageItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.strLabelPageNo, pageItem.pageNo),
            style = typography.labelLarge,
            color = colorScheme.primary.alpha(0.8f),
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = pageItem.chapterNames,
            style = typography.labelLarge,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BookPageRow(item: ReaderLayoutItem) {
    when (item) {
        is ReaderLayoutItem.Bismillah -> Bismillah()
        is ReaderLayoutItem.ChapterTitle -> ChapterTitle(item.chapterNo)
        is ReaderLayoutItem.SectionMarker -> BookSectionMarker(item)
        is ReaderLayoutItem.VerseUI -> BookVerseBlock(item)

        // Kitab səhifəsində yeri yoxdur: surə haqqında kart ayə-ayə rejimin başlığıdır, «günün
        // ayəsi» nişanı isə oxu axınına düşən xromdur.
        is ReaderLayoutItem.ChapterInfo,
        is ReaderLayoutItem.IsVotd -> Unit
    }
}

/** Ayə bloku: ərəbcə → nömrə → tərcümə və qeyd. */
@Composable
private fun BookVerseBlock(verseUi: ReaderLayoutItem.VerseUI) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BookPageMargin, vertical = 8.dp),
    ) {
        QuranTextWbw(verseUi = verseUi)

        // Nömrə tərcümənin üstündədir, ərəbcənin yox: ərəbcə mətn onsuz da ayə sonu nişanını
        // daşıyır, tərcümə isə nömrəsiz axında hansı ayəyə aid olduğunu itirir. Surə nömrəsi ilə
        // birlikdə, çünki bir səhifədə bir neçə surə ola bilər.
        Text(
            text = stringResource(
                Res.string.strLabelVerseSerial,
                verseUi.verse.chapterNo,
                verseUi.verse.verseNo,
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = typography.labelMedium,
            color = colorScheme.primary.alpha(0.7f),
        )

        TranslationText(verseUi = verseUi)

        if (verseUi.showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = colorScheme.outlineVariant.alpha(0.35f),
            )
        }
    }
}

/** Səhifə daxilindəki ruku/rüb sərhədləri — ayə-ayə rejimin bəzəkli nişanının sadə qarşılığı. */
@Composable
private fun BookSectionMarker(marker: ReaderLayoutItem.SectionMarker) {
    if (marker.text.isEmpty()) return

    Text(
        text = marker.text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        style = typography.labelSmall,
        color = colorScheme.onSurface.alpha(0.6f),
        textAlign = TextAlign.Center,
    )
}


/**
 * Kitab səhifələrinin qurma parametrləri — ayə-ayə rejimin siyahısını quran [TextBuilderParams]-ın
 * eynisi, ona görə iki düzülüş eyni şrift, təcvid və tərcümə ayarlarını görür.
 */
@Composable
private fun rememberBookPageBuilderParams(readerVm: ReaderViewModel): TextBuilderParams {
    val colors = colorScheme
    val type = typography
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val isDark = ThemeUtils.observeDarkTheme()
    val verseActions = LocalVerseActions.current

    val arabicEnabled = ReaderPreferences.observeArabicTextEnabled()
    val script = ReaderPreferences.observeQuranScript()
    val arabicSize = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translationSize = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val slugs = ReaderPreferences.observeTranslations()
    val highlightParentheses = ReaderPreferences.observeTranslHighlightParentheses()
    val showParentheses = ReaderPreferences.observeTranslShowParentheses()
    val tajweedEnabled = ReaderPreferences.observeTajweedColorsEnabled()

    val params = remember(
        colors, type, density, textMeasurer, isDark, verseActions, readerVm,
        arabicEnabled, script, arabicSize, translationSize, slugs,
        highlightParentheses, showParentheses, tajweedEnabled,
    ) {
        TextBuilderParams(
            uiConfig = ComposeUiConfig(
                textMeasurer = textMeasurer,
                density = density,
                colors = colors,
                type = type,
                isDark = isDark,
            ),
            verseActions = verseActions,
            fontResolver = readerVm.fontResolver,
            arabicEnabled = arabicEnabled,
            script = script,
            arabicSizeMultiplier = arabicSize,
            translationSizeMultiplier = translationSize,
            slugs = slugs,
            highlightParentheses = highlightParentheses,
            showParentheses = showParentheses,
            tajweedColorsEnabled = tajweedEnabled,
        )
    }

    return params
}


/**
 * Kitab rejiminin oxucusu — müshəf səhifələri üzrə vərəqləyici.
 *
 * Ayə-ayə rejiminin siyahısını əvəz edir (bax [ReaderLayout]); rejim tabı dəyişmir, ona görə app
 * bar, naviqator və düymələr eyni qalır. Vərəqləmə müshəf kimi sağdan soladır, ona görə səhifələr
 * RTL bükümündədir — səhifənin **içi** isə interfeysin öz istiqamətini geri alır.
 *
 * Səhifə düymələri (səs/klaviatura) ayə-ayə rejiminin axını olan `scrollEvent`-i dinləyir: əvvəlcə
 * səhifənin öz içində addımlayır, sonu çatanda vərəqləyir — müshəf və tərcümə səhifələrindəki
 * davranışın eynisi.
 */
@Composable
fun ReaderLayoutBookPageMode(
    readerVm: ReaderViewModel,
    nestedScrollConnection: NestedScrollConnection?,
    bottomChromeInset: Dp = 0.dp,
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val bookPageItems by readerVm.bookPageItems.collectAsState()
    val pageCount = mushafSession.pageCount

    val buildParams = rememberBookPageBuilderParams(readerVm)

    // RTL bükümündən əvvəl oxunur: səhifə axını sağdan soladır, səhifədəki tərcümə mətni isə
    // interfeysin öz istiqamətindədir.
    val appLayoutDirection = LocalLayoutDirection.current

    val initialPageIndex = mushafSession.currentPageNo?.minus(1)?.coerceAtLeast(0) ?: 0

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { pageCount },
    )

    val scrollStates = remember { mutableMapOf<Int, ScrollState>() }

    val navigateToPage by readerVm.navigateToPage.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToPage, pageCount) {
        val targetPage = navigateToPage ?: return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect

        val clamped = targetPage.coerceIn(1, pageCount)
        try {
            pagerState.scrollToPage(clamped - 1)
            readerVm.updateCurrentPageNo(clamped)
            readerVm.updateLastKnownVerseFromTranslationPage(clamped)
        } finally {
            readerVm.consumePageNavigation()
        }
    }

    LaunchedEffect(pagerState, buildParams, mushafSession.version) {
        snapshotFlow {
            listOf(
                pagerState.currentPage + 1,
                pagerState.targetPage + 1,
                pagerState.settledPage + 1,
            )
        }
            .onStart { emit(listOf(mushafSession.currentPageNo ?: 1)) }
            .distinctUntilChanged()
            .collect { anchorPages ->
                readerVm.fetchBookPages(anchorPages, buildParams)
            }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { idx ->
                // Gözlənilən naviqasiya varsa mövqe hələ oturmayıb: onu izləmək lövbəri korlayır.
                if (readerVm.navigateToPage.value != null) return@collect
                readerVm.updateCurrentPageNo(idx + 1)
                readerVm.updateLastKnownVerseFromTranslationPage(idx + 1)
            }
    }

    val stepPercent = AppPreferences.observeReaderScrollStepPercent()

    LaunchedEffect(pagerState, stepPercent) {
        readerVm.scrollEvent.collect { direction ->
            val currentPageIdx = pagerState.currentPage
            val scrollState = scrollStates[currentPageIdx]
            val step = ReaderScrollStep.stepPx(scrollState?.viewportSize ?: 0, stepPercent)

            if (scrollState != null && step > 0f) {
                if (direction > 0) {
                    if (scrollState.value < scrollState.maxValue) {
                        scrollState.animateScrollTo(
                            (scrollState.value + step.toInt()).coerceAtMost(scrollState.maxValue),
                            animationSpec = ReaderScrollStep.animationSpec,
                        )
                    } else if (currentPageIdx < pageCount - 1) {
                        pagerState.animateScrollToPage(currentPageIdx + 1)
                    }
                } else {
                    if (scrollState.value > 0) {
                        scrollState.animateScrollTo(
                            (scrollState.value - step.toInt()).coerceAtLeast(0),
                            animationSpec = ReaderScrollStep.animationSpec,
                        )
                    } else if (currentPageIdx > 0) {
                        pagerState.animateScrollToPage(currentPageIdx - 1)
                    }
                }
            } else if (direction > 0 && currentPageIdx < pageCount - 1) {
                pagerState.animateScrollToPage(currentPageIdx + 1)
            } else if (direction < 0 && currentPageIdx > 0) {
                pagerState.animateScrollToPage(currentPageIdx - 1)
            }
        }
    }

    val pageTurnAnimation = AppPreferences.observeReaderPageTurnAnimation()
    val pageGround = ReaderMode.groundColor(ReaderMode.VerseByVerse)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (nestedScrollConnection == null) Modifier
                    else Modifier.nestedScroll(nestedScrollConnection)
                ),
            beyondViewportPageCount = 1,
        ) { pageIdx ->
            val pageItem = bookPageItems[pageIdx + 1]

            // Effekt RTL bükümünün **içindədir**: üfüqi transformasiyalar vərəqləmənin öz
            // istiqamətini izləməlidir, səhifə mətninin istiqamətini yox.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pageTurnEffect(pageTurnAnimation, pagerState, pageIdx, pageGround),
            ) {
                if (pageItem != null) {
                    val scrollState = scrollStates.getOrPut(pageIdx) { ScrollState(0) }

                    CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                        BookPageContent(
                            pageItem = pageItem,
                            modifier = Modifier.fillMaxWidth(),
                            isScrollable = true,
                            externalScrollState = scrollState,
                            bottomInset = bottomChromeInset,
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
