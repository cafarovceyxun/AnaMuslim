package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.utils.appScopedViewModelStoreOwner
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_down
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.strLabelOutlineCollapseAll
import com.cafarovceyxun.anamuslim.resources.strLabelOutlineExpandAll
import com.cafarovceyxun.anamuslim.resources.strLabelHadithIntroduction
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.viewModels.HadithViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Cildin mündəricatı bir ağacda: kitab → bab → alt bab.
 *
 * Cild ekranındakı hero logosu bunu açır. Mövcud naviqator vərəqi eyni iyerarxiyanı **tab-larla**
 * gəzdirir (bir anda yalnız bir səviyyə görünür); burada isə bütün quruluş eyni siyahıdadır və
 * istənilən düyün açılıb-yığıla bilər, ona görə uzaq bir alt babı tapmaq üçün səviyyələr arasında
 * gedib-gəlmək lazım gəlmir.
 *
 * İki toxunma hədəfi var: **şevron** düyünü açıb-yığır, **sətrin qalanı** həmin yerə keçir. Yarpaq
 * sətirlərdə (alt bab, və ya babsız kitab) şevron yoxdur, bütün sətir keçiddir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithVolumeOutlineSheet(
    isOpen: Boolean,
    volumeSlug: String,
    volumeName: String,
    onDismiss: () -> Unit,
    onNavigate: (book: HadithBook, chapter: HadithChapter?, subChapter: HadithSubChapter?) -> Unit,
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.6f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.9f)) {
            OutlineContent(
                volumeSlug = volumeSlug,
                volumeName = volumeName,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun OutlineContent(
    volumeSlug: String,
    volumeName: String,
    onNavigate: (HadithBook, HadithChapter?, HadithSubChapter?) -> Unit,
) {
    val viewModel = viewModel(appScopedViewModelStoreOwner()) { HadithViewModel() }
    val outline by viewModel.volumeOutline.collectAsState()

    LaunchedEffect(volumeSlug) {
        viewModel.fetchVolumeOutline(volumeSlug)
    }

    // Saxlanılan **yığılmışlardır**, açıqlar yox — boş dəst «hər şey açıq» deməkdir.
    //
    // Əvvəl əksi idi: açıq dəst boş başlayır, data gələndə `LaunchedEffect` onu doldururdu. O,
    // datanın nə vaxt gəlməsindən asılı idi — ViewModel əvvəlki cildin nəticəsini saxladığı üçün
    // bəzən ilk kadrda hazır olur, bəzən sonra gəlirdi, ona görə ağac gah açıq, gah qapalı açılırdı.
    // Tərsinə saxlamaqla açıqlıq default vəziyyətdir və heç bir toxumlamaya ehtiyac qalmır.
    //
    // `remember`, `rememberSaveable` deyil: `Set<String>` default SaveableStateRegistry-nin qəbul
    // etdiyi tiplərdən deyil, saxlanma anında IllegalArgumentException verir.
    var collapsedBooks by remember(volumeSlug) { mutableStateOf(emptySet<String>()) }
    var collapsedChapters by remember(volumeSlug) { mutableStateOf(emptySet<String>()) }

    val data = outline

    // Bir dənə də açıq düyün qalmayıbsa düymə «hamısını aç»a çevrilir — yoxsa yığandan sonra geri
    // yol qalmır.
    val allCollapsed = data != null &&
        data.books.all { it.slug in collapsedBooks || data.chaptersByBook[it.slug].isNullOrEmpty() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = volumeName,
                    style = typography.titleMedium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.strLabelHadithIntroduction),
                    style = typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            if (data != null) {
                // İkon + altında qısa söz, hamısı bir kliklənən blok: tək şevron həm çox kiçik
                // hədəf idi, həm də düymə olduğu bilinmirdi.
                val chevronRotation by animateFloatAsState(if (allCollapsed) 0f else 180f)
                val actionLabel = stringResource(
                    if (allCollapsed) Res.string.strLabelOutlineExpandAll
                    else Res.string.strLabelOutlineCollapseAll
                )

                Column(
                    modifier = Modifier
                        .clip(shapes.medium)
                        .background(colorScheme.surfaceVariant.alpha(0.45f))
                        .border(
                            width = 0.8.dp,
                            color = colorScheme.outlineVariant.alpha(0.7f),
                            shape = shapes.medium,
                        )
                        .clickable(onClickLabel = actionLabel) {
                            if (allCollapsed) {
                                collapsedBooks = emptySet()
                                collapsedChapters = emptySet()
                            } else {
                                collapsedBooks = data.books.mapTo(mutableSetOf()) { it.slug }
                                collapsedChapters = data.chaptersByBook.values
                                    .flatten()
                                    .mapTo(mutableSetOf()) { it.slug }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp).rotate(chevronRotation),
                    )
                    Text(
                        text = actionLabel,
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = colorScheme.outlineVariant.alpha(0.5f))
        if (data == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) { Loader() }
            return@Column
        }

        val rows = remember(data, collapsedBooks, collapsedChapters) {
            buildList {
                data.books.forEach { book ->
                    val chapters = data.chaptersByBook[book.slug].orEmpty()
                    add(OutlineRow.BookRow(book, chapters.isNotEmpty()))

                    if (book.slug in collapsedBooks) return@forEach

                    chapters.forEach { chapter ->
                        val subs = data.subChaptersByChapter[chapter.slug].orEmpty()
                        add(OutlineRow.ChapterRow(book, chapter, subs.isNotEmpty()))

                        if (chapter.slug in collapsedChapters) return@forEach

                        subs.forEach { sub ->
                            add(OutlineRow.SubChapterRow(book, chapter, sub))
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is OutlineRow.BookRow -> OutlineItem(
                        title = row.book.name,
                        arabicTitle = row.book.name_ar,
                        level = 0,
                        expandable = row.hasChildren,
                        expanded = row.book.slug !in collapsedBooks,
                        onToggle = {
                            collapsedBooks = collapsedBooks.toggle(row.book.slug)
                        },
                        onSelect = { onNavigate(row.book, null, null) },
                    )

                    is OutlineRow.ChapterRow -> OutlineItem(
                        title = row.chapter.name,
                        arabicTitle = row.chapter.name_ar,
                        level = 1,
                        expandable = row.hasChildren,
                        expanded = row.chapter.slug !in collapsedChapters,
                        onToggle = {
                            collapsedChapters = collapsedChapters.toggle(row.chapter.slug)
                        },
                        onSelect = { onNavigate(row.book, row.chapter, null) },
                    )

                    is OutlineRow.SubChapterRow -> OutlineItem(
                        title = row.subChapter.name,
                        arabicTitle = row.subChapter.name_ar,
                        level = 2,
                        expandable = false,
                        expanded = false,
                        onToggle = {},
                        onSelect = { onNavigate(row.book, row.chapter, row.subChapter) },
                    )
                }
            }
        }
    }
}

private fun Set<String>.toggle(slug: String): Set<String> =
    if (slug in this) this - slug else this + slug

private sealed interface OutlineRow {
    val key: String

    data class BookRow(val book: HadithBook, val hasChildren: Boolean) : OutlineRow {
        override val key get() = "b:${book.slug}"
    }

    data class ChapterRow(
        val book: HadithBook,
        val chapter: HadithChapter,
        val hasChildren: Boolean,
    ) : OutlineRow {
        override val key get() = "c:${chapter.slug}"
    }

    data class SubChapterRow(
        val book: HadithBook,
        val chapter: HadithChapter,
        val subChapter: HadithSubChapter,
    ) : OutlineRow {
        override val key get() = "s:${subChapter.slug}"
    }
}

@Composable
private fun OutlineItem(
    title: String,
    arabicTitle: String?,
    level: Int,
    expandable: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
) {
    val displayName = rememberHadithDisplayName(title, arabicTitle)
    // Səviyyələr girinti ilə ayrılır; şevron yeri yarpaqlarda da saxlanılır ki, mətnlər bir xətdə
    // dayansın.
    val indent = 12.dp + (level * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(shapes.small)
            .background(
                if (level == 0) colorScheme.surfaceVariant.alpha(0.35f) else colorScheme.surfaceVariant.alpha(0.12f)
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(start = indent)
                .size(32.dp)
                .clip(shapes.small)
                .then(if (expandable) Modifier.clickable(onClick = onToggle) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (expandable) {
                val rotation by animateFloatAsState(if (expanded) 90f else 0f)
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_right),
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).rotate(rotation),
                )
            }
        }

        Text(
            text = displayName.text,
            style = when (level) {
                0 -> typography.labelLarge.withScriptDirection(displayName.isArabic)
                else -> typography.bodyMedium.withScriptDirection(displayName.isArabic)
            },
            fontWeight = if (level == 0) FontWeight.Bold else FontWeight.Normal,
            color = if (level == 0) colorScheme.onSurface else colorScheme.onSurface.alpha(0.85f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clip(shapes.small)
                .clickable(onClick = onSelect)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        )
    }
}
