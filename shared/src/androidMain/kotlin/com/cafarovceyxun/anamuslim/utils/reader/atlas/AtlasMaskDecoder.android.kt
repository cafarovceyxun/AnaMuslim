package com.cafarovceyxun.anamuslim.utils.reader.atlas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** `ALPHA_8` turns the greyscale page into a mask — the same decode `QuranAtlasTextureStore` uses. */
internal actual fun decodeAtlasMask(bytes: ByteArray): ImageBitmap? {
    val options = BitmapFactory.Options().apply {
        inScaled = false
        inPreferredConfig = Bitmap.Config.ALPHA_8
    }

    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}
