package com.cafarovceyxun.anamuslim.utils.univ

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
actual fun rememberImagePicker(onPicked: (ImageBitmap?) -> Unit): ImagePicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)

    // `GetContent` yerinə `PickVisualMedia` daha müasirdir, amma o, activity kitabxanasının
    // versiyasından asılıdır; burada seçilən yeganə şey bir şəkildir, ona görə hər versiyada
    // mövcud olan müqavilə saxlanılır.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            currentOnPicked(null)
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeDownsampled(context, uri) }
            currentOnPicked(bitmap)
        }
    }

    return remember(launcher) {
        object : ImagePicker {
            override fun pick() = launcher.launch("image/*")
        }
    }
}

/**
 * İki keçidli oxuma: əvvəlcə yalnız ölçülər, sonra `inSampleSize` ilə kiçildilmiş piksellər.
 * Tam ölçülü qalereya şəklini yaddaşa almaq 12MP-də ~48 MB tutur və kart onsuz da 1080px-dir.
 */
private fun decodeDownsampled(context: Context, uri: Uri): ImageBitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    val largest = max(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (largest / sample > PICKED_IMAGE_MAX_DIMENSION) sample *= 2

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver
        .openInputStream(uri)
        ?.use { BitmapFactory.decodeStream(it, null, options) }
        ?.asImageBitmap()
} catch (e: Exception) {
    AppLogger.saveError(e, "ImagePicker.decode")
    null
}
