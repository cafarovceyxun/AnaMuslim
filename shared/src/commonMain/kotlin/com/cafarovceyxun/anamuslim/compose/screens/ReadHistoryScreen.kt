package com.cafarovceyxun.anamuslim.compose.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.cafarovceyxun.anamuslim.compose.utils.formatDateTime
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_history
import com.cafarovceyxun.anamuslim.resources.ic_mode_mushaf
import com.cafarovceyxun.anamuslim.resources.ic_mode_translation
import com.cafarovceyxun.anamuslim.resources.ic_mode_verse
import com.cafarovceyxun.anamuslim.resources.labelHizbNo
import com.cafarovceyxun.anamuslim.resources.msgClearReadHistory
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelContinueReading
import com.cafarovceyxun.anamuslim.resources.strLabelJuzNo
import com.cafarovceyxun.anamuslim.resources.strLabelPageNo
import com.cafarovceyxun.anamuslim.resources.strLabelRemove
import com.cafarovceyxun.anamuslim.resources.strLabelRemoveAll
import com.cafarovceyxun.anamuslim.resources.strLabelSurah
import com.cafarovceyxun.anamuslim.resources.strLabelVerseNo
import com.cafarovceyxun.anamuslim.resources.strLabelVerses
import com.cafarovceyxun.anamuslim.resources.strMsgReadHistoryDeleteAll
import com.cafarovceyxun.anamuslim.resources.strMsgReadHistoryNoItems
import com.cafarovceyxun.anamuslim.resources.strTitleReadHistory
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.MessageCard
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.utils.reader.ReadType
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import com.cafarovceyxun.anamuslim.viewModels.ReadHistoryViewModel
import kotlinx.coroutines.launch
import com.cafarovceyxun.anamuslim.compose.theme.LocalAppTextScale

private sealed interface HistoryDeleteTarget {
    data object All : HistoryDeleteTarget
    data class Single(val id: Long) : HistoryDeleteTarget
}

@Composable
fun ReadHistoryScreen(vm: ReadHistoryViewModel = viewModel { ReadHistoryViewModel() }) {
    val scope = rememberCoroutineScope()

    val chapterNames by vm.chapterNames.collectAsStateWithLifecycle()
    val allHistories = vm.allHistories.collectAsLazyPagingItems()

    var deleteTarget by remember { mutableStateOf<HistoryDeleteTarget?>(null) }

    HistoryDeleteDialog(
        target = deleteTarget,
        onDismiss = { deleteTarget = null },
    ) { target ->
        scope.launch {
            when (target) {
                HistoryDeleteTarget.All -> vm.deleteAllHistories()
                is HistoryDeleteTarget.Single -> vm.deleteHistory(target.id)
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(Res.string.strTitleReadHistory),
                actions = {
                    if (allHistories.itemCount > 0) {
                        SimpleTooltip(text = stringResource(Res.string.msgClearReadHistory)) {
                            IconButton(onClick = { deleteTarget = HistoryDeleteTarget.All }) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_delete),
                                    contentDescription = stringResource(Res.string.msgClearReadHistory),
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            allHistories.itemCount == 0 -> {
                MessageCard(
                    icon = Res.drawable.dr_icon_history,
                    message = stringResource(Res.string.strMsgReadHistoryNoItems),
                    modifier = Modifier.padding(paddingValues),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 64.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        allHistories.itemCount,
                        key = { index ->
                            val item = allHistories[index]
                            if (item != null) {
                                return@items item.id.toString()
                            } else {
                                index
                            }
                        },
                    ) { index ->
                        val history = allHistories[index]

                        if (history != null) {
                            ReadHistoryCard(
                                history = history,
                                chapterName = chapterNames.get(history.chapterNo).orEmpty(),
                                onOpen = {
                                    ReaderUiHooks.openReaderFromHistory?.invoke(history)
                                },
                                onDelete = {
                                    deleteTarget = HistoryDeleteTarget.Single(history.id)
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
private fun ReadHistoryCard(
    history: ReadHistoryEntity,
    chapterName: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val readType = ReadType.fromValue(history.readType)
    val title = history.titleLabel(chapterName)
    val subtitle = history.subtitleLabel(chapterName)

    val accentColor = when (readType) {
        ReadType.Chapter -> colorScheme.primary
        ReadType.Juz -> colorScheme.tertiary
        ReadType.Hizb -> colorScheme.secondary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .border(1.dp, colorScheme.outlineVariant.alpha(0.5f), shapes.medium)
            .clickable(onClick = onOpen),
        color = colorScheme.surfaceContainer.alpha(0.75f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(
                                when (ReaderMode.fromValue(history.readerMode)) {
                                    ReaderMode.Reading -> Res.drawable.ic_mode_mushaf
                                    ReaderMode.Translation -> Res.drawable.ic_mode_translation
                                    else -> Res.drawable.ic_mode_verse
                                }
                            ),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colorScheme.onSurface,
                    )

                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface.alpha(0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    Text(
                        text = stringResource(Res.string.strLabelContinueReading),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurface.alpha(0.65f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = formatDateTime(history.datetime, "d MMM, HH:mm"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp * LocalAppTextScale.current),
                        color = colorScheme.onSurface.alpha(0.5f),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                SimpleTooltip(text = stringResource(Res.string.strLabelRemove)) {
                    IconButton(
                        onClick = onDelete,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_delete),
                            contentDescription = stringResource(Res.string.strLabelRemove),
                            tint = colorScheme.onSurface.alpha(0.5f),
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun HistoryDeleteDialog(
    target: HistoryDeleteTarget?,
    onDismiss: () -> Unit,
    onConfirm: (HistoryDeleteTarget) -> Unit,
) {
    AlertDialog(
        isOpen = target != null,
        onClose = onDismiss,
        title = when (target) {
            HistoryDeleteTarget.All -> stringResource(Res.string.msgClearReadHistory)
            is HistoryDeleteTarget.Single -> stringResource(Res.string.strLabelRemove)
            null -> ""
        },
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = onDismiss,
            ),
            AlertDialogAction(
                text = when (target) {
                    HistoryDeleteTarget.All -> stringResource(Res.string.strLabelRemoveAll)
                    is HistoryDeleteTarget.Single -> stringResource(Res.string.strLabelRemove)

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
            HistoryDeleteTarget.All -> stringResource(Res.string.strMsgReadHistoryDeleteAll)
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
fun ReadHistoryEntity.titleLabel(chapterName: String): String {
    if (readerMode == ReaderMode.Reading.value || readerMode == ReaderMode.Translation.value) {
        return pageNo?.let { stringResource(Res.string.strLabelPageNo, it) } ?: "-"
    } else {
        return when (ReadType.fromValue(readType)) {
            ReadType.Chapter -> stringResource(
                Res.string.strLabelSurah,
                chapterName
            )

            ReadType.Juz -> stringResource(Res.string.strLabelJuzNo, divisionNo)
            ReadType.Hizb -> stringResource(Res.string.labelHizbNo, divisionNo)
        }
    }
}

@Composable
fun ReadHistoryEntity.subtitleLabel(chapterName: String): String? {
    val verseLabel = if (fromVerseNo == toVerseNo) {
        stringResource(Res.string.strLabelVerseNo, fromVerseNo)
    } else {
        stringResource(Res.string.strLabelVerses, fromVerseNo, toVerseNo)

    }
    if (readerMode == ReaderMode.Reading.value || readerMode == ReaderMode.Translation.value) {
        return mushafCode?.getQuranScriptName() ?: "-"
    } else {
        return when (ReadType.fromValue(readType)) {
            ReadType.Chapter -> verseLabel
            ReadType.Juz,
            ReadType.Hizb -> chapterName.takeIf { it.isNotBlank() }?.let {
                it + ": " + verseLabel
            }
        }
    }
}
