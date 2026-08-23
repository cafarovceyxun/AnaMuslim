package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.compose.components.READER_FLOATING_CONTROLS_STRIP
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.vector_1
import org.jetbrains.compose.resources.painterResource
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.ReaderFooterNavigator
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkKey
import com.cafarovceyxun.anamuslim.utils.quran.AzerbaijaniSurahNames
import com.cafarovceyxun.anamuslim.db.entities.wbw.WbwWordEntity
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewType
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.utils.reader.MUSHAF_FONT_WIDTH_DP_MAX
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasGlyphPlacement
import com.cafarovceyxun.anamuslim.viewModels.ReaderUiState
import com.cafarovceyxun.anamuslim.viewModels.ReaderViewModel
import com.cafarovceyxun.anamuslim.compose.components.ChapterIcon
import kotlinx.coroutines.flow.distinctUntilChanged

import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    readerVm: ReaderViewModel,
    nestedScrollConnection: NestedScrollConnection?,
    onSyncStateChanged: (Boolean) -> Unit = {},
    /**
     * Room the verse list keeps free under its last item for chrome the screen floats on top of it
     * (mini player, floating controls). Padding rather than a shorter viewport, so the verses stay
     * visible *through* that chrome instead of stopping at an opaque band above it.
     */
    bottomChromeInset: Dp = 0.dp,
) {
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val readerMode by readerVm.readerMode.collectAsState()
    val mushafSession by readerVm.mushafSession.collectAsState()

    if (readerMode == null) {
        return Loader(fill = true)
    }

    // Mode transitions are handled at the source (ReaderViewModel.setReaderMode / initReader);
    // running them here as well raced with those paths and re-navigated to a stale anchor.

    val scope = rememberCoroutineScope()

    // Mushaf rejimi kənardadır: səhifə atlas ilə çəkilir, orada «mətn ölçüsü çarpanı» anlayışı yoxdur.
    // Modifikator `when`-dən əvvəl, şərtsiz qurulur ki, rejim dəyişəndə composable çağırış sırası
    // pozulmasın; qurulması ucuzdur və `enabled = false` olanda heç bir toxunuş emalı əlavə etmir.
    var zoomFeedback by remember { mutableStateOf<ReaderZoomFeedback?>(null) }
    val zoomModifier = Modifier.readerTextZoom(
        enabled = AppPreferences.observeReaderPinchZoomEnabled(),
        arabicMultiplier = ReaderPreferences.observeArabicTextSizeMultiplier(),
        translationMultiplier = ReaderPreferences.observeTranlationTextSizeMultiplier(),
        minMultiplier = ReaderTextZoom.QURAN_MIN,
        maxMultiplier = ReaderTextZoom.QURAN_MAX,
        onZoom = { target, value ->
            zoomFeedback = ReaderZoomFeedback(target, value)
            scope.launch {
                when (target) {
                    ReaderZoomTarget.Arabic -> ReaderPreferences.setArabicTextSizeMultiplier(value)
                    ReaderZoomTarget.Translation ->
                        ReaderPreferences.setTranslationTextSizeMultiplier(value)
                }
            }
        },
    )

    when (readerMode) {
        ReaderMode.Reading -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                val contentWidth =
                    (maxWidth.coerceAtMost(MUSHAF_FONT_WIDTH_DP_MAX.dp))
                        .coerceAtLeast(1.dp)

                ReaderLayoutPageMode(
                    readerVm,
                    contentWidth,
                    nestedScrollConnection,
                    onSyncStateChanged,
                    bottomChromeInset = bottomChromeInset,
                )
            }
        }

        ReaderMode.Translation, ReaderMode.TranslationVertical -> {
            key(mushafSession.version, readerMode) {
                Box(modifier = Modifier.fillMaxSize().then(zoomModifier)) {
                    val initialPageIndex = mushafSession.currentPageNo?.minus(1)?.coerceAtLeast(0) ?: 0
                    val translationListState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)

                    ReaderLayoutTranslationPageMode(
                        readerVm,
                        nestedScrollConnection,
                        externalListState = translationListState
                    )

                    if (readerVm.isAutoScrollGestureMode.value && readerMode == ReaderMode.TranslationVertical) {
                        AutoScrollGestureOverlay(
                            autoScrollSpeed = readerVm.autoScrollSpeed,
                            isAutoScrollGestureMode = readerVm.isAutoScrollGestureMode,
                            autoScrollStep = readerVm.autoScrollStep,
                            onManualScroll = {
                                scope.launch { translationListState.scrollBy(it) }
                            }
                        )
                    }

                    ReaderZoomFeedbackOverlay(zoomFeedback) { zoomFeedback = null }
                }
            }
        }

        else -> {
            key(mushafSession.version, readerMode) {
                Box(modifier = Modifier.fillMaxSize().then(zoomModifier)) {
                    val verseListState = rememberLazyListState()

                    ReaderLayoutVerseMode(
                        readerVm,
                        uiState,
                        nestedScrollConnection,
                        onSyncStateChanged,
                        onManualScroll = {
                            scope.launch { verseListState.scrollBy(it) }
                        },
                        externalListState = verseListState,
                        bottomChromeInset = bottomChromeInset,
                    )

                    ReaderZoomFeedbackOverlay(zoomFeedback) { zoomFeedback = null }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderLayoutVerseMode(
    readerVm: ReaderViewModel,
    uiState: ReaderUiState,
    nestedScrollConnection: NestedScrollConnection?,
    onSyncStateChanged: (Boolean) -> Unit,
    onManualScroll: (Float) -> Unit = {},
    externalListState: androidx.compose.foundation.lazy.LazyListState? = null,
    bottomChromeInset: Dp = 0.dp,
) {
    val internalListState = rememberLazyListState()
    val listState = externalListState ?: internalListState
    val prepared by readerVm.verseByVersePrepared.collectAsStateWithLifecycle()
    val items = prepared.items

    val allBookmarks by readerVm.userRepository.getBookmarksFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val bookmarkedVerseKeys = remember(allBookmarks) {
        allBookmarks.map {
            BookmarkKey(
                chapterNo = it.chapterNo,
                fromVerse = it.fromVerseNo,
                toVerse = it.toVerseNo
            )
        }.toHashSet()
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                // Transient positions while a programmatic navigation is pending would corrupt
                // the cross-mode anchor; the navigation effect updates it once it lands.
                if (readerVm.navigateToVerse.value == null) {
                    readerVm.updateLastKnownVerseFromItems(it)
                }
            }
    }

    LaunchedEffect(prepared) {
        if (items.isNotEmpty() && readerVm.navigateToVerse.value == null) {
            readerVm.updateLastKnownVerseFromItems(listState.firstVisibleItemIndex)
        }
    }

    // Track the last view type we scrolled to top for, to avoid re-scrolling on rotation
    val viewTypeKey = remember(uiState.viewType) {
        when (val vt = uiState.viewType) {
            is ReaderViewType.Chapter -> "chapter:${vt.chapterNo}"
            is ReaderViewType.Juz -> "juz:${vt.juzNo}"
            is ReaderViewType.Hizb -> "hizb:${vt.hizbNo}"
            else -> null
        }
    }
    var lastScrolledViewType by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(viewTypeKey) {
        if (viewTypeKey != null && viewTypeKey != lastScrolledViewType) {
            // Only scroll to top if it's a DIFFERENT view type and we are not navigating to a specific verse
            if (readerVm.navigateToVerse.value == null) {
                listState.scrollToItem(0)
            }
            lastScrolledViewType = viewTypeKey
        }
    }

    var autoScrollSpeed by readerVm.autoScrollSpeed
    var playerVerseSync by readerVm.playerVerseSync

    val playerState = LocalRecitation.current

    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse

    LaunchedEffect(listState, autoScrollSpeed, playerVerseSync, isPlaying, playingVerse, items) {
        if (autoScrollSpeed == null && playerVerseSync && isPlaying && playingVerse.isValid) {
            val currentPlayingIndex = items.indexOfFirst { item ->
                item is ReaderLayoutItem.VerseUI &&
                        item.verse.chapterNo == playingVerse.chapterNo &&
                        item.verse.verseNo == playingVerse.verseNo
            }

            if (currentPlayingIndex >= 0) {
                listState.animateScrollToItem(currentPlayingIndex)
            }
        }
    }

    LaunchedEffect(listState, items, playingVerse) {
        snapshotFlow {
            if (!playingVerse.isValid) return@snapshotFlow false

            val idx = items.indexOfFirst { item ->
                item is ReaderLayoutItem.VerseUI &&
                        item.verse.chapterNo == playingVerse.chapterNo &&
                        item.verse.verseNo == playingVerse.verseNo
            }

            if (idx < 0) return@snapshotFlow false

            return@snapshotFlow true
        }
            .distinctUntilChanged()
            .collect { onSyncStateChanged(it) }
    }

    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToVerse, items) {
        val (chapterNo, verseNo) = navigateToVerse ?: return@LaunchedEffect

        // Only the active mode may consume a navigation request; this composable can still be
        // briefly composed while the mode is switching away.
        if (readerVm.readerMode.value != ReaderMode.VerseByVerse) return@LaunchedEffect

        val idx = items.indexOfFirst { item ->
            item is ReaderLayoutItem.VerseUI &&
                    item.verse.chapterNo == chapterNo &&
                    item.verse.verseNo == verseNo
        }

        if (idx >= 0) {
            val optimalIdx = items.findOptimalScrollIndex(idx)
            listState.scrollToItem(optimalIdx)
            readerVm.consumeVerseNavigation()
            // The trackers above skip updates while the navigation was pending; record the
            // landing position explicitly.
            readerVm.updateLastKnownVerseFromItems(idx)
        }
    }

    AutoScrollEffect(listState, autoScrollSpeed) { autoScrollSpeed = null }

    ReaderKeyScrollEffect(listState, readerVm.scrollEvent)

    TextStyleProvider(prepared.textStyles) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (nestedScrollConnection == null) Modifier
                        else Modifier.nestedScroll(nestedScrollConnection)
                    )
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
                // The floating chrome (controls strip, and via [bottomChromeInset] the mini player
                // and bottom nav) is cleared here rather than by shrinking the viewport, so verses
                // scroll *behind* it instead of ending at an opaque band. A flat 240.dp left a
                // screenful of dead space under the last verse.
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = READER_FLOATING_CONTROLS_STRIP + 16.dp + bottomChromeInset,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = items,
                    key = { item -> item.key },
                ) { item ->
                    TranslationRow(readerVm, item, bookmarkedVerseKeys)
                }

                val footerViewType = uiState.viewType
                if (footerViewType != null && items.isNotEmpty()) {
                    item(key = "verse_reader_nav_footer") {
                        ReaderFooterNavigator(
                            readerVm = readerVm,
                            viewType = footerViewType,
                            listState = listState,
                        )
                    }
                }
            }

            if (readerVm.isAutoScrollGestureMode.value) {
                AutoScrollGestureOverlay(
                    autoScrollSpeed = readerVm.autoScrollSpeed,
                    isAutoScrollGestureMode = readerVm.isAutoScrollGestureMode,
                    autoScrollStep = readerVm.autoScrollStep,
                    onManualScroll = onManualScroll
                )
            }
        }
    }
}


@Composable
private fun TranslationRow(
    readerVm: ReaderViewModel,
    item: ReaderLayoutItem,
    bookmarkedVerseKeys: Set<BookmarkKey>,
) {
    when (item) {
        is ReaderLayoutItem.Bismillah -> Bismillah()
        is ReaderLayoutItem.IsVotd -> IsVotd()
        is ReaderLayoutItem.ChapterInfo -> ChapterInfoCard(item.chapterNo)
        is ReaderLayoutItem.ChapterTitle -> ChapterTitle(item.chapterNo)
        is ReaderLayoutItem.SectionMarker -> SectionMarkerRowProfessional(item)
        is ReaderLayoutItem.VerseUI -> {
            val isBookmarked = BookmarkKey(
                chapterNo = item.verse.chapterNo,
                fromVerse = item.verse.verseNo,
                toVerse = item.verse.verseNo
            ) in bookmarkedVerseKeys

            VerseView(
                verseUi = item,
                isBookmarked = isBookmarked,
                showDivider = item.showDivider,
            )
        }
    }
}

@Composable
private fun SectionMarkerRowProfessional(marker: ReaderLayoutItem.SectionMarker) {
    if (marker.text.isEmpty()) return

    val decorationTint = MaterialTheme.colorScheme.primary
    val translSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()

    val fontSize = (14 * translSizeMult + 2).sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upper icons and Surah name in Azerbaijani
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painterResource(Res.drawable.vector_1),
                contentDescription = null,
                modifier = Modifier.height(36.dp),
                colorFilter = ColorFilter.tint(decorationTint)
            )

            marker.chapterNo?.let { chNo ->
                val surahName = AzerbaijaniSurahNames.getName(chNo) ?: ""
                if (surahName.isNotEmpty()) {
                    Text(
                        text = surahName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Image(
                painterResource(Res.drawable.vector_1),
                contentDescription = null,
                modifier = Modifier
                    .height(36.dp)
                    .scale(-1f, 1f),
                colorFilter = ColorFilter.tint(decorationTint)
            )
        }

        Text(
            text = marker.text,
            style = MaterialTheme.typography.labelSmall.copy(
                textDirection = TextDirection.Ltr
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionMarkerRow(marker: ReaderLayoutItem.SectionMarker) {
    if (marker.text.isEmpty()) return

    val decorationTint = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Image(
            painterResource(Res.drawable.vector_1),
            contentDescription = null,
            modifier = Modifier.height(24.dp),
            colorFilter = ColorFilter.tint(decorationTint)
        )

        Text(
            text = marker.text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp),
            textAlign = TextAlign.Center
        )

        Image(
            painterResource(Res.drawable.vector_1),
            contentDescription = null,
            modifier = Modifier
                .height(24.dp)
                .scale(-1f, 1f),
            colorFilter = ColorFilter.tint(decorationTint)
        )
    }
}

private fun List<ReaderLayoutItem>.findOptimalScrollIndex(targetIndex: Int): Int {
    if (targetIndex <= 0) return targetIndex.fastCoerceAtLeast(0)

    var optimalIndex = targetIndex
    var i = targetIndex - 1

    while (i >= 0) {
        when (this[i]) {
            is ReaderLayoutItem.ChapterInfo,
            is ReaderLayoutItem.Bismillah,
            is ReaderLayoutItem.IsVotd,
            is ReaderLayoutItem.ChapterTitle -> {
                optimalIndex = i
                i--
            }

            else -> break
        }
    }

    return optimalIndex
}
