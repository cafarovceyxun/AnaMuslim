package com.cafarovceyxun.anamuslim.compose.screens.settings

import com.cafarovceyxun.anamuslim.compose.components.common.readableWidthInset
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberNotificationPermission
import com.cafarovceyxun.anamuslim.compose.utils.formatFileSize
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.belowWord
import com.cafarovceyxun.anamuslim.resources.deleteData
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_delete
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.inTooltip
import com.cafarovceyxun.anamuslim.resources.labelDownload
import com.cafarovceyxun.anamuslim.resources.noWbwAvailable
import com.cafarovceyxun.anamuslim.resources.selectWbwLanguage
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDelete
import com.cafarovceyxun.anamuslim.resources.strLabelDownloaded
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.textDownloading
import com.cafarovceyxun.anamuslim.resources.wbwRecitation
import com.cafarovceyxun.anamuslim.resources.wbwRecitationMsg
import com.cafarovceyxun.anamuslim.resources.wbwShowTranslation
import com.cafarovceyxun.anamuslim.resources.wbwShowTransliteration
import com.cafarovceyxun.anamuslim.resources.wbwShowTransliterationMgs
import com.cafarovceyxun.anamuslim.resources.wbwTextSize
import com.cafarovceyxun.anamuslim.resources.wordByWord
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import com.cafarovceyxun.anamuslim.compose.components.common.AlertCard
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.ErrorMessageCard
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogAction
import com.cafarovceyxun.anamuslim.compose.components.dialogs.AlertDialogActionStyle
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.WbwAudioDownloadSheet
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatNumber
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.mediaplayer.WbwAudioDownloadProvider
import com.cafarovceyxun.anamuslim.utils.reader.ReaderTextSizeUtils
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.viewModels.WbwAudioDownloadUiEvent
import com.cafarovceyxun.anamuslim.viewModels.WbwAudioDownloadViewModel
import com.cafarovceyxun.anamuslim.viewModels.WbwSettingsUiState
import com.cafarovceyxun.anamuslim.viewModels.WbwSettingsViewModel
import com.cafarovceyxun.anamuslim.viewModels.WbwUiModel
import kotlinx.coroutines.launch
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton as AppIconButton

@Composable
fun SettingsWbwScreen() {
    val viewModel = viewModel { WbwSettingsViewModel() }
    val wbwAudioDownloadViewModel = viewModel { WbwAudioDownloadViewModel() }
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var wbwAudioSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        wbwAudioDownloadViewModel.events.collect { event ->
            when (event) {
                is WbwAudioDownloadUiEvent.ShowMessage -> {
                    PlatformUtils.showLongToast(event.message)
                }
            }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            AppBar(
                title = stringResource(Res.string.wordByWord),
                actions = {
                    AppIconButton(
                        painterResource(Res.drawable.dr_icon_refresh)
                    ) {
                        viewModel.load(true)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            WbwRows(
                viewModel,
                uiState,
                onOpenWbwAudioDownloadSheet = { wbwAudioSheetOpen = true },
            )
        }

        WbwAudioDownloadSheet(
            isOpen = wbwAudioSheetOpen,
            onDismiss = { wbwAudioSheetOpen = false },
            viewModel = wbwAudioDownloadViewModel,
        )
    }
}

@Composable
private fun WbwRows(
    viewModel: WbwSettingsViewModel,
    uiState: WbwSettingsUiState,
    onOpenWbwAudioDownloadSheet: () -> Unit,
) {
    var deleteDialogData by remember { mutableStateOf<WbwUiModel?>(null) }
    val downloadStates = uiState.downloadStates
    val rows = uiState.rows

    val isAnyDownloading = downloadStates.values.any {
        it is ResourceDownloadStatus.Started || it is ResourceDownloadStatus.InProgress
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = readableWidthInset(),
            end = readableWidthInset(),
            bottom = mainBottomNavigationOuterHeight() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Configurations(onOpenWbwAudioDownloadSheet = onOpenWbwAudioDownloadSheet)
        }

        when {
            uiState.isLoading -> {
                item(key = "loading") {
                    Loader(fill = true)
                }
            }

            uiState.error != null -> {
                // Bound locally: `uiState` now comes from the shared module, where a public
                // property cannot be smart-cast across the module boundary.
                val loadError = uiState.error!!
                item(key = "error") {
                    ErrorMessageCard(
                        error = loadError,
                        onRetry = {
                            viewModel.load(true)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }

            else -> {
                items(rows, key = { it.info.id }) { row ->
                    val status = downloadStates[row.info.id] ?: ResourceDownloadStatus.Idle

                    WbwRow(
                        row,
                        status,
                        viewModel,
                        uiState,
                        isAnyDownloading,
                        onDeleteRequest = {
                            deleteDialogData = row
                        }
                    )
                }
            }
        }

    }

    AlertDialog(
        isOpen = deleteDialogData != null,
        onClose = { deleteDialogData = null },
        title = stringResource(Res.string.deleteData),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                dismissOnClick = false,
                onClick = {
                    if (deleteDialogData != null) {
                        viewModel.deleteWbwData(deleteDialogData!!.info.id)
                    }

                    deleteDialogData = null
                }
            )
        )
    ) {
        Text(deleteDialogData?.info?.langName ?: "")
    }
}

@Composable
private fun WbwRow(
    row: WbwUiModel,
    downloadStatus: ResourceDownloadStatus,
    viewModel: WbwSettingsViewModel,
    uiState: WbwSettingsUiState,
    isAnyDownloading: Boolean,
    onDeleteRequest: () -> Unit
) {
    val appLocale = LocalAppLocale.current
    val isDownloaded = row.isDownloaded
    val isSelected = row.info.id == uiState.selectedWbwId

    val canSelect = isDownloaded
    val isDownloading = downloadStatus is ResourceDownloadStatus.Started ||
            downloadStatus is ResourceDownloadStatus.InProgress

    val onSelect = {
        viewModel.selectLanguage(row.info.id)
    }

    val onDownloadOrUpdate = {
        viewModel.startDownload(row.info.id)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (canSelect) onSelect()
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = {
                if (canSelect) onSelect()
            },
            enabled = canSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = row.info.langName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val subtitle = when {
                downloadStatus is ResourceDownloadStatus.InProgress ->
                    "${stringResource(Res.string.textDownloading)} " +
                        "${appLocale.numeralSystem.formatNumber(downloadStatus.progress)}%"

                downloadStatus is ResourceDownloadStatus.Started -> stringResource(Res.string.textDownloading)
                row.isUpdateAvailable -> stringResource(Res.string.strLabelUpdate)
                isDownloaded -> stringResource(Res.string.strLabelDownloaded)
                else -> null
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (downloadStatus is ResourceDownloadStatus.Failed)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when (downloadStatus) {
            is ResourceDownloadStatus.InProgress -> {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { downloadStatus.progress / 100f },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    IconButton(
                        onClick = {
                            viewModel.cancelDownload(row.info.id)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_close),
                            contentDescription = stringResource(Res.string.strLabelCancel),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            is ResourceDownloadStatus.Started -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }

            is ResourceDownloadStatus.Failed -> {
                AppIconButton(
                    painterResource(Res.drawable.dr_icon_refresh),
                    onClick = onDownloadOrUpdate
                )
            }

            else -> {
                if (row.isUpdateAvailable || !isDownloaded) {
                    val icon = if (row.isUpdateAvailable) Res.drawable.dr_icon_refresh
                    else Res.drawable.dr_icon_download

                    AppIconButton(
                        painter = painterResource(icon),
                        enabled = !isAnyDownloading || isDownloading,
                        onClick = onDownloadOrUpdate,
                    )
                } else {
                    AppIconButton(
                        painter = painterResource(Res.drawable.dr_icon_delete),
                        enabled = !isAnyDownloading,
                        tint = colorScheme.error,
                        onClick = onDeleteRequest
                    )
                }
            }
        }
    }
}

@Composable
private fun Configurations(onOpenWbwAudioDownloadSheet: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current

    val prefs by DataStoreManager.flowMultiple(
        ReaderPreferences.KEY_WBW_SHOW_TRANSLATION,
        ReaderPreferences.KEY_WBW_SHOW_TRANSLITERATION,
        ReaderPreferences.KEY_WBW_TOOLTIP_SHOW_TRANSLATION,
        ReaderPreferences.KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION,
        ReaderPreferences.KEY_WBW_RECITATION,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_WBW,
    ).collectAsStateWithLifecycle(null)

    val preferences = prefs ?: return

    val showTranslation = preferences.get(ReaderPreferences.KEY_WBW_SHOW_TRANSLATION)
    val showTransliteration = preferences.get(ReaderPreferences.KEY_WBW_SHOW_TRANSLITERATION)
    val showTooltipTr = preferences.get(ReaderPreferences.KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
    val showTooltipTrlt = preferences.get(ReaderPreferences.KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
    val recitation = preferences.get(ReaderPreferences.KEY_WBW_RECITATION)
    val wbwTextMult = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_WBW)

    val min = 100
    val max = 160
    val steps = max - min
    val wbwProgress = wbwTextMult * 100

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 16.dp)
    ) {
        SwitchItem(
            title = Res.string.wbwRecitation,
            subtitle = Res.string.wbwRecitationMsg,
            checked = recitation,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwRecitationEnabled(checked)
                }
            },
        )

        // Hidden where the platform has no download subsystem (iOS): the provider falls back to an
        // inert source, so the button would open a full sheet whose every control silently does
        // nothing. Playback is unaffected — iOS streams the per-word clips instead.
        if (WbwAudioDownloadProvider.isAvailable) {
            TextButton(
                onClick = onOpenWbwAudioDownloadSheet,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(36.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = colorScheme.surfaceContainer,
                    contentColor = colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(stringResource(Res.string.labelDownload))
                Icon(
                    painterResource(Res.drawable.dr_icon_download),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp)
                )
            }
        }

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(Res.string.inTooltip))

        SwitchItem(
            title = Res.string.wbwShowTranslation,
            checked = showTooltipTr,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwTooltipShowTranslation(checked)
                }
            },
        )

        SwitchItem(
            title = Res.string.wbwShowTransliteration,
            subtitle = Res.string.wbwShowTransliterationMgs,
            checked = showTooltipTrlt,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwTooltipShowTransliteration(checked)
                }
            },
        )

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(Res.string.belowWord))

        SwitchItem(
            title = Res.string.wbwShowTranslation,
            checked = showTranslation,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwShowTranslation(checked)
                }
            },
        )

        SwitchItem(
            title = Res.string.wbwShowTransliteration,
            subtitle = Res.string.wbwShowTransliterationMgs,
            checked = showTransliteration,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwShowTransliteration(checked)
                }
            },
        )

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(Res.string.wbwTextSize))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                modifier = Modifier.weight(1f),
                value = wbwProgress,
                onValueChange = { v ->
                    coroutineScope.launch {
                        ReaderPreferences.setWbwTextSizeMultiplier(
                            ReaderTextSizeUtils.calculateMultiplier(v.toInt(), min, max),
                        )
                    }
                },
                valueRange = min.toFloat()..max.toFloat(),
                steps = steps,
            )
            Text(
                text = "${appLocale.numeralSystem.formatNumber(wbwProgress.toInt())}%",
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(Res.string.selectWbwLanguage))

        Spacer(Modifier.height(8.dp))

        AlertCard(
            Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                stringResource(Res.string.noWbwAvailable),
                style = typography.bodyMedium
            )
        }
    }
}
