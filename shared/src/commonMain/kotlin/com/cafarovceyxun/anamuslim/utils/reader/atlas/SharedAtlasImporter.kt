package com.cafarovceyxun.anamuslim.utils.reader.atlas

import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.db.entities.atlas.AtlasBundleEntity
import com.cafarovceyxun.anamuslim.db.entities.atlas.AtlasWordShapeEntity
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip
import okio.use

private const val INSERT_CHUNK = 1000

/**
 * Imports an atlas bundle (`meta.json` + `atlas.json` + texture PNGs + `layout.json`) from a zip
 * into the external Quran database and the app's atlas directory.
 *
 * Multiplatform port of Android's `AtlasManager`: the zip is read through okio's `openZip`
 * (no `java.util.zip`) and the 11 MB layout file through [AtlasLayoutParser] instead of
 * `android.util.JsonReader`.
 *
 * **Publish-last instead of one transaction.** Android wrapped the shape insert in
 * `db.withTransaction`; Room's common API has no equivalent that can span a streaming read, so
 * shapes are written first and the bundle row — the thing every reader checks — is upserted last.
 * A crash mid-import therefore leaves orphan shape rows that the next import deletes, never a
 * bundle that claims shapes it does not have.
 */
object SharedAtlasImporter {

    private val lock = Mutex()

    /** Imports [zipPath], replacing anything previously imported under [bundleKey]. */
    suspend fun importFromZip(
        zipPath: Path,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity? = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                importLocked(zipPath, bundleKey, db)
            } catch (e: Exception) {
                AppLogger.saveError(e, "SharedAtlasImporter.importFromZip($bundleKey)")
                null
            }
        }
    }

    /** Imports zip [bytes] (a bundled asset or a fresh download) via a temporary file. */
    suspend fun importFromBytes(
        bytes: ByteArray,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity? = withContext(Dispatchers.IO) {
        val tempZip = AtlasFiles.tempFile("${bundleKey}_import.zip")

        try {
            AppFileSystem.write(tempZip) { sink -> sink.write(bytes) }
            importFromZip(tempZip, bundleKey, db)
        } finally {
            AppFileSystem.delete(tempZip)
        }
    }

    private suspend fun importLocked(
        zipPath: Path,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity {
        val zip = AppFileSystem.fileSystem.openZip(zipPath)

        val metaJson = zip.readEntryText(zipPath, "meta.json")
        val meta = atlasJson.decodeFromString<AtlasMetaRoot>(metaJson)

        val layerJson = zip.readEntryText(zipPath, meta.sizes.firstOrNull()?.atlas.orEmpty())
        val layer = atlasJson.decodeFromString<AtlasLayerJson>(layerJson).also {
            if (it.textures.isEmpty()) error("atlas.json missing textures[]")
        }

        AtlasFiles.deleteTextures(bundleKey)

        for (texture in layer.textures.sortedBy { it.index }) {
            val entry = entryPath(texture.image)
            if (!zip.exists(entry)) error("atlas zip missing texture file: ${texture.image}")

            AppFileSystem.write(AtlasFiles.textureFile(bundleKey, texture.index)) { sink ->
                zip.source(entry).buffer().use { source -> sink.writeAll(source) }
            }
        }

        val dao = db.atlasWordShapeDao()
        dao.deleteShapesForBundle(bundleKey)

        val pending = ArrayList<AtlasWordShapeEntity>(INSERT_CHUNK)

        zip.source(entryPath(meta.layout.file)).buffer().use { source ->
            AtlasLayoutParser.streamWordShapes(
                source = source,
                bundleKey = bundleKey,
                requirePage = meta.isPageScopedGlyphAtlas(),
            ) { shape ->
                pending.add(shape)

                if (pending.size >= INSERT_CHUNK) {
                    dao.insertShapes(ArrayList(pending))
                    pending.clear()
                }
            }
        }

        if (pending.isNotEmpty()) {
            dao.insertShapes(pending)
        }

        val bundle = AtlasBundleEntity(
            bundleKey = bundleKey,
            metaJson = metaJson,
            layerJson = layerJson,
        )

        dao.upsertBundle(bundle)

        return bundle
    }
}

/** Zip entries are addressed as absolute paths inside the archive filesystem. */
private fun entryPath(pathInZip: String): Path =
    ("/" + pathInZip.trimStart('/').replace('\\', '/')).toPath()

private fun FileSystem.readEntryText(zipPath: Path, pathInZip: String): String {
    if (pathInZip.isEmpty()) error("atlas zip reference is empty (zip: $zipPath)")

    val entry = entryPath(pathInZip)
    if (!exists(entry)) error("atlas zip missing $pathInZip")

    return read(entry) { readUtf8() }
}
