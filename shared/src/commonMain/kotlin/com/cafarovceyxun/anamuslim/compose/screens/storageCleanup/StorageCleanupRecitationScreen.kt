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
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationModelBase
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.msgRecitationCleanup
import com.cafarovceyxun.anamuslim.resources.nItems
import com.cafarovceyxun.anamuslim.resources.nothingToCleanup
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.titleRecitationCleanup
import com.cafarovceyxun.anamuslim.resources.titleTranslationVoices
import com.cafarovceyxun.anamuslim.compose.components.common.ErrorMessageCard
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelProvider
import com.cafarovceyxun.anamuslim.viewModels.RecitationBatchDownloadState
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

    val quranRows =
        remember(uiState.quranReciters, uiState.downloadStates) {
            uiState.quranReciters.toCleanupRows(
                kind = RecitationAudioKind.QURAN,
                downloadStates = uiState.downloadStates,
            )
        }

    // Tərcümə səsi (süni səs) də adi qari kimi surə-surə diskə düşür, sadəcə başqa siyahıdadır.
    // Ana ekrandakı «boşaldıla bilər» sayğacı bütün qari qovluqlarını sayır
    // (`getDownloadedAudioStats`), ona görə o səs yerdə görünürdü, amma burada silinə bilmirdi —
    // yalnız `quranReciters` gəzilirdi.
    val translationRows =
        remember(uiState.translationReciters, uiState.downloadStates) {
            uiState.translationReciters.toCleanupRows(
                kind = RecitationAudioKind.TRANSLATION,
                downloadStates = uiState.downloadStates,
            )
        }

    val rows = quranRows + translationRows

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
                items(quranRows, key = { "${it.kind.name}:${it.id}" }) { row ->
                    ReciterCleanupItem(row = row, onDelete = { pendingDelete = row })
                }

                if (translationRows.isNotEmpty()) {
                    item(key = "header_translation") {
                        Text(
                            text = stringResource(Res.string.titleTranslationVoices),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                        )
                    }

                    items(translationRows, key = { "${it.kind.name}:${it.id}" }) { row ->
                        ReciterCleanupItem(row = row, onDelete = { pendingDelete = row })
                    }
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

private fun List<RecitationModelBase>.toCleanupRows(
    kind: RecitationAudioKind,
    downloadStates: Map<String, RecitationBatchDownloadState>,
): List<ReciterCleanupRow> = mapNotNull { model ->
    val count = downloadStates[RecitationDownloadViewModel.stateKey(kind, model.id)]
        ?.downloadedCount
        ?: 0

    if (count <= 0) return@mapNotNull null

    ReciterCleanupRow(
        kind = kind,
        id = model.id,
        name = model.getReciterName(),
        downloadedCount = count,
    )
}

@Composable
private fun ReciterCleanupItem(row: ReciterCleanupRow, onDelete: () -> Unit) {
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
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_delete),
                contentDescription = stringResource(Res.string.strLabelDelete),
            )
        }
    }
    HorizontalDivider()
}
