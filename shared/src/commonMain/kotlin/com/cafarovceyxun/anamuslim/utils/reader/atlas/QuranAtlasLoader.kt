package com.cafarovceyxun.anamuslim.utils.reader.atlas

import androidx.compose.runtime.mutableStateOf
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.utils.reader.atlas.tajweed.TajweedColorSource

/**
 * Platform loader for a fully-built [QuranAtlasBundle]: prebuilt-asset import, texture-page file
 * resolution and image decoding. Registered once at startup — androidMain via `QuranApp.onCreate`,
 * iosMain once the atlas render path lands on iOS.
 */
interface AtlasBundleLoader {
    suspend fun loadBundle(db: ExternalQuranDatabase, bundleKey: String): QuranAtlasBundle?

    /** Evicts decoded texture pages but keeps the bundles — they re-decode from disk on demand. */
    fun trimTextures()

    fun clearCache()
}

/**
 * Platform-neutral facade over the atlas glyph store. Shape lookup / placement decoding is pure
 * (DB + kotlinx.serialization) and lives here; the Context-bound bundle assembly is delegated to
 * the registered [AtlasBundleLoader].
 */
object QuranAtlasLoader {
    /** True while a prebuilt atlas is imported from bundled assets (drives the reader spinner). */
    val isImporting = mutableStateOf(false)

    private var loader: AtlasBundleLoader? = null

    fun setLoader(loader: AtlasBundleLoader) {
        this.loader = loader
    }

    internal fun decodePlacementsJson(placementsJson: String): List<AtlasGlyphPlacement> {
        return try {
            atlasJson.decodeFromString(atlasPlacementListSerializer, placementsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchShape(
        db: ExternalQuranDatabase,
        bundleKey: String,
        word: String,
        page: Int,
    ): List<AtlasGlyphPlacement>? {
        val entity = db.atlasWordShapeDao().getShape(bundleKey, word, page) ?: return null

        return decodePlacementsJson(entity.placementsJson)
    }

    suspend fun getBundle(
        db: ExternalQuranDatabase,
        bundleKey: String,
    ): QuranAtlasBundle? = loader?.loadBundle(db, bundleKey)

    /**
     * Releases the decoded texture pages only.
     *
     * Everything else survives, and a texture re-decodes from its file the next time it is drawn,
     * so this is the safe response to memory pressure while the reader is still on screen.
     */
    fun trimTextures() {
        loader?.trimTextures()
    }

    /**
     * Releases everything the atlas holds: textures, bundles, placement caches and the tajweed
     * class cache.
     *
     * Call this when the UI is gone. Nothing called it before, which is why the reader's memory
     * — up to 32 MB of texture pages plus a placement list for every word ever rendered — stayed
     * resident for the entire process lifetime, background included.
     */
    fun clearCache() {
        loader?.clearCache()
        TajweedColorSource.clearCache()
    }
}
