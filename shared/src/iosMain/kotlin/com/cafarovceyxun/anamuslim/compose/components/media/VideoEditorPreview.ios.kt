package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.muted
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setMuted
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
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
    val currentOnPosition by rememberUpdatedState(onPosition)
    val currentStart by rememberUpdatedState(loopStartMillis)
    val currentEnd by rememberUpdatedState(loopEndMillis)
    val currentPlaying by rememberUpdatedState(playing)

    val controller = remember(url) {
        AVPlayerViewController().apply {
            player = NSURL.URLWithString(url)?.let(::AVPlayer)
            showsPlaybackControls = false
            // iOS 16+ kadrda mətn tapanda öz «Live Text» düyməsini küncə qoyur — hekayədə/redaktorda yad.
            allowsVideoFrameAnalysis = false
            videoGravity = AVLayerVideoGravityResizeAspect
        }
    }

    fun seekExact(millis: Long) {
        controller.player?.seekToTime(
            time = CMTimeMakeWithSeconds(millis / 1000.0, preferredTimescale = TIMESCALE),
            toleranceBefore = kCMTimeZero.readValue(),
            toleranceAfter = kCMTimeZero.readValue(),
        )
    }

    DisposableEffect(controller) {
        val player = controller.player

        // Aralığın sonu faylın sonudursa periodik müşahidəçi oraya çatmadan pleyer dayanır.
        val endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player?.currentItem,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            if (currentPlaying) {
                seekExact(currentStart)
                player?.play()
            }
        }

        val timeObserver = player?.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(POSITION_POLL_SECONDS, NSEC_PER_SEC.toInt()),
            queue = null,
        ) { time ->
            val millis = (CMTimeGetSeconds(time) * 1000).toLong()
            if (currentPlaying && millis >= currentEnd) {
                seekExact(currentStart)
            }
            currentOnPosition(millis)
        }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(endObserver)
            timeObserver?.let { player?.removeTimeObserver(it) }
            player?.pause()
            controller.player = null
        }
    }

    LaunchedEffect(controller, playing) {
        val player = controller.player ?: return@LaunchedEffect
        if (playing) {
            val millis = (CMTimeGetSeconds(player.currentTime()) * 1000).toLong()
            if (millis < currentStart || millis >= currentEnd) seekExact(currentStart)
            player.play()
        } else {
            player.pause()
        }
    }

    LaunchedEffect(controller, muted) {
        controller.player?.setMuted(muted)
    }

    LaunchedEffect(controller, seek) {
        seek?.let { seekExact(it.millis) }
    }

    key(url) {
        UIKitViewController(
            factory = { controller },
            modifier = modifier,
            // Toxunuşu udmasın — üstündəki kadr çərçivəsi və oynat/dayandır Compose jestləridir.
            properties = UIKitInteropProperties(interactionMode = null),
        )
    }
}

private const val TIMESCALE = 600
private const val POSITION_POLL_SECONDS = 0.04
