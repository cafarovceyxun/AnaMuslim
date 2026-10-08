package com.cafarovceyxun.anamuslim.utils.app

import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.duration
import platform.AVFoundation.naturalSize
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.tracksWithMediaType
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectApplyAffineTransform
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Redaktora verilən iOS videosu — seçicinin müvəqqəti faylının **surəti** (sistem orijinalı blok
 * qurtaran kimi silir). Önizləmə də, kodlama da bu fayldan oxuyur; [discard] onu silir.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosPickedVideo private constructor(
    private val file: NSURL,
    override val durationMillis: Long,
    override val width: Int,
    override val height: Int,
) : PickedVideo {

    override val playbackUrl: String = file.absoluteString.orEmpty()

    override suspend fun thumbnails(count: Int, maxHeightPx: Int): List<ByteArray> =
        withContext(Dispatchers.IO) {
            val generator = AVAssetImageGenerator(asset = AVURLAsset(uRL = file, options = null)).apply {
                appliesPreferredTrackTransform = true
                maximumSize = CGSizeMake(maxHeightPx * 2.0, maxHeightPx.toDouble())
            }
            (0 until count).mapNotNull { index ->
                val seconds = durationMillis / 1000.0 * (2 * index + 1) / (2 * count)
                val image = generator.copyCGImageAtTime(
                    requestedTime = CMTimeMakeWithSeconds(seconds, preferredTimescale = 600),
                    actualTime = null,
                    error = null,
                ) ?: return@mapNotNull null
                // `copy…` +1 qaytarır.
                val jpeg = UIImageJPEGRepresentation(UIImage.imageWithCGImage(image), THUMBNAIL_QUALITY)
                CGImageRelease(image)
                jpeg?.toByteArray()
            }
        }

    override suspend fun export(edit: VideoEdit, onProgress: (Float) -> Unit): MediaPickResult {
        val output = NSURL.fileURLWithPath(
            NSTemporaryDirectory() + "picked-video-${NSUUID().UUIDString}.mp4",
        )

        val completed = suspendCancellableCoroutine { continuation ->
            IosVideoTranscoder.transcode(file, output, edit, onProgress) { ok ->
                if (continuation.isActive) continuation.resume(ok)
            }
        }

        // Sakit kadrlarda yeni kadr gəlmir, ona görə irəliləyiş sonda 100%-ə tamamlanır.
        if (completed) onProgress(1f)
        val bytes = if (completed) NSData.dataWithContentsOfURL(output)?.toByteArray() else null
        NSFileManager.defaultManager.removeItemAtURL(output, null)

        // Sıxışdırma alınmasa xam fayl YÜKLƏNMİR: səssizcə 20-30 MB göndərmək məhz bizi egress
        // kvotasından çıxaran davranışdır.
        return when {
            bytes == null || bytes.isEmpty() -> MediaPickResult.Failed
            bytes.size > MediaPickLimits.MAX_UPLOAD_BYTES -> MediaPickResult.StillTooLarge
            else -> MediaPickResult.Picked(PickedMedia(bytes, "video/mp4", isVideo = true))
        }
    }

    override fun discard() {
        NSFileManager.defaultManager.removeItemAtURL(file, null)
    }

    companion object {
        /** Video treki yoxdursa və ya oxunmursa `null` (fayl da silinir). */
        fun open(file: NSURL): IosPickedVideo? {
            val asset = AVURLAsset(uRL = file, options = null)
            val track = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack
            val seconds = CMTimeGetSeconds(asset.duration)
            if (track == null || !seconds.isFinite() || seconds <= 0.0) {
                AppLogger.d(TAG, "Unreadable video")
                NSFileManager.defaultManager.removeItemAtURL(file, null)
                return null
            }

            // Göstərilən ölçü: fırlanma tətbiq olunmuş çərçivə (portret kamera videosu 1920×1080
            // kimi saxlanılır, 90° transform ilə).
            val (width, height) = track.naturalSize.useContents {
                CGRectApplyAffineTransform(CGRectMake(0.0, 0.0, width, height), track.preferredTransform)
            }.useContents { abs(size.width).toInt() to abs(size.height).toInt() }

            return IosPickedVideo(file, (seconds * 1000).toLong(), width, height)
        }

        private const val THUMBNAIL_QUALITY = 0.8
        private const val TAG = "IosPickedVideo"
    }
}
