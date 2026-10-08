package com.cafarovceyxun.anamuslim.compose.components.media

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

internal actual fun ImageBitmap.encodeJpeg(quality: Int): ByteArray? = runCatching {
    ByteArrayOutputStream().use { stream ->
        asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }
}.getOrNull()?.takeIf { it.isNotEmpty() }
