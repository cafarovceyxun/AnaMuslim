package com.cafarovceyxun.anamuslim.utils.reader.atlas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val ATLAS_TEXTURE_CACHE_FRACTION = 8
private const val ATLAS_TEXTURE_CACHE_MIN_BYTES = 8 * 1024 * 1024
private const val ATLAS_TEXTURE_CACHE_MAX_BYTES = 32 * 1024 * 1024

data class QuranAtlasTexture(
    val bitmap: Bitmap,
    val imageBitmap: ImageBitmap,
)

class QuranAtlasTextureStore(
    private val filesByIndex: Map<Int, File>,
) : AtlasTextureSource {
    // No `recycle()` on eviction, deliberately: the evicted `Bitmap` is the very object the cached
    // `ImageBitmap` wraps, and eviction happens off the main thread (prefetch on Dispatchers.IO,
    // memory trims on the main thread while the widget rasterizer draws raw bitmaps in a worker).
    // Recycling it out from under an in-flight draw is a `trying to use a recycled bitmap` crash;
    // dropping the reference instead lets the next GC reclaim the pixels, which is what the trim
    // path is waiting for anyway.
    private val cache = object : LruCache<Int, QuranAtlasTexture>(cacheSizeBytes()) {
        override fun sizeOf(key: Int, value: QuranAtlasTexture): Int =
            value.bitmap.allocationByteCount
    }

    override val size: Int
        get() = filesByIndex.size

    override val onlyIndex: Int?
        get() = filesByIndex.keys.singleOrNull()

    override fun imageBitmapForIndex(index: Int): ImageBitmap? = get(index)?.imageBitmap

    /** Raw Android bitmap for a texture page — used by the widget rasterizer only. */
    fun androidBitmapForIndex(index: Int): Bitmap? = get(index)?.bitmap

    @Synchronized
    fun get(index: Int): QuranAtlasTexture? {
        cache.get(index)?.let { return it }

        val file = filesByIndex[index]?.takeIf { it.isFile && it.length() > 0L } ?: return null
        val decodeOptions = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ALPHA_8
        }
        val bitmap = BitmapFactory.decodeFile(file.path, decodeOptions) ?: return null
        val texture = QuranAtlasTexture(bitmap, bitmap.asImageBitmap())

        cache.put(index, texture)

        return texture
    }

    override suspend fun prefetch(indices: Collection<Int>) {
        val missing = indices
            .asSequence()
            .distinct()
            .filter { cache.get(it) == null }
            .toList()

        if (missing.isEmpty()) return

        withContext(Dispatchers.IO) {
            for (index in missing) {
                get(index)
            }
        }
    }

    override fun clear() {
        cache.evictAll()
    }

    private fun cacheSizeBytes(): Int {
        val runtime = Runtime.getRuntime()
        val available = (runtime.maxMemory() / ATLAS_TEXTURE_CACHE_FRACTION)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        return available.coerceIn(
            ATLAS_TEXTURE_CACHE_MIN_BYTES,
            ATLAS_TEXTURE_CACHE_MAX_BYTES,
        )
    }
}
