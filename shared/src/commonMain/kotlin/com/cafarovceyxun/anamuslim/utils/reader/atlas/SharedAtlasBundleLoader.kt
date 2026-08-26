package com.cafarovceyxun.anamuslim.utils.reader.atlas

import androidx.compose.ui.graphics.ImageBitmap
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.isPrebuiltAtlas
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path

/** Decoded texture pages kept resident; the reader only touches a handful at a time. */
private const val TEXTURE_CACHE_SIZE = 8

/**
 * Texture pages read straight from the atlas directory and decoded through [decodeAtlasMask].
 *
 * ⚠️ These PNGs are **8-bit greyscale with no alpha channel**: the glyph coverage is the
 * luminance, and the renderer paints them with `ColorFilter.tint`, which multiplies the *alpha*.
 * A plain RGBA decode therefore produces fully opaque rectangles — exactly what iOS drew before
 * this seam existed. Both platforms must hand the renderer a real alpha mask.
 *
 * Android keeps its own `QuranAtlasTextureStore` instead: it decodes to `ALPHA_8` (a quarter of
 * the memory for a mask texture), sizes its cache in bytes against the heap, and hands raw
 * `Bitmap`s to the home-screen widget rasterizer. None of that has a common equivalent, and
 * regressing phone memory use to share code would be a bad trade.
 */
class SharedAtlasTextureSource(
    private val filesByIndex: Map<Int, Path>,
) : AtlasTextureSource {

    private val lock = ReentrantLock()
    private val cache = LinkedHashMap<Int, ImageBitmap>()

    override val size: Int
        get() = filesByIndex.size

    override val onlyIndex: Int?
        get() = filesByIndex.keys.singleOrNull()

    override fun imageBitmapForIndex(index: Int): ImageBitmap? {
        // Re-insert on hit so the eldest entry stays the least recently used one (FontResolver).
        lock.withLock { cache.remove(index)?.also { cache[index] = it } }?.let { return it }

        val path = filesByIndex[index] ?: return null
        if ((AppFileSystem.size(path) ?: 0L) <= 0L) return null

        val decoded = try {
            decodeAtlasMask(AppFileSystem.readBytes(path))
        } catch (e: Exception) {
            AppLogger.saveError(e, "SharedAtlasTextureSource.decode($index)")
            null
        } ?: return null

        return lock.withLock {
            cache[index]?.let { return@withLock it }

            cache[index] = decoded
            if (cache.size > TEXTURE_CACHE_SIZE) {
                cache.keys.firstOrNull()?.let { cache.remove(it) }
            }

            decoded
        }
    }

    override suspend fun prefetch(indices: Collection<Int>) {
        val missing = indices.distinct().filter { lock.withLock { cache[it] } == null }
        if (missing.isEmpty()) return

        withContext(Dispatchers.IO) {
            missing.forEach { imageBitmapForIndex(it) }
        }
    }

    override fun clear() {
        lock.withLock { cache.clear() }
    }
}

/**
 * Multiplatform [AtlasBundleLoader]: assembles a [QuranAtlasBundle] from the imported bundle row
 * plus the texture pages on disk, importing the prebuilt archive on first use.
 *
 * The prebuilt zip ships in shared `composeResources` (it used to be an Android asset), so both
 * platforms read the exact same bytes through [Res.readBytes].
 */
class SharedAtlasBundleLoader : AtlasBundleLoader {

    private val lock = ReentrantLock()
    private val bundleCache = mutableMapOf<String, QuranAtlasBundle>()

    override suspend fun loadBundle(
        db: ExternalQuranDatabase,
        bundleKey: String,
    ): QuranAtlasBundle? = withContext(Dispatchers.IO) {
        lock.withLock { bundleCache[bundleKey] }?.let { return@withContext it }

        var entity = db.atlasWordShapeDao().getBundleByKey(bundleKey)

        if (entity == null && bundleKey.isPrebuiltAtlas()) {
            entity = importPrebuilt(db, bundleKey)
        }

        val row = entity ?: return@withContext null

        try {
            val meta = atlasJson.decodeFromString<AtlasMetaRoot>(row.metaJson)
            val layer = atlasJson.decodeFromString<AtlasLayerJson>(row.layerJson)

            val textureFiles = linkedMapOf<Int, Path>()

            for (texture in layer.textures.sortedBy { it.index }) {
                val file = AtlasFiles.textureFile(bundleKey, texture.index)

                // A bundle row without its textures is unusable; report "not available" so the
                // reader falls back to font rendering rather than drawing empty glyphs.
                if ((AppFileSystem.size(file) ?: 0L) <= 0L) return@withContext null

                textureFiles[texture.index] = file
            }

            if (textureFiles.isEmpty()) return@withContext null

            val bundle = QuranAtlasBundle(
                db = db,
                key = bundleKey,
                meta = meta,
                layer = layer,
                textureSource = SharedAtlasTextureSource(textureFiles),
            )

            lock.withLock { bundleCache.getOrPut(bundleKey) { bundle } }
        } catch (e: Exception) {
            AppLogger.saveError(e, "SharedAtlasBundleLoader.loadBundle($bundleKey)")
            null
        }
    }

    private suspend fun importPrebuilt(
        db: ExternalQuranDatabase,
        bundleKey: String,
    ) = try {
        QuranAtlasLoader.isImporting.value = true

        val zipBytes = Res.readBytes(prebuiltAtlasResourcePath(bundleKey))
        SharedAtlasImporter.importFromBytes(zipBytes, bundleKey, db)
    } catch (e: Exception) {
        AppLogger.saveError(e, "SharedAtlasBundleLoader.importPrebuilt($bundleKey)")
        null
    } finally {
        QuranAtlasLoader.isImporting.value = false
    }

    override fun trimTextures() {
        lock.withLock {
            bundleCache.values.forEach { it.clearTextureCache() }
        }
    }

    override fun clearCache() {
        lock.withLock {
            bundleCache.values.forEach {
                it.clearTextureCache()
                it.clearShapeCache()
            }
            bundleCache.clear()
        }
    }
}

/**
 * Compose Resources path of a prebuilt atlas archive.
 *
 * ⚠️ `Res.readBytes` paths are plain strings the compiler never checks — packaging has to be
 * verified by hand after any move (the `chapter_info` lesson).
 */
fun prebuiltAtlasResourcePath(bundleKey: String): String = "files/atlas/$bundleKey/6x.zip"
