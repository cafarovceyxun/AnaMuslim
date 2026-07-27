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
import com.cafarovceyxun.anamuslim.resources.strLabelAdditional
import com.cafarovceyxun.anamuslim.resources.strLabelCountHadiths
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithSubChaptersScreen(
    chapterSlug: String,
    chapterName: String,
    onBack: () -> Unit,
    gridState: LazyGridState,
    onSubChapterClick: (HadithSubChapter) -> Unit,
) {
    val viewModel = viewModel { HadithViewModel() }
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsState()
    val isAuthenticated = session != null

    val subChapters by viewModel.subChapters.collectAsState()
    val hadithCounts by viewModel.subChapterHadithCounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var editorType by remember { mutableStateOf<EditorType?>(null) }
    var subChapterUnderEdit by remember { mutableStateOf<HadithSubChapter?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    val filteredSubChapters = remember(subChapters, searchQuery) {
        if (searchQuery.isEmpty()) subChapters
        else subChapters.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (editorType != null || subChapterUnderEdit != null) {
        HadithEditorScreen(
            type = editorType ?: EditorType.SUB_CHAPTER,
            initialSubChapter = subChapterUnderEdit,
            chapterSlug = chapterSlug,
            onBack = {
                editorType = null
                subChapterUnderEdit = null
                viewModel.fetchSubChapters(chapterSlug)
            }
        )
        return
    }

    LaunchedEffect(chapterSlug) {
        viewModel.fetchSubChapters(chapterSlug)
    }

    val topAppBarState = rememberCollapsingAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.background,
        topBar = {
            CollapsingAppBar(
                title = chapterName,
                scrollBehavior = scrollBehavior,
                logo = painterResource(Res.drawable.dr_icon_read_quran),
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (isAuthenticated) {
                val bottomNavHeight = mainBottomNavFabPadding()
                HadithEditFab(
                    onClick = { editorType = EditorType.SUB_CHAPTER },
                    contentDescription = stringResource(Res.string.strLabelAdditional),
                    modifier = Modifier.padding(bottom = bottomNavHeight),
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && subChapters.isEmpty()) {
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

                    if (filteredSubChapters.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HadithIndexEmptyState()
                        }
                    }

                    items(filteredSubChapters) { subChapter ->
                        val hadithCount = hadithCounts[subChapter.slug] ?: 0
                        HadithEntryCard(
                            title = subChapter.name,
                            leadingText = subChapter.sub_chapter_no.toString(),
                            countText = if (hadithCount > 0) {
                                stringResource(Res.string.strLabelCountHadiths, hadithCount)
                            } else null,
                            countKind = HadithCountKind.HADITH,
                            onEdit = if (isAuthenticated) ({ subChapterUnderEdit = subChapter }) else null,
                            onClick = { onSubChapterClick(subChapter) },
                        )
                    }
                }
            }
        }
    }
}
