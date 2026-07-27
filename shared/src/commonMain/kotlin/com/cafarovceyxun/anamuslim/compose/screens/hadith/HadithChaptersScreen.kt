package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.CollapsingAppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.rememberCollapsingAppBarState
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavFabPadding
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FilterField
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strLabelCountHadiths
import com.cafarovceyxun.anamuslim.resources.strLabelCountSubBabs
import com.cafarovceyxun.anamuslim.resources.strTitleAddBab
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithChaptersScreen(
    bookSlug: String,
    bookName: String,
    onBack: () -> Unit,
    gridState: LazyGridState,
    onChapterClick: (HadithChapter) -> Unit,
) {
    val viewModel = viewModel { HadithViewModel() }
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsState()
    val isAuthenticated = session != null

    val chapters by viewModel.chapters.collectAsState()
    val subChapterCounts by viewModel.chapterSubChapterCounts.collectAsState()
    val hadithCounts by viewModel.chapterHadithCounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var chapterUnderEdit by remember { mutableStateOf<HadithChapter?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredChapters = remember(chapters, searchQuery) {
        if (searchQuery.isEmpty()) chapters
        else chapters.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (showEditor || chapterUnderEdit != null) {
        HadithEditorScreen(
            type = EditorType.CHAPTER,
            initialChapter = chapterUnderEdit,
            bookSlug = bookSlug,
            onBack = {
                showEditor = false
                chapterUnderEdit = null
                viewModel.fetchChapters(bookSlug)
            }
        )
        return
    }

    LaunchedEffect(bookSlug) {
        viewModel.fetchChapters(bookSlug)
    }

    val topAppBarState = rememberCollapsingAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.background,
        topBar = {
            CollapsingAppBar(
                title = bookName,
                scrollBehavior = scrollBehavior,
                logo = painterResource(Res.drawable.dr_icon_read_quran),
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (isAuthenticated) {
                val bottomNavHeight = mainBottomNavFabPadding()
                HadithEditFab(
                    onClick = { showEditor = true },
                    contentDescription = stringResource(Res.string.strTitleAddBab),
                    modifier = Modifier.padding(bottom = bottomNavHeight),
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && chapters.isEmpty()) {
                Loader(true)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FilterField(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            hint = stringResource(Res.string.strHintSearch),
                            keyboardType = KeyboardType.Text,
                        )
                    }

                    if (filteredChapters.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HadithIndexEmptyState()
                        }
                    }

                    items(filteredChapters) { chapter ->
                        // A bab either groups its hadiths into sub-babs or holds them directly, and
                        // tapping it goes wherever the count points — so the counter names whichever
                        // of the two the user is about to open.
                        val subChapterCount = subChapterCounts[chapter.slug] ?: 0
                        val hadithCount = hadithCounts[chapter.slug] ?: 0
                        val hasSubChapters = subChapterCount > 0
                        HadithEntryCard(
                            title = chapter.name,
                            leadingText = chapter.chapter_no.toString(),
                            leadingColor = colorScheme.tertiary,
                            leadingContainerColor = colorScheme.tertiaryContainer,
                            countText = when {
                                hasSubChapters ->
                                    stringResource(Res.string.strLabelCountSubBabs, subChapterCount)
                                hadithCount > 0 ->
                                    stringResource(Res.string.strLabelCountHadiths, hadithCount)
                                else -> null
                            },
                            countKind = if (hasSubChapters) {
                                HadithCountKind.SECTION
                            } else {
                                HadithCountKind.HADITH
                            },
                            onEdit = if (isAuthenticated) ({ chapterUnderEdit = chapter }) else null,
                            onClick = { onChapterClick(chapter) },
                        )
                    }
                }
            }
        }
    }
}
