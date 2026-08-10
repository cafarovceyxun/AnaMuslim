package com.cafarovceyxun.anamuslim.utils.reader.atlas.tajweed

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.db.ExternalQuranDatabase
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity

/**
 * Per-glyph tajweed class lookup for the Uthmani atlas.
 *
 * Serves, for a rendered word `(ayah_id, word_index)` with atlas text `T`, the final per-glyph
 * class [ByteArray] — the context-free base for `T` with any per-occurrence override applied. The
 * merged arrays are cached by `ayahId * 4096 + wordIndex`, exactly like the atlas placement cache
 * (`QuranAtlasBundle.getForWord`), so repeated renders of the same page cost nothing.
 *
 * The palette is exposed as [paletteState] so the render path can tint each non-zero class with
 * `palette[class]`. Its values come from [TajweedPalette] — the single colour authority — not from
 * the bin's embedded palette; see [bytesToPalette] for the (now unused) decoding of that one.
 */
object TajweedColorSource {

    private val lock = ReentrantLock()

    private val EMPTY = ByteArray(0)

    /** Merged classes cache. [EMPTY] marks "looked up, no tajweed" to avoid re-querying. */
    private val mergedCache = mutableMapOf<Int, ByteArray>()

    private val paletteMutable = mutableStateOf<List<Color>?>(null)

    /** [TajweedPalette.colors], or null until a bundle's tajweed data is loaded. */
    val paletteState: State<List<Color>?> get() = paletteMutable

    private var loadedBundleKey: String? = null

    private fun keyFor(word: AyahWordEntity): Int = word.ayahId * 4096 + word.wordIndex

    /**
     * Ensures tajweed data for [bundleKey] is imported and the palette is loaded. Safe and cheap to
     * call before each build. Returns true when tajweed colours are available.
     */
    suspend fun prepare(db: ExternalQuranDatabase, bundleKey: String): Boolean {
        val imported = TajweedImporter.ensureImported(db, bundleKey)
        if (!imported) return false

        if (lock.withLock { loadedBundleKey } != bundleKey || paletteMutable.value == null) {
            // Existence check only — the meta row's embedded palette bytes are no longer used for
            // rendering (superseded by TajweedPalette, see bytesToPalette below).
            db.tajweedDao().getMeta(bundleKey) ?: return false
            lock.withLock {
                if (loadedBundleKey != bundleKey) {
                    mergedCache.clear()
                    loadedBundleKey = bundleKey
                }
            }
            paletteMutable.value = TajweedPalette.colors
        }
        return true
    }

    /**
     * Returns merged per-glyph classes for each of [words], keyed by `ayahId * 4096 + wordIndex`.
     * Words with no tajweed colour (all-zero or absent) are omitted from the result. Call [prepare]
     * (once per build) before this.
     */
    suspend fun getForWords(
        db: ExternalQuranDatabase,
        bundleKey: String,
        words: List<AyahWordEntity>,
    ): Map<Int, ByteArray> {
        if (words.isEmpty()) return emptyMap()

        val result = HashMap<Int, ByteArray>(words.size)
        val missing = ArrayList<AyahWordEntity>()

        lock.withLock {
            for (word in words) {
                val cached = mergedCache[keyFor(word)]
                if (cached != null) {
                    if (cached.isNotEmpty()) result[keyFor(word)] = cached
                } else {
                    missing.add(word)
                }
            }
        }

        if (missing.isEmpty()) return result

        val dao = db.tajweedDao()

        val distinctTexts = missing.map { it.text }.distinct()
        val baseByText = dao.getWordColors(bundleKey, distinctTexts).associateBy({ it.word }, { it.classes })

        val ayahIds = missing.map { it.ayahId }.distinct()
        val overrideByKey = HashMap<Int, ByteArray>()
        for (row in dao.getOverridesForAyahs(ayahIds)) {
            overrideByKey[row.ayahId * 4096 + row.wordIndex] = row.diffs
        }

        lock.withLock {
            for (word in missing) {
                val k = keyFor(word)
                val base = baseByText[word.text]
                val merged = if (base == null) {
                    null
                } else {
                    val diffs = overrideByKey[k]
                    if (diffs == null) base else TajweedBinDecoder.applyOverride(base, diffs)
                }

                // Normalise "no colour" (absent base or all-zero classes) to the empty sentinel so
                // cache hits and misses agree and render never receives a no-op array.
                val hasColour = merged != null && merged.any { it.toInt() != 0 }
                val stored = if (hasColour) merged!! else EMPTY
                mergedCache[k] = stored
                if (hasColour) result[k] = stored
            }
        }

        return result
    }

    // Decodes the ARGB palette embedded in tajweed.bin's meta row. Superseded by [TajweedPalette]
    // as the render-path colour authority; kept for reference/tooling parity, not called for render.
    private fun bytesToPalette(bytes: ByteArray): List<Color> {
        val count = bytes.size / 4
        return List(count) { i ->
            val b0 = bytes[i * 4].toInt() and 0xFF
            val b1 = bytes[i * 4 + 1].toInt() and 0xFF
            val b2 = bytes[i * 4 + 2].toInt() and 0xFF
            val b3 = bytes[i * 4 + 3].toInt() and 0xFF
            val argb = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
            Color(argb)
        }
    }
}
