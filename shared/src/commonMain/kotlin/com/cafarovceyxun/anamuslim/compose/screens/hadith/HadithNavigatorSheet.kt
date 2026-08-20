package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.resources.Res
import org.jetbrains.compose.resources.DrawableResource
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.dr_icon_settings
import com.cafarovceyxun.anamuslim.resources.search_in_babs
import com.cafarovceyxun.anamuslim.resources.search_in_books
import com.cafarovceyxun.anamuslim.resources.strHintSearchSubBabs
import com.cafarovceyxun.anamuslim.resources.strHintSearchVolumes
import com.cafarovceyxun.anamuslim.resources.strTitleBabs
import com.cafarovceyxun.anamuslim.resources.strTitleBooks
import com.cafarovceyxun.anamuslim.resources.strTitleSettings
import com.cafarovceyxun.anamuslim.resources.strTitleSubBabs
import com.cafarovceyxun.anamuslim.resources.strTitleVolumes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FilterField
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithNavigatorSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    initialVolumeSlug: String? = null,
    initialBookSlug: String? = null,
    initialChapterSlug: String? = null,
    initialSubChapterSlug: String? = null,
    onNavigate: (volumeSlug: String?, bookSlug: String?, chapterSlug: String?, subChapterSlug: String?, title: String) -> Unit,
) {
    if (!isOpen) return
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Force refresh content state when sheet is opened by using initial values as a key
    val contentKey = remember(initialVolumeSlug, initialBookSlug, initialChapterSlug, initialSubChapterSlug) {
        "${initialVolumeSlug}_${initialBookSlug}_${initialChapterSlug}_$initialSubChapterSlug"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = MaterialTheme.colorScheme.scrim.alpha(0.6f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.75f)) {
            key(contentKey) {
                HadithNavigatorContent(
                    initialVolumeSlug = initialVolumeSlug,
                    initialBookSlug = initialBookSlug,
                    initialChapterSlug = initialChapterSlug,
                    initialSubChapterSlug = initialSubChapterSlug,
                    onNavigate = onNavigate,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HadithNavigatorContent(
    initialVolumeSlug: String?,
    initialBookSlug: String?,
    initialChapterSlug: String?,
    initialSubChapterSlug: String?,
    onNavigate: (volumeSlug: String?, bookSlug: String?, chapterSlug: String?, subChapterSlug: String?, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    val viewModel = viewModel { HadithViewModel() }
    val scope = rememberCoroutineScope()
    
    var selectedVolumeSlug by remember { mutableStateOf(initialVolumeSlug) }
    var selectedBookSlug by remember { mutableStateOf(initialBookSlug) }
    var selectedChapterSlug by remember { mutableStateOf(initialChapterSlug) }
    var selectedSubChapterSlug by remember { mutableStateOf(initialSubChapterSlug) }

    var selectedTab by remember { 
        mutableIntStateOf(
            when {
                (initialSubChapterSlug != null && initialSubChapterSlug != "DIRECT_VIEW") -> 3
                initialChapterSlug != null -> 2
                initialBookSlug != null -> 1
                else -> 0
            }
        ) 
    }
    
    val tabs = listOf(
        stringResource(Res.string.strTitleVolumes),
        stringResource(Res.string.strTitleBooks),
        stringResource(Res.string.strTitleBabs),
        stringResource(Res.string.strTitleSubBabs)
    )
    
    var isCheckingSubChapters by remember { mutableStateOf(value = false) }

    LaunchedEffect(Unit) {
        if (initialVolumeSlug != null) {
            viewModel.fetchBooks(initialVolumeSlug)
        }
        if (initialBookSlug != null) {
            viewModel.fetchChapters(initialBookSlug)
        }
        if (initialChapterSlug != null) {
            viewModel.fetchSubChapters(initialChapterSlug)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.alpha(0.4f)) }
        ) {
            tabs.forEachIndexed { index, title ->
                val enabled = when (index) {
                    0 -> true
                    1 -> selectedVolumeSlug != null
                    2 -> selectedBookSlug != null
                    3 -> selectedChapterSlug != null
                    else -> false
                }
                
                Tab(
                    selected = selectedTab == index,
                    enabled = enabled,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary 
                                    else if (enabled) MaterialTheme.colorScheme.onSurface.alpha(0.7f)
                                    else MaterialTheme.colorScheme.onSurface.alpha(0.2f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> VolumesTab(viewModel, selectedVolumeSlug) { slug, _ -> 
                    selectedVolumeSlug = slug
                    selectedBookSlug = null
                    selectedChapterSlug = null
                    selectedSubChapterSlug = null
                    viewModel.fetchBooks(slug)
                    viewModel.fetchChapters("") // Clear chapters
                    viewModel.fetchSubChapters("") // Clear subs
                    selectedTab = 1
                }
                1 -> BooksTab(viewModel, selectedBookSlug) { volumeSlug, bookSlug, _ -> 
                    selectedVolumeSlug = volumeSlug
                    selectedBookSlug = bookSlug
                    selectedChapterSlug = null
                    selectedSubChapterSlug = null
                    viewModel.fetchChapters(bookSlug)
                    viewModel.fetchSubChapters("") // Clear subs
                    selectedTab = 2
                }
                2 -> ChaptersTab(viewModel, selectedChapterSlug) { _, bookSlug, chapterSlug, name -> 
                    com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithNav", "Chapter selected: $chapterSlug, parent book: $bookSlug")
                    // Update parent context if search was used
                    if (bookSlug.isNotEmpty()) {
                        selectedBookSlug = bookSlug
                    }
                    selectedChapterSlug = chapterSlug
                    selectedSubChapterSlug = null
                    
                    isCheckingSubChapters = true
                    scope.launch {
                        val hasSub = viewModel.hasSubChapters(chapterSlug)
                        isCheckingSubChapters = false
                        if (hasSub) {
                            com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithNav", "Chapter has sub-chapters, switching tab to 3")
                            viewModel.fetchSubChapters(chapterSlug)
                            selectedTab = 3
                        } else {
                            com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithNav", "Chapter has no sub-chapters, navigating directly")
                            onNavigate(selectedVolumeSlug, selectedBookSlug, chapterSlug, "DIRECT_VIEW", name)
                            onDismiss()
                        }
                    }
                }
                3 -> SubChaptersTab(viewModel, selectedSubChapterSlug) { chapterSlug, slug, name ->
                    com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithNav", "SubChapter selected: $slug, parent chapter: $chapterSlug")
                    // Update parent context if search was used
                    if (chapterSlug.isNotEmpty()) {
                        selectedChapterSlug = chapterSlug
                    }
                    selectedSubChapterSlug = slug
                    onNavigate(selectedVolumeSlug, selectedBookSlug, selectedChapterSlug, slug, name)
                    onDismiss()
                }
            }
            
            if (isCheckingSubChapters) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.alpha(0.5f)), contentAlignment = Alignment.Center) {
                    com.cafarovceyxun.anamuslim.compose.components.common.Loader(true)
                }
            }
        }
    }
}

@Composable
private fun VolumesTab(viewModel: HadithViewModel, currentSlug: String?, onSelect: (String, String) -> Unit) {
    val volumes by viewModel.volumes.collectAsState()
    val hadithActions = LocalHadithActions.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(volumes, query) {
        volumes.filter { hadithNameMatches(query, it.name, it.name_ar) }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(volumes, currentSlug) {
        if (currentSlug != null && volumes.isNotEmpty()) {
            val index = volumes.indexOfFirst { it.slug == currentSlug }
            if (index != -1) {
                delay(100L)
                listState.animateScrollToItem(index)
            }
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterField(
                value = query,
                onValueChange = { query = it },
                hint = stringResource(Res.string.strHintSearchVolumes),
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            IconButton(
                onClick = { hadithActions.onOpenSettings() },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.alpha(0.2f), RoundedCornerShape(14.dp))
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_settings),
                    contentDescription = stringResource(Res.string.strTitleSettings),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filtered, key = { it.slug }) { volume ->
                val displayName = rememberHadithDisplayName(volume.name, volume.name_ar)
                NavigatorItem(
                    title = displayName.text,
                    titleIsArabic = displayName.isArabic,
                    subtitle = volume.description,
                    isSelected = volume.slug == currentSlug,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    badgeIcon = Res.drawable.dr_icon_read_quran,
                    onClick = { onSelect(volume.slug, displayName.text) }
                )
            }
        }
    }
}

@Composable
private fun BooksTab(viewModel: HadithViewModel, currentSlug: String?, onSelect: (String, String, String) -> Unit) {
    val books by viewModel.books.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(books, query) {
        books.filter { hadithNameMatches(query, it.name, it.name_ar) }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(books, currentSlug) {
        if (currentSlug != null && books.isNotEmpty()) {
            val index = books.indexOfFirst { it.slug == currentSlug }
            if (index != -1) {
                delay(100L)
                listState.animateScrollToItem(index)
            }
        }
    }

    Column {
        FilterField(
            value = query,
            onValueChange = { query = it },
            hint = stringResource(Res.string.search_in_books),
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filtered, key = { it.slug }) { book ->
                val displayName = rememberHadithDisplayName(book.name, book.name_ar)
                NavigatorItem(
                    title = displayName.text,
                    titleIsArabic = displayName.isArabic,
                    number = book.book_no.toString(),
                    isSelected = book.slug == currentSlug,
                    badgeColor = MaterialTheme.colorScheme.secondary,
                    onClick = { onSelect(book.volume_slug, book.slug, displayName.text) }
                )
            }
        }
    }
}

@Composable
private fun ChaptersTab(viewModel: HadithViewModel, currentSlug: String?, onSelect: (String, String, String, String) -> Unit) {
    val chapters by viewModel.chapters.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(chapters, query) {
        chapters.filter { hadithNameMatches(query, it.name, it.name_ar) }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(chapters, currentSlug) {
        if (currentSlug != null && chapters.isNotEmpty()) {
            val index = chapters.indexOfFirst { it.slug == currentSlug }
            if (index != -1) {
                delay(100L)
                listState.animateScrollToItem(index)
            }
        }
    }

    Column {
        FilterField(
            value = query,
            onValueChange = { query = it },
            hint = stringResource(Res.string.search_in_babs),
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filtered, key = { it.slug }) { chapter ->
                val displayName = rememberHadithDisplayName(chapter.name, chapter.name_ar)
                NavigatorItem(
                    title = displayName.text,
                    titleIsArabic = displayName.isArabic,
                    number = chapter.chapter_no.toString(),
                    isSelected = chapter.slug == currentSlug,
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { onSelect("", chapter.book_slug, chapter.slug, displayName.text) }
                )
            }
        }
    }
}

@Composable
private fun SubChaptersTab(viewModel: HadithViewModel, currentSlug: String?, onSelect: (String, String, String) -> Unit) {
    val subChapters by viewModel.subChapters.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(subChapters, query) {
        subChapters.filter { hadithNameMatches(query, it.name, it.name_ar) }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(subChapters, currentSlug) {
        if (currentSlug != null && subChapters.isNotEmpty()) {
            val index = subChapters.indexOfFirst { it.slug == currentSlug }
            if (index != -1) {
                delay(100L)
                listState.animateScrollToItem(index)
            }
        }
    }

    Column {
        FilterField(
            value = query,
            onValueChange = { query = it },
            hint = stringResource(Res.string.strHintSearchSubBabs),
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filtered, key = { it.slug }) { sub ->
                val displayName = rememberHadithDisplayName(sub.name, sub.name_ar)
                NavigatorItem(
                    title = displayName.text,
                    titleIsArabic = displayName.isArabic,
                    number = sub.sub_chapter_no.toString(),
                    isSelected = sub.slug == currentSlug,
                    badgeColor = MaterialTheme.colorScheme.primary,
                    onClick = { onSelect(sub.chapter_slug, sub.slug, displayName.text) }
                )
            }
        }
    }
}

@Composable
private fun NavigatorItem(
    title: String,
    subtitle: String? = null,
    number: String? = null,
    isSelected: Boolean = false,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    badgeIcon: DrawableResource? = null,
    /** True when [title] is the level's Arabic name — see [rememberHadithDisplayName]. */
    titleIsArabic: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) badgeColor.alpha(0.08f) else Color.Transparent
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isSelected) badgeColor else badgeColor.alpha(0.12f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (badgeIcon != null) {
                    Icon(
                        painter = painterResource(badgeIcon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else badgeColor
                    )
                } else if (number != null) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else badgeColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = if (titleIsArabic) {
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 19.sp,
                            lineHeight = 30.sp,
                        ).withScriptDirection(
                            arabic = true,
                            arabicFontFamily = hadithArabicFontFamily(HadithPreferences.observeArabicFont()),
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp)
                            .withScriptDirection(arabic = false)
                    },
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) badgeColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.withScriptDirection(arabic = false),
                        color = MaterialTheme.colorScheme.onSurface.alpha(0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (isSelected) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_check),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.alpha(0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 74.dp, end = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.alpha(0.2f)
        )
    }
}
