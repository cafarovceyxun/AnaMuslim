package com.cafarovceyxun.anamuslim.utils.reader.atlas

import androidx.compose.runtime.mutableStateOf
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase

/**
 * Platform loader for a fully-built [QuranAtlasBundle]: prebuilt-asset import, texture-page file
 * resolution and image decoding. Registered once at startup — androidMain via `QuranApp.onCreate`,
 * iosMain once the atlas render path lands on iOS.
 */
interface AtlasBundleLoader {
    suspend fun loadBundle(db: ExternalQuranDatabase, bundleKey: String): QuranAtlasBundle?
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

    fun clearCache() {
        loader?.clearCache()
    }
}
