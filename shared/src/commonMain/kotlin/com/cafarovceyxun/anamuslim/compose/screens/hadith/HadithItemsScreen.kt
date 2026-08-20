@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.cafarovceyxun.anamuslim.compose.screens.hadith

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dialog_add_sub_chapter_or_hadith
import com.cafarovceyxun.anamuslim.resources.direct_hadith
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_heart_filled
import com.cafarovceyxun.anamuslim.resources.dr_icon_info
import com.cafarovceyxun.anamuslim.resources.dr_icon_share
import com.cafarovceyxun.anamuslim.resources.enterFullscreen
import com.cafarovceyxun.anamuslim.resources.exitFullscreen
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelRemove
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarkDeleteThis
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.BookmarkNoteSheet
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.repository.BookmarkAddResult
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkAddedAlready
import com.cafarovceyxun.anamuslim.resources.strMsgHadithBookmarkAdded
import com.cafarovceyxun.anamuslim.resources.strMsgHadithBookmarkAddFailed
import com.cafarovceyxun.anamuslim.resources.strMsgHadithBookmarkRemoved
import org.jetbrains.compose.resources.getString
import com.cafarovceyxun.anamuslim.resources.ic_expand
import com.cafarovceyxun.anamuslim.resources.ic_shrink
import com.cafarovceyxun.anamuslim.resources.new_sub_chapter
import com.cafarovceyxun.anamuslim.resources.nextBab
import com.cafarovceyxun.anamuslim.resources.previousBab
import com.cafarovceyxun.anamuslim.resources.select_option
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strLabelHadithNo
import com.cafarovceyxun.anamuslim.resources.strLabelShare
import com.cafarovceyxun.anamuslim.resources.strMsgDownloadHadithsFromSettings
import com.cafarovceyxun.anamuslim.resources.strTitleNote
import com.cafarovceyxun.anamuslim.resources.strTitleVOTD
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape
import com.cafarovceyxun.anamuslim.compose.utils.appScopedViewModelStoreOwner

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.reader.IsVotd
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderKeyScrollEffect
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderToggleFeedbackOverlay
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderToggleKind
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.AutoScrollSheet
import com.cafarovceyxun.anamuslim.compose.components.reader.AutoScrollEffect
import com.cafarovceyxun.anamuslim.compose.components.reader.AutoScrollGestureOverlay
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import com.cafarovceyxun.anamuslim.compose.theme.alpha as colorAlpha
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.app.KeepScreenOnIfEnabled
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberToggleScreenRotation
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.DailyContentViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithListItem
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import androidx.compose.ui.unit.Dp

private data class HadithNavigationTarget(
    val title: String,
    val volumeSlug: String?,
    val bookSlug: String?,
    val bookName: String?,
    val chapterSlug: String?,
    val chapterName: String?,
    val subChapterSlug: String?,
)

// `[\s\S]` matches any char including newlines, so this needs no RegexOption — DOT_MATCHES_ALL is
// JVM/Native-only and unavailable in the common stdlib (breaks commonMain metadata / IDE analysis).
private val parenthesesRegex = Regex("\\(([\\s\\S]*?)\\)")

private fun formatHadithText(
    text: String,
    showParentheses: Boolean,
    highlightParentheses: Boolean,
    highlightColor: Color
): AnnotatedString {
    var processedText = text
    if (!showParentheses) {
        processedText = processedText.replace(parenthesesRegex, "").replace(Regex("\\s+"), " ").trim()
    }

    return buildAnnotatedString {
        var lastIndex = 0
        val allMatches = mutableListOf<MatchResult>()
        
        // Optionally highlight parentheses
        parenthesesRegex.findAll(processedText).forEach { allMatches.add(it) }
        
        val sortedMatches = allMatches.sortedBy { it.range.first }

        sortedMatches.forEach { matchResult ->
            if (matchResult.range.first >= lastIndex) { // Avoid overlapping if any
                append(processedText.substring(lastIndex, matchResult.range.first))

                if (highlightParentheses) {
                    withStyle(SpanStyle(color = highlightColor)) {
                        append(matchResult.value)
                    }
                } else {
                    append(matchResult.value)
                }
                lastIndex = matchResult.range.last + 1
            }
        }
        append(processedText.substring(lastIndex))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithItemsScreen(
    title: String,
    volumeSlug: String? = null,
    bookSlug: String? = null,
    chapterSlug: String? = null,
    subChapterSlug: String? = null,
    onBack: () -> Unit,
    onNavigate: ((volume: String?, book: String?, chapter: String?, sub: String?, title: String) -> Unit)? = null
) {
    KeepScreenOnIfEnabled()
    val hadithViewModel = viewModel(appScopedViewModelStoreOwner()) { HadithViewModel() }
    
    LaunchedEffect(Unit) {
        hadithViewModel.isReadingActive = true
    }

    DisposableEffect(Unit) {
        onDispose {
            hadithViewModel.isReadingActive = false
        }
    }

    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsStateWithLifecycle()
    val isAuthorized = session != null
    val isAuthenticated = session != null

    val hadiths by hadithViewModel.hadiths.collectAsStateWithLifecycle()
    val subChapters by hadithViewModel.subChapters.collectAsStateWithLifecycle()
    val combinedItems by hadithViewModel.combinedItems.collectAsStateWithLifecycle()
    val isLoading by hadithViewModel.isLoading.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val selectedTab = HadithPreferences.observeViewMode()
    
    var currentTitle by rememberSaveable(title) { mutableStateOf(title) }
    var currentChapterSlug by rememberSaveable(chapterSlug) { mutableStateOf(chapterSlug) }
    var currentSubChapterSlug by rememberSaveable(subChapterSlug) { mutableStateOf(subChapterSlug) }
    var currentBookSlug by rememberSaveable(bookSlug) { mutableStateOf(bookSlug) }

    // Consolidate preference observation at the screen level
    val arabicEnabled = HadithPreferences.observeArabicEnabled()
    val azerbaijaniEnabled = HadithPreferences.observeAzerbaijaniEnabled()
    val sourceEnabled = HadithPreferences.observeSourceEnabled()
    val arabicSizeMult = HadithPreferences.observeArabicSizeMultiplier()
    val azerbaijaniSizeMult = HadithPreferences.observeAzerbaijaniSizeMultiplier()
    val selectedFont = HadithPreferences.observeArabicFont()
    val showParentheses = HadithPreferences.observeShowParentheses()
    val highlightParentheses = HadithPreferences.observeHighlightParentheses()

    val dailyContentViewModel = viewModel { DailyContentViewModel() }
    val todayContent by dailyContentViewModel.todayContent.collectAsStateWithLifecycle()
    
    val arabicFontFamily = hadithArabicFontFamily(selectedFont)

    var editorType by remember { mutableStateOf<EditorType?>(null) }
    var showChoiceDialog by remember { mutableStateOf(value = false) }
    var showSettings by remember { mutableStateOf(value = false) }
    var showAutoScroll by remember { mutableStateOf(value = false) }
    var showNavigator by remember { mutableStateOf(value = false) }
    var isFullscreen by rememberSaveable { mutableStateOf(value = false) }
    var sharingHadith by remember { mutableStateOf<Hadith?>(null) }
    var savingHadith by remember { mutableStateOf<Hadith?>(null) }
    var removingHadith by remember { mutableStateOf<Hadith?>(null) }

    val userRepository = remember { RepositoryProvider.userRepository }
    val bookmarkedHadithIds by userRepository.getBookmarkedHadithIdsFlow()
        .collectAsStateWithLifecycle(emptySet())

    // Saxlanılmış hədisə təkrar basmaq onu siyahıdan çıxarır; yenisi üçün qeyd formu açılır.
    val onHadithBookmarkClick: (Hadith) -> Unit = { hadith ->
        val hadithId = hadith.id
        if (hadithId != null && hadithId in bookmarkedHadithIds) {
            removingHadith = hadith
        } else if (hadithId != null) {
            savingHadith = hadith
        }
    }
    var editingHadith by remember { mutableStateOf<Hadith?>(null) }

    if (editorType != null) {
        HadithEditorScreen(
            type = editorType!!,
            chapterSlug = currentChapterSlug,
            subChapterSlug = if (currentSubChapterSlug == "DIRECT_VIEW") null else currentSubChapterSlug,
            reserveBottomSpace = false,
            onBack = {
                editorType = null
                if (currentSubChapterSlug != null && currentSubChapterSlug != "DIRECT_VIEW") hadithViewModel.fetchHadithsBySubChapter(currentChapterSlug!!, currentSubChapterSlug!!)
                else if (currentChapterSlug != null) hadithViewModel.fetchHadithsByChapter(currentChapterSlug!!)
            }
        )
        return
    }

    if (editingHadith != null) {
        HadithEditorScreen(
            type = EditorType.HADITH,
            initialHadith = editingHadith,
            chapterSlug = currentChapterSlug,
            subChapterSlug = if (currentSubChapterSlug == "DIRECT_VIEW") null else currentSubChapterSlug,
            reserveBottomSpace = false,
            onBack = {
                editingHadith = null
                if (currentSubChapterSlug != null && currentSubChapterSlug != "DIRECT_VIEW") hadithViewModel.fetchHadithsBySubChapter(currentChapterSlug!!, currentSubChapterSlug!!)
                else if (currentChapterSlug != null) hadithViewModel.fetchHadithsByChapter(currentChapterSlug!!)
            }
        )
        return
    }

    if (showChoiceDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showChoiceDialog = false },
            title = { Text(stringResource(Res.string.select_option)) },
                        text = { Text(stringResource(Res.string.dialog_add_sub_chapter_or_hadith)) },
            confirmButton = {
                TextButton(onClick = { 
                    showChoiceDialog = false
                    editorType = EditorType.SUB_CHAPTER 
                }) { Text(stringResource(Res.string.new_sub_chapter)) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showChoiceDialog = false
                    editorType = EditorType.HADITH 
                }) { Text(stringResource(Res.string.direct_hadith)) }
            }
        )
    }

    LaunchedEffect(volumeSlug, currentChapterSlug, currentSubChapterSlug, currentTitle) {
        com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithReader", "Context effect triggered: v=$volumeSlug, c=$currentChapterSlug, s=$currentSubChapterSlug")
        if (volumeSlug != null) {
            hadithViewModel.fetchFullVolume(volumeSlug)
            hadithViewModel.saveReadHistory(volumeSlug, currentBookSlug, currentChapterSlug, currentSubChapterSlug, currentTitle)
        }
        
        if (currentSubChapterSlug != null && currentSubChapterSlug != "DIRECT_VIEW") {
            hadithViewModel.fetchHadithsBySubChapter(currentChapterSlug!!, currentSubChapterSlug!!)
        } else if (currentChapterSlug != null) {
            hadithViewModel.fetchHadithsByChapter(currentChapterSlug!!)
            hadithViewModel.fetchSubChapters(currentChapterSlug!!)
        }
    }

    val appBarDims = rememberHadithAppBarDimensions()
    // Keyed on the target identity so a newly opened bab starts with a fresh scroll
    // position instead of inheriting the previous bab's (which the tracker would then
    // capture as a stale anchor). Params are constant per back-stack entry, so this
    // still survives rotation within the same bab.
    val listState = rememberSaveable(
        volumeSlug, bookSlug, chapterSlug, subChapterSlug,
        saver = LazyListState.Saver
    ) { LazyListState() }

    // Keyed on the target identity: opening a different bab must NOT inherit the
    // previous bab's saved anchor/scroll-progress (otherwise going straight to a
    // mode tab restores the old bab's position). The params are constant for the
    // lifetime of one back-stack entry, so this never resets while switching tabs
    // within the same bab.
    var activeHadithKey by rememberSaveable(volumeSlug, bookSlug, chapterSlug, subChapterSlug) {
        mutableStateOf<String?>(null)
    }
    var activeHadithOffset by rememberSaveable(volumeSlug, bookSlug, chapterSlug, subChapterSlug) { mutableIntStateOf(0) }
    var jumpTrigger by rememberSaveable { mutableIntStateOf(0) }
    var hasScrolledToInitial by rememberSaveable(volumeSlug, bookSlug, chapterSlug, subChapterSlug) { mutableStateOf(value = false) }
    var isSwitchingTab by remember { mutableStateOf(value = false) }
    var lastSelectedTab by remember { mutableIntStateOf(selectedTab) }

    fun getItemKey(item: HadithListItem): String {
        return when (item) {
            is HadithListItem.BookHeader -> "b_${item.book.slug}"
            is HadithListItem.ChapterHeader -> "c_${item.chapter.slug}"
            is HadithListItem.SubChapterHeader -> "s_${item.subChapter.slug}"
            is HadithListItem.ContextGroupedHeader -> {
                val b = item.book?.slug ?: "nb"
                val c = item.chapter?.slug ?: "nc"
                val s = item.subChapter?.slug ?: "ns"
                "g_${b}_${c}_$s"
            }
            is HadithListItem.HadithItem -> "h_${item.hadith.id}"
        }
    }

    // Initialize key if null when hadiths load
    LaunchedEffect(hadiths, hasScrolledToInitial) {
        if (hasScrolledToInitial && activeHadithKey == null && hadiths.isNotEmpty()) {
            activeHadithKey = getItemKey(HadithListItem.HadithItem(hadiths.first()))
        }
    }

    // Kitab/bab/alt-bab başlıqları həm tərcümə (2), həm də ərəbcə (1) rejimdə bir kontekst kartında
    // birləşdirilir — ərəbcə rejim eyni kartı ərəbcə adlarla göstərir. Mode 0 (qarışıq) yalnız cari
    // babın hədislərini verir, ona görə qruplaşma ona toxunmur.
    val processedItems = remember(combinedItems, selectedTab) {
        if (selectedTab == 0) return@remember combinedItems

        val result = mutableListOf<HadithListItem>()
        var i = 0
        while (i < combinedItems.size) {
            val item = combinedItems[i]
            if (item is HadithListItem.BookHeader || item is HadithListItem.ChapterHeader || item is HadithListItem.SubChapterHeader) {
                var book: HadithBook? = null
                var chapter: HadithChapter? = null
                var sub: HadithSubChapter? = null
                
                if (item is HadithListItem.BookHeader) book = item.book
                else if (item is HadithListItem.ChapterHeader) chapter = item.chapter
                else if (item is HadithListItem.SubChapterHeader) sub = item.subChapter
                
                var nextIdx = i + 1
                while (nextIdx < combinedItems.size) {
                    val next = combinedItems[nextIdx]
                    
                    if (next is HadithListItem.HadithItem) break

                    if (next is HadithListItem.BookHeader) {
                        break 
                    } else if (next is HadithListItem.ChapterHeader) {
                        if (book != null && chapter == null) {
                            chapter = next.chapter
                        } else {
                            break
                        }
                    } else if (next is HadithListItem.SubChapterHeader) {
                        if (sub == null) {
                            sub = next.subChapter
                        } else {
                            break
                        }
                    }
                    nextIdx++
                }
                
                result.add(HadithListItem.ContextGroupedHeader(book, chapter, sub))
                i = nextIdx
            } else {
                result.add(item)
                i++
            }
        }
        result
    }

    // A map to track the current context (Book, Chapter, SubChapter) for each item in the list
    val itemContextMap = remember(combinedItems, processedItems, selectedTab, hadiths, currentBookSlug, currentChapterSlug, currentSubChapterSlug) {
        val dataList = if (selectedTab == 0) hadiths.map { HadithListItem.HadithItem(it) } else processedItems
        val map = mutableMapOf<Int, Triple<HadithBook?, HadithChapter?, HadithSubChapter?>>()
        
        // Context tracking for current view (Mode 0)
        if (selectedTab == 0) {
            // In Mode 0, everything belongs to the currently loaded bab/sub-bab
            // Use existing objects from combinedItems if available, otherwise use placeholders from props
            val chapter = combinedItems.filterIsInstance<HadithListItem.ChapterHeader>()
                .find { it.chapter.slug == currentChapterSlug }?.chapter
                ?: HadithChapter(currentChapterSlug ?: "", currentBookSlug ?: "", 0, currentTitle)
                
            val book = combinedItems.filterIsInstance<HadithListItem.BookHeader>()
                .findLast { it.book.slug == currentBookSlug || it.book.slug == chapter.book_slug }?.book
                ?: HadithBook(currentBookSlug ?: "", volumeSlug ?: "", 0, "")
                
            val sub = if (currentSubChapterSlug != null && currentSubChapterSlug != "DIRECT_VIEW") {
                combinedItems.filterIsInstance<HadithListItem.SubChapterHeader>()
                    .find { it.subChapter.slug == currentSubChapterSlug }?.subChapter
                    ?: HadithSubChapter(currentSubChapterSlug!!, currentChapterSlug ?: "", 0, currentTitle)
            } else null

            dataList.forEachIndexed { index, _ ->
                map[index] = Triple(book, chapter, sub)
            }
            return@remember map
        }

        var currentBook: HadithBook? = null
        var currentChapter: HadithChapter? = null
        var currentSub: HadithSubChapter? = null

        // Pre-fill context from combinedItems which has full linear structure
        val fullMap = mutableMapOf<String, Triple<HadithBook?, HadithChapter?, HadithSubChapter?>>()
        combinedItems.forEach { item ->
            when (item) {
                is HadithListItem.BookHeader -> {
                    currentBook = item.book
                    currentChapter = null
                    currentSub = null
                }
                is HadithListItem.ChapterHeader -> {
                    currentChapter = item.chapter
                    currentSub = null
                }
                is HadithListItem.SubChapterHeader -> {
                    currentSub = item.subChapter
                }
                is HadithListItem.HadithItem -> {
                    fullMap[getItemKey(item)] = Triple(currentBook, currentChapter, currentSub)
                }
                else -> {}
            }
        }

        dataList.forEachIndexed { index, item ->
            when (item) {
                is HadithListItem.HadithItem -> {
                    map[index] = fullMap[getItemKey(item)] ?: Triple(null, null, null)
                }
                is HadithListItem.ContextGroupedHeader -> {
                    map[index] = Triple(item.book, item.chapter, item.subChapter)
                }
                is HadithListItem.BookHeader -> {
                    map[index] = Triple(item.book, null, null)
                }
                is HadithListItem.ChapterHeader -> {
                    // Try to find book for this chapter
                    val book = combinedItems.filterIsInstance<HadithListItem.BookHeader>()
                        .findLast { it.book.slug == item.chapter.book_slug }?.book
                    map[index] = Triple(book, item.chapter, null)
                }
                is HadithListItem.SubChapterHeader -> {
                    // Try to find chapter/book for this sub-chapter
                    val chapter = combinedItems.filterIsInstance<HadithListItem.ChapterHeader>()
                        .find { it.chapter.slug == item.subChapter.chapter_slug }?.chapter
                    val book = combinedItems.filterIsInstance<HadithListItem.BookHeader>()
                        .findLast { it.book.slug == chapter?.book_slug }?.book
                    map[index] = Triple(book, chapter, item.subChapter)
                }
            }
        }
        map
    }

    // Trigger data fetch whenever context changes while in Modes 1 or 2
    // This ensures that when we switch to Mode 0, the 'hadiths' list is ready.
    LaunchedEffect(currentChapterSlug, currentSubChapterSlug, selectedTab) {
        if (selectedTab != 0) {
            if (currentSubChapterSlug != null && currentSubChapterSlug != "DIRECT_VIEW") {
                hadithViewModel.fetchHadithsBySubChapter(currentChapterSlug!!, currentSubChapterSlug!!)
            } else if (currentChapterSlug != null) {
                hadithViewModel.fetchHadithsByChapter(currentChapterSlug!!)
            }
        }
    }

    // Save key and update context during scroll
    LaunchedEffect(listState, processedItems, selectedTab, currentChapterSlug, itemContextMap) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (index, scrollOffset) ->
            // Don't capture an anchor while a tab switch is animating, nor before the
            // initial positioning has settled — otherwise the transient top-of-list
            // item (or a leaked scroll position) gets recorded as the anchor/context.
            if (isSwitchingTab || !hasScrolledToInitial) return@collect

            val dataList = if (selectedTab == 0) hadiths.map { HadithListItem.HadithItem(it) } else processedItems
            val offset = 0 // Offset should be 0 as there is no header item in the list currently
            
            val itemIndex = index - offset
            if (itemIndex >= 0 && itemIndex < dataList.size) {
                val item = dataList[itemIndex]

                // Always anchor on a concrete hadith. Headers (chapter/sub/grouped)
                // don't exist in every mode, so anchoring on one degrades into a
                // "jump to the start of the bab" on the next tab switch. When the top
                // item is a header, anchor to the nearest hadith instead so round-trip
                // switches stay stable.
                val anchorHadith = when (item) {
                    is HadithListItem.HadithItem -> item.hadith
                    else -> (itemIndex until dataList.size).firstNotNullOfOrNull { j ->
                        (dataList[j] as? HadithListItem.HadithItem)?.hadith
                    } ?: (itemIndex downTo 0).firstNotNullOfOrNull { j ->
                        (dataList[j] as? HadithListItem.HadithItem)?.hadith
                    }
                }
                val key = if (anchorHadith != null) {
                    getItemKey(HadithListItem.HadithItem(anchorHadith))
                } else {
                    getItemKey(item)
                }
                activeHadithKey = key
                activeHadithOffset = if (item is HadithListItem.HadithItem) scrollOffset else 0

                // Dynamic context tracking (TopBar title and Navigator sync)
                if (selectedTab != 0) {
                    val context = itemContextMap[itemIndex]
                    if (context != null) {
                        val (book, chapter, sub) = context
                        // Update Source of Truth slugs
                        if (book != null && currentBookSlug != book.slug) {
                            currentBookSlug = book.slug
                        }
                        if (chapter != null && currentChapterSlug != chapter.slug) {
                            currentChapterSlug = chapter.slug
                        }
                        if (sub != null && currentSubChapterSlug != sub.slug) {
                            currentSubChapterSlug = sub.slug
                        } else if (sub == null && currentSubChapterSlug != "DIRECT_VIEW" && currentSubChapterSlug != null) {
                            // If we moved out of a sub-chapter into a chapter area
                            currentSubChapterSlug = "DIRECT_VIEW"
                        }

                        val newTitle = sub?.name ?: chapter?.name ?: book?.name ?: title
                        if (currentTitle != newTitle) currentTitle = newTitle
                    }
                }

                com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithScroll", "Saved key: $key at index: $index, offset: $scrollOffset")
            }
        }
    }

    // Restore position on tab change or jump trigger
    LaunchedEffect(selectedTab, jumpTrigger) {
        if (selectedTab == lastSelectedTab && activeHadithKey == null) return@LaunchedEffect
        
        val key = activeHadithKey
        isSwitchingTab = true
        com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithScroll", "Restoring location. Tab: $selectedTab, Key: $key, Trigger: $jumpTrigger")

        // Ensure the target tab's data is loaded (and actually matches the current
        // context) BEFORE restoring. Mode 0 renders only the current bab's `hadiths`,
        // which get cleared to empty on every fetch and are frequently refetched while
        // scrolling in Modes 1/2. Restoring against that stale/empty list was what made
        // the position jump to the top or to a different bab.
        if (selectedTab == 0) {
            val targetSub = currentSubChapterSlug?.takeIf { it != "DIRECT_VIEW" }
            val alreadyLoaded = when {
                targetSub != null -> hadiths.isNotEmpty() && hadiths.all { it.sub_chapter_slug == targetSub }
                currentChapterSlug != null -> hadiths.isNotEmpty() && hadiths.all { it.chapter_slug == currentChapterSlug }
                else -> true
            }
            if (!alreadyLoaded) {
                if (targetSub != null && currentChapterSlug != null) {
                    hadithViewModel.fetchHadithsBySubChapter(currentChapterSlug!!, targetSub)
                } else if (currentChapterSlug != null) {
                    hadithViewModel.fetchHadithsByChapter(currentChapterSlug!!)
                }
                // Bounded wait so a genuinely empty bab doesn't hang the restore.
                withTimeoutOrNull(2000) {
                    snapshotFlow { hadiths }.filter { list ->
                        if (targetSub != null) list.any { it.sub_chapter_slug == targetSub }
                        else list.any { it.chapter_slug == currentChapterSlug }
                    }.first()
                }
            }
        } else {
            // Modes 1/2 use the full-volume list (loaded once on entry).
            withTimeoutOrNull(3000) {
                snapshotFlow { processedItems.isNotEmpty() || !isLoading }.filter { it }.first()
            }
        }

        val dataList = if (selectedTab == 0) hadiths.map { HadithListItem.HadithItem(it) } else processedItems
        val offset = if (selectedTab == 0) 0 else 0 // Header offset not needed with key-based search

        // 1. Try to restore specific scrolled position if we have a key
        var restored = false
        if (key != null) {
            var newIndex = dataList.indexOfFirst { getItemKey(it) == key }
            
            // Cross-tab mapping for scrolled position
            if (newIndex == -1) {
                if (key.startsWith("g_")) {
                    // Grouped Header to anything
                    val parts = key.split("_")
                    val s = parts.getOrNull(3)?.takeIf { it != "ns" }
                    val c = parts.getOrNull(2)?.takeIf { it != "nc" }
                    val b = parts.getOrNull(1)?.takeIf { it != "nb" }
                    newIndex = dataList.indexOfFirst { item ->
                        when (item) {
                            is HadithListItem.SubChapterHeader -> item.subChapter.slug == s
                            is HadithListItem.ChapterHeader -> item.chapter.slug == c
                            is HadithListItem.BookHeader -> item.book.slug == b
                            is HadithListItem.HadithItem -> item.hadith.sub_chapter_slug == s || (s == null && item.hadith.chapter_slug == c)
                            is HadithListItem.ContextGroupedHeader -> item.subChapter?.slug == s && item.chapter?.slug == c && item.book?.slug == b
                        }
                    }
                } else if (key.startsWith("h_")) {
                    // Hadith to anything
                    val hId = key.substringAfter("_")
                    newIndex = dataList.indexOfFirst { item ->
                        item is HadithListItem.HadithItem && item.hadith.id.toString() == hId
                    }
                    
                    // Fallback: If hadith not in list (e.g. grouped headers only), find its header
                    if (newIndex == -1) {
                        val originalHadith = hadiths.find { it.id.toString() == hId }
                        if (originalHadith != null) {
                            newIndex = dataList.indexOfFirst { item ->
                                when (item) {
                                    is HadithListItem.SubChapterHeader -> item.subChapter.slug == originalHadith.sub_chapter_slug
                                    is HadithListItem.ContextGroupedHeader -> {
                                        // Priority to sub-chapter match
                                        (originalHadith.sub_chapter_slug != null && item.subChapter?.slug == originalHadith.sub_chapter_slug) ||
                                        (originalHadith.sub_chapter_slug == null && item.chapter?.slug == originalHadith.chapter_slug)
                                    }
                                    is HadithListItem.ChapterHeader -> item.chapter.slug == originalHadith.chapter_slug && originalHadith.sub_chapter_slug == null
                                    else -> false
                                }
                            }
                        }
                    }
                } else if (key.startsWith("s_") || key.startsWith("c_") || key.startsWith("b_")) {
                    // Simple header to anything (Grouped or Mixed Hadith list)
                    val slug = key.substringAfter("_")
                    val isSub = key.startsWith("s_")
                    val isChap = key.startsWith("c_")

                    newIndex = dataList.indexOfFirst { item ->
                        when (item) {
                            is HadithListItem.SubChapterHeader -> isSub && item.subChapter.slug == slug
                            is HadithListItem.ChapterHeader -> isChap && item.chapter.slug == slug
                            is HadithListItem.ContextGroupedHeader -> {
                                (isSub && item.subChapter?.slug == slug) ||
                                (isChap && item.chapter?.slug == slug) ||
                                (key.startsWith("b_") && item.book?.slug == slug)
                            }
                            is HadithListItem.HadithItem -> {
                                (isSub && item.hadith.sub_chapter_slug == slug) ||
                                (isChap && item.hadith.chapter_slug == slug && item.hadith.sub_chapter_slug == null)
                            }
                            else -> false
                        }
                    }
                } else if (key.startsWith("info_")) {
                    val parts = key.split("_")
                    val bSlug = parts.getOrNull(1)?.takeIf { it != "null" }
                    val cSlug = parts.getOrNull(2)?.takeIf { it != "null" }
                    val sSlug = parts.getOrNull(3)?.takeIf { it != "null" }
                    newIndex = dataList.indexOfFirst { item ->
                        when (item) {
                            is HadithListItem.SubChapterHeader -> item.subChapter.slug == sSlug
                            is HadithListItem.ChapterHeader -> item.chapter.slug == cSlug && (bSlug == null || item.chapter.book_slug == bSlug)
                            is HadithListItem.BookHeader -> item.book.slug == bSlug && cSlug == null
                            is HadithListItem.ContextGroupedHeader -> {
                                (sSlug != null && item.subChapter?.slug == sSlug) ||
                                (sSlug == null && cSlug != null && item.chapter?.slug == cSlug && (bSlug == null || item.chapter?.book_slug == bSlug)) ||
                                (sSlug == null && cSlug == null && bSlug != null && item.book?.slug == bSlug)
                            }
                            else -> false
                        }
                    }
                }
            }

            if (newIndex != -1) {
                val finalOffset = if (key.startsWith("h_") || key.startsWith("info_")) activeHadithOffset else 0
                listState.scrollToItem(newIndex + offset, finalOffset)
                restored = true
                com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithScroll", "Restored via key to index: ${newIndex + offset}")
            }
        }

        // 2. Fallback: If no key or key not found, use current Source of Truth slugs
        if (!restored) {
            val targetIndex = dataList.indexOfFirst { item ->
                when (item) {
                    is HadithListItem.SubChapterHeader -> item.subChapter.slug == currentSubChapterSlug
                    is HadithListItem.ChapterHeader -> item.chapter.slug == currentChapterSlug
                    is HadithListItem.BookHeader -> item.book.slug == currentBookSlug
                    is HadithListItem.ContextGroupedHeader -> {
                        (currentSubChapterSlug != null && item.subChapter?.slug == currentSubChapterSlug) ||
                        (currentSubChapterSlug == null && currentChapterSlug != null && item.chapter?.slug == currentChapterSlug) ||
                        (currentSubChapterSlug == null && currentChapterSlug == null && currentBookSlug != null && item.book?.slug == currentBookSlug)
                    }
                    is HadithListItem.HadithItem -> {
                         (currentSubChapterSlug != null && item.hadith.sub_chapter_slug == currentSubChapterSlug) ||
                         (currentSubChapterSlug == null && currentChapterSlug != null && item.hadith.chapter_slug == currentChapterSlug)
                    }
                }
            }

            if (targetIndex != -1) {
                listState.scrollToItem(targetIndex + offset, 0)
                com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithScroll", "Restored via slugs to index: ${targetIndex + offset}")
            } else if (selectedTab == 0) {
                // IMPORTANT: If we are in Mode 0 (Təkbə-tək), the list is ONLY the hadiths of the CURRENT bab.
                // We try to find the index of the first hadith that matches our current key if it's a hadith key
                val fallbackIndex = if (key?.startsWith("h_") == true) {
                    val hId = key.substringAfter("_")
                    hadiths.indexOfFirst { it.id.toString() == hId }
                } else -1

                if (fallbackIndex != -1) {
                    listState.scrollToItem(fallbackIndex + offset, activeHadithOffset)
                    com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithScroll", "Fallback restored via hadith ID in Mode 0")
                } else {
                    listState.scrollToItem(0, 0)
                    com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithScroll", "Fallback to top (index 0) for Mode 0")
                }
            } else {
                // If we are switching TO Arabic/Translation mode and couldn't find the bab/subbab header,
                // try to find the FIRST HADITH of that bab.
                val hadithIndex = dataList.indexOfFirst { item ->
                    item is HadithListItem.HadithItem && (
                        (currentSubChapterSlug != null && item.hadith.sub_chapter_slug == currentSubChapterSlug) ||
                        (currentSubChapterSlug == null && currentChapterSlug != null && item.hadith.chapter_slug == currentChapterSlug)
                    )
                }
                if (hadithIndex != -1) {
                    listState.scrollToItem(hadithIndex + offset, 0)
                    com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithScroll", "Restored via first hadith to index: ${hadithIndex + offset}")
                }
            }
        }

        lastSelectedTab = selectedTab
        delay(100)
        isSwitchingTab = false
    }

    LaunchedEffect(processedItems, hasScrolledToInitial, selectedTab) {
        if (!hasScrolledToInitial && activeHadithKey == null && processedItems.isNotEmpty()) {
            delay(10) // Reduced delay for faster snap
            if (selectedTab == 0) {
                // In Mixed mode, always scroll to the very top (index 0)
                // which is usually the ChapterInfo or header
                listState.scrollToItem(0)
            } else if (currentSubChapterSlug == "DIRECT_VIEW") {
                val targetIndex = processedItems.indexOfFirst { item ->
                    when (item) {
                        is HadithListItem.ChapterHeader -> item.chapter.slug == currentChapterSlug
                        is HadithListItem.ContextGroupedHeader -> item.chapter?.slug == currentChapterSlug
                        else -> false
                    }
                }
                if (targetIndex != -1) {
                    listState.scrollToItem(targetIndex)
                } else {
                    listState.scrollToItem(0)
                }
            } else if (currentBookSlug == null && currentChapterSlug == null && currentSubChapterSlug == null) {
                listState.scrollToItem(0)
            } else {
                val targetIndex = processedItems.indexOfFirst { item ->
                    when (item) {
                        is HadithListItem.BookHeader -> item.book.slug == currentBookSlug && currentChapterSlug == null
                        is HadithListItem.ChapterHeader -> item.chapter.slug == currentChapterSlug && currentSubChapterSlug == null
                        is HadithListItem.SubChapterHeader -> item.subChapter.slug == currentSubChapterSlug
                        is HadithListItem.ContextGroupedHeader -> {
                            (currentSubChapterSlug != null && item.subChapter?.slug == currentSubChapterSlug) ||
                            (currentSubChapterSlug == null && currentChapterSlug != null && item.chapter?.slug == currentChapterSlug) ||
                            (currentSubChapterSlug == null && currentChapterSlug == null && currentBookSlug != null && item.book?.slug == currentBookSlug)
                        }
                        else -> false
                    }
                }
                if (targetIndex != -1) {
                    listState.scrollToItem(targetIndex)
                } else {
                    listState.scrollToItem(0)
                }
            }
            hasScrolledToInitial = true
        } else if (!hasScrolledToInitial && selectedTab == 0 && hadiths.isNotEmpty()) {
            delay(10)
            // In Mode 0 (Təkbə-tək), the list starts with ChapterInfo (0) and Bismillah (1).
            // We want to scroll to the very top as requested.
            listState.scrollToItem(0)
            hasScrolledToInitial = true
        }
    }

    val navigationTargets = remember(combinedItems, currentChapterSlug, currentSubChapterSlug) {
        if (combinedItems.isEmpty()) return@remember null to null
        
        // Find current target in combined list
        val currentIndex = combinedItems.indexOfFirst { item ->
            when (item) {
                is HadithListItem.ChapterHeader -> {
                    item.chapter.slug == currentChapterSlug && (currentSubChapterSlug == null || currentSubChapterSlug == "DIRECT_VIEW")
                }
                is HadithListItem.SubChapterHeader -> {
                    item.subChapter.slug == currentSubChapterSlug
                }
                else -> false
            }
        }
        
        if (currentIndex == -1) return@remember null to null
        
        // Previous Target
        var prev: HadithNavigationTarget? = null
        for (i in currentIndex - 1 downTo 0) {
            val item = combinedItems[i]
            if (item is HadithListItem.SubChapterHeader) {
                // Find parent chapter for bookSlug
                var bookSlug: String? = null
                var bookName: String? = null
                var chapterName: String? = null
                for (j in i - 1 downTo 0) {
                    val p = combinedItems[j]
                    if (p is HadithListItem.ChapterHeader) {
                        bookSlug = p.chapter.book_slug
                        chapterName = p.chapter.name
                        // Keep searching for book name
                    }
                    if (p is HadithListItem.BookHeader) {
                        bookName = p.book.name
                        break
                    }
                }
                prev = HadithNavigationTarget(
                    title = item.subChapter.name,
                    volumeSlug = volumeSlug,
                    bookSlug = bookSlug,
                    bookName = bookName,
                    chapterSlug = item.subChapter.chapter_slug,
                    chapterName = chapterName,
                    subChapterSlug = item.subChapter.slug
                )
                break
            } else if (item is HadithListItem.ChapterHeader) {
                val hasSubs = if (i + 1 < combinedItems.size) combinedItems[i + 1] is HadithListItem.SubChapterHeader else false
                if (!hasSubs) {
                    var bookName: String? = null
                    for (j in i - 1 downTo 0) {
                        val p = combinedItems[j]
                        if (p is HadithListItem.BookHeader) {
                            bookName = p.book.name
                            break
                        }
                    }
                    prev = HadithNavigationTarget(
                        title = item.chapter.name,
                        volumeSlug = volumeSlug,
                        bookSlug = item.chapter.book_slug,
                        bookName = bookName,
                        chapterSlug = item.chapter.slug,
                        chapterName = item.chapter.name,
                        subChapterSlug = "DIRECT_VIEW"
                    )
                    break
                }
            }
        }
        
        // Next Target
        var next: HadithNavigationTarget? = null
        for (i in currentIndex + 1 until combinedItems.size) {
            val item = combinedItems[i]
            if (item is HadithListItem.SubChapterHeader) {
                var bookSlug: String? = null
                var bookName: String? = null
                var chapterName: String? = null
                for (j in i - 1 downTo 0) {
                    val p = combinedItems[j]
                    if (p is HadithListItem.ChapterHeader) {
                        bookSlug = p.chapter.book_slug
                        chapterName = p.chapter.name
                    }
                    if (p is HadithListItem.BookHeader) {
                        bookName = p.book.name
                        break
                    }
                }
                next = HadithNavigationTarget(
                    title = item.subChapter.name,
                    volumeSlug = volumeSlug,
                    bookSlug = bookSlug,
                    bookName = bookName,
                    chapterSlug = item.subChapter.chapter_slug,
                    chapterName = chapterName,
                    subChapterSlug = item.subChapter.slug
                )
                break
            } else if (item is HadithListItem.ChapterHeader) {
                val hasSubs = if (i + 1 < combinedItems.size) combinedItems[i + 1] is HadithListItem.SubChapterHeader else false
                if (!hasSubs) {
                    var bookName: String? = null
                    for (j in i - 1 downTo 0) {
                        val p = combinedItems[j]
                        if (p is HadithListItem.BookHeader) {
                            bookName = p.book.name
                            break
                        }
                    }
                    next = HadithNavigationTarget(
                        title = item.chapter.name,
                        volumeSlug = volumeSlug,
                        bookSlug = item.chapter.book_slug,
                        bookName = bookName,
                        chapterSlug = item.chapter.slug,
                        chapterName = item.chapter.name,
                        subChapterSlug = "DIRECT_VIEW"
                    )
                    break
                }
            }
        }
        
        prev to next
    }

    val onNavigateToTarget: (HadithNavigationTarget) -> Unit = { target ->
        isSwitchingTab = true
        scope.launch {
            // Force Mode 0 (Mixed) as requested for linear navigation
            HadithPreferences.setViewMode(0)

            // Set target key
            activeHadithKey = when {
                target.subChapterSlug != null && target.subChapterSlug != "DIRECT_VIEW" -> "s_${target.subChapterSlug}"
                target.chapterSlug != null -> "c_${target.chapterSlug}"
                else -> null
            }
            hasScrolledToInitial = false
            jumpTrigger++

            if (onNavigate != null) {
                onNavigate(target.volumeSlug, target.bookSlug, target.chapterSlug, target.subChapterSlug, target.title)
            } else {
                // Fallback to local state if no onNavigate (though ideally always provided)
                hasScrolledToInitial = false
                currentTitle = target.title
                currentBookSlug = target.bookSlug
                currentChapterSlug = target.chapterSlug
                currentSubChapterSlug = target.subChapterSlug

                if (target.subChapterSlug != null && target.subChapterSlug != "DIRECT_VIEW") {
                    hadithViewModel.fetchHadithsBySubChapter(target.chapterSlug!!, target.subChapterSlug)
                } else if (target.chapterSlug != null) {
                    hadithViewModel.fetchHadithsByChapter(target.chapterSlug)
                }
                listState.scrollToItem(0)
            }
        }
    }

    val topAppBarState = rememberTopAppBarState(
        initialHeightOffsetLimit = with(LocalDensity.current) {
            -appBarDims.baseExpandedHeight.toPx()
        }
    )
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

    var autoScrollSpeed by hadithViewModel.autoScrollSpeed

    AutoScrollEffect(listState, autoScrollSpeed) { autoScrollSpeed = null }

    ReaderKeyScrollEffect(listState, hadithViewModel.scrollEvent)

    val effectivelyFullscreen = hadithViewModel.isAutoScrollGestureMode.value || isFullscreen

    BackHandler(enabled = effectivelyFullscreen) {
        if (hadithViewModel.isAutoScrollGestureMode.value) {
            hadithViewModel.isAutoScrollGestureMode.value = false
            hadithViewModel.autoScrollSpeed.value = null
        } else {
            isFullscreen = false
        }
    }

    Scaffold(
        modifier = Modifier.then(
            if (!effectivelyFullscreen) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            else Modifier
        ),
        containerColor = colorScheme.background,
        topBar = {
            if (!effectivelyFullscreen) {
                HadithAppBar(
                    title = currentTitle,
                    onBack = onBack,
                    onSettingsClick = { showSettings = true },
                    autoScrollSpeed = hadithViewModel.autoScrollSpeed,
                    isAutoScrollGestureMode = hadithViewModel.isAutoScrollGestureMode,
                    autoScrollStep = hadithViewModel.autoScrollStep,
                    onVolumeToggle = {
                        hadithViewModel.triggerToggleFeedback(ReaderToggleKind.VolumeKeyNavigation, it)
                    },
                    onKeepScreenOnToggle = {
                        hadithViewModel.triggerToggleFeedback(ReaderToggleKind.KeepScreenOn, it)
                    },
                    onNavigatorClick = { showNavigator = true },
                    scrollBehavior = scrollBehavior,
                    selectedTab = selectedTab,
                    onTabSelected = { mode ->
                        isSwitchingTab = true
                        scope.launch { HadithPreferences.setViewMode(mode) }
                    }
                )
            }
        }
    ) { paddingValues ->
        // Wrap content in a key block to force full reset on navigation
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                val hasData = if (selectedTab == 0) hadiths.isNotEmpty() else combinedItems.isNotEmpty()
                
                if (isLoading && !hasData) {
                    Loader(true)
                } else if (!isLoading && !hasData) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_download),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = colorScheme.primary.colorAlpha(0.2f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(Res.string.strMsgDownloadHadithsFromSettings),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface.colorAlpha(0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .then(
                                if (!effectivelyFullscreen) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                                else Modifier
                            )
                            .then(
                                if (hadithViewModel.isAutoScrollGestureMode.value) Modifier
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
                        contentPadding = PaddingValues(top = 16.dp, bottom = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (selectedTab != 0 && processedItems.isNotEmpty()) {
                            items(
                                count = processedItems.size,
                                key = { index -> getItemKey(processedItems[index]) },
                                contentType = { index ->
                                    when (processedItems[index]) {
                                        is HadithListItem.BookHeader -> "book"
                                        is HadithListItem.ChapterHeader -> "chapter"
                                        is HadithListItem.SubChapterHeader -> "sub"
                                        is HadithListItem.ContextGroupedHeader -> "group"
                                        is HadithListItem.HadithItem -> "hadith"
                                    }
                                }
                            ) { index ->
                                val item = processedItems[index]
                                when (item) {
                                    is HadithListItem.BookHeader -> {
                                        if (selectedTab == 2) BookHeaderItem(
                                            book = item.book,
                                            baseFontSize = 16.sp * azerbaijaniSizeMult,
                                            modifier = Modifier.fillMaxWidth(),
                                            arabicFontFamily = arabicFontFamily
                                        )
                                    }
                                    is HadithListItem.ChapterHeader -> {
                                        if (selectedTab == 2) ChapterHeaderItem(
                                            chapter = item.chapter,
                                            baseFontSize = 16.sp * azerbaijaniSizeMult,
                                            modifier = Modifier.fillMaxWidth(),
                                            arabicFontFamily = arabicFontFamily
                                        )
                                    }
                                    is HadithListItem.SubChapterHeader -> {
                                        if (selectedTab == 2) SubChapterHeaderItem(
                                            subChapter = item.subChapter,
                                            baseFontSize = 16.sp * azerbaijaniSizeMult,
                                            modifier = Modifier.fillMaxWidth(),
                                            arabicFontFamily = arabicFontFamily
                                        )
                                    }
                                    is HadithListItem.ContextGroupedHeader -> {
                                        ContextGroupedHeader(
                                            book = item.book,
                                            chapter = item.chapter,
                                            subChapter = item.subChapter,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                            // Ərəb tabında olduğu kimi, tətbiq dili ərəbcə olanda da
                                            // kontekst kartı öz ərəbcə adlarını göstərir.
                                            arabic = selectedTab == 1 || isArabicAppLanguage(),
                                            arabicFontFamily = arabicFontFamily
                                        )
                                    }
                                    is HadithListItem.HadithItem -> {
                                        HadithCard(
                                            hadith = item.hadith,
                                            viewMode = selectedTab,
                                            arabicEnabled = arabicEnabled,
                                            azerbaijaniEnabled = azerbaijaniEnabled,
                                            sourceEnabled = sourceEnabled,
                                            arabicSizeMult = arabicSizeMult,
                                            azerbaijaniSizeMult = azerbaijaniSizeMult,
                                            arabicFontFamily = arabicFontFamily,
                                            showParentheses = showParentheses,
                                            highlightParentheses = highlightParentheses,
                                            isAuthorized = isAuthorized,
                                            todayContent = todayContent,
                                            modifier = Modifier.fillMaxWidth(),
                                            isBookmarked = item.hadith.id in bookmarkedHadithIds,
                                            onShareRequest = { sharingHadith = it },
                                            onEditRequest = { editingHadith = it },
                                            onBookmarkRequest = onHadithBookmarkClick,
                                            onSetDailyContentRequest = { hadith ->
                                                dailyContentViewModel.setDailyContent(
                                                    DailyContent(
                                                        content_type = "hadith",
                                                        hadith_id = hadith.id,
                                                        text_ar = hadith.text_ar,
                                                        text_az = hadith.text_az,
                                                        source = hadith.source
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        } else if (selectedTab == 0) {
                            items(
                                items = hadiths,
                                key = { getItemKey(HadithListItem.HadithItem(it)) },
                                contentType = { "hadith" }
                            ) { hadith ->
                                HadithCard(
                                    hadith = hadith,
                                    viewMode = selectedTab,
                                    arabicEnabled = arabicEnabled,
                                    azerbaijaniEnabled = azerbaijaniEnabled,
                                    sourceEnabled = sourceEnabled,
                                    arabicSizeMult = arabicSizeMult,
                                    azerbaijaniSizeMult = azerbaijaniSizeMult,
                                    arabicFontFamily = arabicFontFamily,
                                    showParentheses = showParentheses,
                                    highlightParentheses = highlightParentheses,
                                    isAuthorized = isAuthorized,
                                    todayContent = todayContent,
                                    modifier = Modifier.fillMaxWidth(),
                                    isBookmarked = hadith.id in bookmarkedHadithIds,
                                    onShareRequest = { sharingHadith = it },
                                    onEditRequest = { editingHadith = it },
                                    onBookmarkRequest = onHadithBookmarkClick,
                                    onSetDailyContentRequest = { hadith ->
                                        dailyContentViewModel.setDailyContent(
                                            DailyContent(
                                                content_type = "hadith",
                                                hadith_id = hadith.id,
                                                text_ar = hadith.text_ar,
                                                text_az = hadith.text_az,
                                                source = hadith.source
                                            )
                                        )
                                    }
                                )
                            }

                            val (prev, next) = navigationTargets
                            if (prev != null || next != null) {
                                item(key = "nav_buttons") {
                                    HadithNavigationButtons(
                                        previousTarget = prev,
                                        nextTarget = next,
                                        onNavigate = onNavigateToTarget,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Loader(size = 24.dp)
                        }
                    }

                    if (hadithViewModel.isAutoScrollGestureMode.value) {
                        AutoScrollGestureOverlay(
                            autoScrollSpeed = hadithViewModel.autoScrollSpeed,
                            isAutoScrollGestureMode = hadithViewModel.isAutoScrollGestureMode,
                            autoScrollStep = hadithViewModel.autoScrollStep,
                            onManualScroll = {
                                scope.launch { listState.scrollBy(it) }
                            }
                        )
                    }
                }

                if (!hadithViewModel.isAutoScrollGestureMode.value) {
                    val bottomNavHeight = 0.dp
                    HadithFloatingBar(
                        isFullscreen = isFullscreen,
                        onChangeFullscreen = { isFullscreen = it },
                        chromeCollapsedFraction = scrollBehavior.state.collapsedFraction,
                        isAuthenticated = isAuthenticated,
                        onEditClick = {
                            val isAtChapterLevel = currentSubChapterSlug == null || currentSubChapterSlug == "DIRECT_VIEW"
                            if (isAtChapterLevel) {
                                if (hadiths.isEmpty() && subChapters.isEmpty()) {
                                    showChoiceDialog = true
                                } else if (subChapters.isNotEmpty()) {
                                    editorType = EditorType.SUB_CHAPTER
                                } else {
                                    editorType = EditorType.HADITH
                                }
                            } else {
                                editorType = EditorType.HADITH
                            }
                        },
                        bottomOffset = bottomNavHeight
                    )
                }
        }
    }

    HadithSettingsSheet(
        isOpen = showSettings,
        onDismiss = { showSettings = false }
    )

    HadithShareSheet(sharingHadith) { sharingHadith = null }

    AlertDialog(
        isOpen = removingHadith != null,
        onClose = { removingHadith = null },
        title = stringResource(Res.string.strTitleBookmarkDeleteThis),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = { removingHadith = null },
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelRemove),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    val hadithId = removingHadith?.id
                    removingHadith = null

                    if (hadithId != null) {
                        scope.launch {
                            if (userRepository.removeHadithBookmark(hadithId)) {
                                PlatformUtils.showToast(
                                    getString(Res.string.strMsgHadithBookmarkRemoved)
                                )
                            }
                        }
                    }
                },
            ),
        ),
    )

    BookmarkNoteSheet(
        subtitle = savingHadith?.let { stringResource(Res.string.strLabelHadithNo, it.hadith_no) },
        onDismiss = { savingHadith = null },
        onSave = { note ->
            val hadith = savingHadith ?: return@BookmarkNoteSheet
            val hadithId = hadith.id ?: return@BookmarkNoteSheet
            savingHadith = null

            scope.launch {
                val result = userRepository.addHadithBookmark(
                    hadithId = hadithId,
                    volumeSlug = volumeSlug,
                    bookSlug = currentBookSlug,
                    chapterSlug = hadith.chapter_slug,
                    subChapterSlug = hadith.sub_chapter_slug,
                    hadithNo = hadith.hadith_no,
                    title = currentTitle,
                    preview = hadith.text_az.take(160),
                    note = note,
                )
                PlatformUtils.showToast(
                    when (result) {
                        BookmarkAddResult.Added -> getString(Res.string.strMsgHadithBookmarkAdded)
                        BookmarkAddResult.AlreadyBookmarked ->
                            getString(Res.string.strMsgBookmarkAddedAlready)

                        BookmarkAddResult.Failed ->
                            getString(Res.string.strMsgHadithBookmarkAddFailed)
                    }
                )
            }
        },
    )

    AutoScrollSheet(
        autoScrollSpeedProvider = hadithViewModel.autoScrollSpeed,
        isOpen = showAutoScroll,
        onClose = { showAutoScroll = false }
    )

    HadithNavigatorSheet(
        isOpen = showNavigator,
        onDismiss = { showNavigator = false },
        initialVolumeSlug = volumeSlug,
        initialBookSlug = currentBookSlug,
        initialChapterSlug = currentChapterSlug,
        initialSubChapterSlug = currentSubChapterSlug,
        onNavigate = { v, b, c, s, newTitle ->
            com.cafarovceyxun.anamuslim.utils.AppLogger.d("HadithReader", "onNavigate called: v=$v, b=$b, c=$c, s=$s, title=$newTitle")
            isSwitchingTab = true
            scope.launch {
                // Set target key to force jump to the new section
                activeHadithKey = when {
                    s != null && s != "DIRECT_VIEW" -> "s_$s"
                    c != null -> "c_$c"
                    b != null -> "b_$b"
                    else -> null
                }
                hasScrolledToInitial = false
                jumpTrigger++
                
                // CRITICAL: Update local state immediately so if Navigator is reopened, it has fresh context
                currentBookSlug = b
                currentChapterSlug = c
                currentSubChapterSlug = s
                currentTitle = newTitle

                if (onNavigate != null) {
                    onNavigate(v, b, c, s, newTitle)
                } else {
                    // Fallback to local state if no onNavigate
                    if (v != null) hadithViewModel.fetchFullVolume(v)
                    // (current slugs already updated above)

                    if (s != null && s != "DIRECT_VIEW") hadithViewModel.fetchHadithsBySubChapter(c!!, s)
                    else if (c != null) hadithViewModel.fetchHadithsByChapter(c)
                    else if (b != null) hadithViewModel.fetchChapters(b)

                    listState.scrollToItem(0)
                }
            }
        }
    )

    val toggleFeedback by hadithViewModel.toggleFeedback.collectAsStateWithLifecycle()

    ReaderToggleFeedbackOverlay(toggleFeedback)
}

@Composable
private fun HadithFloatingBar(
    isFullscreen: Boolean,
    onChangeFullscreen: (Boolean) -> Unit,
    chromeCollapsedFraction: Float,
    isAuthenticated: Boolean,
    onEditClick: () -> Unit,
    bottomOffset: Dp = 0.dp
) {
    val fullscreenButtonAlpha = if (isFullscreen) 0.65f
    else (1f - chromeCollapsedFraction).coerceIn(0f, 1f)

    if (isFullscreen || fullscreenButtonAlpha > 0.01f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp + bottomOffset, start = 16.dp, end = 16.dp, top = 6.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Row(
                modifier = Modifier.alpha(fullscreenButtonAlpha),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val toggleRotation = rememberToggleScreenRotation()

                if (isAuthenticated) {
                    HadithEditFab(
                        onClick = onEditClick,
                        contentDescription = stringResource(Res.string.strLabelEdit),
                        size = 42.dp,
                    )
                }

                // `toggleRotation?.invoke()` made this compile everywhere but left a *visible,
                // dead* button on iOS, where the seam is null. The project's rule is to hide the
                // affordance instead — same as ReferenceScreen and the reader.
                if (toggleRotation != null) {
                    TextButton(
                        onClick = {
                            toggleRotation.invoke()
                        },
                        modifier = Modifier.height(36.dp),
                        shape = MaterialTheme.shapes.extraLarge,
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
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                TextButton(
                    onClick = {
                        onChangeFullscreen(!isFullscreen)
                    },
                    modifier = Modifier.height(36.dp),
                    shape = MaterialTheme.shapes.extraLarge,
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
                        painter = painterResource(
                            if (isFullscreen) Res.drawable.ic_shrink
                            else Res.drawable.ic_expand
                        ),
                        contentDescription = stringResource(
                            if (isFullscreen) Res.string.exitFullscreen
                            else Res.string.enterFullscreen
                        ),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HadithNavigationButtons(
    previousTarget: HadithNavigationTarget?,
    nextTarget: HadithNavigationTarget?,
    onNavigate: (HadithNavigationTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (previousTarget != null) {
            OutlinedButton(
                onClick = { onNavigate(previousTarget) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, colorScheme.outlineVariant.colorAlpha(0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colorScheme.surfaceVariant.colorAlpha(0.2f),
                    contentColor = colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_left),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.previousBab),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (nextTarget != null) {
            Button(
                onClick = { onNavigate(nextTarget) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.nextBab),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}


@Composable
fun BookHeaderItem(
    book: HadithBook, 
    baseFontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
    arabicFontFamily: FontFamily? = null,
) {
    val displayName = rememberHadithDisplayName(book.name, book.name_ar)
    Box(
        modifier = modifier
            .padding(top = 24.dp, bottom = 8.dp)
            .background(colorScheme.primaryContainer.colorAlpha(0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        val fontSize = (baseFontSize.value + 2).sp
        Text(
            text = displayName.text,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.4f
            ).withScriptDirection(displayName.isArabic, arabicFontFamily),
            color = colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ChapterHeaderItem(
    chapter: HadithChapter, 
    baseFontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
    arabicFontFamily: FontFamily? = null,
) {
    val displayName = rememberHadithDisplayName(chapter.name, chapter.name_ar)
    Column(
        modifier = modifier
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        val fontSize = (baseFontSize.value + 2).sp
        Text(
            text = displayName.text,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.4f
            ).withScriptDirection(displayName.isArabic, arabicFontFamily),
            color = colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            color = colorScheme.outlineVariant.colorAlpha(0.5f)
        )
    }
}

@Composable
fun SubChapterHeaderItem(
    subChapter: HadithSubChapter, 
    baseFontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
    arabicFontFamily: FontFamily? = null,
) {
    val displayName = rememberHadithDisplayName(subChapter.name, subChapter.name_ar)
    val fontSize = (baseFontSize.value + 2).sp
    Text(
        text = displayName.text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = fontSize,
            lineHeight = fontSize * 1.4f
        ).withScriptDirection(displayName.isArabic, arabicFontFamily),
        color = colorScheme.onSurface.colorAlpha(0.7f),
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .padding(top = 8.dp, bottom = 4.dp)
            .fillMaxWidth()
    )
}

/**
 * Hədisin önündəki kontekst kartı — kitab, bab və alt bab adları.
 *
 * [arabic] rejimdə hər sətir öz ərəbcə adını ([HadithBook.name_ar] və s.) ərəb şrifti və RTL
 * düzülüşü ilə göstərir; ərəbcə ad boşdursa həmin sətir azərbaycanca ada qayıdır ki, kontekst
 * heç vaxt itməsin.
 */
@Composable
fun ContextGroupedHeader(
    book: HadithBook?,
    chapter: HadithChapter?,
    subChapter: HadithSubChapter?,
    modifier: Modifier = Modifier,
    arabic: Boolean = false,
    arabicFontFamily: FontFamily? = null,
) {
    Card(
        modifier = modifier
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHighest.colorAlpha(0.3f)),
        border = BorderStroke(width = 1.dp, color = colorScheme.outlineVariant.colorAlpha(0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (book != null) {
                ContextGroupedRow(
                    number = book.book_no.toString(),
                    name = book.name,
                    nameAr = book.name_ar,
                    accentColor = colorScheme.primary,
                    textColor = colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    arabicFontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    arabic = arabic,
                    arabicFontFamily = arabicFontFamily
                )
            }

            if (chapter != null) {
                if (book != null) HorizontalDivider(color = colorScheme.outlineVariant.colorAlpha(0.2f))
                ContextGroupedRow(
                    number = chapter.chapter_no.toString(),
                    name = chapter.name,
                    nameAr = chapter.name_ar,
                    accentColor = colorScheme.secondary,
                    textColor = colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    arabicFontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    arabic = arabic,
                    arabicFontFamily = arabicFontFamily
                )
            }

            if (subChapter != null) {
                if (chapter != null || book != null) HorizontalDivider(color = colorScheme.outlineVariant.colorAlpha(0.2f))
                ContextGroupedRow(
                    number = subChapter.sub_chapter_no.toString(),
                    name = subChapter.name,
                    nameAr = subChapter.name_ar,
                    accentColor = colorScheme.tertiary,
                    textColor = colorScheme.onSurface.colorAlpha(0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    arabicFontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    arabic = arabic,
                    arabicFontFamily = arabicFontFamily
                )
            }
        }
    }
}

/** [ContextGroupedHeader]-in bir sətri: nömrə dairəsi + ad. */
@Composable
private fun ContextGroupedRow(
    number: String,
    name: String,
    nameAr: String?,
    accentColor: Color,
    textColor: Color,
    style: TextStyle,
    arabicFontSize: TextUnit,
    fontWeight: FontWeight,
    arabic: Boolean,
    arabicFontFamily: FontFamily?,
) {
    val arabicName = nameAr?.takeIf { arabic && it.isNotBlank() }
    val showArabicName = arabicName != null
    val text = arabicName ?: name
    val textStyle = if (arabicName != null) {
        style.copy(
            fontSize = arabicFontSize,
            lineHeight = arabicFontSize * 1.6f,
        ).withScriptDirection(arabic = true, arabicFontFamily = arabicFontFamily)
    } else {
        style.withScriptDirection(arabic = false)
    }

    // Hər sətir öz yazısının istiqamətini alır: ərəbcə ad sağdan, azərbaycanca ad soldan. Bu, kartın
    // (və tətbiqin) düzülüşündən asılı deyil — ərəbcə interfeysdə də azərbaycanca ad güzgülənməməlidir.
    CompositionLocalProvider(
        LocalLayoutDirection provides if (showArabicName) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(accentColor.colorAlpha(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = textStyle,
                fontWeight = fontWeight,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ChapterInfoCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF2E7D32), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_info),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
                tint = colorScheme.onSurface.colorAlpha(0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HadithCard(
    hadith: Hadith,
    viewMode: Int,
    arabicEnabled: Boolean,
    azerbaijaniEnabled: Boolean,
    sourceEnabled: Boolean,
    arabicSizeMult: Float,
    azerbaijaniSizeMult: Float,
    arabicFontFamily: FontFamily,
    showParentheses: Boolean,
    highlightParentheses: Boolean,
    isAuthorized: Boolean = false,
    todayContent: DailyContent? = null,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    onShareRequest: (Hadith) -> Unit,
    onEditRequest: (Hadith) -> Unit,
    onSetDailyContentRequest: (Hadith) -> Unit,
    onBookmarkRequest: (Hadith) -> Unit,
) {
    val highlightColor = Color(0xFFE53935) // Match Quran translation highlight color

    val showArabic = remember(viewMode, arabicEnabled) { (viewMode == 0 || viewMode == 1) && arabicEnabled }
    val showAzerbaijani = remember(viewMode, azerbaijaniEnabled) { (viewMode == 0 || viewMode == 2) && azerbaijaniEnabled }
    val showSource = remember(viewMode, sourceEnabled) { (viewMode == 0 || viewMode == 2) && sourceEnabled }

    val formattedAzText = remember(hadith.text_az, showParentheses, highlightParentheses, highlightColor) {
        formatHadithText(hadith.text_az, showParentheses, highlightParentheses, highlightColor)
    }

    val isTodayHdotd = remember(hadith, todayContent) {
        todayContent?.let { it.content_type == "hadith" && it.hadith_id == hadith.id } == true
    }

    Card(
        modifier = modifier
            .combinedClickable(
                onClick = {},
                onLongClick = { if (isAuthorized) onEditRequest(hadith) }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(width = 0.5.dp, color = colorScheme.outlineVariant.colorAlpha(0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            if (isTodayHdotd) {
                IsVotd(isHadith = true)
                Spacer(Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hadith Number Badge — yalnız rəqəm. Nişanın nə olduğunu yeri özü deyir (hər
                // hədisin başında, nömrə formasında), «Hədis №» prefiksi isə kartın eninin bir
                // hissəsini yeyirdi. Ekran oxuyucusu üçün tam etiket semantikada qalır.
                val hadithNoLabel = stringResource(Res.string.strLabelHadithNo, hadith.hadith_no)
                Surface(
                    color = colorScheme.primaryContainer.colorAlpha(0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = hadith.hadith_no.toString(),
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .semantics { contentDescription = hadithNoLabel },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = colorScheme.primary
                    )
                }

                // Actions
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isAuthorized) {
                        IconButton(onClick = { onSetDailyContentRequest(hadith) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_heart_filled),
                                contentDescription = stringResource(Res.string.strTitleVOTD),
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = { onShareRequest(hadith) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_share),
                            contentDescription = stringResource(Res.string.strLabelShare),
                            tint = colorScheme.onSurfaceVariant.colorAlpha(0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onBookmarkRequest(hadith) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isBookmarked) Res.drawable.ic_bookmark_added
                                else Res.drawable.ic_bookmark
                            ),
                            contentDescription = stringResource(Res.string.strLabelBookmark),
                            tint = if (isBookmarked) colorScheme.primary
                            else colorScheme.onSurfaceVariant.colorAlpha(0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (showArabic) {
                val arabicStyle = if (viewMode == 1) {
                    MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp * arabicSizeMult,
                        lineHeight = (28.sp * arabicSizeMult) * 1.6,
                        textAlign = TextAlign.Right,
                        fontFamily = arabicFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp * arabicSizeMult,
                        lineHeight = (24.sp * arabicSizeMult) * 1.6,
                        textAlign = TextAlign.Right,
                        fontFamily = arabicFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = hadith.text_ar,
                    style = arabicStyle,
                    color = colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    softWrap = true
                )
            }

            if (showArabic && showAzerbaijani) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = colorScheme.outlineVariant.colorAlpha(0.2f), thickness = 0.5.dp)
            }

            if (showAzerbaijani) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = formattedAzText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp * azerbaijaniSizeMult,
                        lineHeight = (17.sp * azerbaijaniSizeMult) * 1.6,
                        fontWeight = if (viewMode == 2) FontWeight.Medium else FontWeight.Normal
                    ).withScriptDirection(arabic = false),
                    color = colorScheme.onSurface.colorAlpha(0.9f),
                    modifier = Modifier.fillMaxWidth(),
                    softWrap = true
                )
            }

            if (viewMode == 0 || viewMode == 2) {
                hadith.note?.takeIf { it.isNotEmpty() }?.let { note ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.secondaryContainer.colorAlpha(0.2f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_info),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(Res.string.strTitleNote),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp)
                                    .withScriptDirection(arabic = false),
                                color = colorScheme.onSecondaryContainer.colorAlpha(0.8f)
                            )
                        }
                    }
                }
            }

            if (showSource) {
                hadith.source?.takeIf { it.isNotEmpty() }?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "— $it",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall
                            .copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            .withScriptDirection(arabic = false),
                        color = colorScheme.onSurfaceVariant.colorAlpha(0.5f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
