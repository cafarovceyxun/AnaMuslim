package com.cafarovceyxun.anamuslim.utils.app

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.mediaCompressing
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Photo Picker (`PickVisualMedia`) — qalereya icazəsi tələb etmir, sistem seçici yalnız seçilmiş
 * faylı verir. Ona görə manifestə `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` əlavə etmək lazım deyil.
 *
 * Video qaytarılmazdan əvvəl **sıxışdırılır** (media3 `Transformer`) — səbəbi
 * [MediaPickLimits.MAX_UPLOAD_BYTES]-ın yanındadır.
 */
@Composable
actual fun rememberMediaPicker(onResult: (MediaPickResult) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sıxışdırma bir neçə saniyə çəkir və seçici bağlanandan sonra ekranda heç nə dəyişmir —
    // mətn əvvəlcədən oxunur, `getString` (suspend) yox (bax CLAUDE.md, `rememberCoroutineScope`).
    val compressingMessage = stringResource(Res.string.mediaCompressing)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult

        scope.launch {
            onResult(context.readPicked(uri, compressingMessage))
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

private suspend fun Context.readPicked(uri: Uri, compressingMessage: String): MediaPickResult {
    val mimeType = withContext(Dispatchers.IO) { contentResolver.getType(uri) } ?: "image/jpeg"

    if (mimeType.startsWith("video/")) return readPickedVideo(uri, compressingMessage)

    val bytes = withContext(Dispatchers.IO) {
        runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }
            .onFailure { AppLogger.d(TAG, "Read failed: ${it.message}") }
            .getOrNull()
    } ?: return MediaPickResult.Failed

    if (bytes.isEmpty()) return MediaPickResult.Failed
    if (bytes.size > MediaPickLimits.MAX_BYTES) return MediaPickResult.TooLarge

    return MediaPickResult.Picked(PickedMedia(bytes, mimeType, isVideo = false))
}

private suspend fun Context.readPickedVideo(
    uri: Uri,
    compressingMessage: String,
): MediaPickResult {
    // Uzunluq baytları oxumazdan **əvvəl** yoxlanılır: uzun video yaddaşa çəkilməsin.
    val durationMillis = withContext(Dispatchers.IO) { videoDurationMillis(uri) }
    if (durationMillis != null && durationMillis > MediaPickLimits.MAX_VIDEO_MILLIS) {
        return MediaPickResult.TooLong
    }

    PlatformUtils.showLongToast(compressingMessage)

    // Sıxışdırma alınmasa xam faylı yükləmirik: səssizcə 20-30 MB göndərmək məhz bizi kvotadan
    // çıxaran davranışdır, ona görə admin xətanı görsün.
    val compressed = compressVideo(uri, durationMillis) ?: return MediaPickResult.Failed

    return when {
        compressed.isEmpty() -> MediaPickResult.Failed
        compressed.size > MediaPickLimits.MAX_UPLOAD_BYTES -> MediaPickResult.StillTooLarge
        else -> MediaPickResult.Picked(PickedMedia(compressed, "video/mp4", isVideo = true))
    }
}

private suspend fun Context.compressVideo(uri: Uri, durationMillis: Long?): ByteArray? {
    val output = File(cacheDir, "picked-video-${System.currentTimeMillis()}.mp4")
    return try {
        // `Transformer` Looper tələb edir — start/callback əsas axındadır, fayl oxumaq isə IO-da.
        withContext(Dispatchers.Main) {
            transcode(uri, output.absolutePath, targetVideoBitrate(durationMillis))
        }
        withContext(Dispatchers.IO) { output.takeIf { it.length() > 0 }?.readBytes() }
    } catch (e: Exception) {
        AppLogger.d(TAG, "Compression failed: ${e.message}")
        null
    } finally {
        withContext(Dispatchers.IO) { output.delete() }
    }
}

@OptIn(UnstableApi::class)
private suspend fun Context.transcode(uri: Uri, outputPath: String, bitrate: Int) {
    suspendCancellableCoroutine { continuation ->
        val transformer = Transformer.Builder(this)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(
                DefaultEncoderFactory.Builder(this)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder().setBitrate(bitrate).build(),
                    )
                    .build(),
            )
            .addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                },
            )
            .build()

        val item = EditedMediaItem.Builder(MediaItem.fromUri(uri))
            .setEffects(
                Effects(
                    emptyList(),
                    listOf(Presentation.createForHeight(MediaPickLimits.TARGET_VIDEO_HEIGHT)),
                ),
            )
            .build()

        transformer.start(item, outputPath)
    }
}

/**
 * Bitrate **uzunluqdan** hesablanır: hədəf sabit ölçüdür, yəni 10 saniyəlik yazı da, iki
 * dəqiqəlik yazı da təxminən eyni fayl həcminə düşür. Sabit bitrate uzun videonu yenidən
 * onlarla MB edərdi.
 */
private fun targetVideoBitrate(durationMillis: Long?): Int {
    val seconds = ((durationMillis ?: 0L) / 1000.0).coerceAtLeast(1.0)
    val budget = (MediaPickLimits.TARGET_VIDEO_BYTES * 8 / seconds).toInt() - AUDIO_BITRATE
    return budget.coerceIn(MIN_VIDEO_BITRATE, MAX_VIDEO_BITRATE)
}

private fun Context.videoDurationMillis(uri: Uri): Long? = runCatching {
    // `use` yox: MediaMetadataRetriever yalnız API 29-dan AutoCloseable-dır, minSdk isə 24.
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(this, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } finally {
        retriever.release()
    }
}.onFailure {
    AppLogger.d(TAG, "Duration read failed: ${it.message}")
}.getOrNull()

private const val AUDIO_BITRATE = 96_000
private const val MIN_VIDEO_BITRATE = 400_000
private const val MAX_VIDEO_BITRATE = 1_500_000
private const val TAG = "MediaPicker"
