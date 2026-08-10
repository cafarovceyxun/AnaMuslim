package com.cafarovceyxun.anamuslim.utils.reader.atlas

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceGray
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

/**
 * Draws the page into an 8-bit **grey** Core Graphics context, which yields one luminance byte per
 * pixel, and installs those bytes as a Skia `ALPHA_8` bitmap — so the glyph coverage ends up in
 * the alpha channel, the way the renderer's `ColorFilter.tint` expects.
 *
 * Going through a grey context (rather than reading RGBA and repacking) keeps the temporary buffer
 * at one byte per pixel: these pages are ~14 megapixels, so an RGBA round-trip would spike ~56 MB.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun decodeAtlasMask(bytes: ByteArray): ImageBitmap? {
    if (bytes.isEmpty()) return null

    val data = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }

    val cgImage = UIImage(data = data).CGImage ?: return null

    val width = CGImageGetWidth(cgImage).toInt()
    val height = CGImageGetHeight(cgImage).toInt()
    if (width <= 0 || height <= 0) return null

    val mask = ByteArray(width * height)

    mask.usePinned { pinned ->
        val colorSpace = CGColorSpaceCreateDeviceGray()
        val context = CGBitmapContextCreate(
            data = pinned.addressOf(0),
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = width.toULong(),
            space = colorSpace,
            bitmapInfo = CGImageAlphaInfo.kCGImageAlphaNone.value,
        )

        CGContextDrawImage(
            context,
            CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
            cgImage,
        )

        CGContextRelease(context)
        CGColorSpaceRelease(colorSpace)
    }

    val bitmap = Bitmap()
    val info = ImageInfo(width, height, ColorType.ALPHA_8, ColorAlphaType.PREMUL)

    if (!bitmap.installPixels(info, mask, width)) return null

    bitmap.setImmutable()

    return bitmap.asComposeImageBitmap()
}
