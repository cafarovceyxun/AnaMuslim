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
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Photo Picker (`PickVisualMedia`) — qalereya icazəsi tələb etmir, sistem seçici yalnız seçilmiş
 * faylı verir. Ona görə manifestə `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` əlavə etmək lazım deyil.
 */
@Composable
actual fun rememberMediaPicker(onResult: (MediaPickResult) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult

        scope.launch {
            val result = withContext(Dispatchers.IO) { context.readPicked(uri) }
            onResult(result)
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

private fun Context.readPicked(uri: Uri): MediaPickResult {
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
    val isVideo = mimeType.startsWith("video/")

    if (isVideo) {
        val duration = videoDurationMillis(uri)
        if (duration != null && duration > MediaPickLimits.MAX_VIDEO_MILLIS) {
            return MediaPickResult.TooLong
        }
    }

    val bytes = runCatching {
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.onFailure {
        AppLogger.d(TAG, "Read failed: ${it.message}")
    }.getOrNull() ?: return MediaPickResult.Failed

    if (bytes.isEmpty()) return MediaPickResult.Failed
    if (bytes.size > MediaPickLimits.MAX_BYTES) return MediaPickResult.TooLarge

    return MediaPickResult.Picked(PickedMedia(bytes, mimeType, isVideo))
}

/** Uzunluq baytları oxumazdan **əvvəl** yoxlanılır: uzun video yaddaşa çəkilməsin. */
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

private const val TAG = "MediaPicker"
