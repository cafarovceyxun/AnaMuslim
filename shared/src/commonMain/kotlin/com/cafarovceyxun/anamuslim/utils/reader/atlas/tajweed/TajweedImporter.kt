package com.cafarovceyxun.anamuslim.utils.reader.atlas.tajweed

import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.db.entities.atlas.TajweedMetaEntity
import com.cafarovceyxun.anamuslim.db.entities.atlas.TajweedOverrideEntity
import com.cafarovceyxun.anamuslim.db.entities.atlas.TajweedWordColorEntity
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasFiles
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasLayoutParser
import com.cafarovceyxun.anamuslim.utils.reader.atlas.AtlasMetaRoot
import com.cafarovceyxun.anamuslim.utils.reader.atlas.atlasJson
import com.cafarovceyxun.anamuslim.utils.reader.atlas.isPageScopedGlyphAtlas
import com.cafarovceyxun.anamuslim.utils.reader.atlas.prebuiltAtlasResourcePath
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
 * Imports `tajweed.bin` into the [ExternalQuranDatabase] tajweed tables.
 *
 * The base per-glyph classes in `tajweed.bin` are keyed by **layout-document order**, not by word
 * text (see `tools/tajweed/FORMAT.md`). Mapping a base record to a word therefore needs the
 * `layout.json` document order, which is only cheaply available by streaming the 11 MB layout — so
 * the decoded text→classes rows are persisted here once (mirroring [com.cafarovceyxun.anamuslim
 * .utils.reader.atlas.SharedAtlasImporter]) rather than re-parsing layout on every cold start.
 *
 * Idempotent: [ensureImported] rebuilds only when the shipped bin's schema version differs from the
 * stored one (or the tables are empty), so it is cheap to call before every reader build.
 */
object TajweedImporter {

    private val lock = Mutex()

    /** Compose Resources path of the bundled tajweed data for [bundleKey]. */
    private fun tajweedResourcePath(bundleKey: String): String = "files/atlas/$bundleKey/tajweed.bin"

    /**
     * Ensures the tajweed tables for [bundleKey] match the shipped `tajweed.bin`, importing if
     * needed. Returns true when tajweed data is available afterwards.
     */
    suspend fun ensureImported(
        db: ExternalQuranDatabase,
        bundleKey: String,
    ): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                ensureImportedLocked(db, bundleKey)
            } catch (e: Exception) {
                AppLogger.saveError(e, "TajweedImporter.ensureImported($bundleKey)")
                false
            }
        }
    }

    private suspend fun ensureImportedLocked(
        db: ExternalQuranDatabase,
        bundleKey: String,
    ): Boolean {
        val dao = db.tajweedDao()

        // Fast path: already imported at the current schema version — don't touch the file at all.
        val meta = dao.getMeta(bundleKey)
        if (meta != null &&
            meta.version == TajweedBinDecoder.SCHEMA_VERSION &&
            dao.countWordColors(bundleKey) > 0L
        ) {
            return true
        }

        val gzipBytes = Res.readBytes(tajweedResourcePath(bundleKey))
        val data = TajweedBinDecoder.decodeFile(gzipBytes)

        // Pair each base record with its word text by streaming the atlas layout in document order.
        val words = readLayoutWordsInOrder(bundleKey)
        if (words.size != data.baseRecords.size) {
            // Order guard: a mismatch means the layout and the tajweed file disagree — never risk
            // colouring the wrong word, so bail out and leave tajweed unavailable.
            AppLogger.saveError(
                IllegalStateException(
                    "tajweed base_count=${data.baseRecords.size} != layout documents=${words.size}"
                ),
                "TajweedImporter($bundleKey)",
            )
            return false
        }

        dao.deleteWordColors(bundleKey)

        val pending = ArrayList<TajweedWordColorEntity>(INSERT_CHUNK)
        for (i in words.indices) {
            pending.add(
                TajweedWordColorEntity(
                    bundleKey = bundleKey,
                    word = words[i],
                    classes = data.baseRecords[i].classes,
                )
            )
            if (pending.size >= INSERT_CHUNK) {
                dao.insertWordColors(pending)
                pending.clear()
            }
        }
        if (pending.isNotEmpty()) dao.insertWordColors(pending)

        dao.deleteAllOverrides()
        val overrideRows = ArrayList<TajweedOverrideEntity>(INSERT_CHUNK)
        for (group in data.overrides) {
            for (w in group.words) {
                overrideRows.add(
                    TajweedOverrideEntity(
                        ayahId = group.ayahId,
                        wordIndex = w.wordIndex,
                        diffs = w.packedDiffs,
                    )
                )
                if (overrideRows.size >= INSERT_CHUNK) {
                    dao.insertOverrides(overrideRows)
                    overrideRows.clear()
                }
            }
        }
        if (overrideRows.isNotEmpty()) dao.insertOverrides(overrideRows)

        dao.upsertMeta(
            TajweedMetaEntity(
                bundleKey = bundleKey,
                version = data.version,
                palette = paletteToBytes(data.palette),
            )
        )

        return true
    }

    /** Streams `layout.json` from the prebuilt atlas zip, returning every document's text in order. */
    private suspend fun readLayoutWordsInOrder(bundleKey: String): List<String> {
        val zipBytes = Res.readBytes(prebuiltAtlasResourcePath(bundleKey))
        val tempZip = AtlasFiles.tempFile("${bundleKey}_tajweed_layout.zip")

        return try {
            AppFileSystem.write(tempZip) { sink -> sink.write(zipBytes) }

            val zip = AppFileSystem.fileSystem.openZip(tempZip)

            val metaJson = zip.readEntryText("meta.json")
            val meta = atlasJson.decodeFromString<AtlasMetaRoot>(metaJson)

            val words = ArrayList<String>(21_497)
            zip.source(entryPath(meta.layout.file)).buffer().use { source ->
                AtlasLayoutParser.streamWordShapes(
                    source = source,
                    bundleKey = bundleKey,
                    requirePage = meta.isPageScopedGlyphAtlas(),
                ) { shape ->
                    words.add(shape.word)
                }
            }
            words
        } finally {
            AppFileSystem.delete(tempZip)
        }
    }

    private fun paletteToBytes(palette: IntArray): ByteArray {
        val out = ByteArray(palette.size * 4)
        for (i in palette.indices) {
            val v = palette[i]
            out[i * 4] = (v and 0xFF).toByte()
            out[i * 4 + 1] = ((v ushr 8) and 0xFF).toByte()
            out[i * 4 + 2] = ((v ushr 16) and 0xFF).toByte()
            out[i * 4 + 3] = ((v ushr 24) and 0xFF).toByte()
        }
        return out
    }
}

/** Zip entries are addressed as absolute paths inside the archive filesystem. */
private fun entryPath(pathInZip: String): Path =
    ("/" + pathInZip.trimStart('/').replace('\\', '/')).toPath()

private fun FileSystem.readEntryText(pathInZip: String): String {
    val entry = entryPath(pathInZip)
    if (!exists(entry)) error("atlas zip missing $pathInZip")
    return read(entry) { readUtf8() }
}
