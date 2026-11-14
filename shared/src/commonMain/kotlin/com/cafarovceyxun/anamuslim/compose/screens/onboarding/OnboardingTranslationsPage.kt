package com.cafarovceyxun.anamuslim.compose.screens.onboarding

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import com.cafarovceyxun.anamuslim.resources.dr_icon_quran_script
import com.cafarovceyxun.anamuslim.resources.dr_icon_text_color
import com.cafarovceyxun.anamuslim.resources.dr_icon_translations
import com.cafarovceyxun.anamuslim.resources.ic_mic
import com.cafarovceyxun.anamuslim.resources.ic_volume_up
import com.cafarovceyxun.anamuslim.resources.onboardSectionContent
import com.cafarovceyxun.anamuslim.resources.onboardSectionListening
import com.cafarovceyxun.anamuslim.resources.onboardSectionReading
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.dr_icon_read_quran
import com.cafarovceyxun.anamuslim.resources.labelDownload
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDownloaded
import com.cafarovceyxun.anamuslim.resources.strLabelNotSelected
import com.cafarovceyxun.anamuslim.resources.strLabelRetry
import com.cafarovceyxun.anamuslim.resources.strLabelUpdate
import com.cafarovceyxun.anamuslim.resources.dr_icon_refresh
import com.cafarovceyxun.anamuslim.resources.strMsgSomethingWrong
import com.cafarovceyxun.anamuslim.resources.strTitleHadith
import com.cafarovceyxun.anamuslim.resources.strTitleMushafScript
import com.cafarovceyxun.anamuslim.resources.onboardTitleReciter
import com.cafarovceyxun.anamuslim.resources.textDownloading
import com.cafarovceyxun.anamuslim.resources.wbwAudio
import com.cafarovceyxun.anamuslim.resources.wbwSelectLanguage
import com.cafarovceyxun.anamuslim.resources.recitationDownloadChaptersProgress

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.cafarovceyxun.anamuslim.compose.components.common.ErrorMessageCard
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.Loader
import com.cafarovceyxun.anamuslim.compose.components.common.SearchTextField
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetBare
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
import com.cafarovceyxun.anamuslim.compose.components.dialogs.HadithSyncConfirmDialog
import com.cafarovceyxun.anamuslim.compose.components.dialogs.MessageDialog

@Composable
fun OnboardingTranslationsPage(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val viewModel = viewModel { TranslationViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    var message by remember { mutableStateOf<TranslationUiEvent.ShowMessage?>(null) }

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

    var showSyncHadithsConfirm by remember { mutableStateOf(false) }
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
            val selectedReciter = reciters?.firstOrNull { it.id == selectedReciterId }
            val selectedWbw = wbwState.rows.firstOrNull { it.info.id == wbwState.selectedWbwId }

            // Üç qrup, hər biri bir kart: əvvəl hamısı eyni çəkidə, ayrı-ayrı çərçivələrdə dururdu
            // və hansının nəyə aid olduğu oxunmurdu.
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                OnboardingGroupLabel(stringResource(Res.string.onboardSectionReading))
                OnboardingGroupCard {
                    OnboardingChoiceRow(
                        icon = Res.drawable.dr_icon_quran_script,
                        title = stringResource(Res.string.strTitleMushafScript),
                        value = selectedScript.getQuranScriptName(),
                        onClick = { showScriptSheet = true },
                    )

                    // Tajweed colours apply only to the Uthmani atlas script, so the opt-out is
                    // offered only while that script is selected. Default-on (see
                    // ReaderPreferences.KEY_TAJWEED_COLORS_ENABLED), so it starts checked.
                    if (selectedScript == QuranScriptUtils.SCRIPT_UTHMANI) {
                        OnboardingGroupDivider()
                        OnboardingSwitchRow(
                            icon = Res.drawable.dr_icon_text_color,
                            title = stringResource(Res.string.titleTajweedColors),
                            subtitle = stringResource(Res.string.msgTajweedColors),
                            checked = ReaderPreferences.observeTajweedColorsEnabled(),
                            onCheckedChange = { checked ->
                                scope.launch { ReaderPreferences.setTajweedColorsEnabled(checked) }
                            },
                        )
                    }
                }

                OnboardingGroupLabel(stringResource(Res.string.onboardSectionListening))
                OnboardingGroupCard {
                    OnboardingChoiceRow(
                        icon = Res.drawable.ic_mic,
                        // Spelled out here, unlike in the player: a first-time user has no reason
                        // to know what a "qari" is yet.
                        title = stringResource(Res.string.onboardTitleReciter),
                        value = selectedReciter?.getReciterName()
                            ?: stringResource(Res.string.strLabelNotSelected),
                        onClick = { showReciterSheet = true },
                    )
                    OnboardingGroupDivider()
                    // Word audio is chapter-by-chapter and independent of which language pack is
                    // selected, so it gets its own entry rather than a control on the language row.
                    OnboardingChoiceRow(
                        icon = Res.drawable.ic_volume_up,
                        title = stringResource(Res.string.wbwAudio),
                        value = stringResource(
                            Res.string.recitationDownloadChaptersProgress,
                            wbwAudioState.downloadedChapters.size,
                            QuranMeta.chapterRange.last,
                        ),
                        onClick = { showWbwAudioSheet = true },
                    )
                }

                OnboardingGroupLabel(stringResource(Res.string.onboardSectionContent))
                OnboardingGroupCard {
                    OnboardingHadithSyncItem(
                        status = uiState.hadithSyncStatus,
                        isDownloaded = uiState.isHadithDownloaded,
                        onSync = { showSyncHadithsConfirm = true },
                        onCancel = { viewModel.onEvent(TranslationEvent.CancelHadithSync) },
                    )
                    OnboardingGroupDivider()
                    OnboardingChoiceRow(
                        icon = Res.drawable.dr_icon_translations,
                        title = stringResource(Res.string.wbwSelectLanguage),
                        value = selectedWbw?.info?.langName
                            ?: stringResource(Res.string.strLabelNotSelected),
                        onClick = { showWbwSheet = true },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
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

    HadithSyncConfirmDialog(
        isOpen = showSyncHadithsConfirm,
        isUpdate = uiState.isHadithDownloaded,
        onClose = { showSyncHadithsConfirm = false },
        onConfirmed = {
            viewModel.onEvent(TranslationEvent.SyncHadiths)
            showSyncHadithsConfirm = false
        },
    )

    MessageDialog(
        title = message?.title,
        message = message?.message,
        onClose = { message = null },
    )
}

@Composable
private fun OnboardingHadithSyncItem(
    status: ResourceDownloadStatus,
    isDownloaded: Boolean,
    onSync: () -> Unit,
    onCancel: () -> Unit,
) {
    val isSyncing = status is ResourceDownloadStatus.InProgress || status is ResourceDownloadStatus.Started

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSyncing, onClick = onSync)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingIconTile(Res.drawable.dr_icon_read_quran)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = stringResource(Res.string.strTitleHadith),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    status is ResourceDownloadStatus.InProgress ->
                        "${stringResource(Res.string.textDownloading)} ${status.progress}%"

                    isSyncing -> stringResource(Res.string.textDownloading)
                    isDownloaded -> stringResource(Res.string.strLabelDownloaded)
                    else -> stringResource(Res.string.labelDownload)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isSyncing || !isDownloaded) colorScheme.primary else colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (status is ResourceDownloadStatus.InProgress) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { status.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceVariant,
                )
            }
        }

        when {
            isSyncing -> IconButton(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelCancel),
                onClick = onCancel,
                tint = colorScheme.onSurfaceVariant,
                small = true,
            )

            // Same sync, run again: hadith books get corrections and additions, and there was no
            // way to pull them from here once the first sync had finished.
            isDownloaded -> IconButton(
                painter = painterResource(Res.drawable.dr_icon_refresh),
                contentDescription = stringResource(Res.string.strLabelUpdate),
                onClick = onSync,
                tint = colorScheme.onSurfaceVariant,
                small = true,
            )

            else -> IconButton(
                painter = painterResource(Res.drawable.dr_icon_download),
                contentDescription = stringResource(Res.string.labelDownload),
                onClick = onSync,
                tint = colorScheme.primary,
                small = true,
            )
        }
    }
}
