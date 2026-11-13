package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheetHeader
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.formatFileSize
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_close
import com.cafarovceyxun.anamuslim.resources.hadithSpeechDeleteModel
import com.cafarovceyxun.anamuslim.resources.hadithSpeechDownload
import com.cafarovceyxun.anamuslim.resources.hadithSpeechDownloadFailed
import com.cafarovceyxun.anamuslim.resources.hadithSpeechDownloadLabel
import com.cafarovceyxun.anamuslim.resources.hadithSpeechDownloading
import com.cafarovceyxun.anamuslim.resources.hadithSpeechEngineUnavailable
import com.cafarovceyxun.anamuslim.resources.hadithSpeechModelBody
import com.cafarovceyxun.anamuslim.resources.hadithSpeechModelBroken
import com.cafarovceyxun.anamuslim.resources.hadithSpeechModelInstalled
import com.cafarovceyxun.anamuslim.resources.hadithSpeechModelNote
import com.cafarovceyxun.anamuslim.resources.hadithSpeechModelTitle
import com.cafarovceyxun.anamuslim.resources.hadithSpeechNextHadith
import com.cafarovceyxun.anamuslim.resources.hadithSpeechPlaybackFailed
import com.cafarovceyxun.anamuslim.resources.hadithSpeechPreparing
import com.cafarovceyxun.anamuslim.resources.hadithSpeechPreviousHadith
import com.cafarovceyxun.anamuslim.resources.hadithSpeechStop
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.ic_skip_back
import com.cafarovceyxun.anamuslim.resources.ic_skip_forward
import com.cafarovceyxun.anamuslim.resources.ic_volume_up
import com.cafarovceyxun.anamuslim.resources.strLabelCancel
import com.cafarovceyxun.anamuslim.resources.strLabelHadithNo
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayerProvider
import com.cafarovceyxun.anamuslim.utils.speech.HadithSpeech
import com.cafarovceyxun.anamuslim.utils.speech.OfflineSpeechProvider
import com.cafarovceyxun.anamuslim.utils.speech.OfflineVoiceModel
import com.cafarovceyxun.anamuslim.utils.speech.SpeechFailure
import com.cafarovceyxun.anamuslim.utils.speech.SpeechItem
import com.cafarovceyxun.anamuslim.utils.speech.SpeechState
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Hədis oxucusunun «Dinlə» girişi.
 *
 * Model telefonda yoxdursa əvvəl endirmə vərəqi açılır və istək yadda qalır: endirmə bitən kimi
 * həmin hədis özü səslənir — istifadəçi düyməni ikinci dəfə axtarmasın. Vərəq bağlansa da endirmə
 * davam edir ([OfflineVoiceModel.startDownload] proses-ömürlü scope-dadır).
 */
@Stable
class HadithSpeechLauncher internal constructor() {
    /** Platforma mühərrik qeydiyyat edibmi — `false`-dursa «Dinlə» heç yerdə göstərilmir. */
    val isAvailable: Boolean = OfflineSpeechProvider.isAvailable

    var sheetOpen by mutableStateOf(false)
        internal set

    internal var pending by mutableStateOf<Pair<List<SpeechItem>, Int>?>(null)

    /**
     * [hadith]-dən başlayaraq [list]-i (cari siyahının ardıcıllığı) oxuyur. Hədis siyahıda yoxdursa
     * yalnız onun özü oxunur.
     */
    fun listen(hadith: Hadith, list: List<Hadith>) {
        // Tərcüməsi olmayan hədis növbədə səssiz «keçid» olardı — atılır.
        val items = list.filter { it.text_az.isNotBlank() }.mapNotNull { it.toSpeechItem() }
        val start = items.indexOfFirst { it.id == hadith.id }
        val request = if (start >= 0) items to start
        else listOfNotNull(hadith.toSpeechItem()) to 0
        if (request.first.isEmpty()) return

        if (OfflineVoiceModel.state.value == OfflineVoiceModel.State.Installed) {
            start(request)
        } else {
            pending = request
            sheetOpen = true
        }
    }

    fun openSettings() {
        sheetOpen = true
    }

    internal fun start(request: Pair<List<SpeechItem>, Int>) {
        // Quran qiraəti ilə üst-üstə düşməsin. Media3 nəzarətçisi yalnız əsas axından çağırıla
        // bilər — buna görə pleyerin özündə yox, burada (klik işləyicisində) dayandırılır.
        RecitationPlayerProvider.player.let { if (it.isPlaying) it.pause() }
        HadithSpeech.player.play(request.first, request.second)
    }

    private fun Hadith.toSpeechItem(): SpeechItem? =
        id?.let { SpeechItem(id = it, number = hadith_no, text = text_az) }
}

@Composable
fun rememberHadithSpeechLauncher(): HadithSpeechLauncher {
    val launcher = remember { HadithSpeechLauncher() }
    val model by OfflineVoiceModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { OfflineVoiceModel.refresh() }
    }

    LaunchedEffect(model) {
        if (model == OfflineVoiceModel.State.Installed) {
            launcher.pending?.let { request ->
                launcher.pending = null
                launcher.sheetOpen = false
                launcher.start(request)
            }
        }
    }

    HadithSpeechModelSheet(
        isOpen = launcher.sheetOpen,
        onDismiss = {
            launcher.sheetOpen = false
            launcher.pending = null
        },
    )
    return launcher
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HadithSpeechModelSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    if (!isOpen) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val model by OfflineVoiceModel.state.collectAsStateWithLifecycle()
    val downloadLabel = stringResource(Res.string.hadithSpeechDownloadLabel)
    val size = remember { formatFileSize(OfflineVoiceModel.totalBytes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Top) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            BottomSheetHeader(
                icon = Res.drawable.ic_volume_up,
                title = stringResource(Res.string.hadithSpeechModelTitle),
                hasDragHandle = true,
            )

            Text(
                text = stringResource(Res.string.hadithSpeechModelBody, size),
                style = typography.bodyMedium,
                color = colorScheme.onSurface.alpha(0.85f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.hadithSpeechModelNote),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant.alpha(0.8f),
            )
            Spacer(Modifier.height(20.dp))

            when (val state = model) {
                is OfflineVoiceModel.State.Downloading -> {
                    Text(
                        text = stringResource(Res.string.hadithSpeechDownloading),
                        style = typography.labelLarge,
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        // 0-da iOS-un formatlayıcısı «Zero KB» yazır — o anda yalnız ümumi ölçü.
                        text = if (state.progress <= 0f) size
                        else "${formatFileSize((state.progress * OfflineVoiceModel.totalBytes).toLong())} / $size",
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = { OfflineVoiceModel.delete() },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text(stringResource(Res.string.strLabelCancel)) }
                }

                OfflineVoiceModel.State.Installed -> {
                    Text(
                        text = stringResource(Res.string.hadithSpeechModelInstalled, size),
                        style = typography.labelLarge,
                        color = colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = { OfflineVoiceModel.delete() },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(Res.string.hadithSpeechDeleteModel), color = colorScheme.error)
                    }
                }

                OfflineVoiceModel.State.NotInstalled, OfflineVoiceModel.State.Failed -> {
                    if (state == OfflineVoiceModel.State.Failed) {
                        Text(
                            text = stringResource(Res.string.hadithSpeechDownloadFailed),
                            style = typography.bodySmall,
                            color = colorScheme.error,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Button(
                        onClick = { OfflineVoiceModel.startDownload(downloadLabel) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${stringResource(Res.string.hadithSpeechDownload)} · $size")
                    }
                }
            }
        }
    }
}

/**
 * Səslənən hədisin üzən idarə paneli — axtarış zolağı ([HadithSearchNavBar]) ilə eyni görünüş.
 *
 * Səs oxucudan çıxanda da davam edir (pleyer proses-ömürlüdür), panel isə yalnız oxucudadır; ona görə
 * oxucu bağlananda səs dayandırılır — bax `HadithItemsScreen`-dəki `DisposableEffect`.
 */
@Composable
fun HadithSpeechBar(
    state: SpeechState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = HadithSpeech.player
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (state) {
                is SpeechState.Active -> {
                    Column(modifier = Modifier.widthIn(min = 72.dp, max = 140.dp)) {
                        Text(
                            text = stringResource(Res.string.strLabelHadithNo, state.item.number),
                            style = typography.labelLarge.withScriptDirection(arabic = false),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (state.preparing) stringResource(Res.string.hadithSpeechPreparing)
                            else "${state.chunkIndex + 1}/${state.chunkCount}",
                            style = typography.labelSmall.withScriptDirection(arabic = false),
                            color = colorScheme.onSurfaceVariant.alpha(0.8f),
                        )
                    }

                    SpeechBarButton(
                        icon = Res.drawable.ic_skip_back,
                        label = stringResource(Res.string.hadithSpeechPreviousHadith),
                        onClick = player::previous,
                    )

                    if (state.preparing && !state.paused) {
                        Spacer(Modifier.size(12.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(12.dp))
                    } else {
                        SpeechBarButton(
                            icon = if (state.paused) Res.drawable.ic_play else Res.drawable.ic_pause,
                            label = stringResource(if (state.paused) Res.string.strLabelPlay else Res.string.strLabelPause),
                            onClick = { if (state.paused) player.resume() else player.pause() },
                            emphasized = true,
                        )
                    }

                    SpeechBarButton(
                        icon = Res.drawable.ic_skip_forward,
                        label = stringResource(Res.string.hadithSpeechNextHadith),
                        onClick = player::next,
                        enabled = state.hasNext,
                    )
                }

                is SpeechState.Failed -> {
                    Text(
                        text = stringResource(
                            when (state.reason) {
                                SpeechFailure.ModelLoadFailed -> Res.string.hadithSpeechModelBroken
                                SpeechFailure.PlaybackFailed -> Res.string.hadithSpeechPlaybackFailed
                                SpeechFailure.EngineUnavailable -> Res.string.hadithSpeechEngineUnavailable
                            },
                        ),
                        style = typography.labelMedium,
                        color = colorScheme.error,
                        modifier = Modifier.widthIn(max = 240.dp).padding(vertical = 10.dp),
                    )
                    if (state.reason == SpeechFailure.ModelLoadFailed) {
                        TextButton(onClick = onOpenSettings) {
                            Text(stringResource(Res.string.hadithSpeechDeleteModel))
                        }
                    }
                }

                SpeechState.Idle -> Unit
            }

            SpeechBarButton(
                icon = Res.drawable.dr_icon_close,
                label = stringResource(Res.string.hadithSpeechStop),
                onClick = player::stop,
            )
        }
    }
}

@Composable
private fun SpeechBarButton(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(if (emphasized) 26.dp else 20.dp),
            tint = when {
                !enabled -> colorScheme.onSurfaceVariant.alpha(0.3f)
                emphasized -> colorScheme.primary
                else -> colorScheme.onSurfaceVariant
            },
        )
    }
}
