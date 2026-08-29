package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.WbwSheetData
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.ComposeUiConfig
import com.cafarovceyxun.anamuslim.utils.reader.MUSHAF_PAGE_HORIZONTAL_PADDING
import com.cafarovceyxun.anamuslim.utils.reader.PageBuilderParams
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasGlyphPlacement
import com.cafarovceyxun.anamuslim.utils.reader.atlas.LocalTajweedPalette
import com.cafarovceyxun.anamuslim.utils.reader.atlas.getForWord
import com.cafarovceyxun.anamuslim.utils.reader.atlas.glyphClassesForWord
import com.cafarovceyxun.anamuslim.utils.reader.mushafShowsRuledPageDecoration
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import com.cafarovceyxun.anamuslim.compose.extensions.verticalFadingEdge


private data class MushafPageMeasurementKey(
    val pageNo: Int,
    val contentWidthPx: Int,
    val density: Float,
    val fontScale: Float,
    val styleHash: Int,
)


@Composable
fun ReaderLayoutPageMode(
    readerVm: ReaderViewModel,
    contentWidth: Dp,
    nestedScrollConnection: NestedScrollConnection?,
    onSyncStateChanged: (Boolean) -> Unit = {},
    /** Room kept free under the last mushaf line for the chrome floating over it. See [ReaderLayout]. */
    bottomChromeInset: Dp = 0.dp,
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val pageItems by readerVm.pageItems.collectAsState()

    val sessionLayout = mushafSession.layout
    val pageCount = mushafSession.pageCount
    val ruledPageDecoration = mushafSession.layout.scriptCode.mushafShowsRuledPageDecoration()

    val pagerState = rememberPagerState(
        initialPage = mushafSession.currentPageNo?.let { it - 1 } ?: 0,
        pageCount = { pageCount },
    )

    val scrollStates = remember { mutableMapOf<Int, ScrollState>() }

    val textMeasurer = rememberTextMeasurer(cacheSize = 2048)
    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val typography by rememberUpdatedState(MaterialTheme.typography)
    val density = LocalDensity.current
    val isDark = ThemeUtils.observeDarkTheme()
    val tajweedColorsEnabled = ReaderPreferences.observeTajweedColorsEnabled()

    val pageBuilderParams =
        remember(colors, typography, textMeasurer, density, contentWidth, isDark, tajweedColorsEnabled) {
            PageBuilderParams(
                uiConfig = ComposeUiConfig(
                    colors = colors,
                    type = typography,
                    textMeasurer = textMeasurer,
                    density = density,
                    isDark = isDark,
                ),
                contentWidthPx = with(density) {
                    (contentWidth - MUSHAF_PAGE_HORIZONTAL_PADDING * 2).roundToPx()
                },
                isDark = isDark,
                tajweedColorsEnabled = tajweedColorsEnabled,
            )
        }

    LaunchedEffect(pagerState, pageBuilderParams, mushafSession.version) {
        snapshotFlow {
            listOf(
                pagerState.currentPage + 1,
                pagerState.targetPage + 1,
                pagerState.settledPage + 1,
            )
        }
            .onStart {
                emit(
                    listOf(mushafSession.currentPageNo ?: 1)
                )
            }
            .distinctUntilChanged()
            .collect { anchorPages ->
                readerVm.fetchMushafPages(
                    anchorPages, pageBuilderParams
                )
            }
    }

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
                // Fallback
                if (direction > 0 && currentPageIdx < pageCount - 1) {
                    pagerState.animateScrollToPage(currentPageIdx + 1)
                } else if (direction < 0 && currentPageIdx > 0) {
                    pagerState.animateScrollToPage(currentPageIdx - 1)
                }
            }
        }
    }

    val navigateToPage by readerVm.navigateToPage.collectAsStateWithLifecycle()

    // Note: [navigateToPage] is intentionally NOT a key here. Keying on it restarts this collector
    // on every programmatic navigation, and the guard below then reads the value captured at launch
    // — which goes stale after the navigation is consumed and silently swallows every manual swipe.
    LaunchedEffect(pagerState, pageCount) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { currentPage ->
                // Ignore page reports while a programmatic navigation is still pending; read the
                // *live* request so a just-consumed navigation doesn't leave manual swipes stuck.
                if (readerVm.navigateToPage.value != null) return@collect

                // During a mode switch this pager is still briefly composed; its reports would
                // overwrite the anchor the incoming mode is positioning from.
                if (readerVm.readerMode.value != ReaderMode.Reading) return@collect

                val currentPageNo = currentPage + 1

                readerVm.updateCurrentPageNo(currentPageNo)

                if (pageCount > 0) {
                    readerVm.updateLastKnownVerseFromPage(currentPageNo)
                }
            }
    }

    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToPage, pageCount) {
        val targetPage = navigateToPage ?: return@LaunchedEffect

        if (pageCount <= 0) return@LaunchedEffect

        // Requests are consumed only by the active mode's UI; otherwise a not-yet-disposed
        // composable of the outgoing mode steals navigations meant for the incoming one.
        if (readerVm.readerMode.value != ReaderMode.Reading) return@LaunchedEffect

        val clamped = targetPage.coerceIn(1, pageCount)

        try {
            pagerState.animateScrollToPage(clamped - 1)
            readerVm.updateCurrentPageNo(clamped)
            // Keep the cross-mode anchor in sync so navigator jumps (and other programmatic
            // navigations) carry the position when switching to verse/translation modes.
            readerVm.updateLastKnownVerseFromPage(clamped)
        } finally {
            readerVm.consumePageNavigation()
        }
    }

    LaunchedEffect(navigateToVerse, pageCount) {
        val targetVerse = navigateToVerse ?: return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect

        // Convert verse -> page navigation only while mushaf mode is actually active; during a
        // switch to verse mode this would consume the request before the verse UI sees it.
        if (readerVm.readerMode.value != ReaderMode.Reading) return@LaunchedEffect

        val targetPage = readerVm.resolvePageNo(targetVerse.chapterNo, targetVerse.verseNo)
            ?: run {
                readerVm.consumeVerseNavigation()
                return@LaunchedEffect
            }

        readerVm.consumeVerseNavigation()
        readerVm.requestPageNavigation(targetPage)
    }

    val playerState = LocalRecitation.current
    val isPlayingMushaf = playerState.isAnyPlaying
    val playingVerseMushaf = playerState.playingVerse
    var playerVerseSync by readerVm.playerVerseSync

    LaunchedEffect(playerVerseSync, isPlayingMushaf, playingVerseMushaf, pageCount) {
        if (!playerVerseSync || !isPlayingMushaf || !playingVerseMushaf.isValid || pageCount <= 0) {
            return@LaunchedEffect
        }

        val targetPage =
            readerVm.resolvePageNo(playingVerseMushaf.chapterNo, playingVerseMushaf.verseNo)
                ?: return@LaunchedEffect
        if (targetPage !in 1..pageCount) return@LaunchedEffect

        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledIdx ->
                val currentPage = settledIdx + 1
                if (currentPage != targetPage) {
                    readerVm.requestPageNavigation(targetPage)
                }
            }
    }

    LaunchedEffect(playingVerseMushaf, pageCount) {
        if (!playingVerseMushaf.isValid || pageCount <= 0) {
            onSyncStateChanged(false)
            return@LaunchedEffect
        }

        val expectedPage =
            readerVm.resolvePageNo(playingVerseMushaf.chapterNo, playingVerseMushaf.verseNo)

        if (expectedPage == null || expectedPage !in 1..pageCount) {
            onSyncStateChanged(false)
            return@LaunchedEffect
        }

        snapshotFlow { pagerState.settledPage + 1 }
            .distinctUntilChanged()
            .collect { current ->
                onSyncStateChanged(current == expectedPage)
            }
    }

    val pageTurnAnimation = AppPreferences.observeReaderPageTurnAnimation()
    val pageGround = ReaderMode.groundColor(ReaderMode.Reading)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) { page ->
                val pageNo = page + 1
                val item = pageItems[pageNo]
                val scrollState = scrollStates.getOrPut(page) { ScrollState(0) }

                // Effekt səhifənin **xaricindədir**: `key` müshəf düzülüşü dəyişəndə səhifəni
                // yenidən qurur, transformasiya isə vərəqləyicinin yuvasına bağlıdır.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .pageTurnEffect(pageTurnAnimation, pagerState, page, pageGround),
                ) {
                    key(sessionLayout, page) {
                        PageModePage(
                            item = item,
                            contentWidth = contentWidth,
                            ruledPageDecoration = ruledPageDecoration,
                            nestedScrollConnection = nestedScrollConnection,
                            externalScrollState = scrollState,
                            bottomChromeInset = bottomChromeInset,
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun PageModePage(
    item: QuranPageItem?,
    contentWidth: Dp,
    ruledPageDecoration: Boolean,
    nestedScrollConnection: NestedScrollConnection?,
    externalScrollState: ScrollState? = null,
    bottomChromeInset: Dp = 0.dp,
) {
    if (item == null) {
        return Loader(true)
    }

    val scrollState = externalScrollState ?: rememberScrollState()

    // Ayə nömrəsi balonu səhifə boyu tək olmalıdır, ona görə vəziyyət sətirlərdən yuxarıda saxlanılır.
    var activeMarkerWord by remember { mutableStateOf<AyahWordEntity?>(null) }

    val playerState = LocalRecitation.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse

    val playingWordKeys = remember(isPlaying, item.lines, playingVerse) {
        if (!isPlaying) return@remember emptySet()

        item.lines
            .filterIsInstance<QuranPageLineItem.Text>()
            .flatMap { it.words }
            .filter { word -> playingVerse.doesEqual(word.ayahId) }
            .map { word -> word.ayahId to word.wordIndex }
            .toSet()
    }

    TextStyleProvider(emptyMap()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = contentWidth)
                    .verticalFadingEdge(scrollState, color = colorScheme.surface, length = 24.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        Modifier
                            .verticalScroll(scrollState)
                            .padding(top = 16.dp, bottom = 64.dp + bottomChromeInset)
                            .then(
                                if (nestedScrollConnection == null) Modifier
                                else Modifier.nestedScroll(nestedScrollConnection)
                            )
                            .then(
                                if (ruledPageDecoration) {
                                    Modifier
                                } else {
                                    Modifier.padding(
                                        start = MUSHAF_PAGE_HORIZONTAL_PADDING,
                                        end = MUSHAF_PAGE_HORIZONTAL_PADDING,
                                    )
                                }
                            )
                            .fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (ruledPageDecoration) {
                                        Modifier.mushafPageOuterFrameBorder(
                                            color = colorScheme.outline.alpha(0.5f),
                                            strokeWidth = 1.dp,
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                item.lines.forEachIndexed { index, line ->
                                    key(line.lineNo) {
                                        val showLineRuleBelow =
                                            ruledPageDecoration && index < item.lines.lastIndex

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(
                                                    if (showLineRuleBelow) {
                                                        Modifier.mushafHorizontalRuleBelow(
                                                            color = colorScheme.outline.alpha(0.5f),
                                                            strokeWidth = 1.dp,
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                ),
                                        ) {
                                            if (ruledPageDecoration) {
                                                val hPadding =
                                                    if (line is QuranPageLineItem.Title) 0.dp
                                                    else MUSHAF_PAGE_HORIZONTAL_PADDING

                                                Column(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            start = hPadding,
                                                            end = hPadding,
                                                        ),
                                                ) {
                                                    MushafLineContent(
                                                        line = line,
                                                        playingWordKeys = playingWordKeys,
                                                        ruledPageDecoration = ruledPageDecoration,
                                                        activeMarkerWord = activeMarkerWord,
                                                        onActiveMarkerChange = { activeMarkerWord = it },
                                                    )
                                                }
                                            } else {
                                                MushafLineContent(
                                                    line = line,
                                                    playingWordKeys = playingWordKeys,
                                                    ruledPageDecoration = ruledPageDecoration,
                                                    activeMarkerWord = activeMarkerWord,
                                                    onActiveMarkerChange = { activeMarkerWord = it },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MushafLineContent(
    line: QuranPageLineItem,
    playingWordKeys: Set<Pair<Int, Int>>,
    ruledPageDecoration: Boolean,
    activeMarkerWord: AyahWordEntity?,
    onActiveMarkerChange: (AyahWordEntity?) -> Unit,
) {
    when (line) {
        is QuranPageLineItem.Title -> ChapterTitle(line.chapterNo, ruledPageDecoration)
        is QuranPageLineItem.Bismillah -> Bismillah()
        is QuranPageLineItem.Text -> MushafLineText(
            textLine = line,
            layout = line.layout,
            playingWordKeys = playingWordKeys,
            activeMarkerWord = activeMarkerWord,
            onActiveMarkerChange = onActiveMarkerChange,
        )
    }
}

@Composable
private fun MushafLineText(
    textLine: QuranPageLineItem.Text,
    layout: MushafLineLayout,
    playingWordKeys: Set<Pair<Int, Int>>,
    activeMarkerWord: AyahWordEntity?,
    onActiveMarkerChange: (AyahWordEntity?) -> Unit,
) {
    val fittedStyle = layout.fittedStyle
    val centeredGap = layout.centeredGap

    if (textLine.centered) {
        Box(Modifier.fillMaxWidth()) {
            MushafWordsRow(
                textLine = textLine,
                fittedStyle = fittedStyle,
                playingWordKeys = playingWordKeys,
                horizontalArrangement = Arrangement.spacedBy(
                    centeredGap, Alignment.CenterHorizontally
                ),
                modifier = Modifier.align(Alignment.Center),
                activeMarkerWord = activeMarkerWord,
                onActiveMarkerChange = onActiveMarkerChange,
            )
        }
    } else {
        MushafWordsRow(
            textLine = textLine,
            fittedStyle = fittedStyle,
            playingWordKeys = playingWordKeys,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            activeMarkerWord = activeMarkerWord,
            onActiveMarkerChange = onActiveMarkerChange,
        )
    }
}

@Composable
private fun MushafWordsRow(
    textLine: QuranPageLineItem.Text,
    fittedStyle: TextStyle,
    playingWordKeys: Set<Pair<Int, Int>>,
    horizontalArrangement: Arrangement.Horizontal,
    activeMarkerWord: AyahWordEntity?,
    onActiveMarkerChange: (AyahWordEntity?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = textLine.words
    val wordRects = remember(words) {
        mutableStateListOf<Rect?>().apply { repeat(words.size) { add(null) } }
    }

    val highlightRects = mergedMushafHighlightRects(words, wordRects, playingWordKeys)
    val highlightColor = colorScheme.primary.alpha(0.3f)

    val wbwState = LocalWbwState.current
    val quranTextStyles = LocalQuranTextStyle.current
    val shouldShowTooltip = !wbwState.isWbwSheetOpen

    val tajweedPalette = LocalTajweedPalette.current
    val tajweedClasses = textLine.tajweedClasses

    // Children are positioned before their parent, so this lands after the word rects above; the
    // draw below reads it as state and simply repaints once it is known.
    var rowOriginInRoot by remember(words) { mutableStateOf(Offset.Zero) }

    Row(
        modifier = modifier
            .onGloballyPositioned { rowOriginInRoot = it.positionInRoot() }
            .drawBehind {
                for (rect in highlightRects) {
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(
                            rect.left - rowOriginInRoot.x,
                            rect.top - rowOriginInRoot.y,
                        ),
                        size = Size(rect.width, rect.height),
                    )
                }
            },
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((index, word) in words.withIndex()) {
            // Measured in root space, not positionInParent(): a word wrapped in a tooltip is no
            // longer this row's direct child, and its offset *within that wrapper* is near zero —
            // which dragged the playback highlight all the way out to the row's edge. The row's own
            // origin is subtracted at draw time, where it is always known.
            val positionModifier = Modifier.onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInRoot()
                val sz = coordinates.size
                wordRects[index] = Rect(
                    pos.x,
                    pos.y,
                    pos.x + sz.width,
                    pos.y + sz.height,
                )
            }

            // Sətrin sonundakı ayə nömrəsi sözün özü kimi render olunur; onun söz-söz tərcüməsi
            // yoxdur, ona görə WBW balonu əvəzinə ayə əməlləri balonu açılır.
            if (word.isLastWordOfAyah) {
                val isMarkerActive = activeMarkerWord == word

                val markerAnchor: @Composable (Modifier) -> Unit = { anchorModifier ->
                    Word(
                        active = isMarkerActive,
                        word = word,
                        atlasPlacements = textLine.atlasPlacements.getForWord(word),
                        glyphClasses = tajweedClasses.glyphClassesForWord(word),
                        tajweedPalette = tajweedPalette,
                        fittedStyle = fittedStyle,
                        onClick = {
                            wbwState.onDismissTooltip()
                            onActiveMarkerChange(if (isMarkerActive) null else word)
                        },
                        modifier = anchorModifier,
                    )
                }

                if (isMarkerActive) {
                    val pair = QuranMeta.getVerseNoFromAyahId(word.ayahId)

                    VerseMarkerTooltip(
                        verse = ChapterVersePair(pair.first, pair.second),
                        onDismiss = { onActiveMarkerChange(null) },
                        anchor = { markerAnchor(positionModifier) },
                    )
                } else {
                    markerAnchor(positionModifier)
                }
            } else if (wbwState.activeTooltipWord == word && shouldShowTooltip) {
                WbwTooltip(
                    word = word,
                    onDismiss = { wbwState.onDismissTooltip() },
                    onOpenSheet = {
                        val pair = QuranMeta.getVerseNoFromAyahId(word.ayahId)

                        wbwState.toggleWbwSheet(
                            WbwSheetData(
                                chapterNo = pair.first,
                                verseNo = pair.second,
                                wordIndex = word.wordIndex,
                            )
                        )
                    },
                    textStyles = quranTextStyles,
                ) {
                    Word(
                        active = true,
                        word = word,
                        atlasPlacements = textLine.atlasPlacements.getForWord(word),
                        glyphClasses = tajweedClasses.glyphClassesForWord(word),
                        tajweedPalette = tajweedPalette,
                        fittedStyle = fittedStyle,
                        onClick = { wbwState.onWordClick(word) },
                        modifier = positionModifier,
                    )
                }
            } else {
                Word(
                    active = wbwState.activeTooltipWord == word,
                    word = word,
                    atlasPlacements = textLine.atlasPlacements.getForWord(word),
                    glyphClasses = tajweedClasses.glyphClassesForWord(word),
                    tajweedPalette = tajweedPalette,
                    fittedStyle = fittedStyle,
                    onClick = {
                        onActiveMarkerChange(null)
                        wbwState.onWordClick(word)
                    },
                    modifier = positionModifier,
                )
            }
        }
    }
}

@Composable
private fun Word(
    active: Boolean,
    word: AyahWordEntity,
    atlasPlacements: List<AtlasGlyphPlacement>?,
    fittedStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyphClasses: ByteArray? = null,
    tajweedPalette: List<Color>? = null,
) {
    QuranWordText(
        modifier = modifier
            .background(
                if (active) colorScheme.primary.alpha(0.3f) else Color.Transparent,
                shape = shapes.small
            )
            .clickable { onClick() },
        word = word,
        atlasPlacements = atlasPlacements,
        style = fittedStyle,
        glyphClasses = glyphClasses,
        tajweedPalette = tajweedPalette,
    )
}

private fun mergedMushafHighlightRects(
    words: List<AyahWordEntity>,
    wordRects: List<Rect?>,
    playingWordKeys: Set<Pair<Int, Int>>,
): List<Rect> {
    if (words.isEmpty()) return emptyList()

    val result = mutableListOf<Rect>()
    var i = 0

    while (i < words.size) {
        val w = words[i]
        val highlighted = (w.ayahId to w.wordIndex) in playingWordKeys

        if (!highlighted) {
            i++
            continue
        }

        var rect = wordRects.getOrNull(i) ?: run {
            i++
            continue
        }

        val ayahId = w.ayahId

        var j = i + 1

        while (j < words.size) {
            val w2 = words[j]
            if ((w2.ayahId to w2.wordIndex) !in playingWordKeys || w2.ayahId != ayahId) {
                break
            }
            val r2 = wordRects.getOrNull(j) ?: break
            rect = Rect(
                minOf(rect.left, r2.left),
                minOf(rect.top, r2.top),
                maxOf(rect.right, r2.right),
                maxOf(rect.bottom, r2.bottom),
            )
            j++
        }

        result.add(rect)

        i = j
    }

    return result
}

private fun Modifier.mushafHorizontalRuleBelow(
    color: Color,
    strokeWidth: Dp,
): Modifier = drawBehind {
    val h = strokeWidth.toPx()
    val y = size.height - h / 2f
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = h,
    )
}

private fun Modifier.mushafPageOuterFrameBorder(
    color: Color,
    strokeWidth: Dp,
): Modifier = drawBehind {
    val w = strokeWidth.toPx()
    val half = w / 2f
    drawRect(
        color = color,
        topLeft = Offset(half, half),
        size = Size(size.width - w, size.height - w),
        style = Stroke(width = w),
    )
}
