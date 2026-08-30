package com.cafarovceyxun.anamuslim.utils.app

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
 * Photo Picker (`PickVisualMedia`) — qalereya icazəsi tələb etmir, sistem seçicidən yalnız seçilmiş
 * faylı verir. Ona görə manifestə `READ_MEDIA_IMAGES` əlavə etmək lazım deyil.
 */
@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult

        scope.launch {
            val picked = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@runCatching null
                    PickedImage(bytes, context.contentResolver.getType(uri) ?: "image/jpeg")
                }.onFailure {
                    AppLogger.d("ImagePicker", "Read failed: ${it.message}")
                }.getOrNull()
            }

            picked?.let(onPicked)
        }
    }

    return remember(launcher) {
        {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}
