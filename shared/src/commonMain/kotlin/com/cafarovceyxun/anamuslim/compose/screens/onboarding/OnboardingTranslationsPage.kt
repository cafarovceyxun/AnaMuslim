package com.cafarovceyxun.anamuslim.compose.screens.onboarding

import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.labelDownload
import com.cafarovceyxun.anamuslim.resources.strLabelAllChapters
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDownloaded
import com.cafarovceyxun.anamuslim.resources.strLabelNotSelected
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.strLabelSelectTranslation
import com.cafarovceyxun.anamuslim.resources.strMsgSomethingWrong
import com.cafarovceyxun.anamuslim.resources.strTitleHadith
import com.cafarovceyxun.anamuslim.resources.strTitleMushafScript
import com.cafarovceyxun.anamuslim.resources.onboardTitleReciter
import com.cafarovceyxun.anamuslim.resources.textDownloading
import com.cafarovceyxun.anamuslim.resources.wbwAudio
import com.cafarovceyxun.anamuslim.resources.wbwSelectLanguage
import com.cafarovceyxun.anamuslim.resources.recitationDownloadChaptersProgress

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.components.transls.TranslModel
import com.cafarovceyxun.anamuslim.compose.components.common.ErrorMessageCard
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.SearchTextField
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetBare
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.WbwAudioDownloadSheet
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.resources.titleTajweedColors
import com.cafarovceyxun.anamuslim.resources.msgTajweedColors
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import com.cafarovceyxun.anamuslim.viewModels.ReciterSelectorViewModel
import com.cafarovceyxun.anamuslim.viewModels.TranslationEvent
import com.cafarovceyxun.anamuslim.viewModels.TranslationUiEvent
import com.cafarovceyxun.anamuslim.viewModels.TranslationViewModel
import com.cafarovceyxun.anamuslim.viewModels.WbwAudioDownloadViewModel
import com.cafarovceyxun.anamuslim.viewModels.WbwSettingsViewModel
import com.cafarovceyxun.anamuslim.compose.components.dialogs.MessageDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.TranslationConfirm
import com.cafarovceyxun.anamuslim.compose.components.dialogs.TranslationConfirmDialog
import com.cafarovceyxun.anamuslim.compose.extensions.verticalFadingEdge

@Composable
fun OnboardingTranslationsPage(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val viewModel = viewModel { TranslationViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    var message by remember { mutableStateOf<TranslationUiEvent.ShowMessage?>(null) }
    var confirm by remember { mutableStateOf<TranslationConfirm?>(null) }

    // The three resources added next to translations and hadith. Word-by-word downloads inline;
    // reciter and script are stored as preferences and fetched later — see OnboardingResourceSections.
    val wbwViewModel = viewModel { WbwSettingsViewModel() }
    val wbwState by wbwViewModel.uiState.collectAsState()

    // Same view model instance the picker sheet resolves, so the summary row shows a name rather
    // than an id as soon as the list arrives.
    val reciterViewModel = viewModel { ReciterSelectorViewModel() }
    val reciters by reciterViewModel.quranReciters.collectAsState()
    val selectedReciterId = RecitationPreferences.observeReciterId()
    val selectedScript = ReaderPreferences.observeQuranScript()

    val wbwAudioViewModel = viewModel { WbwAudioDownloadViewModel() }
    val wbwAudioState by wbwAudioViewModel.uiState.collectAsState()

    var showWbwSheet by remember { mutableStateOf(false) }
    var showWbwAudioSheet by remember { mutableStateOf(false) }
    var showReciterSheet by remember { mutableStateOf(false) }
    var showScriptSheet by remember { mutableStateOf(false) }

    // var showDeleteHadithsConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(TranslationEvent.Refresh)

        viewModel.events.collect { event ->
            when (event) {
                is TranslationUiEvent.ShowMessage -> message = event
            }
        }
    }

    when {
        uiState.isLoading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
        }

        uiState.error != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.strMsgSomethingWrong),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.onEvent(TranslationEvent.Refresh) }) {
                    Text(stringResource(Res.string.strLabelRetry))
                }
            }
        }

        else -> {
            val listState = rememberLazyListState()

            Box(modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .verticalFadingEdge(listState)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 4.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            item {
                                OnboardingChoiceRow(
                                    title = stringResource(Res.string.strTitleMushafScript),
                                    value = selectedScript.getQuranScriptName(),
                                    onClick = { showScriptSheet = true },
                                )
                            }

                            // Tajweed colours apply only to the Uthmani atlas script, so the opt-out
                            // is offered only while that script is selected. Default-on (see
                            // ReaderPreferences.KEY_TAJWEED_COLORS_ENABLED), so it starts checked.
                            if (selectedScript == QuranScriptUtils.SCRIPT_UTHMANI) {
                                item {
                                    SwitchItem(
                                        title = Res.string.titleTajweedColors,
                                        subtitle = Res.string.msgTajweedColors,
                                        checked = ReaderPreferences.observeTajweedColorsEnabled(),
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                ReaderPreferences.setTajweedColorsEnabled(checked)
                                            }
                                        },
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                ListItemCategoryLabel(
                                    title = stringResource(Res.string.strLabelSelectTranslation),
                                )
                            }

                            val isAnyDownloading = uiState.downloadStates.values.any {
                                it is ResourceDownloadStatus.Started || it is ResourceDownloadStatus.InProgress
                            }

                            for (group in uiState.translationGroups) {
                                items(
                                    items = group.translations,
                                    key = { it.bookInfo.slug },
                                ) { transl ->
                                    val slug = transl.bookInfo.slug
                                    val downloadState = uiState.downloadStates[slug] ?: ResourceDownloadStatus.Idle

                                    OnboardingTranslationRow(
                                        translation = transl,
                                        isSelected = uiState.selectedSlugs.contains(slug),
                                        downloadState = downloadState,
                                        onToggle = { checked ->
                                            viewModel.onEvent(
                                                TranslationEvent.SelectionChanged(transl, checked),
                                            )
                                        },
                                        onDownload = {
                                            if (!isAnyDownloading) {
                                                confirm = TranslationConfirm.Download(transl)
                                            }
                                        },
                                        onCancelDownload = {
                                            confirm = TranslationConfirm.CancelDownload(transl)
                                        },
                                        onUpdate = {
                                            confirm = TranslationConfirm.ForceUpdate(transl)
                                        },
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 52.dp),
                                        color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        thickness = 0.5.dp,
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                OnboardingHadithSyncItem(
                                    status = uiState.hadithSyncStatus,
                                    isDownloaded = uiState.isHadithDownloaded,
                                    onSync = { viewModel.onEvent(TranslationEvent.SyncHadiths) },
                                    onCancel = { viewModel.onEvent(TranslationEvent.CancelHadithSync) },
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(10.dp))

                                val selectedReciter = reciters?.firstOrNull { it.id == selectedReciterId }

                                OnboardingChoiceRow(
                                    // Spelled out here, unlike in the player: a first-time user has
                                    // no reason to know what a "qari" is yet.
                                    title = stringResource(Res.string.onboardTitleReciter),
                                    value = selectedReciter?.getReciterName()
                                        ?: stringResource(Res.string.strLabelNotSelected),
                                    onClick = { showReciterSheet = true },
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(10.dp))

                                val selectedWbw = wbwState.rows
                                    .firstOrNull { it.info.id == wbwState.selectedWbwId }

                                OnboardingChoiceRow(
                                    title = stringResource(Res.string.wbwSelectLanguage),
                                    value = selectedWbw?.info?.langName
                                        ?: stringResource(Res.string.strLabelNotSelected),
                                    onClick = { showWbwSheet = true },
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))

                                // Word audio is chapter-by-chapter and independent of which language
                                // pack is selected, so it gets its own entry rather than a control
                                // on the language row.
                                val downloadedChapters = wbwAudioState.downloadedChapters.size

                                OnboardingChoiceRow(
                                    title = stringResource(Res.string.wbwAudio),
                                    value = stringResource(
                                        Res.string.recitationDownloadChaptersProgress,
                                        downloadedChapters,
                                        QuranMeta.chapterRange.last,
                                    ),
                                    onClick = { showWbwAudioSheet = true },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    OnboardingWbwSheet(
        isOpen = showWbwSheet,
        onClose = { showWbwSheet = false },
    )

    // The settings sheet, unchanged: it already owns the per-chapter list, the "download all"
    // confirmation and the notification-permission prompt.
    WbwAudioDownloadSheet(
        isOpen = showWbwAudioSheet,
        onDismiss = { showWbwAudioSheet = false },
        viewModel = wbwAudioViewModel,
    )

    OnboardingReciterSheet(
        isOpen = showReciterSheet,
        onClose = { showReciterSheet = false },
    )

    OnboardingScriptSheet(
        isOpen = showScriptSheet,
        onClose = { showScriptSheet = false },
    )

    MessageDialog(
        title = message?.title,
        message = message?.message,
        onClose = { message = null },
    )

    TranslationConfirmDialog(
        confirm = confirm,
        onClose = { confirm = null },
        onConfirmed = { action ->
            val slug = action.translation.bookInfo.slug
            when (action) {
                is TranslationConfirm.Download -> viewModel.onEvent(TranslationEvent.DownloadTranslation(slug))
                is TranslationConfirm.CancelDownload -> viewModel.onEvent(TranslationEvent.CancelDownload(slug))
                is TranslationConfirm.ForceUpdate -> viewModel.onEvent(TranslationEvent.ForceUpdateTranslation(slug))
                is TranslationConfirm.Delete -> viewModel.onEvent(TranslationEvent.DeleteTranslation(slug))
            }
            confirm = null
        },
    )
}

@Composable
private fun OnboardingHadithSyncItem(
    status: ResourceDownloadStatus,
    isDownloaded: Boolean,
    onSync: () -> Unit,
    onCancel: () -> Unit
) {
    val isSyncing = status is ResourceDownloadStatus.InProgress || status is ResourceDownloadStatus.Started
    val progress = if (status is ResourceDownloadStatus.InProgress) status.progress / 100f else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant,
                            )
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_read_quran),
                                contentDescription = null,
                                tint = if (isDownloaded) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.strTitleHadith),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        if (isSyncing && status is ResourceDownloadStatus.InProgress) {
                            Text(
                                text = "${stringResource(Res.string.textDownloading)} ${status.progress}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Text(
                                text = if (isDownloaded) stringResource(Res.string.strLabelDownloaded) else stringResource(Res.string.strLabelAllChapters),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (!isDownloaded && !isSyncing) {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_download),
                        contentDescription = null,
                        onClick = onSync,
                        tint = colorScheme.primary,
                        small = true
                    )
                } else if (isDownloaded && !isSyncing) {
                    // Same sync, run again: hadith books get corrections and additions, and there
                    // was no way to pull them from here once the first sync had finished.
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_refresh),
                        contentDescription = stringResource(Res.string.strLabelUpdate),
                        onClick = onSync,
                        tint = colorScheme.onSurfaceVariant,
                        small = true
                    )
                } else if (isSyncing) {
                    IconButton(
                        painter = painterResource(Res.drawable.dr_icon_close),
                        contentDescription = stringResource(Res.string.strLabelCancel),
                        onClick = onCancel,
                        tint = colorScheme.onSurfaceVariant,
                        small = true
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTranslationRow(
    translation: TranslModel,
    isSelected: Boolean,
    downloadState: ResourceDownloadStatus,
    onToggle: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onUpdate: () -> Unit,
) {
    val bookInfo = translation.bookInfo

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colorScheme.primary),
                onClick = {
                    if (translation.isDownloaded) {
                        onToggle(!isSelected)
                    } else {
                        if (downloadState is ResourceDownloadStatus.Started || downloadState is ResourceDownloadStatus.InProgress) {
                            onCancelDownload()
                        } else {
                            onDownload()
                        }
                    }
                },
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (translation.isDownloaded) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    uncheckedColor = colorScheme.onSurfaceVariant,
                ),
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (downloadState) {
                    is ResourceDownloadStatus.Started -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.primary
                        )
                    }

                    is ResourceDownloadStatus.InProgress -> {
                        CircularProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.primary,
                            trackColor = colorScheme.surfaceVariant
                        )
                    }

                    else -> {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_download),
                            contentDescription = stringResource(Res.string.labelDownload),
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp),
        ) {
            Text(
                text = bookInfo.bookName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))

            if (downloadState is ResourceDownloadStatus.InProgress) {
                Text(
                    text = stringResource(Res.string.textDownloading) + " ${downloadState.progress}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (downloadState is ResourceDownloadStatus.Started) {
                Text(
                    text = stringResource(Res.string.textDownloading),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = bookInfo.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Light,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val isBusy = downloadState is ResourceDownloadStatus.InProgress ||
                downloadState is ResourceDownloadStatus.Started

        if (!translation.isDownloaded && isBusy) {
            IconButton(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelCancel),
                onClick = onCancelDownload,
                tint = colorScheme.onSurfaceVariant,
                small = true
            )
        } else if (translation.isDownloaded && !isBusy) {
            // Same affordance the hadith row has: re-download this book so corrections published
            // after the first download actually arrive.
            IconButton(
                painter = painterResource(Res.drawable.dr_icon_refresh),
                contentDescription = stringResource(Res.string.strLabelUpdate),
                onClick = onUpdate,
                tint = colorScheme.onSurfaceVariant,
                small = true
            )
        }
    }
}
