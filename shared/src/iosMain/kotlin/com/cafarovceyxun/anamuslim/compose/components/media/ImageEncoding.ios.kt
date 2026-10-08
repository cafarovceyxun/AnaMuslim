package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/** Compose iOS-da Skia üzərindədir — kodlayıcı da onundur (bax `ImageBitmapUIImage.ios.kt`). */
internal actual fun ImageBitmap.encodeJpeg(quality: Int): ByteArray? = runCatching {
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes
}.getOrNull()?.takeIf { it.isNotEmpty() }
