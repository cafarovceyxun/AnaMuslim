package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVURLAsset
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * `PHPickerViewController` — proses xaricində işləyən sistem seçicisi, ona görə **foto icazəsi
 * istəmir** və `Info.plist`-ə `NSPhotoLibraryUsageDescription` əlavə etmək lazım deyil.
 *
 * Şəkil `public.jpeg` kimi istənilir (iPhone HEIC saxlaya bilər, item provider özü çevirir), video
 * isə fayl kimi: uzunluğu baytları oxumazdan **əvvəl** `AVURLAsset` ilə ölçürük.
 *
 * Video redaktora ([IosPickedVideo]) gedir və «Hazır»-da **sıxışdırılır** ([IosVideoTranscoder] —
 * HEVC, 720p, 30 fps) — səbəbi [MediaPickLimits.MAX_UPLOAD_BYTES]-ın yanındadır. Böyük şəkil isə
 * kiçildilib JPEG kimi yazılır.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberPlatformMediaPicker(onResult: (PlatformMediaPick) -> Unit): (() -> Unit)? {
    val currentOnResult by rememberUpdatedState(onResult)

    // PHPickerViewController.delegate zəif referansdır: delegate-i burada saxlamasaq seçici
    // açılan kimi toplanır və nəticə heç vaxt gəlmir.
    val delegate = remember {
        MediaPickerDelegate { result -> currentOnResult(result) }
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
    private val onResult: (PlatformMediaPick) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    private var active = true

    fun detach() {
        active = false
    }

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)

        // İmtina — heç nə çağırılmır.
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider ?: return

        if (provider.hasItemConformingToTypeIdentifier(MOVIE_UTI)) {
            loadVideo(provider)
        } else {
            loadImage(provider)
        }
    }

    private fun loadImage(provider: NSItemProvider) {
        provider.loadDataRepresentationForTypeIdentifier(JPEG_UTI) { data, error ->
            error?.let { AppLogger.d(TAG, "Image load failed: ${it.localizedDescription}") }
            val bytes = data?.let { downscaleJpeg(it) ?: it }?.toByteArray()
            deliver(bytes, "image/jpeg", isVideo = false)
        }
    }

    private fun loadVideo(provider: NSItemProvider) {
        // `loadFileRepresentation` müvəqqəti fayl verir: uzunluğu oradan ölçürük.
        provider.loadFileRepresentationForTypeIdentifier(MOVIE_UTI) { url, error ->
            error?.let { AppLogger.d(TAG, "Video load failed: ${it.localizedDescription}") }

            if (url == null) {
                post(PlatformMediaPick.Done(MediaPickResult.Failed))
                return@loadFileRepresentationForTypeIdentifier
            }

            val seconds = CMTimeGetSeconds(AVURLAsset(uRL = url, options = null).duration)
            if (seconds.isFinite() && seconds * 1000 > MediaPickLimits.MAX_SOURCE_VIDEO_MILLIS) {
                post(PlatformMediaPick.Done(MediaPickResult.TooLong))
                return@loadFileRepresentationForTypeIdentifier
            }

            // ⚠️ Sistem bu faylı blok qurtaran kimi silir, redaktor isə sonra açılır — əvvəlcə
            // surət çıxarırıq (redaktor bağlananda [IosPickedVideo.discard] silir).
            val video = copyToTemporary(url)?.let(IosPickedVideo::open)
            post(
                video?.let { PlatformMediaPick.Video(it) }
                    ?: PlatformMediaPick.Done(MediaPickResult.Failed),
            )
        }
    }

    private fun copyToTemporary(url: NSURL): NSURL? {
        // Redaktor ekranı tam tutur, yəni eyni anda ancaq biri açıq ola bilər — əvvəlki surətlər
        // (tətbiq redaktor açıqkən öldürülübsə `discard` heç çağırılmayıb) burada silinir.
        purgeStaleSources()
        val extension = url.pathExtension?.takeIf { it.isNotBlank() } ?: "mov"
        val destination = NSURL.fileURLWithPath(
            NSTemporaryDirectory() + "$SOURCE_PREFIX${NSUUID().UUIDString}.$extension",
        )
        val copied = NSFileManager.defaultManager.copyItemAtURL(url, destination, null)
        if (!copied) AppLogger.d(TAG, "Temp copy failed")
        return if (copied) destination else null
    }

    /**
     * Qısa kənarı [MediaPickLimits.IMAGE_MAX_SHORT_SIDE]-a endirir və JPEG kimi yenidən yazır;
     * lazım deyilsə (kiçik, yüngül fayl) və ya nəticə böyük çıxarsa `null` — orijinal gedir.
     * `drawInRect` EXIF istiqamətini özü tətbiq edir. Renderer arxa fon axınında təhlükəsizdir.
     */
    private fun downscaleJpeg(data: NSData): NSData? {
        val image = UIImage.imageWithData(data) ?: return null
        val (width, height) = image.size.useContents {
            (width * image.scale).toInt() to (height * image.scale).toInt()
        }

        val target = MediaPickLimits.scaledSize(width, height, MediaPickLimits.IMAGE_MAX_SHORT_SIDE)
        // EXIF ilə fırlanmış foto həmişə yenidən çəkilir: redaktor və hekayə pleyeri baytları
        // istiqamətsiz açır, orijinal getsə şəkil yan düşər.
        val upright = image.imageOrientation == UIImageOrientation.UIImageOrientationUp
        if (target == null && upright && data.length.toLong() <= MediaPickLimits.IMAGE_PASSTHROUGH_BYTES) {
            return null
        }
        val (targetWidth, targetHeight) = target ?: (width to height)

        val format = UIGraphicsImageRendererFormat.preferredFormat().apply {
            scale = 1.0
            opaque = true
        }
        val renderer = UIGraphicsImageRenderer(
            size = CGSizeMake(targetWidth.toDouble(), targetHeight.toDouble()),
            format = format,
        )
        val encoded = renderer.JPEGDataWithCompressionQuality(
            MediaPickLimits.IMAGE_JPEG_QUALITY / 100.0,
        ) { _ ->
            image.drawInRect(CGRectMake(0.0, 0.0, targetWidth.toDouble(), targetHeight.toDouble()))
        }
        // Ölçü dəyişmədisə və yenidən kodlama faylı böyütdüsə orijinal qalır.
        return encoded.takeIf { target != null || !upright || it.length < data.length }
    }

    private fun purgeStaleSources() {
        val directory = NSTemporaryDirectory()
        NSFileManager.defaultManager.contentsOfDirectoryAtPath(directory, error = null)
            ?.filterIsInstance<String>()
            ?.filter { it.startsWith(SOURCE_PREFIX) }
            ?.forEach { NSFileManager.defaultManager.removeItemAtPath(directory + it, error = null) }
    }

    private fun deliver(bytes: ByteArray?, mimeType: String, isVideo: Boolean) {
        val result = when {
            bytes == null || bytes.isEmpty() -> MediaPickResult.Failed
            bytes.size > MediaPickLimits.MAX_BYTES -> MediaPickResult.TooLarge
            else -> MediaPickResult.Picked(PickedMedia(bytes, mimeType, isVideo))
        }
        post(
            if (result is MediaPickResult.Picked) {
                PlatformMediaPick.Image(result.media)
            } else {
                PlatformMediaPick.Done(result)
            },
        )
    }

    /** Nəticə arxa fon növbəsində gəlir; UI-yə yalnız əsas axından toxunulur. */
    private fun post(result: PlatformMediaPick) {
        dispatch_async(dispatch_get_main_queue()) {
            if (active) onResult(result)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)

    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}

/** HEIC şəkillər də bu UTI ilə istənəndə item provider tərəfindən JPEG-ə çevrilir. */
private const val JPEG_UTI = "public.jpeg"
private const val MOVIE_UTI = "public.movie"
private const val SOURCE_PREFIX = "picked-source-"
private const val TAG = "MediaPicker"
