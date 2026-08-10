@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cafarovceyxun.anamuslim.compose.screens.search

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_filter
import com.cafarovceyxun.anamuslim.resources.dr_icon_mic
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_search
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.results
import com.cafarovceyxun.anamuslim.resources.searchTipArabic
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strHintSearchQuran
import com.cafarovceyxun.anamuslim.resources.strLabelOff
import com.cafarovceyxun.anamuslim.resources.strLabelOn
import com.cafarovceyxun.anamuslim.resources.strTitleFilters
import com.cafarovceyxun.anamuslim.resources.strTitleQuran
import com.cafarovceyxun.anamuslim.resources.strTitleReaderChapters
import com.cafarovceyxun.anamuslim.resources.titleGlobalSearch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.cafarovceyxun.anamuslim.compose.navigation.MainTab
import com.cafarovceyxun.anamuslim.compose.navigation.TabReselectState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cafarovceyxun.anamuslim.compose.components.common.AppBarDefaults
import com.cafarovceyxun.anamuslim.compose.components.common.BackButton
import com.cafarovceyxun.anamuslim.compose.components.common.SearchTextField
import com.cafarovceyxun.anamuslim.compose.components.common.appBarInsetsPadding
import com.cafarovceyxun.anamuslim.compose.components.common.appBarRowHeight
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.search.QuickLinks
import com.cafarovceyxun.anamuslim.compose.components.search.SearchEmptyScrollContent
import com.cafarovceyxun.anamuslim.compose.components.search.SearchFiltersSheet
import com.cafarovceyxun.anamuslim.compose.components.search.SearchHistorySuggestionStrip
import com.cafarovceyxun.anamuslim.compose.components.search.SurahSearchResults
import com.cafarovceyxun.anamuslim.compose.components.search.TextSearchResults
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.theme.arabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.compose.utils.screenWidthDp
import com.cafarovceyxun.anamuslim.viewModels.QuranSearchViewModel
import com.cafarovceyxun.anamuslim.db.search.SearchHistoryEntry
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

import androidx.navigation.NavController

@Composable
fun SearchScreen(
    navController: NavController,
    supportsVoiceSearch: Boolean,
    voiceSearchFlow: SharedFlow<String>,
    onVoiceSearchClick: (inQuranText: Boolean) -> Unit,
) {
    val viewModel = viewModel { QuranSearchViewModel() }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshSearchHistory()
    }

    LaunchedEffect(Unit) {
        voiceSearchFlow.collect { viewModel.onQueryChange(it) }
    }

    val query by viewModel.searchQuery.collectAsState()

    // Only one `SearchQueryField` is composed at a time — the bar picks the compact or the full
    // layout — so a single requester for both call sites is safe.
    val queryFieldFocus = remember { FocusRequester() }

    // The field is the whole point of this tab, so re-tap always puts the caret back in it; with the
    // query cleared first, that reads as "start over" rather than "nothing happened".
    TabReselectState.OnTabReselect(MainTab.SEARCH) {
        if (query.isNotEmpty()) viewModel.onQueryChange("")
        queryFieldFocus.requestFocus()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            SearchTopBar(
                viewModel = viewModel,
                supportsVoiceSearch = supportsVoiceSearch,
                onVoiceSearchClick = onVoiceSearchClick,
                onFilterClick = { showFilterSheet = true },
                queryFieldFocus = queryFieldFocus,
            )
        },
    ) { padding ->
        // Only the top inset comes from the Scaffold: the bottom of the screen is deliberately left
        // open so results scroll *under* the floating bottom nav instead of stopping short of it in
        // a dead band. Each list clears the bar with its own `contentPadding`.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                QuickLinks(viewModel)
                SearchResultsPane(viewModel, navController, query)
            }
        }
    }

    val filtersState by viewModel.currentFilters.collectAsState()
    val availableTranslations by viewModel.availableTranslations.collectAsState()
    val quranTextEnabledState by viewModel.quranTextEnabled.collectAsState()

    SearchFiltersSheet(
        isOpen = showFilterSheet,
        onDismiss = { showFilterSheet = false },
        filters = filtersState,
        availableTranslations = availableTranslations,
        quranTextEnabled = quranTextEnabledState,
        onApplyFilters = {
            viewModel.setFilters(it)
            showFilterSheet = false
        }
    )
}

/**
 * The screen's whole top chrome as one Material 3 surface: navigation, query field, and the source
 * chips that scope the query.
 *
 * In landscape all three collapse onto the single [AppBarDefaults.BarHeightLandscape] navigation row:
 * the title drops (the field's own placeholder already says what the screen is), the field takes the
 * freed width, and the chips sit beside it. Landscape is wide and short, so spending width on chrome
 * is nearly free while spending height is not — the old stacked bar left barely a card's worth of
 * room for results.
 */
@Composable
private fun SearchTopBar(
    viewModel: QuranSearchViewModel,
    supportsVoiceSearch: Boolean,
    onVoiceSearchClick: (inQuranText: Boolean) -> Unit,
    onFilterClick: () -> Unit,
    queryFieldFocus: FocusRequester,
) {
    val compact = isLandscape()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surfaceContainer,
        shadowElevation = AppBarDefaults.ShadowElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appBarInsetsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .appBarRowHeight()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton()

                if (compact) {
                    SearchQueryField(
                        viewModel = viewModel,
                        supportsVoiceSearch = supportsVoiceSearch,
                        onVoiceSearchClick = onVoiceSearchClick,
                        focusRequester = queryFieldFocus,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                    )

                    SearchSourceChips(
                        viewModel = viewModel,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.titleGlobalSearch),
                        style = AppBarDefaults.titleStyle,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                    )
                }

                FilterAction(viewModel, onFilterClick)
            }

            if (!compact) {
                SearchQueryField(
                    viewModel = viewModel,
                    supportsVoiceSearch = supportsVoiceSearch,
                    onVoiceSearchClick = onVoiceSearchClick,
                    focusRequester = queryFieldFocus,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                )

                SearchSourceChips(
                    viewModel = viewModel,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                )
            } else {
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SearchQueryField(
    viewModel: QuranSearchViewModel,
    supportsVoiceSearch: Boolean,
    onVoiceSearchClick: (inQuranText: Boolean) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val onLabel = stringResource(Res.string.strLabelOn)
    val offLabel = stringResource(Res.string.strLabelOff)
    val arabicTipLabel = stringResource(Res.string.searchTipArabic)

    val query by viewModel.searchQuery.collectAsState()
    val quranTextEnabled by viewModel.quranTextEnabled.collectAsState()

    // In Quran-script mode the whole field flips: RTL layout so the caret starts at the right edge
    // and the icons mirror with it, plus the Arabic face and RTL text direction for what is typed.
    // Entering Arabic into a left-to-right Latin-styled box is what made the mode awkward to use.
    CompositionLocalProvider(
        LocalLayoutDirection provides if (quranTextEnabled) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
    SearchTextField(
        value = query,
        onValueChange = viewModel::onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        textStyle = if (quranTextEnabled) {
            typography.bodyLarge.copy(
                fontFamily = arabicFontFamily(),
                textDirection = TextDirection.Rtl,
            )
        } else null,
        placeholder = stringResource(
            if (quranTextEnabled) Res.string.searchTipArabic else Res.string.strHintSearchQuran
        ),
        // Matches the compact bar row in landscape so the field never outgrows the bar it sits in.
        minHeight = if (isLandscape()) 40.dp else 48.dp,
        leadingIcon = {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_search),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingContent = {
            if (supportsVoiceSearch) {
                FieldIconButton(
                    icon = Res.drawable.dr_icon_mic,
                    contentDescription = stringResource(Res.string.strHintSearch),
                    onClick = { onVoiceSearchClick(viewModel.quranTextEnabled.value) },
                )
            }

            FieldIconButton(
                icon = Res.drawable.dr_icon_quran_script,
                contentDescription = arabicTipLabel,
                tint = if (quranTextEnabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                onClick = {
                    viewModel.toggleQuranTextEnabled {
                        // Resolved up-front: Compose MP's getString is suspend, and this
                        // callback is not.
                        val stateLabel = if (it) onLabel else offLabel
                        PlatformUtils.showLongToast("$stateLabel: $arabicTipLabel")
                    }
                },
            )
        },
    )
    }
}

@Composable
private fun FieldIconButton(
    icon: org.jetbrains.compose.resources.DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = colorScheme.onSurfaceVariant,
) {
    SimpleTooltip(text = contentDescription) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(28.dp),
            colors = IconButtonDefaults.iconButtonColors(contentColor = tint),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FilterAction(
    viewModel: QuranSearchViewModel,
    onClick: () -> Unit,
) {
    val activeFilters by viewModel.currentFilters.collectAsState()
    val label = stringResource(Res.string.strTitleFilters)

    SimpleTooltip(text = label) {
        IconButton(onClick = onClick) {
            Box {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_filter),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                )
                if (!activeFilters.isEmpty) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Which corpora the query runs against, as Material 3 filter chips.
 *
 * These were full-size checkboxes with bolded labels, which read as a settings form rather than as
 * query scope and cost a whole 48dp row. Chips are the Material 3 component for exactly this, and
 * they fit beside the field even on the compact landscape bar.
 */
@Composable
private fun SearchSourceChips(
    viewModel: QuranSearchViewModel,
    modifier: Modifier = Modifier,
) {
    val filters by viewModel.currentFilters.collectAsState()

    // Wrap-content, not fillMaxWidth: the same strip has to sit under the field in portrait and
    // *beside* it on the landscape bar row, where filling the width would push the field out.
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceChip(
            label = stringResource(Res.string.strTitleQuran),
            selected = filters.searchQuran,
            onClick = { viewModel.toggleQuranSearch() },
        )
        SourceChip(
            label = stringResource(Res.string.hadith),
            selected = filters.searchHadith,
            onClick = { viewModel.toggleHadithSearch() },
        )
    }
}

@Composable
private fun SourceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = typography.labelLarge,
                maxLines = 1,
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else null,
    )
}

@Composable
private fun ColumnScope.SearchResultsPane(
    viewModel: QuranSearchViewModel,
    navController: NavController,
    query: String,
) {
    val quickLinks by viewModel.quickLinks.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    val historySuggestions = remember(query, quickLinks, searchHistory) {
        viewModel.historySuggestionsForDisplay(query, quickLinks)
    }

    val onHistorySelect: (String) -> Unit = { text ->
        viewModel.recordSearchQuery(text)
        viewModel.onQueryChange(text)
    }

    if (query.isBlank()) {
        SearchHistorySuggestionStrip(
            suggestions = historySuggestions,
            onSelect = onHistorySelect,
            modifier = Modifier.fillMaxWidth(),
        )
        SearchEmptyScrollContent(viewModel, Modifier.weight(1f))
        return
    }

    TabbedResults(viewModel, navController, historySuggestions, onHistorySelect)
}

private enum class SearchResultTab {
    results, chapters
}

@Composable
private fun ColumnScope.TabbedResults(
    viewModel: QuranSearchViewModel,
    navController: NavController,
    historySuggestions: List<SearchHistoryEntry>,
    onHistorySelect: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val tabs = remember {
        listOf(
            SearchResultTab.results,
            SearchResultTab.chapters,
        )
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )

    // No history write on tab changes: this collector fired on first composition too, saving whatever
    // half-typed word was in the field at that moment. The view model now saves a query once it
    // settles, which is both earlier and correct.
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val filterState by viewModel.currentFilters.collectAsState()
    val quranTextSearch by viewModel.quranTextEnabled.collectAsState()

    LaunchedEffect(filterState, quranTextSearch) {
        searchResults.refresh()
    }

    val surahResults by viewModel.surahResults.collectAsState()

    SearchTabsBar(
        tabs = tabs,
        counts = mapOf(
            SearchResultTab.results to if (searchResults.loadState.refresh is LoadState.Loading) null else searchResults.itemCount,
            SearchResultTab.chapters to surahResults?.size,
        ),
        selectedTabIndex = pagerState.currentPage,
        historySuggestions = historySuggestions,
        onHistorySelect = onHistorySelect,
        onTabSelected = {
            scope.launch {
                pagerState.animateScrollToPage(it)
            }
        },
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> TextSearchResults(viewModel, searchResults, !filterState.isEmpty, navController)
            1 -> SurahSearchResults(viewModel, surahResults)
        }
    }
}


/**
 * Minimum window width at which the recent-query chips and the results tabs share one row. Below it
 * the two would fight over the same few hundred dp, so they stay stacked.
 */
private val InlineStripMinWidth = 600.dp

/** Cap on the shared row's chip strip, so a long history can never crowd out the tabs beside it. */
private val SuggestionStripMaxWidth = 340.dp

/**
 * The band between the search field and the results: recent-query chips on the start edge, the
 * results/chapters tabs after them.
 *
 * On a wide (typically landscape) window these sit on one row, because there the scarce axis is
 * height — two stacked 40dp bands are most of what a short window has to give the results. With no
 * recent queries to show the strip renders nothing and the tabs simply stay where they were, at the
 * start edge.
 */
@Composable
private fun SearchTabsBar(
    tabs: List<SearchResultTab>,
    counts: Map<SearchResultTab, Int?>,
    selectedTabIndex: Int,
    historySuggestions: List<SearchHistoryEntry>,
    onHistorySelect: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
) {
    val inline = screenWidthDp() >= InlineStripMinWidth

    if (!inline) {
        SearchHistorySuggestionStrip(
            suggestions = historySuggestions,
            onSelect = onHistorySelect,
            modifier = Modifier.fillMaxWidth(),
        )
        SearchResultTabs(tabs, counts, selectedTabIndex, onTabSelected, fillWidth = true)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Measure order is what makes this work. A Row sizes its unweighted children first, against
        // the full width, and a scrollable tab row given the full width takes all of it — so weighting
        // the strip left it with nothing and the chips vanished. Unweighted, the strip measures to its
        // own chips (capped by [SuggestionStripMaxWidth], it scrolls past that), and the tabs take the
        // remainder as the weighted child, landing immediately to its right. An empty strip emits
        // nothing at all, which is what leaves the tabs at the start edge when there is no history.
        SearchHistorySuggestionStrip(
            suggestions = historySuggestions,
            onSelect = onHistorySelect,
            modifier = Modifier.widthIn(max = SuggestionStripMaxWidth),
            containerColor = Color.Transparent,
            contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            SearchResultTabs(tabs, counts, selectedTabIndex, onTabSelected, fillWidth = false)
        }
    }
}

@Composable
private fun SearchResultTabs(
    tabs: List<SearchResultTab>,
    counts: Map<SearchResultTab, Int?>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    fillWidth: Boolean,
) {
    val landscape = isLandscape()

    // The compact landscape row keeps the results from paying a full 48dp for two words.
    val rowModifier = Modifier
        .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
        .height(if (landscape) 40.dp else 48.dp)

    val tabContent: @Composable () -> Unit = {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTabIndex == index,
                selectedContentColor = colorScheme.primary,
                unselectedContentColor = colorScheme.onSurfaceVariant,
                onClick = { onTabSelected(index) },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                when (tab) {
                                    SearchResultTab.results -> Res.string.results
                                    SearchResultTab.chapters -> Res.string.strTitleReaderChapters
                                }
                            ),
                            style = typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val count = counts[tab]

                        if (count != null) {
                            Badge(
                                containerColor = colorScheme.secondaryContainer,
                                contentColor = colorScheme.onSecondaryContainer,
                            ) {
                                Text("$count")
                            }
                        }
                    }
                },
            )
        }
    }

    // Material 3 splits fixed and scrollable tabs by available width, and two tabs stretched across a
    // landscape window are exactly the case fixed tabs are not for: the labels end up marooned at the
    // one-quarter and three-quarter marks. Start-aligned scrollable tabs keep them read as a pair —
    // and they are the only option at all when the row is sharing its width with the chip strip.
    if (landscape || !fillWidth) {
        // A scrollable tab row sizes its own container to its tabs — which is exactly what the
        // shared-row layout needs, and why the full-width case paints the background on a wrapper
        // instead: left to itself the row would end mid-screen with results showing through beside it.
        Box(
            modifier = if (fillWidth) {
                Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow)
            } else {
                Modifier
            }
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                edgePadding = 12.dp,
                modifier = rowModifier,
                tabs = tabContent,
            )
        }
    } else {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colorScheme.surfaceContainerLow,
            modifier = rowModifier,
            tabs = tabContent,
        )
    }
}
