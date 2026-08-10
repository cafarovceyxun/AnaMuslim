package com.cafarovceyxun.anamuslim.compose.screens.storageCleanup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.msgRecitationCleanup
import com.cafarovceyxun.anamuslim.resources.nItems
import com.cafarovceyxun.anamuslim.resources.nothingToCleanup
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.titleRecitationCleanup
import com.cafarovceyxun.anamuslim.compose.components.common.ErrorMessageCard
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelProvider
import com.cafarovceyxun.anamuslim.viewModels.RecitationDownloadEvent
import com.cafarovceyxun.anamuslim.viewModels.RecitationDownloadViewModel

private data class ReciterCleanupRow(
    val kind: RecitationAudioKind,
    val id: String,
    val name: String,
    val downloadedCount: Int,
)

@Composable
fun StorageCleanupRecitationScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val viewModel = viewModel { RecitationDownloadViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    val rows =
        remember(uiState.quranReciters, uiState.downloadStates) {
            buildList {
                uiState.quranReciters.forEach { m ->
                    val st = uiState.downloadStates[RecitationDownloadViewModel.stateKey(
                        RecitationAudioKind.QURAN,
                        m.id
                    )]
                    if ((st?.downloadedCount ?: 0) > 0) {
                        add(
                            ReciterCleanupRow(
                                kind = RecitationAudioKind.QURAN,
                                id = m.id,
                                name = m.getReciterName(),
                                downloadedCount = st!!.downloadedCount,
                            ),
                        )
                    }
                }
            }
        }

    var pendingDelete by remember { mutableStateOf<ReciterCleanupRow?>(null) }

    val innerModifier = modifier.padding(contentPadding)

    when {
        uiState.isLoading -> {
            Column(
                modifier = innerModifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.error != null -> {
            ErrorMessageCard(
                error = uiState.error!!,
                onRetry = { viewModel.onEvent(RecitationDownloadEvent.Refresh) },
                modifier = innerModifier
                    .fillMaxSize(),
            )
        }

        rows.isEmpty() -> {
            Text(
                text = stringResource(Res.string.nothingToCleanup),
                style = MaterialTheme.typography.bodyLarge,
                modifier = innerModifier
                    .fillMaxSize()
                    .padding(24.dp),
            )
        }

        else -> {
            LazyColumn(
                modifier = innerModifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(rows, key = { "${it.kind.name}:${it.id}" }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.name,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(Res.string.nItems, row.downloadedCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                        IconButton(onClick = { pendingDelete = row }) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_delete),
                                contentDescription = stringResource(Res.string.strLabelDelete),
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    val toDelete = pendingDelete
    AlertDialog(
        isOpen = toDelete != null,
        onClose = { pendingDelete = null },
        title = stringResource(Res.string.titleRecitationCleanup),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    toDelete?.let { row ->
                        RecitationModelProvider.source.deleteReciterAudioDirectory(row.id)
                        viewModel.onEvent(RecitationDownloadEvent.Refresh)
                    }
                    pendingDelete = null
                },
            ),
        ),
    ) {
        if (toDelete != null) {
            Text(
                text = stringResource(
                    Res.string.msgRecitationCleanup,
                    toDelete.name,
                ),
            )
        }
    }
}
