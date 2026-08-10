package com.cafarovceyxun.anamuslim.utils.reader.atlas

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes an atlas texture page into an **alpha mask**.
 *
 * The pages are 8-bit greyscale PNGs without an alpha channel; the reader tints them with
 * `ColorFilter.tint`, which only scales alpha. So the luminance has to become the alpha channel —
 * a plain RGBA decode yields opaque blocks instead of glyph shapes. Android has done this all
 * along by decoding straight into `ALPHA_8`; iOS needs the equivalent conversion.
 */
internal expect fun decodeAtlasMask(bytes: ByteArray): ImageBitmap?
