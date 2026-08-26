package com.cafarovceyxun.anamuslim.utils.reader.atlas

import android.content.Context
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.utils.reader.isPrebuiltAtlas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Android [AtlasBundleLoader]: assembles a [QuranAtlasBundle] over [QuranAtlasTextureStore].
 *
 * Only the texture store is Android-specific — it decodes to `ALPHA_8`, sizes its cache in bytes
 * against the heap and hands raw `Bitmap`s to the home-screen widget rasterizer. Importing is the
 * shared [SharedAtlasImporter]; iOS uses [SharedAtlasBundleLoader] with the same importer.
 * Registered in `QuranApp.onCreate` via `QuranAtlasLoader.setLoader(...)`.
 */
class AndroidAtlasBundleLoader(
    private val appContext: Context,
) : AtlasBundleLoader {

    private val bundleCache = ConcurrentHashMap<String, QuranAtlasBundle>()

    override suspend fun loadBundle(
        db: ExternalQuranDatabase,
        bundleKey: String,
    ): QuranAtlasBundle? = withContext(Dispatchers.IO) {
        bundleCache[bundleKey]?.let { return@withContext it }

        var ent = db.atlasWordShapeDao().getBundleByKey(bundleKey)

        if (ent == null && bundleKey.isPrebuiltAtlas()) {
            try {
                withContext(Dispatchers.Main) { QuranAtlasLoader.isImporting.value = true }

                // The prebuilt archive moved from `app/assets` into shared `composeResources`,
                // so both platforms now import the very same bytes through the shared importer.
                val zipBytes = Res.readBytes(prebuiltAtlasResourcePath(bundleKey))
                ent = SharedAtlasImporter.importFromBytes(zipBytes, bundleKey, db)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { QuranAtlasLoader.isImporting.value = false }
            }
        }

        val entity = ent ?: return@withContext null

        try {
            val meta = atlasJson.decodeFromString<AtlasMetaRoot>(entity.metaJson)
            val layer = atlasJson.decodeFromString<AtlasLayerJson>(entity.layerJson)

            val textureFiles = linkedMapOf<Int, java.io.File>()

            for (t in layer.textures.sortedBy { it.index }) {
                val imageFile = java.io.File(AtlasFiles.textureFile(bundleKey, t.index).toString())

                if (!imageFile.isFile || imageFile.length() == 0L) {
                    return@withContext null
                }

                textureFiles[t.index] = imageFile
            }

            if (textureFiles.isEmpty()) {
                return@withContext null
            }

            val bundle = QuranAtlasBundle(
                db,
                key = bundleKey,
                meta = meta,
                layer = layer,
                textureSource = QuranAtlasTextureStore(textureFiles),
            )

            bundleCache[bundleKey] = bundle
            bundle
        } catch (e: Exception) {
            null
        }
    }

    override fun trimTextures() {
        bundleCache.values.forEach { it.clearTextureCache() }
    }

    override fun clearCache() {
        bundleCache.values.forEach {
            it.clearTextureCache()
            it.clearShapeCache()
        }
        bundleCache.clear()
    }
}
