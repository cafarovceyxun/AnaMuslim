package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

/**
 * Converts a Compose [ImageBitmap] to a `UIImage` by way of Skia's PNG encoder — Compose
 * Multiplatform on iOS is Skia-backed, so this is the supported bridge (there is no
 * `asUIImage()` in the public API).
 */
@OptIn(ExperimentalForeignApi::class)
internal fun ImageBitmap.toUIImage(): UIImage? {
    val png = Image.makeFromBitmap(asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG)
        ?.bytes
        ?: return null

    if (png.isEmpty()) return null

    val data = png.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = png.size.toULong())
    }
    return UIImage.imageWithData(data)
}
