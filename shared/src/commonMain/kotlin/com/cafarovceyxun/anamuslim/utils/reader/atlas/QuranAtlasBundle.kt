package com.cafarovceyxun.anamuslim.utils.reader.atlas

import androidx.compose.ui.graphics.ImageBitmap
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.db.entities.atlas.AtlasWordShapeEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val ATLAS_PREFETCH_CHUNK = 400

class QuranAtlasBundle(
    val db: ExternalQuranDatabase,
    val key: String,
    val meta: AtlasMetaRoot,
    val layer: AtlasLayerJson,
    val textureSource: AtlasTextureSource,
) {
    val textureCount: Int
        get() = textureSource.size

    fun textureForGlyph(glyph: AtlasGlyphJson): ImageBitmap? =
        textureSource.imageBitmapForIndex(glyph.textureIndex)

    private val shapeCache = mutableMapOf<String, List<AtlasGlyphPlacement>>()
    private val shapeLock = ReentrantLock()

    private fun cachedShape(ck: String): List<AtlasGlyphPlacement>? =
        shapeLock.withLock { shapeCache[ck] }

    /** Stores [list] under [ck] unless already present (first writer wins), returning the winner. */
    private fun putShapeIfAbsent(ck: String, list: List<AtlasGlyphPlacement>): List<AtlasGlyphPlacement> =
        shapeLock.withLock { shapeCache.getOrPut(ck) { list } }

    private fun resolveQueryPage(readerPage: Int): Int =
        if (meta.isPageScopedGlyphAtlas() && readerPage > 0) {
            readerPage
        } else {
            AtlasWordShapeEntity.ATLAS_PAGE_NONE
        }

    private fun cacheKey(page: Int, word: String): String = "$page\u0000$word"

    suspend fun getPlacements(word: String, pageNo: Int): List<AtlasGlyphPlacement> {
        val queryPage = resolveQueryPage(pageNo)
        val ck = cacheKey(queryPage, word)

        val placements = cachedShape(ck) ?: run {
            val fetched = QuranAtlasLoader.fetchShape(db, key, word, queryPage) ?: emptyList()
            putShapeIfAbsent(ck, fetched)
        }

        prefetchTexturesForPlacements(placements)

        return placements
    }

    suspend fun prefetchShapes(pairs: Collection<Pair<String, Int>>) {
        val distinct = pairs
            .asSequence()
            .filter { it.first.isNotEmpty() }
            .map { (w, rp) -> w to resolveQueryPage(rp) }
            .distinctBy { cacheKey(it.second, it.first) }
            .filter { cachedShape(cacheKey(it.second, it.first)) == null }
            .toList()

        if (distinct.isEmpty()) return

        coroutineScope {
            distinct.groupBy { it.second }.map { (page, group) ->
                async(Dispatchers.IO) {
                    val words = group.map { it.first }.distinct()

                    words.chunked(ATLAS_PREFETCH_CHUNK).forEach { chunk ->
                        val rows = db.atlasWordShapeDao().getShapesForWords(key, chunk, page)

                        val byWord = rows.associateBy { it.word }

                        for (w in chunk) {
                            val list = byWord[w]?.let { entity ->
                                QuranAtlasLoader.decodePlacementsJson(entity.placementsJson)
                            } ?: emptyList()

                            putShapeIfAbsent(cacheKey(page, w), list)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun getPlacementsForWords(
        words: List<AyahWordEntity>,
        readerPage: Int,
    ): Map<Int, List<AtlasGlyphPlacement>> {
        if (words.isEmpty()) return emptyMap()

        prefetchShapes(words.map { it.text to readerPage })

        val placements = getPrefetchedPlacementsForWords(words, readerPage)

        prefetchTexturesForPlacementLists(placements.values)

        return placements
    }

    fun getPrefetchedPlacementsForWords(
        words: List<AyahWordEntity>,
        readerPage: Int,
    ): Map<Int, List<AtlasGlyphPlacement>> {
        if (words.isEmpty()) return emptyMap()

        val page = resolveQueryPage(readerPage)

        return buildMap(words.size) {
            for (word in words) {
                put(
                    word.atlasPlacementMapKey(),
                    cachedShape(cacheKey(page, word.text)) ?: emptyList()
                )
            }
        }
    }

    suspend fun prefetchTexturesForPlacementLists(
        placementLists: Collection<List<AtlasGlyphPlacement>>,
    ) {
        val textureIndices = placementLists
            .asSequence()
            .flatten()
            .mapNotNull { placement -> layer.glyphs[placement.gid.toString()]?.textureIndex }
            .toList()

        textureSource.prefetch(textureIndices)
    }

    private suspend fun prefetchTexturesForPlacements(
        placements: List<AtlasGlyphPlacement>,
    ) {
        val textureIndices = placements.mapNotNull { placement ->
            layer.glyphs[placement.gid.toString()]?.textureIndex
        }

        textureSource.prefetch(textureIndices)
    }

    fun clearTextureCache() {
        textureSource.clear()
    }
}

fun Map<Int, List<AtlasGlyphPlacement>>.getForWord(word: AyahWordEntity): List<AtlasGlyphPlacement>? {
    return this[word.atlasPlacementMapKey()]
}

/** Per-glyph tajweed classes for [word], keyed the same way as the atlas placement map. */
fun Map<Int, ByteArray>.glyphClassesForWord(word: AyahWordEntity): ByteArray? {
    return this[word.atlasPlacementMapKey()]
}

private fun AyahWordEntity.atlasPlacementMapKey(): Int =
    ayahId * 4096 + wordIndex
