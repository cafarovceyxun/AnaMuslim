package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import com.cafarovceyxun.anamuslim.compose.utils.listColumnCount
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
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavContentPadding
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavFabPadding
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FilterField
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strLabelCountBabs
import com.cafarovceyxun.anamuslim.resources.strLabelHadithIntroduction
import com.cafarovceyxun.anamuslim.resources.strTitleAddBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithBooksScreen(
    volumeSlug: String,
    volumeName: String,
    onBack: () -> Unit,
    gridState: LazyGridState,
    onBookClick: (HadithBook) -> Unit,
    /** Hero logosuna toxunma — cildin mündəricat ağacını açır (vərəq yuxarıda saxlanılır). */
    onShowOutline: () -> Unit,
) {
    val viewModel = viewModel { HadithViewModel() }
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsState()
    val isAuthenticated = session != null

    val books by viewModel.books.collectAsState()
    val chapterCounts by viewModel.bookChapterCounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showBookEditor by remember { mutableStateOf(false) }
    var bookUnderEdit by remember { mutableStateOf<HadithBook?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isEmpty()) books
        else books.filter { hadithNameMatches(searchQuery, it.name, it.name_ar) }
    }

    if (showBookEditor || bookUnderEdit != null) {
        HadithEditorScreen(
            type = EditorType.BOOK,
            initialBook = bookUnderEdit,
            volumeSlug = volumeSlug,
            onBack = {
                showBookEditor = false
                bookUnderEdit = null
                // The list is a one-shot read of the local table, so an edit only shows up if we
                // pull it again on the way back.
                viewModel.fetchBooks(volumeSlug)
            }
        )
        return
    }

    LaunchedEffect(volumeSlug) {
        viewModel.fetchBooks(volumeSlug)
    }

    val topAppBarState = rememberCollapsingAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.background,
        topBar = {
            CollapsingAppBar(
                title = volumeName,
                scrollBehavior = scrollBehavior,
                logo = painterResource(Res.drawable.dr_icon_read_quran),
                onLogoClick = onShowOutline,
                logoLabel = stringResource(Res.string.strLabelHadithIntroduction),
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (isAuthenticated) {
                val bottomNavHeight = mainBottomNavFabPadding()
                HadithEditFab(
                    onClick = { showBookEditor = true },
                    contentDescription = stringResource(Res.string.strTitleAddBook),
                    modifier = Modifier.padding(bottom = bottomNavHeight),
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && books.isEmpty()) {
                Loader(true)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(listColumnCount()),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        // The edit FAB shares this strip with the bar, so the last row has to clear it too.
                        bottom = mainBottomNavContentPadding(if (isAuthenticated) 88.dp else 16.dp)
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

                    if (filteredBooks.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HadithIndexEmptyState()
                        }
                    }

                    items(filteredBooks) { book ->
                        val chapterCount = chapterCounts[book.slug] ?: 0
                        val displayName = rememberHadithDisplayName(book.name, book.name_ar)
                        HadithEntryCard(
                            // Only in the two-column layout: side by side, mismatched card heights are visible.
                            uniformHeight = listColumnCount() > 1,
                            title = displayName.text,
                            titleIsArabic = displayName.isArabic,
                            arabicTitle = displayName.secondaryArabic,
                            leadingText = book.book_no.toString(),
                            leadingColor = colorScheme.secondary,
                            leadingContainerColor = colorScheme.secondaryContainer,
                            titleMaxLines = 2,
                            countText = if (chapterCount > 0) {
                                stringResource(Res.string.strLabelCountBabs, chapterCount)
                            } else null,
                            onEdit = if (isAuthenticated) ({ bookUnderEdit = book }) else null,
                            onClick = { onBookClick(book) },
                        )
                    }
                }
            }
        }
    }
}
