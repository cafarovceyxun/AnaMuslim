package com.cafarovceyxun.anamuslim.compose.components.player


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_lock_keyhole_closed
import com.cafarovceyxun.anamuslim.resources.ic_lock_open
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.resources.strLabelVerseSerialWithChapter
import com.cafarovceyxun.anamuslim.resources.verseSyncOff
import com.cafarovceyxun.anamuslim.resources.verseSyncOn
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer
import com.cafarovceyxun.anamuslim.utils.mediaplayer.rememberCurrentReciterNameForAudioOption
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationServiceState
import com.cafarovceyxun.anamuslim.utils.univ.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    controller: RecitationPlayer,
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    isSyncing: Boolean,
    onSyncRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPlayPause: (() -> Unit)? = null,
    onExpand: () -> Unit,
) {
    val verse = state.currentVerse
    val repository = remember { RepositoryProvider.quranRepository }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(verse.chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            repository.getChapterName(verse.chapterNo)
        }
    }

    val reciterNames = rememberCurrentReciterNameForAudioOption()
    val (positionMs, durationMs) = rememberTimestamp(isPlaying, controller)

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand,
            ),
    ) {
        MiniProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniPlayerThumbnail(
                verse = verse,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        Res.string.strLabelVerseSerialWithChapter,
                        chapterName,
                        verse.chapterNo,
                        verse.verseNo
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = reciterNames,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            initialDelayMillis = 900,
                            repeatDelayMillis = 1_200,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.alpha(0.8f),
                    maxLines = 1,
                )
            }

            if (onSyncRequest != null) {
                SimpleTooltip(
                    text = if (isSyncing) stringResource(Res.string.verseSyncOn) else stringResource(Res.string.verseSyncOff)
                ) {
                    IconButton(
                        onClick = onSyncRequest,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painterResource(if (isSyncing) Res.drawable.ic_lock_keyhole_closed else Res.drawable.ic_lock_open),
                            contentDescription = stringResource(
                                if (isSyncing) Res.string.verseSyncOn else
                                    Res.string.verseSyncOff
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = if (isSyncing) colorScheme.primary else colorScheme.onSurfaceVariant.alpha(0.7f),
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = colorScheme.primary.copy(alpha = 0.2f),
                        spotColor = colorScheme.primary.copy(alpha = 0.3f),
                    )
                    .clip(CircleShape)
                    .background(colorScheme.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (onPlayPause != null) onPlayPause()
                            else controller.playPause()
                        },
                    ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        painterResource(
                            if (isPlaying) Res.drawable.ic_pause
                            else Res.drawable.ic_play,
                        ),
                        contentDescription = if (isPlaying) stringResource(Res.string.strLabelPause)
                        else stringResource(Res.string.strLabelPlay),
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Timestamp(
                positionMs = positionMs,
                durationMs = durationMs,
            )
        }
    }
}


@Composable
private fun MiniProgressBar(
    positionMs: Long,
    durationMs: Long,
) {
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
        color = colorScheme.primary,
        trackColor = colorScheme.primary.copy(alpha = 0.15f),
    )
}

@Composable
fun Timestamp(
    positionMs: Long,
    durationMs: Long,
) {
    val current = formatDuration(positionMs)
    val total = formatDuration(durationMs)

    BoxWithConstraints(
        modifier = Modifier.padding(start = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.wrapContentHeight()
            ) {
                Text(
                    text = current,
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    text = total,
                    color = colorScheme.onSurfaceVariant.alpha(0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = current,
                    color = colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    text = " / ",
                    color = colorScheme.onSurfaceVariant.alpha(0.4f),
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = total,
                    color = colorScheme.onSurfaceVariant.alpha(0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun rememberTimestamp(
    isPlaying: Boolean,
    controller: RecitationPlayer,
): Pair<Long, Long> {
    var positionMs by remember { mutableLongStateOf(controller.currentPositionMs) }
    var durationMs by remember { mutableLongStateOf(controller.durationMs) }

    LaunchedEffect(isPlaying) {
        positionMs = controller.currentPositionMs
        durationMs = controller.durationMs
        while (isPlaying && isActive) {
            positionMs = controller.currentPositionMs
            durationMs = controller.durationMs
            delay(250)
        }

        if (!isPlaying) {
            positionMs = controller.currentPositionMs
            durationMs = controller.durationMs
        }
    }

    return positionMs to durationMs
}
