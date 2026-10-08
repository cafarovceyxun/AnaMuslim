package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
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
import platform.darwin.NSEC_PER_SEC

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun StoryVideo(
    url: String,
    modifier: Modifier,
    paused: Boolean,
    onProgress: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnProgress by rememberUpdatedState(onProgress)

    // Diskdə varsa oradan, yoxsa şəbəkədən və eyni anda keşə (bax `StoryVideoCache.ios.kt`).
    val source = remember(url) { StoryVideoSource.create(url) }

    val controller = remember(source) {
        AVPlayerViewController().apply {
            player = source.player
            showsPlaybackControls = false
            // iOS 16+ kadrda mətn tapanda öz «Live Text» düyməsini küncə qoyur — hekayədə/redaktorda yad.
            allowsVideoFrameAnalysis = false
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

        // Fayl açılmasa hekayə qara kadrda ilişib qalardı — növbəti slayda keçirik.
        val failureObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemFailedToPlayToEndTimeNotification,
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
            NSNotificationCenter.defaultCenter.removeObserver(failureObserver)
            timeObserver?.let { controller.player?.removeTimeObserver(it) }
            controller.player?.pause()
            controller.player = null
            source.release()
        }
    }

    // Hekayə dayandırılanda video da dayanır; davam edəndə qaldığı yerdən oynayır.
    LaunchedEffect(controller, paused) {
        if (paused) controller.player?.pause() else controller.player?.play()
    }

    // ⚠️ `UIKitViewController` factory-si düyün ömründə **bir dəfə** işləyir. Url dəyişəndə
    // `remember(url)` yeni `AVPlayerViewController` verir, ekranda isə köhnəsi qalırdı — və onun
    // pleyeri dispose-da `null`-a düşdüyü üçün ikinci hekayədə kadr açılmır, zolaq isə yeni
    // pleyerin mövqeyi ilə irəliləyirdi. `key` görünüşü də kontrollerlə birlikdə yeniləyir.
    key(url) {
        UIKitViewController(
            factory = { controller },
            modifier = modifier,
            // ⚠️ Interop görünüşü toxunuşları **udur**: `interactionMode` verilməsə hekayənin
            // jestləri (pauza, sağ/sol keçid, aşağı sürüşdürmə) video slaydında Compose-a
            // çatmırdı — şəkildə işləyən hər şey videoda ölü qalırdı. `null` = görünüş toxunuş
            // almır; idarəetmə düymələri onsuz da bağlıdır, ona görə itirilən heç nə yoxdur.
            // (Yalnız `AVPlayerViewController.view.userInteractionEnabled = false` bəs etmir:
            // toxunuşu udan interop konteyneridir, uşaq görünüş yox.)
            properties = UIKitInteropProperties(interactionMode = null),
        )
    }
}

private const val POSITION_POLL_SECONDS = 0.06
