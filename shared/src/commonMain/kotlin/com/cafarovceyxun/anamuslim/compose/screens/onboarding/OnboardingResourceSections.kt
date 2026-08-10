package com.cafarovceyxun.anamuslim.compose.screens.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetBare
import com.cafarovceyxun.anamuslim.compose.screens.settings.ReciterDownloadCard
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_right
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.dr_icon_download
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.labelDownload
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelDownloaded
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.resources.strLabelRecommended
import com.cafarovceyxun.anamuslim.resources.strMsgSomethingWrong
import com.cafarovceyxun.anamuslim.resources.strTitleScripts
import com.cafarovceyxun.anamuslim.resources.onboardTitleReciter
import com.cafarovceyxun.anamuslim.resources.textDownloading
import com.cafarovceyxun.anamuslim.resources.wbwSelectLanguage
import com.cafarovceyxun.anamuslim.resources.wbwShowTranslation
import com.cafarovceyxun.anamuslim.utils.managers.ResourceDownloadStatus
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayerProvider
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import com.cafarovceyxun.anamuslim.utils.reader.getQuranScriptName
import com.cafarovceyxun.anamuslim.utils.reader.isKFQPCScript
import com.cafarovceyxun.anamuslim.utils.reader.isQuranAtlasScript
import com.cafarovceyxun.anamuslim.viewModels.RecitationBatchDownloadState
import com.cafarovceyxun.anamuslim.viewModels.RecitationDownloadEvent
import com.cafarovceyxun.anamuslim.viewModels.RecitationDownloadViewModel
import com.cafarovceyxun.anamuslim.viewModels.ScriptEvent
import com.cafarovceyxun.anamuslim.viewModels.ScriptsViewModel
import com.cafarovceyxun.anamuslim.viewModels.WbwSettingsViewModel
import com.cafarovceyxun.anamuslim.viewModels.WbwUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The resource sections onboarding adds on top of translations and hadith: word-by-word language
 * packs, the reciter and the mushaf script.
 *
 * All three are one-line summaries that open a picker sheet, so the page stays short enough to see
 * at a glance. What a sheet offers follows what the resource costs:
 * - **Word-by-word** must be downloaded before it can be selected at all — a pack is data, and an
 *   absent one has nothing to show.
 * - **Script** can be selected either way, but downloading first is offered here so a user who
 *   picks V4 does not open the reader onto an unrendered mushaf.
 * - **Reciter** is selectable immediately and can be previewed; its full audio (hundreds of MB) is
 *   an explicit, per-reciter download, never implied by selecting one.
 */

/** Al-'Alaq — short, and its opening is the passage most listeners recognise. */
private const val PREVIEW_CHAPTER_NO = 96

/** The sample stops here rather than running the whole surah. */
private const val PREVIEW_LAST_VERSE_NO = 10

/** Same density the reader's own atlas download asks for. */
private const val ATLAS_DENSITY_LEVEL = 6

/**
 * Word-by-word picker. Unlike the reciter and script sheets this one downloads: a pack has to be on
 * the device before it can be selected, so a row is a download button until it is, then a radio.
 */
@Composable
internal fun OnboardingWbwSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    if (!isOpen) return

    val scope = rememberCoroutineScope()
    val viewModel = viewModel { WbwSettingsViewModel() }
    val state by viewModel.uiState.collectAsState()
    val rows = state.rows

    BottomSheetBare(
        isOpen = true,
        onDismiss = onClose,
        header = {
            Text(
                text = stringResource(Res.string.wbwSelectLanguage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        },
    ) {
        if (rows.isEmpty()) {
            OnboardingSectionPlaceholder(isLoading = state.isLoading)
            return@BottomSheetBare
        }

        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            rows.forEach { row ->
                OnboardingWbwRow(
                    row = row,
                    isSelected = row.info.id == state.selectedWbwId,
                    downloadState = state.downloadStates[row.info.id]
                        ?: ResourceDownloadStatus.Idle,
                    // The sheet stays open after a download starts so its progress is visible;
                    // picking a language is the action that closes it.
                    onSelect = {
                        viewModel.selectLanguage(row.info.id)
                        onClose()
                    },
                    onDownload = { viewModel.startDownload(row.info.id) },
                    onCancelDownload = { viewModel.cancelDownload(row.info.id) },
                )
            }

            // Only meaningful once a pack is installed — with nothing downloaded the toggle would
            // govern text that cannot be shown.
            if (state.selectedWbwId != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                )

                SwitchItem(
                    title = Res.string.wbwShowTranslation,
                    checked = ReaderPreferences.observeWbwShowTranslation(),
                    onCheckedChange = { checked ->
                        scope.launch { ReaderPreferences.setWbwShowTranslation(checked) }
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Stand-in for a list that has not arrived yet (or came back empty, e.g. offline). */
@Composable
internal fun OnboardingSectionPlaceholder(isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = colorScheme.primary,
            )
        } else {
            Text(
                text = stringResource(Res.string.strMsgSomethingWrong),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A single word-by-word pack: download when missing, select when present. */
@Composable
private fun OnboardingWbwRow(
    row: WbwUiModel,
    isSelected: Boolean,
    downloadState: ResourceDownloadStatus,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    val isDownloading = downloadState is ResourceDownloadStatus.Started ||
            downloadState is ResourceDownloadStatus.InProgress

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colorScheme.primary),
                onClick = {
                    when {
                        row.isDownloaded -> onSelect()
                        isDownloading -> onCancelDownload()
                        else -> onDownload()
                    }
                },
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                row.isDownloaded -> RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                )

                downloadState is ResourceDownloadStatus.InProgress -> CircularProgressIndicator(
                    progress = { downloadState.progress / 100f },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceVariant,
                )

                downloadState is ResourceDownloadStatus.Started -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.primary,
                )

                else -> Icon(
                    painter = painterResource(Res.drawable.dr_icon_download),
                    contentDescription = stringResource(Res.string.labelDownload),
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp, end = 8.dp),
        ) {
            Text(
                text = row.info.langName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    downloadState is ResourceDownloadStatus.InProgress ->
                        "${stringResource(Res.string.textDownloading)} ${downloadState.progress}%"

                    downloadState is ResourceDownloadStatus.Started ->
                        stringResource(Res.string.textDownloading)

                    row.isDownloaded -> stringResource(Res.string.strLabelDownloaded)
                    else -> row.info.langCode.uppercase()
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                color = if (isDownloading) colorScheme.primary else colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isDownloading) {
            IconButton(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelCancel),
                onClick = onCancelDownload,
                tint = colorScheme.onSurfaceVariant,
                small = true,
            )
        }
    }
}

/** One-line "current choice" row that opens a picker sheet. */
@Composable
internal fun OnboardingChoiceRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colorScheme.primary),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            painter = painterResource(Res.drawable.dr_icon_chevron_right),
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Reciter picker with a preview and an offline download per reciter.
 *
 * The preview plays the opening of Al-'Alaq through the app's real [RecitationPlayer] rather than a
 * throwaway audio object: resolving a reciter's URL template, falling back between sources and
 * reusing already-downloaded files all live there, and duplicating it for a sample would drift.
 * Two consequences are handled explicitly below — the player is a single shared session, so the
 * preview has to be stopped when the sheet closes, and [RecitationPlayer.setReciter] moves that
 * session onto the previewed voice, so the user's own choice is put back afterwards.
 */
@Composable
internal fun OnboardingReciterSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    if (!isOpen) return

    val scope = rememberCoroutineScope()
    val viewModel = viewModel { RecitationDownloadViewModel() }
    val state by viewModel.uiState.collectAsState()
    val selectedId = RecitationPreferences.observeReciterId()

    val player = remember { RecitationPlayerProvider.player }
    val playerState by player.state.collectAsState()
    val isPlaying by player.isPlayingState.collectAsState()

    var previewReciterId by remember { mutableStateOf<String?>(null) }
    // Read inside onDispose, which runs after the composable's own snapshot reads are gone.
    val latestSelectedId by rememberUpdatedState(selectedId)
    val latestPreviewId by rememberUpdatedState(previewReciterId)

    fun restoreChosenReciter() {
        latestSelectedId?.let { player.setReciter(it, RecitationAudioKind.QURAN) }
    }

    fun stopPreview() {
        previewReciterId = null
        player.stop()
        restoreChosenReciter()
    }

    DisposableEffect(Unit) {
        player.connect()
        onDispose {
            // A preview left running would keep playing under the main app (and, on Android, keep
            // its notification up) after onboarding is gone.
            if (latestPreviewId != null) {
                player.stop()
                latestSelectedId?.let { player.setReciter(it, RecitationAudioKind.QURAN) }
            }
            player.disconnect()
        }
    }

    // The player has no "play verses 1..N" mode — it runs to the end of the chapter — so the sample
    // is bounded here, by watching the verse it reports.
    LaunchedEffect(playerState.currentVerse, previewReciterId) {
        if (previewReciterId == null) return@LaunchedEffect

        val verse = playerState.currentVerse
        if (verse.chapterNo != PREVIEW_CHAPTER_NO || verse.verseNo > PREVIEW_LAST_VERSE_NO) {
            stopPreview()
        }
    }

    BottomSheetBare(
        isOpen = true,
        onDismiss = onClose,
        header = {
            Text(
                text = stringResource(Res.string.onboardTitleReciter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        },
    ) {
        val rows = state.quranReciters

        if (rows.isEmpty()) {
            OnboardingSectionPlaceholder(isLoading = state.isLoading)
            return@BottomSheetBare
        }

        LazyColumn(
            modifier = Modifier.heightIn(max = 460.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(rows, key = { it.id }) { reciter ->
                val isPreviewing = reciter.id == previewReciterId
                // The view model keys its states by kind *and* id; the bare id never matches.
                val key = RecitationDownloadViewModel.stateKey(
                    RecitationAudioKind.QURAN,
                    reciter.id,
                )

                ReciterDownloadCard(
                    title = reciter.getReciterName(),
                    subtitle = reciter.getStyleName(),
                    state = state.downloadStates[key]
                        ?: RecitationBatchDownloadState(0, 0, QuranMeta.chapterRange.last),
                    downloadBlockedByOther = false,
                    // Onboarding only downloads; deleting belongs to settings.
                    onDeleteAll = null,
                    // In onboarding the card's job is picking a reciter, not opening its chapters.
                    onOpenChapters = {
                        player.stop()
                        previewReciterId = null
                        player.setReciter(reciter.id, RecitationAudioKind.QURAN)
                        scope.launch { RecitationPreferences.setReciterId(reciter.id) }
                        onClose()
                    },
                    onDownload = {
                        viewModel.onEvent(
                            RecitationDownloadEvent.StartDownload(
                                RecitationAudioKind.QURAN,
                                reciter.id,
                            ),
                        )
                    },
                    onCancel = {
                        viewModel.onEvent(
                            RecitationDownloadEvent.CancelDownload(
                                RecitationAudioKind.QURAN,
                                reciter.id,
                            ),
                        )
                    },
                    selected = reciter.id == selectedId,
                    showChevron = false,
                    leading = {
                        OnboardingReciterPreviewButton(
                            isPlaying = isPreviewing && isPlaying,
                            isLoading = isPreviewing &&
                                    playerState.resolvingChapterNo == PREVIEW_CHAPTER_NO,
                            onClick = {
                                when {
                                    !isPreviewing -> {
                                        previewReciterId = reciter.id
                                        player.setReciter(reciter.id, RecitationAudioKind.QURAN)
                                        player.start(ChapterVersePair(PREVIEW_CHAPTER_NO, 1))
                                    }

                                    isPlaying -> player.pause()
                                    else -> player.resume()
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

/** Play/pause for the sample recitation, drawn inside the card's round badge. */
@Composable
private fun OnboardingReciterPreviewButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = colorScheme.primary,
        )
        return
    }

    IconButton(
        painter = painterResource(if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play),
        contentDescription = stringResource(
            if (isPlaying) Res.string.strLabelPause else Res.string.strLabelPlay
        ),
        onClick = onClick,
        tint = colorScheme.primary,
        small = true,
    )
}

/**
 * Mushaf script picker: a script can now be fetched from here instead of only being selected, so a
 * user who picks V4 during onboarding does not meet an unrendered mushaf on first open.
 */
@Composable
internal fun OnboardingScriptSheet(
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    if (!isOpen) return

    val scope = rememberCoroutineScope()
    val viewModel = viewModel { ScriptsViewModel() }
    val state by viewModel.uiState.collectAsState()
    val scripts = remember { QuranScriptUtils.availableScripts() }
    val selectedScript = ReaderPreferences.observeQuranScript()

    // Whether each script's resources are already on disk. Recomputed whenever a download state
    // changes, since that is the only thing that can turn a "missing" into an "installed".
    val installed by produceState(emptyMap<String, Boolean>(), state.downloadStates) {
        value = withContext(Dispatchers.IO) {
            scripts.keys.associateWith { key ->
                when {
                    key.isKFQPCScript() ->
                        QuranScriptUtils.getKFQPCFontDownloadedCount(key).remaining == 0

                    key.isQuranAtlasScript() -> viewModel.isAtlasInstalled(key)
                    else -> true
                }
            }
        }
    }

    BottomSheetBare(
        isOpen = true,
        onDismiss = onClose,
        header = {
            Text(
                text = stringResource(Res.string.strTitleScripts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 420.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(scripts.keys.toList(), key = { it }) { script ->
                val downloadState = state.downloadStates[script] ?: ResourceDownloadStatus.Idle

                OnboardingPickerRow(
                    title = script.getQuranScriptName(),
                    subtitle = null,
                    badge = if (script == QuranScriptUtils.SCRIPT_KFQPC_V4) {
                        stringResource(Res.string.strLabelRecommended)
                    } else {
                        null
                    },
                    selected = script == selectedScript,
                    onClick = {
                        scope.launch {
                            ReaderPreferences.setQuranScriptWithVariant(
                                script,
                                scripts[script]?.firstOrNull(),
                            )
                        }
                        onClose()
                    },
                    trailing = {
                        OnboardingScriptDownloadButton(
                            // Unknown until the disk scan lands; treat that as installed so the
                            // button does not flash in for a frame.
                            isInstalled = installed[script] ?: true,
                            downloadState = downloadState,
                            onDownload = {
                                if (script.isKFQPCScript()) {
                                    viewModel.onEvent(ScriptEvent.DownloadScript(script))
                                } else {
                                    viewModel.onEvent(
                                        ScriptEvent.DownloadAtlas(script, ATLAS_DENSITY_LEVEL),
                                    )
                                }
                            },
                            onCancel = { viewModel.onEvent(ScriptEvent.CancelDownload(script)) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun OnboardingScriptDownloadButton(
    isInstalled: Boolean,
    downloadState: ResourceDownloadStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    when {
        downloadState is ResourceDownloadStatus.InProgress -> Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                progress = { downloadState.progress / 100f },
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant,
            )
            IconButton(
                painter = painterResource(Res.drawable.dr_icon_close),
                contentDescription = stringResource(Res.string.strLabelCancel),
                onClick = onCancel,
                tint = colorScheme.onSurfaceVariant,
                small = true,
            )
        }

        downloadState is ResourceDownloadStatus.Started -> CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = colorScheme.primary,
        )

        isInstalled -> Unit

        else -> IconButton(
            painter = painterResource(Res.drawable.dr_icon_download),
            contentDescription = stringResource(Res.string.labelDownload),
            onClick = onDownload,
            tint = colorScheme.onSurfaceVariant,
            small = true,
        )
    }
}

@Composable
private fun OnboardingPickerRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colorScheme.primary),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)

        if (leading != null) leading()

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary,
                    maxLines = 1,
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (trailing != null) trailing()
    }
}
