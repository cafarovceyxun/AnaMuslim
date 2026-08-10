@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.compose.components.common.CollapsingAppBar
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.rememberCollapsingAppBarState
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strLabelCountBooks
import com.cafarovceyxun.anamuslim.resources.strMsgDownloadHadithsFirst
import com.cafarovceyxun.anamuslim.resources.strTitleAddVolume
import com.cafarovceyxun.anamuslim.resources.strTitleHadith
import com.cafarovceyxun.anamuslim.resources.strTitleSettings
import com.cafarovceyxun.anamuslim.compose.utils.appScopedViewModelStoreOwner

import androidx.compose.foundation.gestures.animateScrollBy
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.utils.reader.ReaderScrollStep
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import com.cafarovceyxun.anamuslim.compose.utils.listColumnCount
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.cafarovceyxun.anamuslim.compose.navigation.MainTab
import com.cafarovceyxun.anamuslim.compose.navigation.TabReselectState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavContentPadding
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavFabPadding
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.ProgressOverlay
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FilterField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.compose.utils.serializableStateSaver
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume
import kotlinx.serialization.builtins.nullable
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import com.cafarovceyxun.anamuslim.utils.univ.EditEvent
import com.cafarovceyxun.anamuslim.utils.univ.EventBus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithIndexScreen(
    initialVolumeSlug: String? = null,
    initialBookSlug: String? = null,
    initialChapterSlug: String? = null,
    initialSubChapterSlug: String? = null,
    initialTitle: String? = null,
    initialHadithId: Long? = null,
    onNavigateToItems: ((volume: String?, book: String?, chapter: String?, sub: String?, title: String) -> Unit)? = null,
    /**
     * Leaving the screen entirely, from the volumes root where [stepBack] has nothing left to undo.
     * Null when this is a tab root and there is nowhere to go — the app bar then draws no back arrow
     * at all, which is the point: the root used to pass a no-op here, so the arrow was drawn and did
     * nothing. Non-null when the screen was pushed (the shared `HadithDetail` route, Android's
     * `ActivityHadith`).
     */
    onExit: (() -> Unit)? = null,
) {
    val viewModel = viewModel(appScopedViewModelStoreOwner()) { HadithViewModel() }
    val scope = rememberCoroutineScope()
    
    // Logic to auto-open if initials are provided
    LaunchedEffect(initialVolumeSlug, initialChapterSlug, onNavigateToItems) {
        if (initialVolumeSlug != null && (initialChapterSlug != null || initialSubChapterSlug != null) && onNavigateToItems != null) {
            onNavigateToItems(initialVolumeSlug, initialBookSlug, initialChapterSlug, initialSubChapterSlug, initialTitle ?: "")
        }
    }

    var selectedVolume by rememberSaveable(
        stateSaver = serializableStateSaver(HadithVolume.serializer().nullable)
    ) {
        mutableStateOf(initialVolumeSlug?.let { HadithVolume(it, initialTitle ?: "") })
    }
    var selectedBook by rememberSaveable(
        stateSaver = serializableStateSaver(HadithBook.serializer().nullable)
    ) {
        mutableStateOf(initialBookSlug?.let { HadithBook(it, initialVolumeSlug ?: "", 0, "") })
    }
    var selectedChapter by rememberSaveable(
        stateSaver = serializableStateSaver(HadithChapter.serializer().nullable)
    ) {
        mutableStateOf(initialChapterSlug?.let { HadithChapter(it, initialBookSlug ?: "", 0, "") })
    }
    var selectedSubChapter by rememberSaveable(
        stateSaver = serializableStateSaver(HadithSubChapter.serializer().nullable)
    ) {
        mutableStateOf(initialSubChapterSlug?.let {
            if (it != "DIRECT_VIEW") HadithSubChapter(it, initialChapterSlug ?: "", 0, initialTitle ?: "") else null
        })
    }

    var showDirectHadiths by rememberSaveable { mutableStateOf(initialSubChapterSlug == "DIRECT_VIEW") }
    var showVolumeEditor by rememberSaveable { mutableStateOf(false) }
    var volumeUnderEdit by rememberSaveable(
        stateSaver = serializableStateSaver(HadithVolume.serializer().nullable)
    ) {
        mutableStateOf<HadithVolume?>(null)
    }
    LaunchedEffect(initialHadithId) {
        if (initialHadithId != null) {
            scope.launch {
                val hadith = viewModel.getHadithById(initialHadithId)
                if (hadith != null) {
                    val chapterSlug = hadith.chapter_slug
                    if (chapterSlug != null) {
                        val chapter = viewModel.getChapterBySlug(chapterSlug)
                        val book = chapter?.let { viewModel.getBookBySlug(it.book_slug) }
                        
                        selectedVolume = book?.let { HadithVolume(it.volume_slug, "") }
                        selectedBook = book
                        selectedChapter = chapter
                        selectedSubChapter = hadith.sub_chapter_slug?.let { 
                            if (it != "DIRECT_VIEW") HadithSubChapter(it, chapterSlug, 0, "") else null
                        }
                    }
                    showDirectHadiths = (hadith.sub_chapter_slug == null || hadith.sub_chapter_slug == "DIRECT_VIEW")
                }
            }
        }
    }

    val volumesListState = rememberLazyGridState()
    val booksListState = rememberLazyGridState()
    val chaptersListState = rememberLazyGridState()
    val subChaptersListState = rememberLazyGridState()

    // The index grids move by the same viewport-relative step as the readers; unlike them it picks
    // whichever grid is on screen. `direction` is 1 down / -1 up (see [HadithViewModel.scrollEvent]).
    val scrollStepPercent = AppPreferences.observeReaderScrollStepPercent()
    LaunchedEffect(scrollStepPercent) {
        viewModel.scrollEvent.collect { direction ->
            val grid = when {
                selectedSubChapter != null || showDirectHadiths -> null // Collected in HadithItemsScreen
                selectedChapter != null -> subChaptersListState
                selectedBook != null -> chaptersListState
                selectedVolume != null -> booksListState
                else -> volumesListState
            } ?: return@collect

            val step = ReaderScrollStep.stepPx(grid.layoutInfo.viewportSize.height, scrollStepPercent)
            if (step <= 0f) return@collect
            grid.animateScrollBy(direction * step, ReaderScrollStep.animationSpec)
        }
    }

    val isUpdating = false // viewModel.isUpdating.collectAsState()
    val isUpdated = false // viewModel.isUpdated.collectAsState()

    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsStateWithLifecycle()
    val isAuthenticated = session != null

    LaunchedEffect(Unit) {
        EventBus.events.collectLatest { event ->
            if (event is EditEvent) {
                if (isAuthenticated) {
                    showVolumeEditor = true
                }
            }
        }
    }

    // Unlike every other tab, this screen keeps its levels in `rememberSaveable` state instead of on
    // the back stack, so the route stays `hadith` and the bottom bar stays visible however deep the
    // user is — which is what makes re-tapping the tab a usable way back out. False only at the
    // volumes root, where there is nothing above to return to.
    val canStepBack = showVolumeEditor || volumeUnderEdit != null || showDirectHadiths ||
        selectedSubChapter != null || selectedChapter != null || selectedBook != null ||
        selectedVolume != null

    // The single definition of "one level up", shared by the tab re-tap, the system back gesture and
    // every app-bar back button below. Kept in one place because these used to be four separate
    // literals that had to agree with each other.
    fun stepBack() {
        when {
            showVolumeEditor || volumeUnderEdit != null -> {
                showVolumeEditor = false
                volumeUnderEdit = null
            }
            // Direct view is reached from a chapter that has no sub-chapters, so its way back is the
            // chapter list two levels up — not the sub-chapter list that was never shown.
            showDirectHadiths -> {
                showDirectHadiths = false
                selectedChapter = null
            }
            selectedSubChapter != null -> selectedSubChapter = null
            selectedChapter != null -> selectedChapter = null
            selectedBook != null -> selectedBook = null
            selectedVolume != null -> selectedVolume = null
        }
    }

    TabReselectState.OnTabReselect(MainTab.HADITH) {
        if (canStepBack) stepBack() else scope.launch { volumesListState.animateScrollToItem(0) }
    }

    if (showVolumeEditor || volumeUnderEdit != null) {
        HadithEditorScreen(
            type = EditorType.VOLUME,
            initialVolume = volumeUnderEdit,
            reserveBottomSpace = true,
            onBack = { stepBack() }
        )
        return
    }

    when {
        selectedSubChapter != null || showDirectHadiths -> {
            val title = selectedSubChapter?.name ?: selectedChapter?.name ?: ""
            val subSlug = if (showDirectHadiths) "DIRECT_VIEW" else selectedSubChapter?.slug

            // Geri naviqasiya məntiqi
            val handleBack = { stepBack() }

            BackHandler(onBack = handleBack)

            HadithItemsScreen(
                title = title,
                volumeSlug = selectedVolume?.slug,
                bookSlug = selectedBook?.slug,
                chapterSlug = selectedChapter?.slug,
                subChapterSlug = subSlug,
                onBack = handleBack,
                onNavigate = { v, b, c, s, newTitle ->
                    if (onNavigateToItems != null) {
                        onNavigateToItems(v, b, c, s, newTitle)
                    } else {
                        selectedVolume = v?.let { HadithVolume(it, "") }
                        selectedBook = b?.let { HadithBook(it, v ?: "", 0, "") }
                        selectedChapter = c?.let { HadithChapter(it, b ?: "", 0, "") }
                        selectedSubChapter = s?.let { if (it != "DIRECT_VIEW") HadithSubChapter(it, c ?: "", 0, newTitle) else null }
                        showDirectHadiths = (s == "DIRECT_VIEW")
                    }
                }
            )
        }
        selectedChapter != null -> {
            BackHandler { stepBack() }
            HadithSubChaptersScreen(
                chapterSlug = selectedChapter!!.slug,
                chapterName = selectedChapter!!.name,
                onBack = { stepBack() },
                gridState = subChaptersListState,
                onSubChapterClick = {
                    scope.launch {
                        HadithPreferences.setViewMode(0) // Force Mixed Mode for direct selection
                        if (onNavigateToItems != null) {
                            onNavigateToItems(selectedVolume?.slug, selectedBook?.slug, selectedChapter?.slug, it.slug, it.name)
                        } else {
                            selectedSubChapter = it
                        }
                    }
                }
            )
        }
        selectedBook != null -> {
            BackHandler { stepBack() }
            HadithChaptersScreen(
                bookSlug = selectedBook!!.slug,
                bookName = selectedBook!!.name,
                onBack = { stepBack() },
                gridState = chaptersListState,
                onChapterClick = { chapter ->
                    scope.launch {
                        val hasSub = viewModel.hasSubChapters(chapter.slug)
                        // Force Mixed Mode for direct selection
                        HadithPreferences.setViewMode(0)
                        if (hasSub) {
                            selectedChapter = chapter
                        } else {
                            if (onNavigateToItems != null) {
                                onNavigateToItems(selectedVolume?.slug, selectedBook?.slug, chapter.slug, "DIRECT_VIEW", chapter.name)
                            } else {
                                selectedChapter = chapter
                                showDirectHadiths = true
                            }
                        }
                    }
                }
            )
        }
        selectedVolume != null -> {
            BackHandler { stepBack() }
            HadithBooksScreen(
                volumeSlug = selectedVolume!!.slug,
                volumeName = selectedVolume!!.name,
                onBack = { stepBack() },
                gridState = booksListState,
                onBookClick = { selectedBook = it }
            )
        }
        else -> {
            // No wrapper Scaffold here: `HadithVolumesList` brings its own Scaffold and app bar, and
            // that bar already clears the status bar and camera cutout itself. Wrapping it and
            // re-applying `paddingValues.calculateTopPadding()` pushed the whole screen — bar
            // included — below the inset, leaving an unpainted strip around the camera cutout that
            // no other hadith level had.
            HadithVolumesList(
                gridState = volumesListState,
                onVolumeClick = { selectedVolume = it },
                onBack = onExit,
                isAuthenticated = isAuthenticated,
                onAddClick = { showVolumeEditor = true },
                onEditVolume = { volumeUnderEdit = it }
            )
        }
    }

    ProgressOverlay(
        isUpdating = isUpdating,
        isUpdated = isUpdated
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HadithVolumesList(
    gridState: LazyGridState,
    onVolumeClick: (HadithVolume) -> Unit,
    /** Null draws no back arrow — see [HadithIndexScreen]'s `onExit`. */
    onBack: (() -> Unit)?,
    isAuthenticated: Boolean,
    onAddClick: () -> Unit,
    onEditVolume: (HadithVolume) -> Unit,
) {
    val viewModel = viewModel { HadithViewModel() }
    val volumes by viewModel.volumes.collectAsState()
    val bookCounts by viewModel.volumeBookCounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val filteredVolumes = remember(volumes, searchQuery) {
        if (searchQuery.isEmpty()) volumes
        else volumes.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val topAppBarState = rememberCollapsingAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            val hadithActions = LocalHadithActions.current
            CollapsingAppBar(
                title = stringResource(Res.string.strTitleHadith),
                scrollBehavior = scrollBehavior,
                logo = painterResource(Res.drawable.dr_icon_read_quran),
                onBack = onBack,
                actions = {
                    val settingsLabel = stringResource(Res.string.strTitleSettings)
                    SimpleTooltip(text = settingsLabel) {
                        IconButton(
                            onClick = { hadithActions.onOpenSettings() },
                            painter = painterResource(Res.drawable.dr_icon_settings),
                            contentDescription = settingsLabel,
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAuthenticated) {
                val bottomNavHeight = mainBottomNavFabPadding()
                HadithEditFab(
                    onClick = onAddClick,
                    contentDescription = stringResource(Res.string.strTitleAddVolume),
                    modifier = Modifier.padding(bottom = bottomNavHeight),
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(listColumnCount()),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                // The edit FAB sits in this strip too, so the last row has to clear it as well —
                // but only when it is actually there, or every reader gets a hole at the end.
                bottom = mainBottomNavContentPadding(if (isAuthenticated) 88.dp else 16.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading && volumes.isEmpty()) {
                item { Loader(true) }
            } else if (volumes.isEmpty()) {
                item {
                    val hadithActions = LocalHadithActions.current
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_download),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = colorScheme.primary.alpha(0.5f)
                            )
                            Text(
                                text = stringResource(Res.string.strMsgDownloadHadithsFirst),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    hadithActions.onOpenSettings()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(Res.string.strTitleSettings))
                            }
                        }
                    }
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterField(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        hint = stringResource(Res.string.strHintSearch),
                        keyboardType = KeyboardType.Text,
                    )
                }

                if (filteredVolumes.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HadithIndexEmptyState()
                    }
                }

                items(filteredVolumes) { volume ->
                    val bookCount = bookCounts[volume.slug] ?: 0
                    HadithEntryCard(
                        // Only in the two-column layout: side by side, mismatched card heights are visible.
                        uniformHeight = listColumnCount() > 1,
                        title = volume.name,
                        arabicTitle = volume.name_ar,
                        titleStyle = MaterialTheme.typography.titleMedium,
                        leadingIcon = Res.drawable.dr_icon_read_quran,
                        subtitle = volume.author,
                        supportingText = volume.description,
                        countText = if (bookCount > 0) {
                            stringResource(Res.string.strLabelCountBooks, bookCount)
                        } else null,
                        onEdit = if (isAuthenticated) ({ onEditVolume(volume) }) else null,
                        onClick = { onVolumeClick(volume) },
                    )
                }
            }
        }
    }
}

