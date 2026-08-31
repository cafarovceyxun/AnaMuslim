package com.cafarovceyxun.anamuslim.compose.components.common

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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
actual fun StoryVideo(
    url: String,
    modifier: Modifier,
    paused: Boolean,
    onProgress: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnProgress by rememberUpdatedState(onProgress)

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnFinished()
            }

            // Fayl açılmasa hekayə qara kadrda ilişib qalardı — növbəti slayda keçirik.
            override fun onPlayerError(error: PlaybackException) {
                AppLogger.d(TAG, "Playback failed: ${error.message}")
                currentOnFinished()
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Hekayə dayandırılanda video da dayanır; davam edəndə qaldığı yerdən oynayır.
    LaunchedEffect(player, paused) {
        player.playWhenReady = !paused
    }

    // ExoPlayer mövqe axını vermir, ona görə kadr sürətinə yaxın intervalla oxunur — zolaq
    // videonun öz vaxtı ilə irəliləsin.
    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            if (duration > 0) {
                currentOnProgress((player.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
            }
            delay(POSITION_POLL_MILLIS)
        }
    }

    // ⚠️ `AndroidView`-in factory-si düyün ömründə **bir dəfə** işləyir. Url dəyişəndə
    // `remember(url)` yeni `ExoPlayer` qaytarır, ekrandakı `PlayerView` isə köhnə — artıq
    // buraxılmış — pleyerdə qalırdı: ikinci hekayədə kadr açılmır, zolaq isə yeni pleyerin
    // mövqeyi ilə irəliləyirdi. `key` görünüşü də pleyerlə birlikdə yeniləyir.
    key(url) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            // Eyni url-da belə görünüş yenidən qurulsa pleyer bağlanmış qalmasın.
            update = { view -> view.player = player },
            modifier = modifier,
        )
    }
}

private const val POSITION_POLL_MILLIS = 60L
private const val TAG = "StoryVideo"
