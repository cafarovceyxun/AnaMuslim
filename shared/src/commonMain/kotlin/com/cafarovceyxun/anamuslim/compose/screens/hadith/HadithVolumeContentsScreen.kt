@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.CollapsingAppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.rememberCollapsingAppBarState
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavContentPadding
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavFabPadding
import com.cafarovceyxun.anamuslim.compose.components.reader.navigator.FilterField
import com.cafarovceyxun.anamuslim.compose.components.search.SearchEverywhereRow
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.theme.hadithArabicFontFamily
import com.cafarovceyxun.anamuslim.compose.utils.appScopedViewModelStoreOwner
import com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.hadithAddSubBab
import com.cafarovceyxun.anamuslim.resources.hadithBabNumber
import com.cafarovceyxun.anamuslim.resources.hadithBookProgress
import com.cafarovceyxun.anamuslim.resources.hadithOutlineMatches
import com.cafarovceyxun.anamuslim.resources.strActionAddBabName
import com.cafarovceyxun.anamuslim.resources.strActionBulkAdd
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelCountBabs
import com.cafarovceyxun.anamuslim.resources.strLabelCountBooks
import com.cafarovceyxun.anamuslim.resources.strLabelCountHadiths
import com.cafarovceyxun.anamuslim.resources.strLabelCountSubBabs
import com.cafarovceyxun.anamuslim.resources.strLabelEdit
import com.cafarovceyxun.anamuslim.resources.strLabelHadithIntroduction
import com.cafarovceyxun.anamuslim.resources.strLabelOutlineCollapseAll
import com.cafarovceyxun.anamuslim.resources.strLabelOutlineExpandAll
import com.cafarovceyxun.anamuslim.resources.strLabelReadCompleted
import com.cafarovceyxun.anamuslim.resources.strLabelResumeReading
import com.cafarovceyxun.anamuslim.resources.strTitleAddBook
import com.cafarovceyxun.anamuslim.utils.hadith.HadithCompletion
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithOutline
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.viewModels.AuthViewModel
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Akkordeon görünüşlərinin ([HadithIndexStyle.PAGED]-dən başqa hamısı) açıq/yığılı vəziyyəti.
 *
 * [HadithIndexScreen]-də saxlanılır, bu ekranda yox: bab açılanda ekran kompozisiyadan çıxır
 * (yerində açılan hədis siyahısı onu əvəz edir) və geri qayıdanda istifadəçi tərk etdiyi açıq
 * kitabları və sürüşmə yerini görməlidir.
 */
@Stable
class HadithContentsState internal constructor(
    expandedBooks: Set<String>,
    expandedChapters: Set<String>,
    stripBook: String?,
) {
    var expandedBooks by mutableStateOf(expandedBooks)
    var expandedChapters by mutableStateOf(expandedChapters)

    /** [HadithIndexStyle.BOOK_STRIP]-də seçilmiş kitab; `null` = son oxunan, o da yoxdursa birinci. */
    var stripBook by mutableStateOf(stripBook)

    /** Siyahının sürüşəcəyi sətrin açarı ([ContentsItem.key]) — sətir görünəndə sıfırlanır. */
    var pendingFocus by mutableStateOf<String?>(null)

    /**
     * Akkordeon və ağacda bab kitab kartının **içindədir** (ayrıca siyahı elementi deyil), ona görə
     * [pendingFocus] yalnız karta qədər sürüşə bilər. Babın özü görünməyə bunu oxuyub gəlir.
     */
    var focusChapter by mutableStateOf<String?>(null)

    fun toggleBook(slug: String) {
        expandedBooks = expandedBooks.toggled(slug)
    }

    fun toggleChapter(slug: String) {
        expandedChapters = expandedChapters.toggled(slug)
    }

    /** [HadithIndexStyle.SINGLE_BOOK]: yeni kitab açılanda əvvəlki yığılır və başlıq yuxarı gəlir. */
    fun toggleSingleBook(slug: String) {
        if (slug in expandedBooks) {
            expandedBooks = emptySet()
        } else {
            expandedBooks = setOf(slug)
            pendingFocus = headerKey(slug)
        }
    }

    /**
     * Kənardan gələn hədəfi göstərir — mündəricat ağacı vərəqi, axtarış nəticəsi, və ya ayrıca
     * kitab/bab ünvanı ilə açılan ekran. Bu görünüşlərdə kitab və bab ayrı ekran deyil, ona görə
     * «oraya get» həmin düyünü açıb ona sürüşmək deməkdir.
     */
    fun reveal(style: HadithIndexStyle, bookSlug: String, chapterSlug: String?) {
        when (style) {
            HadithIndexStyle.SINGLE_BOOK -> expandedBooks = setOf(bookSlug)
            HadithIndexStyle.BOOK_STRIP -> stripBook = bookSlug
            else -> expandedBooks = expandedBooks + bookSlug
        }
        if (chapterSlug != null) expandedChapters = expandedChapters + chapterSlug

        pendingFocus = when (style) {
            HadithIndexStyle.SINGLE_BOOK -> chapterSlug?.let(::chapterKey) ?: headerKey(bookSlug)
            HadithIndexStyle.BOOK_STRIP -> chapterSlug?.let(::chapterKey) ?: STRIP_KEY
            else -> {
                focusChapter = chapterSlug
                bookKey(bookSlug)
            }
        }
    }

    companion object {
        // `Set<String>` SaveableStateRegistry-nin qəbul etdiyi tip deyil (Android-də saxlanma anında
        // IllegalArgumentException) — ArrayList kimi yazılır.
        @Suppress("UNCHECKED_CAST")
        val Saver: Saver<HadithContentsState, Any> = listSaver(
            save = {
                listOf(ArrayList(it.expandedBooks), ArrayList(it.expandedChapters), it.stripBook.orEmpty())
            },
            restore = {
                HadithContentsState(
                    expandedBooks = (it[0] as List<String>).toSet(),
                    expandedChapters = (it[1] as List<String>).toSet(),
                    stripBook = (it[2] as String).ifEmpty { null },
                )
            },
        )
    }
}

/** Cild dəyişəndə vəziyyət sıfırlanır — başqa cildin açıq kitabları burada mənasızdır. */
@Composable
fun rememberHadithContentsState(volumeSlug: String?): HadithContentsState =
    rememberSaveable(volumeSlug, saver = HadithContentsState.Saver) {
        HadithContentsState(emptySet(), emptySet(), null)
    }

/**
 * [HadithContentsState.focusChapter] bu baba işarə edəndə sətri görünən sahəyə gətirir. Gecikmə
 * kitabın açılma animasiyası və karta sürüşmə bitsin deyədir — yoxsa iki sürüşmə bir-birini ləğv edir.
 */
@Composable
private fun revealOnFocus(chapterSlug: String, state: HadithContentsState): Modifier {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(state.focusChapter) {
        if (state.focusChapter != chapterSlug) return@LaunchedEffect
        delay(450)
        requester.bringIntoView()
        state.focusChapter = null
    }
    return Modifier.bringIntoViewRequester(requester)
}

private fun Set<String>.toggled(slug: String): Set<String> =
    if (slug in this) this - slug else this + slug

private const val STRIP_KEY = "strip"
private fun bookKey(slug: String) = "b:$slug"
private fun headerKey(slug: String) = "h:$slug"
private fun chapterKey(slug: String) = "c:$slug"

private class BookNode(val book: HadithBook, val chapters: List<ChapterNode>) {
    val subCount: Int get() = chapters.sumOf { it.subs.size }
    val hadithCount: Int get() = chapters.sumOf { it.hadithCount }
}

private class ChapterNode(
    val book: HadithBook,
    val chapter: HadithChapter,
    val subs: List<SubNode>,
    /** Alt babların hədisləri də daxil — bax [HadithOutline.chapterHadithCounts]. */
    val hadithCount: Int,
) {
    val hasSubs: Boolean get() = subs.isNotEmpty()
}

private class SubNode(val sub: HadithSubChapter, val hadithCount: Int)

private fun HadithOutline.toNodes(): List<BookNode> = books.sortedBy { it.book_no }.map { book ->
    BookNode(
        book = book,
        chapters = chaptersByBook[book.slug].orEmpty().sortedBy { it.chapter_no }.map { chapter ->
            ChapterNode(
                book = book,
                chapter = chapter,
                subs = subChaptersByChapter[chapter.slug].orEmpty()
                    .sortedBy { it.sub_chapter_no }
                    .map { SubNode(it, subChapterHadithCounts[it.slug] ?: 0) },
                hadithCount = chapterHadithCounts[chapter.slug] ?: 0,
            )
        },
    )
}

/**
 * Siyahının bir sətri. Hamısı düz siyahıdadır ki, [HadithContentsState.pendingFocus] açarın
 * indeksini tapıb ora sürüşə bilsin, və yapışqan başlıqlar ([BookHeader], [Strip]) ayrıca element
 * olsun.
 */
private sealed interface ContentsItem {
    val key: String

    data object Search : ContentsItem { override val key = "search" }
    data object Everywhere : ContentsItem { override val key = "everywhere" }
    data object MatchesTitle : ContentsItem { override val key = "matches" }
    data object Empty : ContentsItem { override val key = "empty" }
    data object Toolbar : ContentsItem { override val key = "toolbar" }
    data object Strip : ContentsItem { override val key = STRIP_KEY }

    class StripHero(val node: BookNode) : ContentsItem { override val key = "hero" }
    class Book(val node: BookNode) : ContentsItem { override val key = bookKey(node.book.slug) }
    class BookHeader(val node: BookNode) : ContentsItem { override val key = headerKey(node.book.slug) }
    class Chapter(val node: ChapterNode) : ContentsItem { override val key = chapterKey(node.chapter.slug) }
    class BookMatch(val node: BookNode) : ContentsItem { override val key = "mb:" + node.book.slug }
    class Match(val match: OutlineMatch) : ContentsItem { override val key = "m:" + match.key }
}

/** Sətirlərin paylaşdığı vəziyyət və əməliyyatlar — hər sətrə on parametr ötürməmək üçün. */
private class ContentsScope(
    val state: HadithContentsState,
    val completion: HadithCompletion,
    val lastReadByBook: Map<String, HadithReadHistoryEntity>,
    val arabicFont: FontFamily,
    val openChapter: (ChapterNode) -> Unit,
    val openSub: (ChapterNode, SubNode) -> Unit,
    val resume: (HadithBook) -> Unit,
    /** Yalnız admində dolur — uzun basma hamıda aktiv olsaydı boş vəd olardı. */
    val onBookLongClick: ((HadithBook) -> Unit)?,
    val onChapterLongClick: ((HadithChapter) -> Unit)?,
    val onSubLongClick: ((HadithSubChapter) -> Unit)?,
) {
    fun history(book: HadithBook): HadithReadHistoryEntity? =
        lastReadByBook[book.slug]?.takeIf { it.chapterSlug != null }

    fun isLastReadChapter(node: ChapterNode): Boolean =
        history(node.book)?.chapterSlug == node.chapter.slug

    fun isLastReadSub(node: ChapterNode, sub: SubNode): Boolean =
        history(node.book)?.let {
            it.chapterSlug == node.chapter.slug && it.subChapterSlug == sub.sub.slug
        } == true

    fun isExpanded(node: ChapterNode): Boolean =
        node.hasSubs && node.chapter.slug in state.expandedChapters

    /** Alt babı olan bab yerində açılıb-yığılır, olmayan isə birbaşa hədislərə keçir (dua kimi). */
    fun onChapterClick(node: ChapterNode) {
        if (node.hasSubs) state.toggleChapter(node.chapter.slug) else openChapter(node)
    }

    fun nameStyle(base: TextStyle, arabic: Boolean): TextStyle =
        if (arabic) {
            base.withArabicNameSize(base.fontSize).withScriptDirection(arabic = true, arabicFontFamily = arabicFont)
        } else {
            base.withScriptDirection(arabic = false)
        }

    fun arabicLineStyle(base: TextStyle): TextStyle =
        base.withArabicNameSize(base.fontSize, lineHeightRatio = 1.6f)
            .withScriptDirection(arabic = true, arabicFontFamily = arabicFont)
}

/** Admin uzun basma menyusunun hədəfi. */
private sealed interface NodeMenu {
    data class Book(val book: HadithBook) : NodeMenu
    data class Chapter(val chapter: HadithChapter) : NodeMenu
}

/** Açıq redaktor — [HadithBooksScreen]/[HadithChaptersScreen]-dəki ilə eyni ekranlar. */
private sealed interface ContentsEditor {
    data object AddBook : ContentsEditor
    data class EditBook(val book: HadithBook) : ContentsEditor
    data class AddChapter(val bookSlug: String) : ContentsEditor
    data class BulkAdd(val book: HadithBook) : ContentsEditor
    data class EditChapter(val chapter: HadithChapter) : ContentsEditor
    data class AddSub(val chapterSlug: String) : ContentsEditor
    data class EditSub(val sub: HadithSubChapter) : ContentsEditor
}

/**
 * Cildin bütün mündəricatı bir siyahıda — dua siyahısının akkordeon dili ilə.
 *
 * [HadithIndexStyle.PAGED]-in kitab → bab → alt bab ekranlarını əvəz edir; hədis siyahısı isə
 * dəyişmir, [onOpen] onu [HadithIndexScreen]-in adi seçim yolu ilə açır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithVolumeContentsScreen(
    volumeSlug: String,
    volumeName: String,
    style: HadithIndexStyle,
    state: HadithContentsState,
    listState: LazyListState,
    onBack: () -> Unit,
    /** Hero logosu — cildin mündəricat ağacı vərəqi. */
    onShowOutline: () -> Unit,
    /** Bab/alt bab seçimi — mündəricat vərəqi ilə eyni seam (`navigateToOutlineNode`). */
    onOpen: (HadithBook, HadithChapter?, HadithSubChapter?) -> Unit,
) {
    val viewModel = viewModel(appScopedViewModelStoreOwner()) { HadithViewModel() }
    val authViewModel = viewModel { AuthViewModel() }
    val session by authViewModel.session.collectAsState()
    val isAuthenticated = session != null

    val outline by viewModel.volumeOutline.collectAsState()
    LaunchedEffect(volumeSlug) { viewModel.fetchVolumeOutline(volumeSlug) }

    LaunchedEffect(Unit) { viewModel.observeCompletion() }
    val completion by viewModel.completion.collectAsState()

    val lastReadFlow = remember { RepositoryProvider.userRepository.getLatestHadithHistoryPerBookFlow() }
    val lastReadByBook by lastReadFlow.collectAsState(emptyMap())

    var editor by remember { mutableStateOf<ContentsEditor?>(null) }
    var menu by remember { mutableStateOf<NodeMenu?>(null) }

    editor?.let { current ->
        val close = {
            editor = null
            viewModel.fetchVolumeOutline(volumeSlug)
        }
        BackHandler { close() }
        ContentsEditorHost(current, volumeSlug, onBack = close)
        return
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val query = searchQuery.trim()

    // Yalnız yüklənmiş cild: ViewModel app-scoped-dur və əvvəlki cildin ağacını hələ saxlaya bilər.
    val data = outline?.takeIf { it.books.firstOrNull()?.volume_slug.let { slug -> slug == null || slug == volumeSlug } }
    val nodes = remember(data) { data?.toNodes().orEmpty() }

    val stripBook = state.stripBook
        ?: nodes.mapNotNull { node -> lastReadByBook[node.book.slug]?.let { node to it.datetime } }
            .maxByOrNull { it.second }?.first?.book?.slug
        ?: nodes.firstOrNull()?.book?.slug

    val items = remember(nodes, style, query, state.expandedBooks, stripBook, data) {
        buildList {
            add(ContentsItem.Search)

            if (query.isNotEmpty()) {
                add(ContentsItem.Everywhere)
                val bookMatches = nodes.filter { hadithNameMatches(query, it.book.name, it.book.name_ar) }
                val matches = data?.nameMatches(query).orEmpty()
                bookMatches.forEach { add(ContentsItem.BookMatch(it)) }
                if (matches.isNotEmpty()) {
                    add(ContentsItem.MatchesTitle)
                    matches.forEach { add(ContentsItem.Match(it)) }
                }
                if (bookMatches.isEmpty() && matches.isEmpty()) add(ContentsItem.Empty)
                return@buildList
            }

            if (nodes.isEmpty()) {
                add(ContentsItem.Empty)
                return@buildList
            }

            when (style) {
                HadithIndexStyle.SINGLE_BOOK -> nodes.forEach { node ->
                    if (node.book.slug in state.expandedBooks) {
                        add(ContentsItem.BookHeader(node))
                        node.chapters.forEach { add(ContentsItem.Chapter(it)) }
                    } else {
                        add(ContentsItem.Book(node))
                    }
                }

                HadithIndexStyle.BOOK_STRIP -> {
                    val selected = nodes.firstOrNull { it.book.slug == stripBook } ?: nodes.first()
                    add(ContentsItem.Strip)
                    add(ContentsItem.StripHero(selected))
                    selected.chapters.forEach { add(ContentsItem.Chapter(it)) }
                }

                HadithIndexStyle.TREE -> {
                    add(ContentsItem.Toolbar)
                    nodes.forEach { add(ContentsItem.Book(it)) }
                }

                else -> nodes.forEach { add(ContentsItem.Book(it)) }
            }
        }
    }

    // Yapışqan başlıq (tək açıq kitab, kitab zolağı) babın üstünü örtür — sürüşmə onun altında dayanır.
    val stickyOffsetPx = with(LocalDensity.current) { 88.dp.roundToPx() }
    LaunchedEffect(state.pendingFocus, items) {
        val key = state.pendingFocus ?: return@LaunchedEffect
        val index = items.indexOfFirst { it.key == key }
        if (index < 0) return@LaunchedEffect

        val behindSticky = key.startsWith("c:") &&
            (style == HadithIndexStyle.SINGLE_BOOK || style == HadithIndexStyle.BOOK_STRIP)
        listState.animateScrollToItem(index, if (behindSticky) -stickyOffsetPx else 0)
        // Sürüşmədən SONRA: açarı əvvəl sıfırlasaq effekt yenidən başlayır və sürüşməni ləğv edir.
        state.pendingFocus = null
    }

    fun findChapter(slug: String): ChapterNode? =
        nodes.firstNotNullOfOrNull { book -> book.chapters.firstOrNull { it.chapter.slug == slug } }

    val scope = ContentsScope(
        state = state,
        completion = completion,
        lastReadByBook = lastReadByBook,
        arabicFont = hadithArabicFontFamily(HadithPreferences.observeArabicFont()),
        openChapter = { onOpen(it.book, it.chapter, null) },
        openSub = { chapter, sub -> onOpen(chapter.book, chapter.chapter, sub.sub) },
        resume = { book ->
            lastReadByBook[book.slug]?.takeIf { it.chapterSlug != null }?.let { history ->
                // Bitmiş babda hədəf növbətisinə sürüşür — hesab ViewModel-dədir. Real obyektlər
                // ağacdan götürülür ki, oxucunun başlığı boş gəlməsin.
                viewModel.resolveResumeTarget(history) { target ->
                    val chapterNode = findChapter(target.chapterSlug)
                    val targetBook = chapterNode?.book ?: book
                    val chapter = chapterNode?.chapter
                        ?: HadithChapter(target.chapterSlug, targetBook.slug, 0, "")
                    val sub = target.subChapterSlug?.let { slug ->
                        chapterNode?.subs?.firstOrNull { it.sub.slug == slug }?.sub
                            ?: HadithSubChapter(slug, target.chapterSlug, 0, "")
                    }
                    onOpen(targetBook, chapter, sub)
                }
            }
        },
        onBookLongClick = if (isAuthenticated) ({ menu = NodeMenu.Book(it) }) else null,
        onChapterLongClick = if (isAuthenticated) ({ menu = NodeMenu.Chapter(it) }) else null,
        onSubLongClick = if (isAuthenticated) ({ editor = ContentsEditor.EditSub(it) }) else null,
    )

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
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (isAuthenticated) {
                HadithEditFab(
                    onClick = { editor = ContentsEditor.AddBook },
                    contentDescription = stringResource(Res.string.strTitleAddBook),
                    modifier = Modifier.padding(bottom = mainBottomNavFabPadding()),
                )
            }
        },
    ) { paddingValues ->
        Box(
            // Siyahı bar-ın altından başlayır, arxasından yox: yapışqan başlıq siyahının yuxarı
            // kənarına yapışır və orada bar-ın altında qalardı.
            modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (data == null) {
                Loader(true)
                return@Box
            }

            LazyColumn(
                state = listState,
                // Planşetdə tək sütun ekranın bütün enini tutsa sətirlər oxunmaz dərəcədə uzanır.
                modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = mainBottomNavContentPadding(if (isAuthenticated) 88.dp else 16.dp),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { item ->
                    when (item) {
                        is ContentsItem.BookHeader -> stickyHeader(key = item.key) {
                            OpenBookHeader(item.node, scope)
                        }

                        ContentsItem.Strip -> stickyHeader(key = item.key) {
                            BookStrip(nodes, stripBook, scope) { slug ->
                                state.stripBook = slug
                                state.pendingFocus = STRIP_KEY
                            }
                        }

                        else -> item(key = item.key) {
                            when (item) {
                                ContentsItem.Search -> FilterField(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    hint = stringResource(Res.string.strHintSearch),
                                    keyboardType = KeyboardType.Text,
                                )

                                // Qutu yalnız ADLARI süzür; söz hədisin mətnindədirsə cavab axtarış ekranındadır.
                                ContentsItem.Everywhere -> ReaderUiHooks.openSearch?.let { openSearch ->
                                    SearchEverywhereRow(query = query, onClick = { openSearch(query) })
                                }

                                ContentsItem.MatchesTitle -> Text(
                                    text = stringResource(Res.string.hadithOutlineMatches),
                                    style = typography.titleSmall,
                                    color = colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp),
                                )

                                ContentsItem.Empty -> HadithIndexEmptyState()

                                ContentsItem.Toolbar -> TreeToolbar(nodes, state)

                                is ContentsItem.StripHero -> StripHero(item.node, scope, ::findChapter)

                                is ContentsItem.Book -> when (style) {
                                    HadithIndexStyle.SINGLE_BOOK -> ClosedBookCard(item.node, scope)
                                    HadithIndexStyle.TREE -> TreeBookCard(item.node, scope)
                                    else -> AccordionBookCard(item.node, scope)
                                }

                                is ContentsItem.Chapter -> when (style) {
                                    HadithIndexStyle.BOOK_STRIP -> StripChapterCard(item.node, scope)
                                    else -> NumberedChapterBlock(item.node, scope)
                                }

                                is ContentsItem.BookMatch -> {
                                    val name = rememberHadithDisplayName(item.node.book.name, item.node.book.name_ar)
                                    HadithEntryCard(
                                        title = name.text,
                                        titleIsArabic = name.isArabic,
                                        arabicTitle = name.secondaryArabic,
                                        leadingText = item.node.book.book_no.toString(),
                                        leadingColor = colorScheme.secondary,
                                        leadingContainerColor = colorScheme.secondaryContainer,
                                        titleMaxLines = 2,
                                        countText = stringResource(Res.string.strLabelCountBabs, item.node.chapters.size),
                                        onClick = {
                                            searchQuery = ""
                                            state.reveal(style, item.node.book.slug, null)
                                        },
                                    )
                                }

                                is ContentsItem.Match -> OutlineMatchCard(item.match) {
                                    val match = item.match
                                    val opensInPlace = match.subChapter == null &&
                                        findChapter(match.chapter.slug)?.hasSubs == true
                                    if (opensInPlace) {
                                        // Alt babı olan babın öz hədis siyahısı yoxdur — onu
                                        // mündəricatda açıb göstəririk.
                                        searchQuery = ""
                                        state.reveal(style, match.book.slug, match.chapter.slug)
                                    } else {
                                        onOpen(match.book, match.chapter, match.subChapter)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    menu?.let { target ->
        NodeMenuDialog(
            target = target,
            onDismiss = { menu = null },
            onPick = { picked ->
                menu = null
                editor = picked
            },
        )
    }
}

@Composable
private fun ContentsEditorHost(editor: ContentsEditor, volumeSlug: String, onBack: () -> Unit) {
    when (editor) {
        ContentsEditor.AddBook -> HadithEditorScreen(
            type = EditorType.BOOK, volumeSlug = volumeSlug, onBack = onBack,
        )

        is ContentsEditor.EditBook -> HadithEditorScreen(
            type = EditorType.BOOK, initialBook = editor.book, volumeSlug = volumeSlug, onBack = onBack,
        )

        is ContentsEditor.AddChapter -> HadithEditorScreen(
            type = EditorType.CHAPTER, bookSlug = editor.bookSlug, onBack = onBack,
        )

        is ContentsEditor.BulkAdd -> HadithBulkAddScreen(
            bookSlug = editor.book.slug,
            bookName = hadithTitleText(editor.book.name, editor.book.name_ar),
            onBack = onBack,
        )

        is ContentsEditor.EditChapter -> HadithEditorScreen(
            type = EditorType.CHAPTER,
            initialChapter = editor.chapter,
            bookSlug = editor.chapter.book_slug,
            onBack = onBack,
        )

        is ContentsEditor.AddSub -> HadithEditorScreen(
            type = EditorType.SUB_CHAPTER, chapterSlug = editor.chapterSlug, onBack = onBack,
        )

        is ContentsEditor.EditSub -> HadithEditorScreen(
            type = EditorType.SUB_CHAPTER,
            initialSubChapter = editor.sub,
            chapterSlug = editor.sub.chapter_slug,
            onBack = onBack,
        )
    }
}

/**
 * Uzun basma menyusu — pilləli görünüşdə FAB-larda olan əməliyyatlar burada bir yerdədir, çünki bu
 * görünüşdə ayrıca kitab/bab ekranı (və onların FAB-ı) yoxdur.
 */
@Composable
private fun NodeMenuDialog(
    target: NodeMenu,
    onDismiss: () -> Unit,
    onPick: (ContentsEditor) -> Unit,
) {
    val (title, options) = when (target) {
        is NodeMenu.Book -> hadithTitleText(target.book.name, target.book.name_ar) to listOf(
            Res.string.strLabelEdit to ContentsEditor.EditBook(target.book),
            Res.string.strActionAddBabName to ContentsEditor.AddChapter(target.book.slug),
            Res.string.strActionBulkAdd to ContentsEditor.BulkAdd(target.book),
        )

        is NodeMenu.Chapter -> hadithTitleText(target.chapter.name, target.chapter.name_ar) to listOf(
            Res.string.strLabelEdit to ContentsEditor.EditChapter(target.chapter),
            Res.string.hadithAddSubBab to ContentsEditor.AddSub(target.chapter.slug),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column {
                options.forEach { (label, editor) ->
                    Text(
                        text = stringResource(label),
                        style = typography.bodyLarge,
                        color = colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shapes.small)
                            .clickable { onPick(editor) }
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.strLabelCancel)) }
        },
    )
}

// ── Ortaq hissələr ──────────────────────────────────────────────────────────────────────────────

/**
 * «Oxumağa davam et» nişanı — dua və hədis siyahılarındakı ilə eyni görkəm. [onClick] yalnız kitab
 * səviyyəsində verilir (sətir siyahını, nişan isə son yeri açır); bab/alt bab sətrində sətrin özü
 * ora aparır, ona görə nişan orada basılmır.
 */
@Composable
private fun ResumeMark(size: Dp, onClick: (() -> Unit)? = null) {
    val label = stringResource(Res.string.strLabelResumeReading)

    SimpleTooltip(text = label) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colorScheme.primaryContainer.alpha(0.45f))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_history),
                contentDescription = label,
                tint = colorScheme.primary,
                modifier = Modifier.size(size * 0.53f),
            )
        }
    }
}

@Composable
private fun DoneMark(size: Dp = 18.dp) {
    Icon(
        painter = painterResource(Res.drawable.dr_icon_check),
        contentDescription = stringResource(Res.string.strLabelReadCompleted),
        tint = colorScheme.primary,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun ExpandChevron(expanded: Boolean, size: Dp = 18.dp, collapsedRotation: Float = 0f) {
    val rotation by animateFloatAsState(if (expanded) 90f else collapsedRotation)
    Icon(
        painter = painterResource(Res.drawable.dr_icon_chevron_right),
        contentDescription = null,
        tint = colorScheme.onSurfaceVariant.alpha(0.9f),
        modifier = Modifier.size(size).rotate(rotation),
    )
}

@Composable
private fun NumberTile(number: Int, size: Dp, container: Color, content: Color, shape: Shape) {
    Box(
        modifier = Modifier.size(size).clip(shape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
private fun ArabicSecondLine(text: String, scope: ContentsScope, maxLines: Int, color: Color = colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = scope.arabicLineStyle(typography.bodyMedium),
        color = color,
        // Abzas kartın qalan sətirləri ilə eyni kənara dayanır — bax HadithEntryCard.
        textAlign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextAlign.Right else TextAlign.Left,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun chapterCountText(node: ChapterNode): String? = when {
    node.hasSubs -> stringResource(Res.string.strLabelCountSubBabs, node.subs.size)
    node.hadithCount > 0 -> stringResource(Res.string.strLabelCountHadiths, node.hadithCount)
    else -> null
}

@Composable
private fun CountLabel(text: String) {
    Text(
        text = text,
        style = typography.labelSmall.withScriptDirection(arabic = false),
        color = colorScheme.onSurfaceVariant.alpha(0.75f),
        maxLines = 1,
    )
}

/** Babın sağ ucu: davam nişanı, ✓, say və (alt babı varsa) şevron. */
@Composable
private fun RowScope.ChapterTrailing(node: ChapterNode, scope: ContentsScope, markSize: Dp) {
    val expanded = scope.isExpanded(node)

    // Açıq babda nişan alt babın öz sətrinə keçir — orada daha dəqiq yer deyir.
    if (scope.isLastReadChapter(node) && !expanded) {
        Spacer(Modifier.width(6.dp))
        ResumeMark(markSize)
    }
    if (scope.completion.isChapterCompleted(node.chapter.slug)) {
        Spacer(Modifier.width(6.dp))
        DoneMark(16.dp)
    }
    chapterCountText(node)?.let {
        Spacer(Modifier.width(8.dp))
        CountLabel(it)
    }
    if (node.hasSubs) {
        Spacer(Modifier.width(4.dp))
        ExpandChevron(expanded, 16.dp)
    }
}

/**
 * Alt bab sətri. [topic] dua mövzusunun görkəmidir (kitab zolağının kartında); qalan görünüşlərdə
 * sətir bir pillə içəridə, açıq fonda və nömrəli durur ki, babdan ayrılsın.
 */
@Composable
private fun SubRow(chapter: ChapterNode, sub: SubNode, scope: ContentsScope, label: String?, topic: Boolean) {
    val name = rememberHadithDisplayName(sub.sub.name, sub.sub.name_ar)
    val shape = RoundedCornerShape(if (topic) 10.dp else 8.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (topic) {
                    Modifier.background(colorScheme.surfaceVariant.alpha(0.4f))
                } else {
                    Modifier
                        .background(colorScheme.surface.alpha(0.7f))
                        .border(0.5.dp, colorScheme.outlineVariant.alpha(0.45f), shape)
                }
            )
            .combinedClickable(
                onLongClick = scope.onSubLongClick?.let { { it(sub.sub) } },
                onClick = { scope.openSub(chapter, sub) },
            )
            .padding(start = if (topic) 14.dp else 12.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (label != null) {
            Text(
                text = label,
                style = typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.tertiary,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        Text(
            text = name.text,
            style = scope.nameStyle(if (topic) typography.bodyMedium else typography.bodySmall, name.isArabic),
            color = colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (scope.isLastReadSub(chapter, sub)) {
            Spacer(Modifier.width(6.dp))
            ResumeMark(26.dp)
        }
        if (scope.completion.isNodeCompleted(sub.sub.slug)) {
            Spacer(Modifier.width(6.dp))
            DoneMark(16.dp)
        }
        if (sub.hadithCount > 0) {
            Spacer(Modifier.width(8.dp))
            CountLabel(stringResource(Res.string.strLabelCountHadiths, sub.hadithCount))
        }
    }
}

@Composable
private fun SubList(chapter: ChapterNode, scope: ContentsScope, modifier: Modifier, numbered: Boolean, topic: Boolean) {
    AnimatedVisibility(
        visible = scope.isExpanded(chapter),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (topic) 4.dp else 2.dp)) {
            chapter.subs.forEach { sub ->
                val label = when {
                    !numbered -> null
                    topic -> sub.sub.sub_chapter_no.toString()
                    else -> "${chapter.chapter.chapter_no}.${sub.sub.sub_chapter_no}"
                }
                SubRow(chapter, sub, scope, label, topic)
            }
        }
    }
}

private val CardShape = RoundedCornerShape(16.dp)

@Composable
private fun ContentsCard(content: @Composable () -> Unit) {
    Surface(
        color = colorScheme.surfaceContainerLow,
        shape = CardShape,
        border = BorderStroke(0.5.dp, colorScheme.outlineVariant.alpha(0.5f)),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

// ── A: Akkordeon kartlar ────────────────────────────────────────────────────────────────────────

/** Dua başlıq kartının hədis qarşılığı: kitab yerində açılır, bablar mövzu sətirləri kimi içindədir. */
@Composable
private fun AccordionBookCard(node: BookNode, scope: ContentsScope) {
    val book = node.book
    val expanded = book.slug in scope.state.expandedBooks
    val name = rememberHadithDisplayName(book.name, book.name_ar)

    ContentsCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClickLabel = stringResource(
                            if (expanded) Res.string.strLabelOutlineCollapseAll else Res.string.strLabelOutlineExpandAll
                        ),
                        onLongClick = scope.onBookLongClick?.let { { it(book) } },
                        onClick = { scope.state.toggleBook(book.slug) },
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name.text,
                        style = scope.nameStyle(typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), name.isArabic),
                        color = colorScheme.onSurface,
                    )
                    CountLabel(stringResource(Res.string.strLabelCountBabs, node.chapters.size))
                }

                name.secondaryArabic?.let { arabic ->
                    Text(
                        text = arabic,
                        style = typography.titleMedium.withScriptDirection(arabic = true, arabicFontFamily = scope.arabicFont),
                        color = colorScheme.onSurface.alpha(0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp).widthIn(max = 170.dp),
                    )
                }

                // Açıq kartda nişan babın öz sətrinə keçir (dua kimi).
                if (scope.history(book) != null && !expanded) {
                    ResumeMark(34.dp) { scope.resume(book) }
                    Spacer(Modifier.width(6.dp))
                }
                if (scope.completion.isBookCompleted(book.slug)) {
                    DoneMark()
                    Spacer(Modifier.width(6.dp))
                }
                ExpandChevron(expanded)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    node.chapters.forEach { TopicChapterRow(it, scope) }
                }
            }
        }
    }
}

@Composable
private fun TopicChapterRow(node: ChapterNode, scope: ContentsScope) {
    val name = rememberHadithDisplayName(node.chapter.name, node.chapter.name_ar)

    Column(modifier = revealOnFocus(node.chapter.slug, scope.state)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surfaceVariant.alpha(0.4f))
                .combinedClickable(
                    onLongClick = scope.onChapterLongClick?.let { { it(node.chapter) } },
                    onClick = { scope.onChapterClick(node) },
                )
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name.text,
                style = scope.nameStyle(typography.bodyMedium, name.isArabic),
                color = colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ChapterTrailing(node, scope, markSize = 28.dp)
        }
        SubList(node, scope, Modifier.padding(start = 14.dp, top = 4.dp), numbered = true, topic = false)
    }
}

// ── B: Tək açıq kitab ───────────────────────────────────────────────────────────────────────────

@Composable
private fun BookTitleBlock(node: BookNode, scope: ContentsScope, onContainer: Boolean, modifier: Modifier) {
    val name = rememberHadithDisplayName(node.book.name, node.book.name_ar)
    val total = node.chapters.size
    val done = node.chapters.count { scope.completion.isChapterCompleted(it.chapter.slug) }
    val titleColor = if (onContainer) colorScheme.onPrimaryContainer else colorScheme.onSurface
    val metaColor = if (onContainer) colorScheme.onPrimaryContainer.alpha(0.8f) else colorScheme.onSurfaceVariant.alpha(0.8f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = name.text,
            style = scope.nameStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), name.isArabic),
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val count = stringResource(Res.string.strLabelCountBabs, total)
        Text(
            text = if (done > 0) count + " · " + stringResource(Res.string.hadithBookProgress, done, total) else count,
            style = typography.labelSmall.withScriptDirection(arabic = false),
            color = metaColor,
        )
        if (done > 0 && total > 0) {
            // İrəliləyiş zolağı: kitabın neçə babı oxunub-bitib.
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .widthIn(max = 160.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (onContainer) colorScheme.primary.alpha(0.2f) else colorScheme.outlineVariant.alpha(0.6f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(done.toFloat() / total)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun ClosedBookCard(node: BookNode, scope: ContentsScope) {
    val book = node.book
    val name = rememberHadithDisplayName(book.name, book.name_ar)

    ContentsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onLongClick = scope.onBookLongClick?.let { { it(book) } },
                    onClick = { scope.state.toggleSingleBook(book.slug) },
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NumberTile(
                number = book.book_no,
                size = 36.dp,
                container = colorScheme.secondaryContainer.alpha(0.6f),
                content = colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(10.dp),
            )
            BookTitleBlock(node, scope, onContainer = false, modifier = Modifier.weight(1f))
            name.secondaryArabic?.let { arabic ->
                Text(
                    text = arabic,
                    style = typography.titleSmall.withScriptDirection(arabic = true, arabicFontFamily = scope.arabicFont),
                    color = colorScheme.onSurface.alpha(0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp),
                )
            }
            if (scope.history(book) != null) ResumeMark(34.dp) { scope.resume(book) }
            if (scope.completion.isBookCompleted(book.slug)) DoneMark()
            ExpandChevron(expanded = false)
        }
    }
}

/** Açıq kitabın yapışqan başlığı — sürüşdükcə yuxarıda qalır, toxunanda kitabı yığır. */
@Composable
private fun OpenBookHeader(node: BookNode, scope: ContentsScope) {
    val book = node.book
    val name = rememberHadithDisplayName(book.name, book.name_ar)

    // Fon siyahının arxa fonudur: altından sürüşən sətirlər başlığın kənarlarında görünməsin.
    Box(modifier = Modifier.fillMaxWidth().background(colorScheme.background).padding(vertical = 4.dp)) {
        Surface(
            color = colorScheme.primaryContainer,
            shape = CardShape,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClickLabel = stringResource(Res.string.strLabelOutlineCollapseAll),
                        onLongClick = scope.onBookLongClick?.let { { it(book) } },
                        onClick = { scope.state.toggleSingleBook(book.slug) },
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NumberTile(
                    number = book.book_no,
                    size = 36.dp,
                    container = colorScheme.primary,
                    content = colorScheme.onPrimary,
                    shape = RoundedCornerShape(10.dp),
                )
                BookTitleBlock(node, scope, onContainer = true, modifier = Modifier.weight(1f))
                name.secondaryArabic?.let { arabic ->
                    Text(
                        text = arabic,
                        style = typography.titleSmall.withScriptDirection(arabic = true, arabicFontFamily = scope.arabicFont),
                        color = colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp),
                    )
                }
                // Yuxarı baxan şevron — «yığ».
                ExpandChevron(expanded = false, collapsedRotation = -90f)
            }
        }
    }
}

/** Nömrəli bab sətri: tək açıq kitabın içində, ərəbcə adı altında. */
@Composable
private fun NumberedChapterBlock(node: ChapterNode, scope: ContentsScope) {
    val name = rememberHadithDisplayName(node.chapter.name, node.chapter.name_ar)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceContainerLow)
                .combinedClickable(
                    onLongClick = scope.onChapterLongClick?.let { { it(node.chapter) } },
                    onClick = { scope.onChapterClick(node) },
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberTile(
                number = node.chapter.chapter_no,
                size = 26.dp,
                container = colorScheme.tertiaryContainer.alpha(0.7f),
                content = colorScheme.onTertiaryContainer,
                shape = CircleShape,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.text,
                    style = scope.nameStyle(typography.bodyMedium, name.isArabic),
                    color = colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                name.secondaryArabic?.let { ArabicSecondLine(it, scope, maxLines = 1) }
            }
            ChapterTrailing(node, scope, markSize = 28.dp)
        }
        SubList(node, scope, Modifier.padding(start = 36.dp), numbered = true, topic = false)
    }
}

// ── C: Kitab zolağı ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BookStrip(nodes: List<BookNode>, selected: String?, scope: ContentsScope, onSelect: (String) -> Unit) {
    val rowState = rememberLazyListState()
    val selectedIndex = nodes.indexOfFirst { it.book.slug == selected }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) rowState.animateScrollToItem(selectedIndex)
    }

    Box(modifier = Modifier.fillMaxWidth().background(colorScheme.background).padding(vertical = 4.dp)) {
        LazyRow(state = rowState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(nodes, key = { it.book.slug }) { node ->
                BookChip(node, node.book.slug == selected, scope) { onSelect(node.book.slug) }
            }
        }
    }
}

@Composable
private fun BookChip(node: BookNode, selected: Boolean, scope: ContentsScope, onClick: () -> Unit) {
    val name = rememberHadithDisplayName(node.book.name, node.book.name_ar)

    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(if (selected) colorScheme.primaryContainer else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) colorScheme.primary.alpha(0.4f) else colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumberTile(
            number = node.book.book_no,
            size = 28.dp,
            container = if (selected) colorScheme.primary else colorScheme.surfaceVariant,
            content = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            shape = CircleShape,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = name.text,
            style = scope.nameStyle(typography.labelLarge, name.isArabic),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
        )
    }
}

@Composable
private fun StripHero(node: BookNode, scope: ContentsScope, findChapter: (String) -> ChapterNode?) {
    val book = node.book
    val name = rememberHadithDisplayName(book.name, book.name_ar)
    val meta = listOfNotNull(
        stringResource(Res.string.strLabelCountBabs, node.chapters.size),
        node.subCount.takeIf { it > 0 }?.let { stringResource(Res.string.strLabelCountSubBabs, it) },
        node.hadithCount.takeIf { it > 0 }?.let { stringResource(Res.string.strLabelCountHadiths, it) },
    ).joinToString(" · ")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name.text,
                    style = scope.nameStyle(typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), name.isArabic),
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                name.secondaryArabic?.let { arabic ->
                    Text(
                        text = arabic,
                        style = typography.titleLarge.withScriptDirection(arabic = true, arabicFontFamily = scope.arabicFont),
                        color = colorScheme.onSurface.alpha(0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp).widthIn(max = 170.dp),
                    )
                }
            }
            Text(
                text = meta,
                style = typography.bodySmall.withScriptDirection(arabic = false),
                color = colorScheme.onSurfaceVariant,
            )
        }

        scope.history(book)?.let { history ->
            val chapterNode = history.chapterSlug?.let(findChapter)
            val subName = chapterNode?.subs?.firstOrNull { it.sub.slug == history.subChapterSlug }?.sub
                ?.let { hadithTitleText(it.name, it.name_ar) }
            val chapterName = chapterNode?.chapter?.let { hadithTitleText(it.name, it.name_ar) }
            val path = listOfNotNull(chapterName, subName).joinToString(" › ").ifEmpty { history.title }

            Surface(
                color = colorScheme.primaryContainer,
                shape = CardShape,
                modifier = Modifier.fillMaxWidth(),
                onClick = { scope.resume(book) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_history),
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.strLabelResumeReading),
                            style = typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.primary,
                        )
                        Text(
                            text = path,
                            style = typography.bodyMedium.withScriptDirection(arabic = isArabicAppLanguage()),
                            color = colorScheme.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Bab kartı — dua başlıq kartının özü: alt bablar onun mövzu sətirləridir. */
@Composable
private fun StripChapterCard(node: ChapterNode, scope: ContentsScope) {
    val name = rememberHadithDisplayName(node.chapter.name, node.chapter.name_ar)
    val expanded = scope.isExpanded(node)

    ContentsCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onLongClick = scope.onChapterLongClick?.let { { it(node.chapter) } },
                        onClick = { scope.onChapterClick(node) },
                    )
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = name.text,
                        style = scope.nameStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), name.isArabic),
                        color = colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    CountLabel(
                        listOfNotNull(
                            stringResource(Res.string.hadithBabNumber, node.chapter.chapter_no),
                            chapterCountText(node),
                        ).joinToString(" · ")
                    )
                    name.secondaryArabic?.let { ArabicSecondLine(it, scope, maxLines = 2) }
                }
                if (scope.isLastReadChapter(node) && !expanded) {
                    Spacer(Modifier.width(6.dp))
                    ResumeMark(34.dp)
                }
                if (scope.completion.isChapterCompleted(node.chapter.slug)) {
                    Spacer(Modifier.width(6.dp))
                    DoneMark()
                }
                if (node.hasSubs) {
                    Spacer(Modifier.width(6.dp))
                    ExpandChevron(expanded)
                }
            }
            SubList(
                node, scope,
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                numbered = false,
                topic = true,
            )
        }
    }
}

// ── D: Kompakt ağac ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TreeToolbar(nodes: List<BookNode>, state: HadithContentsState) {
    val anyOpen = nodes.any { it.book.slug in state.expandedBooks }
    val meta = listOf(
        stringResource(Res.string.strLabelCountBooks, nodes.size),
        stringResource(Res.string.strLabelCountBabs, nodes.sumOf { it.chapters.size }),
        stringResource(Res.string.strLabelCountSubBabs, nodes.sumOf { it.subCount }),
    ).joinToString(" · ")

    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = meta,
            style = typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = {
                if (anyOpen) {
                    state.expandedBooks = emptySet()
                    state.expandedChapters = emptySet()
                } else {
                    state.expandedBooks = nodes.mapTo(mutableSetOf()) { it.book.slug }
                    state.expandedChapters = nodes.flatMap { it.chapters }
                        .filter { it.hasSubs }
                        .mapTo(mutableSetOf()) { it.chapter.slug }
                }
            },
        ) {
            Text(stringResource(if (anyOpen) Res.string.strLabelOutlineCollapseAll else Res.string.strLabelOutlineExpandAll))
        }
    }
}

/** Ağacın səviyyə xətti — RTL-də sağ kənara keçir. */
private fun Modifier.treeGuide(color: Color): Modifier = drawBehind {
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width else 0f
    drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
}

@Composable
private fun TreeBookCard(node: BookNode, scope: ContentsScope) {
    val book = node.book
    val expanded = book.slug in scope.state.expandedBooks
    val name = rememberHadithDisplayName(book.name, book.name_ar)

    ContentsCard {
        Column(modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(shapes.small)
                    .combinedClickable(
                        onLongClick = scope.onBookLongClick?.let { { it(book) } },
                        onClick = { scope.state.toggleBook(book.slug) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) { ExpandChevron(expanded) }
                NumberTile(
                    number = book.book_no,
                    size = 28.dp,
                    container = colorScheme.secondaryContainer.alpha(0.6f),
                    content = colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = name.text,
                    style = scope.nameStyle(typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), name.isArabic),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (scope.history(book) != null && !expanded) {
                    Spacer(Modifier.width(6.dp))
                    ResumeMark(28.dp) { scope.resume(book) }
                }
                if (scope.completion.isBookCompleted(book.slug)) {
                    Spacer(Modifier.width(6.dp))
                    DoneMark(16.dp)
                }
                Spacer(Modifier.width(8.dp))
                CountLabel(stringResource(Res.string.strLabelCountBabs, node.chapters.size))
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 15.dp, bottom = 8.dp)
                        .treeGuide(colorScheme.outlineVariant)
                        .padding(start = 4.dp),
                ) {
                    node.chapters.forEach { TreeChapter(it, scope) }
                }
            }
        }
    }
}

@Composable
private fun TreeChapter(node: ChapterNode, scope: ContentsScope) {
    val name = rememberHadithDisplayName(node.chapter.name, node.chapter.name_ar)
    val expanded = scope.isExpanded(node)

    Column(modifier = revealOnFocus(node.chapter.slug, scope.state)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clip(shapes.small)
                .combinedClickable(
                    onLongClick = scope.onChapterLongClick?.let { { it(node.chapter) } },
                    onClick = { scope.onChapterClick(node) },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                if (node.hasSubs) {
                    ExpandChevron(expanded, 15.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colorScheme.onSurfaceVariant.alpha(0.4f)),
                    )
                }
            }
            Text(
                text = node.chapter.chapter_no.toString(),
                style = typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.tertiary,
                modifier = Modifier.widthIn(min = 22.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = name.text,
                style = scope.nameStyle(typography.bodyMedium, name.isArabic),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (scope.isLastReadChapter(node) && !expanded) {
                Spacer(Modifier.width(6.dp))
                ResumeMark(24.dp)
            }
            if (scope.completion.isChapterCompleted(node.chapter.slug)) {
                Spacer(Modifier.width(6.dp))
                DoneMark(14.dp)
            }
            chapterCountText(node)?.let {
                Spacer(Modifier.width(8.dp))
                CountLabel(it)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 13.dp, bottom = 4.dp)
                    .treeGuide(colorScheme.outlineVariant.alpha(0.6f))
                    .padding(start = 10.dp),
            ) {
                node.subs.forEach { sub -> TreeSubRow(node, sub, scope) }
            }
        }
    }
}

@Composable
private fun TreeSubRow(chapter: ChapterNode, sub: SubNode, scope: ContentsScope) {
    val name = rememberHadithDisplayName(sub.sub.name, sub.sub.name_ar)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(shapes.small)
            .combinedClickable(
                onLongClick = scope.onSubLongClick?.let { { it(sub.sub) } },
                onClick = { scope.openSub(chapter, sub) },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${chapter.chapter.chapter_no}.${sub.sub.sub_chapter_no}",
            style = typography.labelSmall,
            color = colorScheme.tertiary,
            modifier = Modifier.widthIn(min = 32.dp).padding(end = 6.dp),
        )
        Text(
            text = name.text,
            style = scope.nameStyle(typography.bodySmall, name.isArabic),
            color = colorScheme.onSurface.alpha(0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (scope.isLastReadSub(chapter, sub)) {
            Spacer(Modifier.width(6.dp))
            ResumeMark(24.dp)
        }
        if (scope.completion.isNodeCompleted(sub.sub.slug)) {
            Spacer(Modifier.width(6.dp))
            DoneMark(14.dp)
        }
        if (sub.hadithCount > 0) {
            Spacer(Modifier.width(8.dp))
            CountLabel(stringResource(Res.string.strLabelCountHadiths, sub.hadithCount))
        }
    }
}
