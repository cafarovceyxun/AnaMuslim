package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavFabPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strTitleAddBook
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FilterField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithBooksScreen(
    volumeSlug: String, 
    volumeName: String, 
    onBack: () -> Unit, 
    gridState: LazyGridState,
    onBookClick: (HadithBook) -> Unit,
) {
    val viewModel = viewModel { HadithViewModel() }
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsState()
    val isAuthenticated = session != null

    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showBookEditor by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isEmpty()) books
        else books.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (showBookEditor) {
        HadithEditorScreen(
            type = EditorType.BOOK,
            volumeSlug = volumeSlug,
            onBack = { showBookEditor = false }
        )
        return
    }

    LaunchedEffect(volumeSlug) {
        viewModel.fetchBooks(volumeSlug)
    }

    val density = LocalDensity.current
    val (expandedHeight, collapsedHeight) = getHadithIndexHeaderHeights()
    
    val topAppBarState = rememberTopAppBarState(
        initialHeightOffsetLimit = with(density) {
            -(expandedHeight - collapsedHeight).toPx()
        }
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    
    val isLandscape = isLandscape()
    
    LaunchedEffect(isLandscape, topAppBarState.heightOffsetLimit) {
        if (isLandscape && topAppBarState.heightOffsetLimit < 0f) {
            topAppBarState.heightOffset = topAppBarState.heightOffsetLimit
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.background,
        topBar = {
            HadithCollapsingToolbar(
                title = volumeName,
                scrollBehavior = scrollBehavior,
                expandedHeight = expandedHeight,
                collapsedHeight = collapsedHeight,
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

                    items(filteredBooks) { book ->
                        BookCard(book) { onBookClick(book) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCard(book: HadithBook, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(width = 1.dp, color = colorScheme.outlineVariant.alpha(0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colorScheme.secondaryContainer.alpha(0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.book_no.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.secondary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.onSurfaceVariant.alpha(0.3f)
            )
        }
    }
}
