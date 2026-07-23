package com.cafarovceyxun.anamuslim.compose.screens.reference

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.theme.alpha as colorAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.api.resolveInventoryUrl
import com.cafarovceyxun.anamuslim.components.ReferenceThumbnail
import com.cafarovceyxun.anamuslim.components.ReferenceVerseModel
import com.cafarovceyxun.anamuslim.compose.components.common.Chip
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.player.MINI_PLAYER_HEIGHT
import com.cafarovceyxun.anamuslim.compose.components.player.MiniPlayerVisibility
import com.cafarovceyxun.anamuslim.compose.components.player.RecitationPlayerSheet
import com.cafarovceyxun.anamuslim.compose.components.player.rememberMiniPlayerVisibilityState
import com.cafarovceyxun.anamuslim.compose.components.reader.LocalReaderViewModel
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderLayoutItem
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderProvider
import com.cafarovceyxun.anamuslim.compose.components.reader.TextStyleProvider
import com.cafarovceyxun.anamuslim.compose.components.reader.VerseView
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.QuickReferenceVerses
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.parseVerses
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkKey
import com.cafarovceyxun.anamuslim.utils.extensions.isSingleValue
import com.cafarovceyxun.anamuslim.utils.reader.ComposeUiConfig
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import com.cafarovceyxun.anamuslim.utils.reader.ReaderItemsBuilder
import com.cafarovceyxun.anamuslim.utils.reader.TextBuilderParams
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import com.cafarovceyxun.anamuslim.compose.extensions.horizontalFadingEdge
import com.cafarovceyxun.anamuslim.compose.components.common.RemoteImage
import com.cafarovceyxun.anamuslim.compose.utils.NumeralSystem
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.isExpandedWindow
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.compose.utils.rememberSystemBack
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberToggleScreenRotation
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_arrow_left
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strLabelAllChapters
import com.cafarovceyxun.anamuslim.resources.strLabelBack
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strLabelOpenInReader
import com.cafarovceyxun.anamuslim.resources.strMsgTranslNoneSelected
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.min

private sealed class ReferenceRow {
    data class Description(
        val title: String,
        val desc: String?,
        val thumbnail: ReferenceThumbnail?
    ) : ReferenceRow()

    data class SectionTitle(
        val segmentKey: String,
        val ref: QuickReferenceVerses.Range,
        val titleText: String,
    ) : ReferenceRow()

    data class VerseRow(
        val verseUi: ReaderLayoutItem.VerseUI,
        val quranTextStyle: TextStyle? = null,
    ) : ReferenceRow()
}

private const val REFERENCE_VERSE_CHUNK_SIZE = 32
private const val REFERENCE_MAX_IN_FLIGHT_CHUNKS = 2

private data class ReferenceSegment(
    val segmentIndex: Int,
    val chapterNo: Int,
    val versesRangeStr: String,
    val ref: QuickReferenceVerses.Range,
    val chapterName: String,
)

private data class ReferenceChunkRequest(
    val segment: ReferenceSegment,
    val verseNos: List<Int>,
    val isFirstChunk: Boolean,
    val isLastChunk: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen(refModel: ReferenceVerseModel) {
    var selectedChapterChip by rememberSaveable { mutableIntStateOf(0) }
    val savedTranslations = ReaderPreferences.observeTranslations()
    val translationSlugs = remember(refModel, savedTranslations) {
        resolveTranslationSlugs(refModel, savedTranslations)
    }

    ReaderProvider {
        ReferenceScreenContent(
            refModel = refModel,
            translationSlugs = translationSlugs,
            selectedChapterChip = selectedChapterChip,
            onChapterChipChange = { selectedChapterChip = it },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceScreenContent(
    refModel: ReferenceVerseModel,
    translationSlugs: Set<String>,
    selectedChapterChip: Int,
    onChapterChipChange: (Int) -> Unit,
) {
    val vm = LocalReaderViewModel.current

    val textMeasurer = rememberTextMeasurer()
    val colors by rememberUpdatedState(colorScheme)
    val type by rememberUpdatedState(typography)
    val density = LocalDensity.current
    val isDark = ThemeUtils.observeDarkTheme()

    val rows = remember { mutableStateListOf<ReferenceRow>() }
    var loading by remember { mutableStateOf(true) }
    var chapterNames by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    val verseActions = LocalVerseActions.current

    val allBookmarks by vm.userRepository.getBookmarksFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val referenceBookmarkKeys by remember {
        derivedStateOf {
            rows.asSequence().mapNotNull { row ->
                when (row) {
                    is ReferenceRow.SectionTitle -> BookmarkKey(
                        row.ref.chapterNo,
                        row.ref.range.first,
                        row.ref.range.last,
                    )

                    is ReferenceRow.VerseRow -> BookmarkKey(
                        row.verseUi.verse.chapterNo,
                        row.verseUi.verse.verseNo,
                        row.verseUi.verse.verseNo,
                    )

                    else -> null
                }
            }.toHashSet()
        }
    }

    val bookmarkedKeys = remember(allBookmarks, referenceBookmarkKeys) {
        allBookmarks.asSequence()
            .map { BookmarkKey(it.chapterNo, it.fromVerseNo, it.toVerseNo) }
            .filter { it in referenceBookmarkKeys }
            .toHashSet()
    }

    LaunchedEffect(refModel, selectedChapterChip, translationSlugs, isDark) {
        loading = true

        rows.clear()
        rows.add(ReferenceRow.Description(refModel.title, refModel.desc, refModel.thumbnail))

        chapterNames = withContext(Dispatchers.IO) {
            vm.repository.getChapterNames(refModel.chapters.toList())
        }

        val params = TextBuilderParams(
            uiConfig = ComposeUiConfig(
                colors = colors,
                type = type,
                density = density,
                textMeasurer = textMeasurer,
                isDark = isDark,
            ),
            fontResolver = vm.fontResolver,
            verseActions = verseActions,
            arabicEnabled = ReaderPreferences.getArabicTextEnabled(),
            script = ReaderPreferences.getQuranScript(),
            arabicSizeMultiplier = ReaderPreferences.getArabicTextSizeMultiplier(),
            translationSizeMultiplier = ReaderPreferences.getTranslationTextSizeMultiplier(),
            slugs = translationSlugs,
        )

        buildReferenceRows(
            refModel = refModel,
            selectedChapterFilter = selectedChapterChip,
            params = params,
            chapterNamesByNo = chapterNames,
            onChunkBuilt = { chunkRows ->
                if (chunkRows.isNotEmpty()) {
                    rows.addAll(chunkRows)
                    loading = false
                }
            },
        )

        loading = false
    }

    val listState = rememberLazyListState()
    val chaptersGroupState = rememberLazyListState()
    
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
    
    val isLandscape = isLandscape()


    LaunchedEffect(isLandscape, topAppBarState.heightOffsetLimit) {
        if (isLandscape && topAppBarState.heightOffsetLimit < 0f) {
            topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
        }
    }

    val showCollapsedTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 150
        }
    }

    val showTwoPane = isExpandedWindow()

    val playerVisibilityState = rememberMiniPlayerVisibilityState(
        MiniPlayerVisibility.HIDDEN_BY_DEFAULT
    )
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val chromeCollapsedFraction = scrollBehavior.state.collapsedFraction

    val dynamicBottomPadding =
        navBarBottomInset + (if (playerVisibilityState.isVisible) MINI_PLAYER_HEIGHT else 0.dp) * (1f - chromeCollapsedFraction)

    Box {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    modifier = Modifier.shadow(if (showTwoPane) 2.dp else 0.dp),
                    title = {
                        if (showCollapsedTitle) {
                            Text(
                                text = refModel.title,
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    navigationIcon = {
                        val back = rememberSystemBack()

                        SimpleTooltip(stringResource(Res.string.strLabelBack)) {
                            IconButton(
                                onClick = { back?.invoke() },
                                painter = painterResource(Res.drawable.dr_icon_arrow_left),
                                contentDescription = stringResource(Res.string.strLabelBack),
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surfaceContainer
                    ),
                )
            },
            containerColor = colorScheme.surfaceContainer
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val contentPane: @Composable (Modifier) -> Unit = { modifier ->
                    if (loading) {
                        Loader(fill = true)
                    } else {
                        Column(modifier = modifier) {
                            if (translationSlugs.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.strMsgTranslNoneSelected),
                                    color = colorScheme.error,
                                    style = typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colorScheme.surface)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }

                            val referencePageTextStyles by remember {
                                derivedStateOf {
                                    buildMap {
                                        for (row in rows) {
                                            if (row is ReferenceRow.VerseRow) {
                                                row.quranTextStyle?.let {
                                                    put(
                                                        row.verseUi.verse.pageNo,
                                                        it
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            TextStyleProvider(referencePageTextStyles) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                                    contentPadding = PaddingValues(bottom = dynamicBottomPadding + 64.dp)
                                ) {
                                    items(
                                        items = rows,
                                        key = { row ->
                                            when (row) {
                                                is ReferenceRow.Description -> "desc"
                                                is ReferenceRow.SectionTitle -> row.segmentKey
                                                is ReferenceRow.VerseRow -> row.verseUi.key
                                            }
                                        },
                                    ) { row ->
                                        when (row) {
                                            is ReferenceRow.Description -> ReferenceDescription(row)

                                            is ReferenceRow.SectionTitle -> ReferenceSectionTitle(
                                                row = row,
                                                isBookmarked = bookmarkedKeys.contains(
                                                    BookmarkKey(
                                                        row.ref.chapterNo,
                                                        row.ref.range.first,
                                                        row.ref.range.last,
                                                    ),
                                                ),
                                                onOpenInReader = { chapterNo, range ->
                                                    ReaderUiHooks.openVerseRangeWithTranslations
                                                        ?.invoke(
                                                            chapterNo,
                                                            range.first,
                                                            range.last,
                                                            translationSlugs,
                                                        )
                                                },
                                            )

                                            is ReferenceRow.VerseRow -> ReferenceVerseViewWrapped(
                                                verseUi = row.verseUi,
                                                isBookmarked = bookmarkedKeys.contains(
                                                    BookmarkKey(
                                                        row.verseUi.verse.chapterNo,
                                                        row.verseUi.verse.verseNo,
                                                        row.verseUi.verse.verseNo,
                                                    ),
                                                ),
                                            )
                                        }
                                    }

                                    if (loading) {
                                        item("loading-footer") {
                                            Loader(fill = false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showTwoPane) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ReferenceChapterChipsSidebar(
                            selectedChapterChip = selectedChapterChip,
                            chapterNames = chapterNames,
                            chapters = refModel.chapters,
                            onChapterChipChange = onChapterChipChange,
                            listState = chaptersGroupState,
                        )

                        VerticalDivider(color = colorScheme.outlineVariant.alpha(0.6f))

                        contentPane(Modifier.weight(1f))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ReferenceChapterChipsTopBar(
                            selectedChapterChip = selectedChapterChip,
                            chapterNames = chapterNames,
                            chapters = refModel.chapters,
                            onChapterChipChange = onChapterChipChange,
                            listState = chaptersGroupState,
                        )
                        contentPane(Modifier.weight(1f))
                    }
                }
            }
        }

        ReferenceFloatingBar(
            chromeCollapsedFraction = chromeCollapsedFraction
        )
    }
}

@Composable
private fun ReferenceFloatingBar(
    chromeCollapsedFraction: Float
) {
    val buttonAlpha = (1f - chromeCollapsedFraction).coerceIn(0f, 1f)
    // Null where the platform owns orientation itself (iOS) — then there is nothing to show.
    val toggleRotation = rememberToggleScreenRotation() ?: return

    if (buttonAlpha > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 6.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            TextButton(
                onClick = { toggleRotation.invoke() },
                modifier = Modifier.height(32.dp).alpha(buttonAlpha),
                shape = shapes.extraLarge,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.surfaceContainer,
                    contentColor = colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp
                ),
                contentPadding = PaddingValues(vertical = 0.dp, horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ScreenRotation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ReferenceChapterChipsTopBar(
    selectedChapterChip: Int,
    chapterNames: Map<Int, String>,
    chapters: Set<Int>,
    onChapterChipChange: (Int) -> Unit,
    listState: LazyListState,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .horizontalFadingEdge(listState, color = colorScheme.surface)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainer),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                Chip(
                    selected = selectedChapterChip == 0,
                    onClick = { onChapterChipChange(0) },
                    label = { Text(stringResource(Res.string.strLabelAllChapters)) },
                )
            }

            items(chapters.toList()) {
                Chip(
                    selected = selectedChapterChip == it,
                    onClick = { onChapterChipChange(it) },
                    label = {
                        Text(
                            chapterNames[it] ?: it.toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ReferenceChapterChipsSidebar(
    selectedChapterChip: Int,
    chapterNames: Map<Int, String>,
    chapters: Set<Int>,
    onChapterChipChange: (Int) -> Unit,
    listState: LazyListState,
) {
    val sidebarWidth = 220.dp

    LazyColumn(
        state = listState,
        modifier = Modifier
            .width(sidebarWidth)
            .background(colorScheme.surfaceContainer),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            ReferenceSidebarItem(
                selected = selectedChapterChip == 0,
                onClick = { onChapterChipChange(0) },
                text = stringResource(Res.string.strLabelAllChapters),
            )
        }

        items(chapters.toList()) {
            ReferenceSidebarItem(
                selected = selectedChapterChip == it,
                onClick = { onChapterChipChange(it) },
                text = chapterNames[it] ?: it.toString(),
            )
        }
    }
}

@Composable
private fun ReferenceSidebarItem(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
) {
    val containerColor = if (selected) colorScheme.primary else Color.Transparent
    val contentColor = if (selected) colorScheme.onPrimary else colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.small)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = typography.bodyMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun ReferenceDescription(row: ReferenceRow.Description) {
    val downloadSource = AppPreferences.observeResourceDownloadProxy()

    val gradient = Brush.verticalGradient(
        colors = listOf(
            colorScheme.surfaceContainer,
            Color.Transparent,
        ),
    )


    // Only remote thumbnails are rendered: nothing in the app ever constructs a
    // `ReferenceThumbnail.ResourceId` (it survives solely as a bundle-decoding branch), and a
    // platform drawable id has no meaning in shared code.
    val heroImageUrl: String? = remember(row.thumbnail, downloadSource) {
        when (row.thumbnail) {
            is ReferenceThumbnail.RemoteUrl -> resolveInventoryUrl(row.thumbnail.url)
            else -> null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (heroImageUrl != null) {
            Surface(shape = shapes.medium) {
                RemoteImage(
                    url = heroImageUrl,
                    contentDescription = row.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                )
            }
        }

        Text(
            text = row.title,
            style = typography.titleMedium,
            color = colorScheme.primary,
        )

        if (!row.desc.isNullOrBlank()) {
            Text(
                text = row.desc,
                color = colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun ReferenceSectionTitle(
    row: ReferenceRow.SectionTitle,
    isBookmarked: Boolean,
    onOpenInReader: (Int, IntRange) -> Unit,
) {
    val verseActions = LocalVerseActions.current

    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .background(colorScheme.surface, shape)
                .border(1.dp, colorScheme.outlineVariant, shape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.titleText,
                style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.primary,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = {
                    verseActions.onBookmarkRequest?.invoke(
                        row.ref.chapterNo, row.ref.range
                    )
                },
                painter = painterResource(
                    if (isBookmarked) Res.drawable.ic_bookmark_added
                    else Res.drawable.ic_bookmark,
                ),
                contentDescription = stringResource(Res.string.strLabelBookmark),
                tint = if (isBookmarked) colorScheme.primary
                else colorScheme.onSurface.alpha(0.7f),
            )

            TextButton(
                onClick = { onOpenInReader(row.ref.chapterNo, row.ref.range) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = shapes.small
            ) {
                Text(
                    text = stringResource(Res.string.strLabelOpenInReader),
                    style = typography.labelMedium
                )
            }
        }

        HorizontalDivider(
            color = colorScheme.outlineVariant,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReferenceVerseViewWrapped(
    verseUi: ReaderLayoutItem.VerseUI,
    isBookmarked: Boolean,
) {
    Surface {
        VerseView(
            verseUi = verseUi,
            isBookmarked = isBookmarked,
            showDivider = verseUi.showDivider,
        )
    }
}

private fun resolveTranslationSlugs(
    refModel: ReferenceVerseModel,
    savedTranslations: Set<String>,
): Set<String> {
    val fromModel = refModel.translSlugs.filter { it.isNotBlank() }.toSet()
    if (fromModel.isNotEmpty()) return fromModel

    val first = savedTranslations.firstOrNull().orEmpty()

    if (first.isNotBlank()) return setOf(first)

    return TranslUtils.defaultTranslationSlugs()
}

private suspend fun buildReferenceRows(
    refModel: ReferenceVerseModel,
    selectedChapterFilter: Int,
    params: TextBuilderParams,
    chapterNamesByNo: Map<Int, String>,
    onChunkBuilt: suspend (List<ReferenceRow>) -> Unit,
) = coroutineScope {
    var segments = parseReferenceSegments(
        refModel = refModel,
        selectedChapterFilter = selectedChapterFilter,
        chapterNamesByNo = chapterNamesByNo,
    )

    if (segments.isEmpty()) return@coroutineScope

    val numerals = appLocale().numeralSystem
    val chunkRequests = buildChunkRequests(segments)
    val sectionTitleBySegmentIndex = buildSectionTitlesBySegment(
        segments = segments,
        numerals = numerals,
    )

    val totalChunkCount = chunkRequests.size
    val buildStartMark = TimeSource.Monotonic.markNow()
    var firstChunkDone = false
    var windowStart = 0

    while (windowStart < totalChunkCount) {
        val windowEnd = min(windowStart + REFERENCE_MAX_IN_FLIGHT_CHUNKS, totalChunkCount)
        val window = chunkRequests.subList(windowStart, windowEnd)

        val builtWindow = withContext(Dispatchers.IO) {
            window.map { req ->
                async {
                    val prepared = ReaderItemsBuilder.buildQuickReferenceItems(
                        params = params,
                        chapterNo = req.segment.chapterNo,
                        verseNos = req.verseNos,
                    )
                    req to prepared
                }
            }.awaitAll()
        }

        for ((req, prepared) in builtWindow) {
            val verseUis =
                prepared?.items?.filterIsInstance<ReaderLayoutItem.VerseUI>().orEmpty()
            val textStyles = prepared?.textStyles.orEmpty()
            val chunkRows = ArrayList<ReferenceRow>(verseUis.size + 1)

            if (req.isFirstChunk) {
                chunkRows.add(
                    ReferenceRow.SectionTitle(
                        segmentKey = "st-${req.segment.segmentIndex}-${req.segment.chapterNo}-${req.segment.versesRangeStr}",
                        ref = req.segment.ref,
                        titleText = sectionTitleBySegmentIndex[req.segment.segmentIndex].orEmpty(),
                    ),
                )
            }

            for ((idx, verseUi) in verseUis.withIndex()) {
                val showDivider = if (req.isLastChunk) idx != verseUis.lastIndex else true
                chunkRows.add(
                    ReferenceRow.VerseRow(
                        verseUi = verseUi.copy(
                            key = "ref-${req.segment.segmentIndex}-${verseUi.key}",
                            showDivider = showDivider,
                        ),
                        quranTextStyle = textStyles[verseUi.verse.pageNo],
                    ),
                )
            }

            if (chunkRows.isNotEmpty()) {
                onChunkBuilt(chunkRows)
                if (!firstChunkDone) {
                    val firstChunkMs = buildStartMark.elapsedNow().inWholeMilliseconds
                    AppLogger.d("ReferenceScreen first chunk ready in ms =", firstChunkMs.toString())
                    firstChunkDone = true
                }
            }
        }

        windowStart = windowEnd
    }
}

private fun parseReferenceSegments(
    refModel: ReferenceVerseModel,
    selectedChapterFilter: Int,
    chapterNamesByNo: Map<Int, String>,
): List<ReferenceSegment> {
    val segments = ArrayList<ReferenceSegment>()
    var segmentIndex = 0

    for (refStr in refModel.verses) {
        val parts = refStr.split(":")
        if (parts.size < 2) continue

        val chapterNo = parts[0].trim().toIntOrNull() ?: continue
        if (selectedChapterFilter != 0 && chapterNo != selectedChapterFilter) continue

        val versesRangeStr = parts[1]
        val ref = parseVerses(chapterNo, versesRangeStr)
        if (ref !is QuickReferenceVerses.Range) continue

        segments.add(
            ReferenceSegment(
                segmentIndex = segmentIndex,
                chapterNo = chapterNo,
                versesRangeStr = versesRangeStr,
                ref = ref,
                chapterName = chapterNamesByNo[chapterNo].orEmpty(),
            ),
        )
        segmentIndex++
    }

    return segments
}

private fun buildChunkRequests(
    segments: List<ReferenceSegment>,
): List<ReferenceChunkRequest> {
    val requests = ArrayList<ReferenceChunkRequest>()

    for (segment in segments) {
        val startVerse = segment.ref.range.first
        val endVerse = segment.ref.range.last
        if (startVerse > endVerse) continue

        val chunkCount = ((endVerse - startVerse) / REFERENCE_VERSE_CHUNK_SIZE) + 1
        for (chunkIndex in 0 until chunkCount) {
            val chunkStart = startVerse + (chunkIndex * REFERENCE_VERSE_CHUNK_SIZE)
            val chunkEnd = min(chunkStart + REFERENCE_VERSE_CHUNK_SIZE - 1, endVerse)
            val chunkVerseNos = (chunkStart..chunkEnd).toList()
            requests.add(
                ReferenceChunkRequest(
                    segment = segment,
                    verseNos = chunkVerseNos,
                    isFirstChunk = chunkIndex == 0,
                    isLastChunk = chunkIndex == chunkCount - 1,
                ),
            )
        }
    }

    return requests
}

private fun buildSectionTitlesBySegment(
    segments: List<ReferenceSegment>,
    numerals: NumeralSystem?,
): Map<Int, String> {
    return buildMap(segments.size) {
        for (segment in segments) {
            val chapterLabel = segment.chapterName.ifBlank { segment.chapterNo.toString() }
            val chapter = numerals.formatNumber(segment.chapterNo)
            val from = numerals.formatNumber(segment.ref.range.first)

            // Replaces `String.format(platformLocale, "%1$s %2$d:%3$d", ...)`: the locale only ever
            // shaped the digits (no grouping, no decimals), which `formatNumber` now does.
            val text = if (segment.ref.range.isSingleValue) {
                "$chapterLabel $chapter:$from"
            } else {
                "$chapterLabel $chapter:$from-${numerals.formatNumber(segment.ref.range.last)}"
            }

            put(segment.segmentIndex, text)
        }
    }
}
