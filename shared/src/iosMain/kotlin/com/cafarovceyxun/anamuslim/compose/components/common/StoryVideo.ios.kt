package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.currentItem
import platform.AVFoundation.pause
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.duration
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun StoryVideo(
    url: String,
    modifier: Modifier,
    onProgress: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnProgress by rememberUpdatedState(onProgress)

    val controller = remember(url) {
        AVPlayerViewController().apply {
            player = NSURL.URLWithString(url)?.let(::AVPlayer)
            showsPlaybackControls = false
            videoGravity = AVLayerVideoGravityResizeAspect
        }
    }

    DisposableEffect(controller) {
        // Bitmə bildirişi məhz bu elementə bağlanır; başqa videonun bitməsi hekayəni sürüşdürməsin.
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = controller.player?.currentItem,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> currentOnFinished() }

        // AVPlayer mövqeni özü bildirir — zolaq videonun öz vaxtı ilə irəliləsin.
        val timeObserver = controller.player?.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(POSITION_POLL_SECONDS, NSEC_PER_SEC.toInt()),
            queue = null,
        ) { time ->
            val duration = controller.player?.currentItem?.duration?.let(::CMTimeGetSeconds) ?: 0.0
            if (duration.isFinite() && duration > 0.0) {
                currentOnProgress((CMTimeGetSeconds(time) / duration).toFloat().coerceIn(0f, 1f))
            }
        }

        controller.player?.play()

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            timeObserver?.let { controller.player?.removeTimeObserver(it) }
            controller.player?.pause()
            controller.player = null
        }
    }

    UIKitViewController(
        factory = { controller },
        modifier = modifier,
    )
}

private const val POSITION_POLL_SECONDS = 0.06
