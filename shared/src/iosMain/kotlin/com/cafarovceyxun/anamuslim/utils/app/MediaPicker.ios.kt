package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSData
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * `PHPickerViewController` — proses xaricində işləyən sistem seçicisi, ona görə **foto icazəsi
 * istəmir** və `Info.plist`-ə `NSPhotoLibraryUsageDescription` əlavə etmək lazım deyil.
 *
 * Şəkil `public.jpeg` kimi istənilir (iPhone HEIC saxlaya bilər, item provider özü çevirir), video
 * isə fayl kimi: uzunluğu baytları oxumazdan **əvvəl** `AVURLAsset` ilə ölçürük ki, iki dəqiqədən
 * uzun yazı yaddaşa çəkilməsin.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberMediaPicker(onResult: (MediaPickResult) -> Unit): (() -> Unit)? {
    val currentOnResult by rememberUpdatedState(onResult)

    // PHPickerViewController.delegate zəif referansdır: delegate-i burada saxlamasaq seçici
    // açılan kimi toplanır və nəticə heç vaxt gəlmir.
    val delegate = remember {
        MediaPickerDelegate { result -> result?.let { currentOnResult(it) } }
    }

    DisposableEffect(delegate) {
        onDispose { delegate.detach() }
    }

    return remember(delegate) {
        {
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (root == null) {
                AppLogger.d(TAG, "No root view controller to present from")
            } else {
                val configuration = PHPickerConfiguration().apply {
                    filter = PHPickerFilter.anyFilterMatchingSubfilters(
                        listOf(PHPickerFilter.imagesFilter(), PHPickerFilter.videosFilter()),
                    )
                    selectionLimit = 1
                }

                val controller = PHPickerViewController(configuration)
                controller.delegate = delegate
                root.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class MediaPickerDelegate(
    private val onResult: (MediaPickResult?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    private var active = true

    fun detach() {
        active = false
    }

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (provider == null) {
            onResult(null)
            return
        }

        if (provider.hasItemConformingToTypeIdentifier(MOVIE_UTI)) {
            loadVideo(provider)
        } else {
            loadImage(provider)
        }
    }

    private fun loadImage(provider: NSItemProvider) {
        provider.loadDataRepresentationForTypeIdentifier(JPEG_UTI) { data, error ->
            error?.let { AppLogger.d(TAG, "Image load failed: ${it.localizedDescription}") }
            deliver(data?.toByteArray(), "image/jpeg", isVideo = false)
        }
    }

    private fun loadVideo(provider: NSItemProvider) {
        // `loadFileRepresentation` müvəqqəti fayl verir: uzunluğu oradan ölçüb, yalnız qəbul
        // olunarsa baytları oxuyuruq. Fayl bu blokdan sonra silinir, ona görə iş burada bitir.
        provider.loadFileRepresentationForTypeIdentifier(MOVIE_UTI) { url, error ->
            error?.let { AppLogger.d(TAG, "Video load failed: ${it.localizedDescription}") }

            if (url == null) {
                post(MediaPickResult.Failed)
                return@loadFileRepresentationForTypeIdentifier
            }

            val seconds = CMTimeGetSeconds(AVURLAsset(uRL = url, options = null).duration)
            if (seconds.isFinite() && seconds * 1000 > MediaPickLimits.MAX_VIDEO_MILLIS) {
                post(MediaPickResult.TooLong)
                return@loadFileRepresentationForTypeIdentifier
            }

            val bytes = NSData.dataWithContentsOfURL(url)?.toByteArray()
            deliver(bytes, "video/quicktime", isVideo = true)
        }
    }

    private fun deliver(bytes: ByteArray?, mimeType: String, isVideo: Boolean) {
        val result = when {
            bytes == null || bytes.isEmpty() -> MediaPickResult.Failed
            bytes.size > MediaPickLimits.MAX_BYTES -> MediaPickResult.TooLarge
            else -> MediaPickResult.Picked(PickedMedia(bytes, mimeType, isVideo))
        }
        post(result)
    }

    /** Nəticə arxa fon növbəsində gəlir; UI-yə yalnız əsas axından toxunulur. */
    private fun post(result: MediaPickResult) {
        dispatch_async(dispatch_get_main_queue()) {
            if (active) onResult(result)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)

    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}

/** HEIC şəkillər də bu UTI ilə istənəndə item provider tərəfindən JPEG-ə çevrilir. */
private const val JPEG_UTI = "public.jpeg"
private const val MOVIE_UTI = "public.movie"
private const val TAG = "MediaPicker"
