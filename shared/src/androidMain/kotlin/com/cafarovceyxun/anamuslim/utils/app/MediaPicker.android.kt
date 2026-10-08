package com.cafarovceyxun.anamuslim.utils.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaCodecInfo
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.InAppMuxer
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Photo Picker (`PickVisualMedia`) — qalereya icazəsi tələb etmir, sistem seçici yalnız seçilmiş
 * faylı verir. Ona görə manifestə `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` əlavə etmək lazım deyil.
 *
 * Video redaktora ([PickedVideo]) gedir və «Hazır»-da **sıxışdırılır** (media3 `Transformer`, HEVC,
 * 720p, 30 fps) — səbəbi [MediaPickLimits.MAX_UPLOAD_BYTES]-ın yanındadır. Böyük şəkil isə
 * kiçildilib JPEG kimi yazılır.
 */
@Composable
internal actual fun rememberPlatformMediaPicker(onResult: (PlatformMediaPick) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult

        scope.launch {
            onResult(context.applicationContext.readPicked(uri))
        }
    }

    return remember(launcher) {
        {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )
        }
    }
}

private suspend fun Context.readPicked(uri: Uri): PlatformMediaPick {
    val mimeType = withContext(Dispatchers.IO) { contentResolver.getType(uri) } ?: "image/jpeg"

    if (mimeType.startsWith("video/")) return readPickedVideo(uri)

    return when (val result = readPickedImage(uri, mimeType)) {
        is MediaPickResult.Picked -> PlatformMediaPick.Image(result.media)
        else -> PlatformMediaPick.Done(result)
    }
}

private suspend fun Context.readPickedImage(uri: Uri, mimeType: String): MediaPickResult {
    val bytes = withContext(Dispatchers.IO) {
        runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }
            .onFailure { AppLogger.d(TAG, "Read failed: ${it.message}") }
            .getOrNull()
    } ?: return MediaPickResult.Failed

    if (bytes.isEmpty()) return MediaPickResult.Failed
    if (bytes.size > MediaPickLimits.MAX_BYTES) return MediaPickResult.TooLarge

    // Kiçiltmə alınmasa (məs. tanınmayan format) orijinal gedir — şəkil üçün sərt qapı yoxdur,
    // bucket-in öz limiti onsuz da tutur.
    val downscaled = withContext(Dispatchers.Default) { downscaleImage(bytes) }
    return MediaPickResult.Picked(
        downscaled?.let { PickedMedia(it, "image/jpeg", isVideo = false) }
            ?: PickedMedia(bytes, mimeType, isVideo = false),
    )
}

/**
 * Qısa kənarı [MediaPickLimits.IMAGE_MAX_SHORT_SIDE]-a endirir və JPEG kimi yazır. Lazım deyilsə
 * (kiçik və yüngül fayl) `null` — orijinal olduğu kimi gedir.
 *
 * `BitmapFactory` EXIF fırlanmasını tətbiq etmir: kamera şəkli yan düşməsin deyə əl ilə çevrilir.
 */
private fun downscaleImage(bytes: ByteArray): ByteArray? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) return null

    val target = MediaPickLimits.scaledSize(width, height, MediaPickLimits.IMAGE_MAX_SHORT_SIDE)
    // EXIF ilə fırlanmış foto həmişə yenidən yazılır: redaktor və hekayə pleyeri baytları EXIF-siz
    // açır, orijinal getsə şəkil yan düşər.
    val rotation = exifRotationDegrees(bytes)
    if (target == null && rotation == 0 && bytes.size <= MediaPickLimits.IMAGE_PASSTHROUGH_BYTES) return null
    val (targetWidth, targetHeight) = target ?: (width to height)

    // Əvvəl 2-nin qüvvəti ilə kobud kiçiltmə (yaddaş üçün), sonra dəqiq ölçüyə.
    var sampleSize = 1
    while (width / (sampleSize * 2) >= targetWidth && height / (sampleSize * 2) >= targetHeight) {
        sampleSize *= 2
    }
    val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return null

    val matrix = Matrix().apply {
        postScale(
            targetWidth.toFloat() / decoded.width,
            targetHeight.toFloat() / decoded.height,
        )
        rotation.takeIf { it != 0 }?.let { postRotate(it.toFloat()) }
    }
    val output = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (output !== decoded) decoded.recycle()

    // Şəffaf PNG JPEG-də qara fon alır — hekayə şəkli üçün ağ fon daha təbiidir.
    val opaque = if (output.hasAlpha()) {
        Bitmap.createBitmap(output.width, output.height, Bitmap.Config.ARGB_8888).also { background ->
            Canvas(background).apply {
                drawColor(Color.WHITE)
                drawBitmap(output, 0f, 0f, null)
            }
            output.recycle()
        }
    } else {
        output
    }

    ByteArrayOutputStream().use { stream ->
        opaque.compress(Bitmap.CompressFormat.JPEG, MediaPickLimits.IMAGE_JPEG_QUALITY, stream)
        opaque.recycle()
        stream.toByteArray()
    }.takeIf { it.isNotEmpty() && (target != null || rotation != 0 || it.size < bytes.size) }
    // ↑ Ölçü dəyişmədisə və yenidən kodlama faylı böyütdüsə orijinal qalır.
}.onFailure {
    AppLogger.d(TAG, "Image downscale failed: ${it.message}")
}.getOrNull()

private fun exifRotationDegrees(bytes: ByteArray): Int = runCatching {
    when (
        ExifInterface(bytes.inputStream()).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}.getOrDefault(0)

private suspend fun Context.readPickedVideo(uri: Uri): PlatformMediaPick {
    // Uzunluq faylı oxumazdan **əvvəl** yoxlanılır.
    val info = withContext(Dispatchers.IO) { videoInfo(uri) }
        ?: return PlatformMediaPick.Done(MediaPickResult.Failed)
    val durationMillis = info.durationMillis ?: 0L
    if (durationMillis > MediaPickLimits.MAX_SOURCE_VIDEO_MILLIS) {
        return PlatformMediaPick.Done(MediaPickResult.TooLong)
    }
    return PlatformMediaPick.Video(AndroidPickedVideo(this, uri, info))
}

/**
 * Photo Picker-in verdiyi `content://` icazəsi Activity yaşadıqca qüvvədədir — redaktor eyni
 * Activity-dədir, ona görə fayl kopyalanmır: önizləmə də, kodlama da birbaşa uri-dən oxuyur.
 */
private class AndroidPickedVideo(
    private val context: Context,
    private val uri: Uri,
    private val info: VideoInfo,
) : PickedVideo {
    override val durationMillis: Long = info.durationMillis ?: 0L
    override val width: Int = info.width
    override val height: Int = info.height
    override val playbackUrl: String = uri.toString()

    override suspend fun thumbnails(count: Int, maxHeightPx: Int): List<ByteArray> =
        withContext(Dispatchers.IO) { context.videoThumbnails(uri, durationMillis, count, maxHeightPx) }

    override suspend fun export(edit: VideoEdit, onProgress: (Float) -> Unit): MediaPickResult {
        // Sıxışdırma alınmasa xam faylı yükləmirik: səssizcə 20-30 MB göndərmək məhz bizi kvotadan
        // çıxaran davranışdır, ona görə admin xətanı görsün.
        val compressed = context.compressVideo(uri, info, edit, onProgress) ?: return MediaPickResult.Failed

        return when {
            compressed.isEmpty() -> MediaPickResult.Failed
            compressed.size > MediaPickLimits.MAX_UPLOAD_BYTES -> MediaPickResult.StillTooLarge
            else -> MediaPickResult.Picked(PickedMedia(compressed, "video/mp4", isVideo = true))
        }
    }

    override fun discard() = Unit
}

private fun Context.videoThumbnails(
    uri: Uri,
    durationMillis: Long,
    count: Int,
    maxHeightPx: Int,
): List<ByteArray> = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(this, uri)
        (0 until count).mapNotNull { index ->
            val timeUs = durationMillis * 1000 * (2 * index + 1) / (2 * count)
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return@mapNotNull null
            val scale = (maxHeightPx.toFloat() / frame.height).coerceAtMost(1f)
            val scaled = Bitmap.createScaledBitmap(
                frame,
                (frame.width * scale).toInt().coerceAtLeast(1),
                (frame.height * scale).toInt().coerceAtLeast(1),
                true,
            )
            if (scaled !== frame) frame.recycle()
            ByteArrayOutputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, stream)
                scaled.recycle()
                stream.toByteArray()
            }
        }
    } finally {
        retriever.release()
    }
}.onFailure {
    AppLogger.d(TAG, "Thumbnails failed: ${it.message}")
}.getOrDefault(emptyList())

private suspend fun Context.compressVideo(
    uri: Uri,
    info: VideoInfo,
    edit: VideoEdit,
    onProgress: (Float) -> Unit,
): ByteArray? {
    val output = File(cacheDir, "picked-video-${System.currentTimeMillis()}.mp4")
    return try {
        val bitrate = MediaPickLimits.targetVideoBitrate(edit.durationMillis)
        // `Transformer` Looper tələb edir — start/callback/irəliləyiş əsas axındadır, fayl isə IO-da.
        withContext(Dispatchers.Main) {
            transcode(uri, output.absolutePath, bitrate, info, edit, onProgress)
        }
        onProgress(1f)
        withContext(Dispatchers.IO) { output.takeIf { it.length() > 0 }?.readBytes() }
    } catch (e: Exception) {
        AppLogger.d(TAG, "Compression failed: ${e.message}")
        null
    } finally {
        withContext(Dispatchers.IO) { output.delete() }
    }
}

@OptIn(UnstableApi::class)
private suspend fun Context.transcode(
    uri: Uri,
    outputPath: String,
    bitrate: Int,
    info: VideoInfo,
    edit: VideoEdit,
    onProgress: (Float) -> Unit,
) {
    val completion = CompletableDeferred<Unit>()
    val transformer = Transformer.Builder(this)
        // HEVC kodlayıcısı olmayan cihazda `DefaultEncoderFactory` (fallback default açıqdır)
        // H.264-ə düşür; H.264-də isə profili özü «High»-a qaldırır (API 29+).
        .setVideoMimeType(MimeTypes.VIDEO_H265)
        .setAudioMimeType(MimeTypes.AUDIO_AAC)
        .setEncoderFactory(
            DefaultEncoderFactory.Builder(this)
                .setRequestedVideoEncoderSettings(
                    VideoEncoderSettings.Builder()
                        .setBitrate(bitrate)
                        // Sabit bitrate sakit kadra da eyni bit verir; VBR onu hərəkətə saxlayır.
                        .setBitrateMode(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                        .build(),
                )
                .build(),
        )
        // Platformanın `MediaMuxer`-i `moov`-u faylın SONUNA yazır — pleyer oynatmazdan əvvəl
        // faylın axırını ayrıca istəməli olur, keşli iOS pleyeri isə bütün faylı gözləyir.
        // media3-ün öz muxer-i onu əvvələ qoyur («fast start»).
        .setMuxerFactory(InAppMuxer.Factory.Builder().build())
        .addListener(
            object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    completion.complete(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    completion.completeExceptionally(exportException)
                }
            },
        )
        .build()

    val mediaItem = MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(edit.startMillis)
                .setEndPositionMs(edit.endMillis)
                .build(),
        )
        .build()

    val item = EditedMediaItem.Builder(mediaItem)
        .setRemoveAudio(edit.muted)
        .setEffects(Effects(emptyList(), videoEffects(info, edit.crop)))
        .build()

    transformer.start(item, outputPath)

    coroutineScope {
        val polling = launch {
            val holder = ProgressHolder()
            while (true) {
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress / 100f)
                }
                delay(PROGRESS_POLL_MILLIS)
            }
        }
        try {
            completion.await()
        } catch (e: CancellationException) {
            // Redaktor bağlanıbsa kodlayıcı arxa fonda boşuna işləməsin.
            transformer.cancel()
            throw e
        } finally {
            polling.cancel()
        }
    }
}

/**
 * Effektlər dekoderdən **sonra** işləyir, dekoder isə kadrı artıq ekrandakı istiqamətə çevirir —
 * ona görə ölçü [VideoInfo]-nun fırlanmış (göstərilən) ölçüsündən hesablanır.
 */
@OptIn(UnstableApi::class)
private fun videoEffects(info: VideoInfo, crop: VideoCrop): List<Effect> = buildList {
    // Mənbə 30 fps-dən yavaşdırsa heç bir kadr atılmır (hər kadr hədəf intervala ən yaxındır).
    add(FrameDropEffect.createDefaultFrameDropEffect(MediaPickLimits.TARGET_VIDEO_FRAME_RATE.toFloat()))

    // `Crop` NDC koordinatlarındadır (-1..1, y yuxarı), bizim paylar isə yuxarı-sol mənşəlidir.
    if (!crop.isFull) {
        add(
            Crop(
                crop.left * 2 - 1,
                crop.right * 2 - 1,
                1 - crop.bottom * 2,
                1 - crop.top * 2,
            ),
        )
    }

    val width = ((crop.right - crop.left) * info.width).toInt()
    val height = ((crop.bottom - crop.top) * info.height).toInt()
    val presentation = if (width <= 0 || height <= 0) {
        // Ölçü oxunmadı — portret fərziyyəsi ilə uzun kənar.
        Presentation.createForHeight(MediaPickLimits.TARGET_VIDEO_SHORT_SIDE * 16 / 9)
    } else {
        // Kəsilmiş kadrın ölçüsü təkdirsə kodlayıcı üçün cütə yuvarlaqlaşdırılır.
        val (targetWidth, targetHeight) =
            MediaPickLimits.scaledSize(width, height, MediaPickLimits.TARGET_VIDEO_SHORT_SIDE)
                ?: ((width / 2 * 2).coerceAtLeast(2) to (height / 2 * 2).coerceAtLeast(2))
        Presentation.createForWidthAndHeight(targetWidth, targetHeight, Presentation.LAYOUT_SCALE_TO_FIT)
    }
    add(presentation)
}

/** Göstərilən ölçü — [width]/[height] fırlanma tətbiq olunmuş haldadır. */
private class VideoInfo(val durationMillis: Long?, val width: Int, val height: Int)

private fun Context.videoInfo(uri: Uri): VideoInfo? = runCatching {
    // `use` yox: MediaMetadataRetriever yalnız API 29-dan AutoCloseable-dır, minSdk isə 24.
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(this, uri)
        fun int(key: Int) = retriever.extractMetadata(key)?.toIntOrNull() ?: 0
        val width = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val rotated = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) % 180 != 0
        VideoInfo(
            durationMillis = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull(),
            width = if (rotated) height else width,
            height = if (rotated) width else height,
        )
    } finally {
        retriever.release()
    }
}.onFailure {
    AppLogger.d(TAG, "Metadata read failed: ${it.message}")
}.getOrNull()

private const val THUMBNAIL_QUALITY = 80
private const val PROGRESS_POLL_MILLIS = 200L
private const val TAG = "MediaPicker"
