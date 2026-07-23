package com.cafarovceyxun.anamuslim.utils.reader.atlas

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform seam for atlas texture pages. The reader render path only needs a Compose
 * [ImageBitmap] per texture index; the concrete decoder (Android `BitmapFactory`, iOS Skia)
 * lives in each platform's source set and is supplied through [AtlasBundleLoader].
 */
interface AtlasTextureSource {
    val size: Int

    /** The single texture index when this atlas has exactly one page, else `null`. */
    val onlyIndex: Int?

    /** Decoded (and cached) texture for [index], or `null` if the page is missing/unreadable. */
    fun imageBitmapForIndex(index: Int): ImageBitmap?

    /** Warms the cache for [indices] off the main thread. */
    suspend fun prefetch(indices: Collection<Int>)

    fun clear()
}
