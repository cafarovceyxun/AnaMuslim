package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.*
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.extensions.orMinusOne
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BookmarkViewerData(
    val chapterNo: Int,
    val fromVerse: Int,
    val toVerse: Int,
    val showOpenInReaderButton: Boolean = true,
    val startInEditMode: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkViewerSheet(
    data: BookmarkViewerData?,
    onOpenInReader: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit = { _, _, _ -> },
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (data == null) {
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val repo = RepositoryProvider.userRepository

    val bookmark = repo.getBookmarkFlow(data.chapterNo, data.fromVerse, data.toVerse)
        .collectAsStateWithLifecycle(null).value

    var editing by remember(data) { mutableStateOf(data.startInEditMode) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    if (bookmark == null) {
        return
    }

    val chapterNo = bookmark.chapterNo.orMinusOne()
    val fromVerseNo = bookmark.fromVerseNo.orMinusOne()
    val toVerseNo = bookmark.toVerseNo.orMinusOne()
    var note by remember(bookmark.id) { mutableStateOf(bookmark.note.orEmpty()) }

    val repository = RepositoryProvider.quranRepository
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            repository.getChapterName(chapterNo)
        }
    }

    LaunchedEffect(editing, bookmark) {
        if (editing) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else if (!editing) {
            keyboard?.hide()
        }
    }

    fun saveBookmark() {
        coroutineScope.launch {
            repo.updateBookmark(
                chapterNo,
                fromVerseNo,
                toVerseNo,
                note.trim().takeIf { it.isNotEmpty() },
            )
        }
    }

    fun dismissAfterPersist() {
        saveBookmark()
        onClose()
    }

    ModalBottomSheet(
        onDismissRequest = { dismissAfterPersist() },
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        AlertDialog(
            isOpen = showDeleteConfirm,
            onClose = { showDeleteConfirm = false },
            title = stringResource(Res.string.strTitleBookmarkDeleteThis),
            actions = listOf(
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelCancel),
                    onClick = { showDeleteConfirm = false },
                ),
                AlertDialogAction(
                    text = stringResource(Res.string.strLabelRemove),
                    style = AlertDialogActionStyle.Danger,
                    onClick = {
                        coroutineScope.launch {
                            val removed = repo.removeFromBookmark(chapterNo, fromVerseNo, toVerseNo)
                            PlatformUtils.showToast(
                                if (removed) org.jetbrains.compose.resources.getString(Res.string.strMsgBookmarkRemoved)
                                else org.jetbrains.compose.resources.getString(Res.string.strMsgBookmarkRemoveFailed)
                            )
                            onClose()
                        }
                    },
                ),
            ),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (editing) {
                    Text(
                        text = stringResource(Res.string.strTitleAddNote),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.strTitleBookmark),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (!editing) {
                    // Test
                    IconButton(onClick = { editing = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_edit),
                            contentDescription = stringResource(Res.string.strLabelEdit),
                            tint = colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_delete),
                            contentDescription = stringResource(Res.string.strLabelRemove),
                            tint = colorScheme.error
                        )
                    }
                } else {
                    TextButton(
                        onClick = {
                            saveBookmark()
                            editing = false
                        },
                    ) {
                        Text(stringResource(Res.string.strLabelDone))
                    }
                }
            }

            HorizontalDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.strLabelSurah, chapterName),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = if (fromVerseNo == toVerseNo) stringResource(
                        Res.string.strLabelVerseNoWithColon,
                        fromVerseNo
                    ) else stringResource(Res.string.strLabelVersesWithColon, fromVerseNo, toVerseNo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.strTitleNote),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )

                if (editing) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp)
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(stringResource(Res.string.strHintBookmarkViewerNote))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                saveBookmark()
                                editing = false
                            },
                        ),
                    )
                } else {
                    val hasNote = note.isNotBlank()

                    Text(
                        text = if (!hasNote) stringResource(Res.string.noNoteProvided) else note,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hasNote) colorScheme.onSurface
                        else colorScheme.onSurface.alpha(0.6f),
                        fontStyle = if (hasNote) FontStyle.Normal else FontStyle.Italic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                    )
                }
            }

            if (data.showOpenInReaderButton && !editing) {
                Button(
                    onClick = {
                        onOpenInReader(chapterNo, fromVerseNo, toVerseNo)
                        onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(stringResource(Res.string.strLabelOpenInReader))
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
