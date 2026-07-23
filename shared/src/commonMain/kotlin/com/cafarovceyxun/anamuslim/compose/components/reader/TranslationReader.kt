package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelPageNo
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.commonFontFamily
import com.cafarovceyxun.anamuslim.compose.theme.platformTextStyle
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.TranslationPageBuilderParams
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.utils.quran.QuranGlyphs
import com.cafarovceyxun.anamuslim.compose.components.ChapterIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart


@Composable
fun ReaderLayoutTranslationPageMode(
    readerVm: ReaderViewModel,
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection?,
    externalListState: androidx.compose.foundation.lazy.LazyListState? = null
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val translationPageItems by readerVm.translationPageItems.collectAsState()
    val readerMode by readerVm.readerMode.collectAsState()
    val isVertical = readerMode == ReaderMode.TranslationVertical

    val pageCount = mushafSession.pageCount
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val verseActions = LocalVerseActions.current

    val translSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val highlightParentheses = ReaderPreferences.observeTranslHighlightParentheses()
    val showParentheses = ReaderPreferences.observeTranslShowParentheses()

    val buildParams = remember(
        colors, typography, verseActions, translSizeMult, highlightParentheses, showParentheses
    ) {
        TranslationPageBuilderParams(
            colors = colors,
            type = typography,
            verseActions = verseActions,
            translationSizeMultiplier = translSizeMult,
            highlightParentheses = highlightParentheses,
            showParentheses = showParentheses,
        )
    }

    val initialPageIndex = mushafSession.currentPageNo?.minus(1)?.coerceAtLeast(0) ?: 0
    val internalListState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)
    val listState = externalListState ?: internalListState

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { pageCount }
    )

    val scrollStates = remember { mutableMapOf<Int, ScrollState>() }

    val navigateToPage by readerVm.navigateToPage.collectAsStateWithLifecycle()

    // Translation UIs may only act while one of the translation modes is live; during a mode
    // switch this composable is still briefly composed and would steal navigations or corrupt
    // the cross-mode position anchor.
    fun isTranslationModeActive(): Boolean {
        val mode = readerVm.readerMode.value
        return mode == ReaderMode.Translation || mode == ReaderMode.TranslationVertical
    }

    LaunchedEffect(navigateToPage, pageCount) {
        val targetPage = navigateToPage ?: return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect
        if (!isTranslationModeActive()) return@LaunchedEffect

        val clamped = targetPage.coerceIn(1, pageCount)
        try {
            if (isVertical) {
                listState.scrollToItem(clamped - 1)
            } else {
                pagerState.scrollToPage(clamped - 1)
            }
            readerVm.updateCurrentPageNo(clamped)
            // Keep the cross-mode anchor in sync so navigator jumps carry the position across modes.
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
                readerVm.fetchTranslationPages(anchorPages, buildParams)
            }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { idx ->
                // Read the live navigation request; the lifecycle-aware state can freeze stale
                // across a mode switch and otherwise swallow manual swipes.
                if (readerVm.navigateToPage.value != null) return@collect
                if (!isTranslationModeActive()) return@collect
                readerVm.updateCurrentPageNo(idx + 1)
                readerVm.updateLastKnownVerseFromTranslationPage(idx + 1)
            }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (isVertical) {
            LaunchedEffect(listState, buildParams, mushafSession.version) {
                snapshotFlow {
                    val visible = listState.layoutInfo.visibleItemsInfo
                    if (visible.isEmpty()) listOf(listState.firstVisibleItemIndex + 1)
                    else visible.map { it.index + 1 }
                }
                    .onStart { emit(listOf(mushafSession.currentPageNo ?: 1)) }
                    .distinctUntilChanged()
                    .collect { anchorPages ->
                        readerVm.fetchTranslationPages(anchorPages, buildParams)
                    }
            }

            LaunchedEffect(listState) {
                snapshotFlow { listState.firstVisibleItemIndex }
                    .distinctUntilChanged()
                    .collect { idx ->
                        if (readerVm.navigateToPage.value != null) return@collect
                        if (!isTranslationModeActive()) return@collect
                        readerVm.updateCurrentPageNo(idx + 1)
                        readerVm.updateLastKnownVerseFromTranslationPage(idx + 1)
                    }
            }

            LaunchedEffect(Unit) {
                readerVm.smartScrollEvent.collect { direction ->
                    listState.animateScrollBy(direction * 400f)
                }
            }

            var autoScrollSpeed by readerVm.autoScrollSpeed
            LaunchedEffect(listState, autoScrollSpeed) {
                val speed = autoScrollSpeed

                while (speed != null && isVertical) {
                    listState.scrollBy(speed)
                    delay(16L)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier)
                    .then(
                        if (readerVm.isAutoScrollGestureMode.value) Modifier
                        else Modifier.pointerInput(autoScrollSpeed) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.pressed }) {
                                        autoScrollSpeed = null
                                    }
                                }
                            }
                        }
                    ),
                contentPadding = PaddingValues(bottom = 100.dp), // Some padding at bottom
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pageCount) { pageIdx ->
                    val pageItem = translationPageItems[pageIdx + 1]
                    if (pageItem != null) {
                        TranslationPageCard(
                            pageIdx = pageIdx,
                            pageItem = pageItem,
                            colors = colors,
                            typography = typography,
                            modifier = Modifier.fillMaxWidth(),
                            isScrollable = false
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        } else {
            LaunchedEffect(pagerState) {
                readerVm.smartScrollEvent.collect { direction ->
                    val currentPageIdx = pagerState.currentPage
                    val scrollState = scrollStates[currentPageIdx]

                    if (scrollState != null) {
                        if (direction > 0) { // Next / Scroll Down
                            if (scrollState.value < scrollState.maxValue) {
                                scrollState.animateScrollTo(
                                    (scrollState.value + 400).coerceAtMost(scrollState.maxValue)
                                )
                            } else if (currentPageIdx < pageCount - 1) {
                                pagerState.animateScrollToPage(currentPageIdx + 1)
                            }
                        } else { // Prev / Scroll Up
                            if (scrollState.value > 0) {
                                scrollState.animateScrollTo(
                                    (scrollState.value - 400).coerceAtLeast(0)
                                )
                            } else if (currentPageIdx > 0) {
                                pagerState.animateScrollToPage(currentPageIdx - 1)
                            }
                        }
                    } else {
                        // Fallback to simple page flipping if scrollState is not available
                        if (direction > 0 && currentPageIdx < pageCount - 1) {
                            pagerState.animateScrollToPage(currentPageIdx + 1)
                        } else if (direction < 0 && currentPageIdx > 0) {
                            pagerState.animateScrollToPage(currentPageIdx - 1)
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier),
                contentPadding = PaddingValues(horizontal = 0.dp),
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1
            ) { pageIdx ->
                val pageItem = translationPageItems[pageIdx + 1]
                if (pageItem != null) {
                    val scrollState = scrollStates.getOrPut(pageIdx) { ScrollState(0) }
                    TranslationPageCard(
                        pageIdx = pageIdx,
                        pageItem = pageItem,
                        colors = colors,
                        typography = typography,
                        modifier = Modifier.fillMaxWidth(),
                        isScrollable = true,
                        externalScrollState = scrollState
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun BismillahDecorative(
    colors: ColorScheme,
    typography: Typography,
    modifier: Modifier = Modifier
) {
    val translSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val baseFontSize = 36.sp
    val targetFontSize = (baseFontSize.value * translSizeMult).sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = QuranGlyphs.Special.BISMILLAH,
            style = typography.headlineLarge.copy(
                fontSize = targetFontSize,
                fontFamily = commonFontFamily(),
                platformStyle = platformTextStyle
            ),
            color = colors.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SurahHeaderDecorative(
    swl: SurahWithLocalizations,
    colors: ColorScheme,
    typography: Typography,
    modifier: Modifier = Modifier
) {
    val translSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val baseFontSize = 16.sp // Matching bodyLarge default
    val targetFontSize = (baseFontSize.value * translSizeMult + 2).sp
    val iconSize = (targetFontSize.value * 2.0f).sp

    val borderRadius = 12.dp
    val primaryColor = colors.primary

    Box(
        modifier = modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(borderRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.primaryContainer.alpha(0.2f),
                        colors.primaryContainer.alpha(0.1f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.alpha(0.3f),
                        primaryColor.alpha(0.1f)
                    )
                ),
                shape = RoundedCornerShape(borderRadius)
            )
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Surah Calligraphy Icon on Top
            ChapterIcon(
                chapterNo = swl.surah.surahNo,
                color = primaryColor,
                fontSize = iconSize,
                withPrefix = true
            )

            Spacer(Modifier.height(8.dp))

            // Surah Name below
            Text(
                text = swl.getCurrentName(),
                style = typography.titleLarge.copy(
                    fontSize = targetFontSize
                ),
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
private fun TranslationPageCard(
    pageIdx: Int,
    pageItem: TranslationPageItem,
    colors: ColorScheme,
    typography: Typography,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true,
    externalScrollState: ScrollState? = null
) {
    val scrollState = externalScrollState ?: rememberScrollState()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = colors.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outlineVariant.alpha(0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isScrollable) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(bottom = if (isScrollable) 100.dp else 0.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.strLabelPageNo, pageIdx + 1),
                    style = typography.labelLarge,
                    color = colors.primary.alpha(0.8f),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = pageItem.chapterNames,
                    style = typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp),
                color = colors.outlineVariant.alpha(0.5f)
            )

            pageItem.sections.forEach { section ->
                when (section) {
                    is TranslationPageSection.Text -> {
                        Text(
                            text = section.annotatedText,
                            style = typography.bodyLarge.copy(
                                lineHeight = 28.sp,
                                letterSpacing = 0.25.sp
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    is TranslationPageSection.Title -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SurahHeaderDecorative(
                                swl = section.swl,
                                colors = colors,
                                typography = typography
                            )
                        }
                    }

                    is TranslationPageSection.Bismillah -> {
                        BismillahDecorative(
                            colors = colors,
                            typography = typography,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }

                    is TranslationPageSection.Divider -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            color = colors.outlineVariant.alpha(0.3f)
                        )
                    }
                }
            }
        }
    }
}
