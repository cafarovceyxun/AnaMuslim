package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
actual fun VideoEditorPreview(
    url: String,
    modifier: Modifier,
    playing: Boolean,
    muted: Boolean,
    loopStartMillis: Long,
    loopEndMillis: Long,
    seek: SeekRequest?,
    onPosition: (Long) -> Unit,
) {
    val context = LocalContext.current
    val currentOnPosition by rememberUpdatedState(onPosition)
    val currentStart by rememberUpdatedState(loopStartMillis)
    val currentEnd by rememberUpdatedState(loopEndMillis)
    val currentPlaying by rememberUpdatedState(playing)

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(player, playing) {
        // Dayandırılmış başlanğıcdan oynadılanda aralığın əvvəlindən başlasın.
        if (playing && (player.currentPosition < currentStart || player.currentPosition >= currentEnd)) {
            player.seekTo(currentStart)
        }
        player.playWhenReady = playing
    }

    LaunchedEffect(player, muted) {
        player.volume = if (muted) 0f else 1f
    }

    LaunchedEffect(player, seek) {
        seek?.let { player.seekTo(it.millis) }
    }

    // Mövqe axını yoxdur — kadr sürətinə yaxın intervalla oxunur; aralığın sonunda dövrə vurur.
    LaunchedEffect(player) {
        while (true) {
            val position = player.currentPosition
            if (currentPlaying && position >= currentEnd) {
                player.seekTo(currentStart)
                player.playWhenReady = true
            }
            currentOnPosition(position)
            delay(POSITION_POLL_MILLIS)
        }
    }

    // Factory düyün ömründə bir dəfə işləyir — url dəyişəndə görünüş də yenilənsin.
    key(url) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view -> view.player = player },
            modifier = modifier,
        )
    }
}

private const val POSITION_POLL_MILLIS = 40L
