package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCard
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_bookmark
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_check
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import com.cafarovceyxun.anamuslim.resources.dr_icon_arrow_left
import com.cafarovceyxun.anamuslim.resources.ic_bookmark_added
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkRemoved
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkRemoveFailed
import com.cafarovceyxun.anamuslim.resources.strLabelSelectedCount
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarks
import com.cafarovceyxun.anamuslim.resources.strLabelRemove
import com.cafarovceyxun.anamuslim.resources.strLabelRemoveAll
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkNoItems
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarkDeleteAll
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarkDeleteCount
import com.cafarovceyxun.anamuslim.resources.strTitleBookmarkDeleteThis
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkDeleteAll
import com.cafarovceyxun.anamuslim.resources.strMsgBookmarkDeleteSelected
import com.cafarovceyxun.anamuslim.resources.strLabelVerseNo
import com.cafarovceyxun.anamuslim.resources.strLabelVerses
import com.cafarovceyxun.anamuslim.resources.strLabelBookmark
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.BookmarkViewerData
import com.cafarovceyxun.anamuslim.compose.components.reader.dialogs.BookmarkViewerSheet
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.compose.utils.formatBookmarkDate
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import androidx.compose.foundation.clickable
import com.cafarovceyxun.anamuslim.db.entities.user.HadithBookmarkEntity
import com.cafarovceyxun.anamuslim.resources.strMsgNoSavedHadiths
import com.cafarovceyxun.anamuslim.resources.strTitleSavedHadiths
import com.cafarovceyxun.anamuslim.resources.strTitleSavedVerses
import com.cafarovceyxun.anamuslim.viewModels.BookmarksViewModel
import kotlinx.coroutines.launch

private sealed interface BookmarkDeleteTarget {
    data object All : BookmarkDeleteTarget
    data class Single(val id: Long) : BookmarkDeleteTarget
    data class Selected(val ids: Set<Long>) : BookmarkDeleteTarget
}

@Composable
fun BookmarksScreen(
    vm: BookmarksViewModel = viewModel { BookmarksViewModel() },
    onOpenInReader: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit = { _, _, _ -> },
    onOpenHadith: (title: String, chapterSlug: String?, subChapterSlug: String?) -> Unit =
        { _, _, _ -> },
) {
    val scope = rememberCoroutineScope()
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    // 0 — ayələr, 1 — hədislər. Seçim rejimi tablar arasında keçiddə sıfırlanır ki, silinmə
    // yanlış siyahıya düşməsin.
    var selectedTab by remember { mutableStateOf(0) }
    val isHadithTab = selectedTab == 1

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var viewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    var deleteTarget by remember { mutableStateOf<BookmarkDeleteTarget?>(null) }

    LaunchedEffect(selectedTab) { selectedIds = emptySet() }

    LaunchedEffect(uiState.bookmarks, uiState.hadithBookmarks, selectedTab) {
        val existingIds = if (isHadithTab) {
            uiState.hadithBookmarks.map { it.hadithId }.toSet()
        } else {
            uiState.bookmarks.map { it.id }.toSet()
        }
        selectedIds = selectedIds.intersect(existingIds)
    }

    val selecting = selectedIds.isNotEmpty()
    val visibleCount = if (isHadithTab) uiState.hadithBookmarks.size else uiState.bookmarks.size

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    BackHandler(enabled = selecting) {
        selectedIds = emptySet()
    }

    BookmarkDeleteDialog(
        target = deleteTarget,
        onDismiss = { deleteTarget = null },
        onConfirm = { target ->
            scope.launch {
                // `All` gives no Toast (matching prior behavior); Single/Selected report the outcome.
                val removed: Boolean? = when (target) {
                    BookmarkDeleteTarget.All -> {
                        if (isHadithTab) vm.removeAllHadithBookmarks() else vm.removeAllBookmarks()
                        null
                    }

                    is BookmarkDeleteTarget.Single ->
                        if (isHadithTab) vm.removeHadithBookmark(target.id)
                        else vm.removeBookmark(target.id)

                    is BookmarkDeleteTarget.Selected ->
                        (if (isHadithTab) vm.removeHadithBookmarks(target.ids)
                        else vm.removeBookmarks(target.ids)).also { selectedIds = emptySet() }
                }
                if (removed != null) {
                    val msg = if (removed) org.jetbrains.compose.resources.getString(Res.string.strMsgBookmarkRemoved)
                    else org.jetbrains.compose.resources.getString(Res.string.strMsgBookmarkRemoveFailed)
                    PlatformUtils.showToast(msg)
                }
            }
        })

    BookmarkViewerSheet(
        data = viewerData,
        onClose = { viewerData = null },
        onOpenInReader = onOpenInReader
    )

    Scaffold(
        topBar = {
            AppBar(
                title = if (selecting) {
                    stringResource(Res.string.strLabelSelectedCount, selectedIds.size)
                } else {
                    stringResource(Res.string.strTitleBookmarks)
                }, actions = {
                    if (visibleCount > 0) {
                        val tooltip = if (selecting) {
                            stringResource(Res.string.strLabelRemove)
                        } else {
                            stringResource(Res.string.strLabelRemoveAll)
                        }

                        SimpleTooltip(text = tooltip) {
                            IconButton(
                                onClick = {
                                    deleteTarget = if (selecting) {
                                        BookmarkDeleteTarget.Selected(selectedIds)
                                    } else {
                                        BookmarkDeleteTarget.All
                                    }
                                }) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_delete),
                                    contentDescription = tooltip,
                                )
                            }
                        }
                    }
                })
        }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            BookmarkTabs(
                selectedIndex = selectedTab,
                verseCount = uiState.bookmarks.size,
                hadithCount = uiState.hadithBookmarks.size,
                onSelect = { selectedTab = it },
            )

            when {
                uiState.isLoading -> Loader(fill = true)

                visibleCount == 0 -> MessageCard(
                    icon = Res.drawable.ic_bookmark,
                    message = stringResource(
                        if (isHadithTab) Res.string.strMsgNoSavedHadiths
                        else Res.string.strMsgBookmarkNoItems
                    ),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 64.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isHadithTab) {
                        items(uiState.hadithBookmarks, key = { it.hadithId }) { bookmark ->
                            HadithBookmarkItemCard(
                                bookmark = bookmark,
                                selected = selectedIds.contains(bookmark.hadithId),
                                selecting = selecting,
                                onClick = {
                                    if (selecting) {
                                        selectedIds = selectedIds.toggle(bookmark.hadithId)
                                    } else {
                                        onOpenHadith(
                                            bookmark.title,
                                            bookmark.chapterSlug,
                                            bookmark.subChapterSlug,
                                        )
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds.toggle(bookmark.hadithId)
                                },
                            )
                        }
                    } else {
                        items(uiState.bookmarks, key = { it.id }) { bookmark ->
                            BookmarkItemCard(
                                bookmark = bookmark,
                                chapterName = uiState.chapterNames[bookmark.chapterNo].orEmpty(),
                                selected = selectedIds.contains(bookmark.id),
                                selecting = selecting,
                                onClick = {
                                    if (selecting) {
                                        selectedIds = selectedIds.toggle(bookmark.id)
                                    } else {
                                        viewerData = bookmark.toViewerData()
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds.toggle(bookmark.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkTabs(
    selectedIndex: Int,
    verseCount: Int,
    hadithCount: Int,
    onSelect: (Int) -> Unit,
) {
    val tabs = listOf(
        stringResource(Res.string.strTitleSavedVerses) to verseCount,
        stringResource(Res.string.strTitleSavedHadiths) to hadithCount,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, (label, count) ->
            val isSelected = index == selectedIndex

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (count > 0) "$label ($count)" else label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HadithBookmarkItemCard(
    bookmark: HadithBookmarkEntity,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colorScheme.primary else colorScheme.outline.alpha(0.3f)
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selecting) colorScheme.primary else colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selecting) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_check),
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = bookmark.hadithNo.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Light
                            ),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatBookmarkDate(bookmark.dateTime),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Light
                        ),
                    )
                }
            }

            bookmark.preview?.takeIf { it.isNotBlank() }?.let { preview ->
                HorizontalDivider()
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface.alpha(0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp),
                )
            }

            bookmark.note?.takeIf { it.isNotBlank() }?.let { note ->
                HorizontalDivider()
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun BookmarkDeleteDialog(
    target: BookmarkDeleteTarget?,
    onDismiss: () -> Unit,
    onConfirm: (BookmarkDeleteTarget) -> Unit,
) {
    AlertDialog(
        isOpen = target != null, onClose = onDismiss, title = when (target) {
            BookmarkDeleteTarget.All -> stringResource(Res.string.strTitleBookmarkDeleteAll)
            is BookmarkDeleteTarget.Selected -> stringResource(
                Res.string.strTitleBookmarkDeleteCount,
                target.ids.size,
            )

            is BookmarkDeleteTarget.Single -> stringResource(Res.string.strTitleBookmarkDeleteThis)
            null -> ""
        }, actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = onDismiss,
            ), AlertDialogAction(
                text = when (target) {
                    BookmarkDeleteTarget.All -> stringResource(Res.string.strLabelRemoveAll)
                    is BookmarkDeleteTarget.Selected, is BookmarkDeleteTarget.Single -> stringResource(
                        Res.string.strLabelRemove
                    )

                    null -> ""
                },
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    target?.let { onConfirm(it) }
                },
            )
        )
    ) {
        val message = when (target) {
            BookmarkDeleteTarget.All -> stringResource(Res.string.strMsgBookmarkDeleteAll)
            is BookmarkDeleteTarget.Selected -> stringResource(Res.string.strMsgBookmarkDeleteSelected)
            else -> null
        }

        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookmarkItemCard(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    chapterName: String,
    selected: Boolean,
    selecting: Boolean,
    onLongClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colorScheme.primary else colorScheme.outline.alpha(0.3f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selecting) colorScheme.primary
                            else colorScheme.background
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selecting) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_check),
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            bookmark.chapterNo.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Light
                            ),
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (bookmark.fromVerseNo == bookmark.toVerseNo) chapterName + ": " + stringResource(
                            Res.string.strLabelVerseNo, bookmark.fromVerseNo
                        )
                        else chapterName + ": " + stringResource(
                            Res.string.strLabelVerses, bookmark.fromVerseNo, bookmark.toVerseNo
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Text(
                        formatBookmarkDate(bookmark.dateTime),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Light
                        ),
                    )
                }
            }

            if (!bookmark.note.isNullOrBlank()) {
                HorizontalDivider()

                Text(
                    text = buildAnnotatedString {
                        appendInlineContent("user_note", "[icon]")
                        append(" ")
                        append(bookmark.note)
                    },
                    inlineContent = mapOf(
                        "user_note" to InlineTextContent(
                            Placeholder(
                                width = 16.sp,
                                height = 16.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                            )
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_edit),
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}



private fun BookmarkEntity.toViewerData(startInEditMode: Boolean = false): BookmarkViewerData {
    return BookmarkViewerData(
        chapterNo = chapterNo,
        fromVerse = fromVerseNo,
        toVerse = toVerseNo,
        startInEditMode = startInEditMode,
    )
}

private fun Set<Long>.toggle(id: Long): Set<Long> {
    return toMutableSet().apply {
        if (!add(id)) {
            remove(id)
        }
    }
}
