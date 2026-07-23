@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cafarovceyxun.anamuslim.compose.screens.search

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.clear
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_filter
import com.cafarovceyxun.anamuslim.resources.dr_icon_mic
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.results
import com.cafarovceyxun.anamuslim.resources.searchTipArabic
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strHintSearchQuran
import com.cafarovceyxun.anamuslim.resources.strLabelClose
import com.cafarovceyxun.anamuslim.resources.strLabelOff
import com.cafarovceyxun.anamuslim.resources.strLabelOn
import com.cafarovceyxun.anamuslim.resources.strTitleFilters
import com.cafarovceyxun.anamuslim.resources.strTitleQuran
import com.cafarovceyxun.anamuslim.resources.strTitleReaderChapters
import com.cafarovceyxun.anamuslim.resources.titleGlobalSearch
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.search.QuickLinks
import com.cafarovceyxun.anamuslim.compose.components.search.SearchEmptyScrollContent
import com.cafarovceyxun.anamuslim.compose.components.search.SearchFiltersSheet
import com.cafarovceyxun.anamuslim.compose.components.search.SearchHistorySuggestionStrip
import com.cafarovceyxun.anamuslim.compose.components.search.SurahSearchResults
import com.cafarovceyxun.anamuslim.compose.components.search.TextSearchResults
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.viewModels.QuranSearchViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            AppBar(
                title = stringResource(Res.string.titleGlobalSearch),
                actions = {
                    if (supportsVoiceSearch) {
                        IconButton(
                            onClick = {
                                onVoiceSearchClick(viewModel.quranTextEnabled.value)
                            }
                        ) {
                            Icon(
                                painterResource(Res.drawable.dr_icon_mic),
                                contentDescription = stringResource(Res.string.strHintSearch),
                            )
                        }
                    }

                    val activeFilters by viewModel.currentFilters.collectAsState()

                    IconButton(
                        onClick = { showFilterSheet = true },
                    ) {
                        Box {
                            Icon(
                                painterResource(Res.drawable.dr_icon_filter),
                                contentDescription = stringResource(Res.string.strTitleFilters),
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
                },
                shadowElevation = 0.dp
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 100.dp),
        ) {
            SearchBox(viewModel)
            
            SearchSourceFilters(viewModel)

            val query by viewModel.searchQuery.collectAsState()
            val quickLinks by viewModel.quickLinks.collectAsState()
            val searchHistory by viewModel.searchHistory.collectAsState()

            val historySuggestions = remember(query, quickLinks, searchHistory) {
                viewModel.historySuggestionsForDisplay(query, quickLinks)
            }

            SearchHistorySuggestionStrip(
                suggestions = historySuggestions,
                onSelect = { text ->
                    viewModel.recordSearchQuery(text)
                    viewModel.onQueryChange(text)
                },
            )

            QuickLinks(viewModel)

            TabbedResults(viewModel, navController)
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

@Composable
private fun SearchBox(viewModel: QuranSearchViewModel) {
    val onLabel = stringResource(Res.string.strLabelOn)
    val offLabel = stringResource(Res.string.strLabelOff)
    val arabicTipLabel = stringResource(Res.string.searchTipArabic)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var hasFocus by remember { mutableStateOf(false) }
    val bgColor = colorScheme.background

    val query by viewModel.searchQuery.collectAsState()
    val quranTextEnabled by viewModel.quranTextEnabled.collectAsState()

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorScheme.surfaceContainer,
            )
            .padding(
                start = 16.dp, end = 16.dp, bottom = 16.dp
            )
            .border(
                width = 1.dp,
                color = if (hasFocus) colorScheme.outline.alpha(0.75f) else colorScheme.outline.alpha(
                    0.4f
                ),
                shape = shapes.medium
            )
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged {
                hasFocus = it.hasFocus

                if (it.isFocused) {
                    keyboardController?.show()
                }
            },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = bgColor,
            focusedContainerColor = bgColor,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent
        ),
        placeholder = {
            Text(
                stringResource(if (quranTextEnabled) Res.string.searchTipArabic else Res.string.strHintSearchQuran),
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onBackground.alpha(0.6f)
            )
        },
        trailingIcon = {
            Row() {
                if (query.isNotEmpty()) {
                    SimpleTooltip(text = stringResource(Res.string.clear)) {
                        IconButton(
                            onClick = { viewModel.onQueryChange("") },
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_close),
                                contentDescription = stringResource(Res.string.strLabelClose),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                SimpleTooltip(text = stringResource(Res.string.searchTipArabic)) {
                    IconButton(
                        onClick = {
                            viewModel.toggleQuranTextEnabled() {
                                // Resolved up-front: Compose MP's getString is suspend, and this
                                // callback is not.
                                val stateLabel = if (it) onLabel else offLabel
                                PlatformUtils.showLongToast("$stateLabel: $arabicTipLabel")
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (quranTextEnabled) colorScheme.primary else LocalContentColor.current
                        ),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_quran_script),
                            contentDescription = stringResource(Res.string.searchTipArabic),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
        value = query,
        onValueChange = viewModel::onQueryChange,
        textStyle = MaterialTheme.typography.labelLarge,
        shape = MaterialTheme.shapes.medium,
        singleLine = true,
    )
}

private enum class SearchResultTab {
    results, chapters
}

@Composable
private fun ColumnScope.TabbedResults(viewModel: QuranSearchViewModel, navController: NavController) {
    val query by viewModel.searchQuery.collectAsState()

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

    if (query.isBlank()) {
        SearchEmptyScrollContent(viewModel, Modifier.weight(1f))
        return
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect {
                viewModel.recordCurrentSearchQuery()
            }
    }

    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val filterState by viewModel.currentFilters.collectAsState()
    val quranTextSearch by viewModel.quranTextEnabled.collectAsState()

    LaunchedEffect(filterState, quranTextSearch) {
        searchResults.refresh()
    }

    val surahResults by viewModel.surahResults.collectAsState()

    SearchResultTabs(
        tabs,
        counts = mapOf(
            SearchResultTab.results to if (searchResults.loadState.refresh is LoadState.Loading) null else searchResults.itemCount,
            SearchResultTab.chapters to surahResults?.size,
        ),
        selectedTabIndex = pagerState.currentPage,
    ) {
        scope.launch {
            pagerState.animateScrollToPage(it)
        }
    }

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


@Composable
private fun SearchResultTabs(
    tabs: List<SearchResultTab>,
    counts: Map<SearchResultTab, Int?>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainer),
        shadowElevation = 2.dp,
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = colorScheme.surfaceContainer,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index

                Tab(
                    selected = isSelected,
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
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            val count = counts[tab]

                            if (count != null) {
                                Badge(
                                    containerColor = Color.Gray.alpha(0.5f),
                                    contentColor = colorScheme.onBackground
                                ) {
                                    Text("$count")
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchSourceFilters(viewModel: QuranSearchViewModel) {
    val filters by viewModel.currentFilters.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceCheckbox(
            label = stringResource(Res.string.strTitleQuran),
            checked = filters.searchQuran,
            onCheckedChange = { viewModel.toggleQuranSearch() }
        )
        SourceCheckbox(
            label = stringResource(Res.string.hadith),
            checked = filters.searchHadith,
            onCheckedChange = { viewModel.toggleHadithSearch() }
        )
    }
}

@Composable
private fun SourceCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shapes.small)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                uncheckedColor = colorScheme.onSurface.alpha(0.6f)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}
