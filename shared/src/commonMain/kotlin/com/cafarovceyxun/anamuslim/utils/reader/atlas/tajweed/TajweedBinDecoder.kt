package com.cafarovceyxun.anamuslim.utils.reader.atlas.tajweed

import okio.Buffer
import okio.GzipSource
import okio.buffer
import okio.use

/**
 * Per-word base glyph classes: one class byte (0..7) per glyph, in glyph order. Class `0` means
 * "no tajweed colour". Positionally aligned with the atlas glyph placements for the same word text.
 */
class TajweedBaseRecord(val classes: ByteArray)

/**
 * A per-occurrence override for one word: pairs of `(glyph_index, class)` flattened into
 * [packedDiffs] as `[glyph_index, class, glyph_index, class, …]` (two bytes per diff). This is the
 * decoder's own compact storage form; the v3 file encodes glyph_index as a uvarint (always one byte
 * in this corpus, since no word has ≥ 128 glyphs).
 */
class TajweedOverrideWord(val wordIndex: Int, val packedDiffs: ByteArray)

/** All overrides carried by a single ayah, sorted by word index. */
class TajweedOverrideGroup(val ayahId: Int, val words: List<TajweedOverrideWord>)

/**
 * Fully decoded `tajweed.bin` body: palette + document-ordered base records + per-occurrence
 * overrides. Base record `i` is the tajweed base for the i-th `layout.json` document (same order
 * the atlas [AtlasLayoutParser] streams), which the importer pairs with that document's word text.
 */
class TajweedBinData(
    val version: Int,
    val numClasses: Int,
    /** ARGB (0xAARRGGBB) per class id, size [numClasses]; class 0 is the unused sentinel. */
    val palette: IntArray,
    val baseRecords: List<TajweedBaseRecord>,
    val overrides: List<TajweedOverrideGroup>,
)

/**
 * Decoder for the `tajweed.bin` schema (see `tools/tajweed/FORMAT.md`). Written strictly from the
 * format contract: the file is a single gzip stream whose inflated body is parsed here. All
 * integers are little-endian unsigned; `uvarint` is unsigned LEB128.
 *
 * The decoder is pure (bytes in, data out) so it can be unit-tested without the atlas or DB.
 */
object TajweedBinDecoder {

    /**
     * The `tajweed.bin` data version the app currently ships. Used by [TajweedImporter] to decide
     * when to re-import (stored version != this ⇒ rebuild). Bump on every data regen, even when the
     * byte encoding is unchanged — that is what triggers the on-device refresh.
     */
    const val SCHEMA_VERSION = 7

    /**
     * Oldest *encoding* the decoder can read. The wire format has been stable since v3 (nibble-packed
     * base records + uvarint-glyph/u8-class overrides); later versions like v4 change only the data,
     * not the layout. Gating decode on the encoding — not on an exact version — keeps colouring alive
     * across data bumps, instead of silently failing whenever [SCHEMA_VERSION] and the shipped file
     * are momentarily out of step.
     */
    const val MIN_ENCODING_VERSION = 3

    /** Inflates a gzip stream (the shipped `tajweed.bin`) to its raw body bytes. */
    fun gunzip(gzipBytes: ByteArray): ByteArray {
        val source = Buffer().write(gzipBytes)
        return GzipSource(source).buffer().use { it.readByteArray() }
    }

    /** Decodes a gzip `tajweed.bin` file end to end. */
    fun decodeFile(gzipBytes: ByteArray): TajweedBinData = decodeBody(gunzip(gzipBytes))

    /** Decodes an already-inflated body. */
    fun decodeBody(body: ByteArray): TajweedBinData {
        val cursor = ByteCursor(body)

        // Header.
        val magic = cursor.readBytes(4)
        require(magic[0] == 'T'.code.toByte() && magic[1] == 'J'.code.toByte() &&
            magic[2] == 'W'.code.toByte() && magic[3] == 'D'.code.toByte()) {
            "tajweed.bin: bad magic"
        }
        val version = cursor.readU8()
        require(version >= MIN_ENCODING_VERSION) {
            "tajweed.bin: unsupported version $version (need >= $MIN_ENCODING_VERSION)"
        }
        cursor.readU8() // flags (reserved)
        val numClasses = cursor.readU8()
        cursor.readU8() // reserved

        // Palette: numClasses × u32 ARGB.
        val palette = IntArray(numClasses) { cursor.readU32().toInt() }

        // Rule-name table: self-describing, skipped via its length.
        val rnLen = cursor.readU16()
        cursor.skip(rnLen)

        // Base section.
        val baseCount = cursor.readU32().toInt()
        val baseRecords = ArrayList<TajweedBaseRecord>(baseCount)
        repeat(baseCount) {
            val glyphCount = cursor.readU8()
            val packedLen = (glyphCount + 1) / 2
            val packed = cursor.readBytes(packedLen)
            val classes = ByteArray(glyphCount)
            for (g in 0 until glyphCount) {
                val b = packed[g / 2].toInt() and 0xFF
                classes[g] = (if (g % 2 == 0) b and 0x0F else (b ushr 4) and 0x0F).toByte()
            }
            baseRecords.add(TajweedBaseRecord(classes))
        }

        // Override section.
        val ayahCount = cursor.readU32().toInt()
        val overrides = ArrayList<TajweedOverrideGroup>(ayahCount)
        var prevAyahId = 0
        repeat(ayahCount) {
            val ayahId = prevAyahId + cursor.readUvarint()
            prevAyahId = ayahId

            val wordCount = cursor.readU8()
            val words = ArrayList<TajweedOverrideWord>(wordCount)
            repeat(wordCount) {
                val wordIndex = cursor.readU8()
                val diffCount = cursor.readU8()
                // v3: each diff is uvarint(glyph_index) + u8(class). We store it flattened as two
                // bytes per diff — [glyph_index, class] — since glyph_index is < 128 in this corpus.
                val diffs = ByteArray(diffCount * 2)
                for (d in 0 until diffCount) {
                    val glyphIndex = cursor.readUvarint()
                    val cls = cursor.readU8()
                    diffs[d * 2] = glyphIndex.toByte()
                    diffs[d * 2 + 1] = cls.toByte()
                }
                words.add(TajweedOverrideWord(wordIndex, diffs))
            }
            overrides.add(TajweedOverrideGroup(ayahId, words))
        }

        return TajweedBinData(
            version = version,
            numClasses = numClasses,
            palette = palette,
            baseRecords = baseRecords,
            overrides = overrides,
        )
    }

    /**
     * Applies [packedDiffs] onto a copy of [base] and returns the merged per-glyph classes.
     * [packedDiffs] is the decoder's flattened form: two bytes per diff, `[glyph_index, class]`.
     * Out-of-range glyph indices are ignored.
     */
    fun applyOverride(base: ByteArray, packedDiffs: ByteArray): ByteArray {
        if (packedDiffs.isEmpty()) return base
        val merged = base.copyOf()
        var i = 0
        while (i + 1 < packedDiffs.size) {
            val glyphIndex = packedDiffs[i].toInt() and 0xFF
            val cls = packedDiffs[i + 1].toInt() and 0xFF
            if (glyphIndex < merged.size) merged[glyphIndex] = cls.toByte()
            i += 2
        }
        return merged
    }
}

/** Minimal little-endian cursor over a byte array. */
private class ByteCursor(private val data: ByteArray) {
    private var pos = 0

    fun readU8(): Int = data[pos++].toInt() and 0xFF

    fun readU16(): Int {
        val b0 = readU8()
        val b1 = readU8()
        return b0 or (b1 shl 8)
    }

    fun readU32(): Long {
        val b0 = readU8().toLong()
        val b1 = readU8().toLong()
        val b2 = readU8().toLong()
        val b3 = readU8().toLong()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readUvarint(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = readU8()
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result
    }

    fun readBytes(count: Int): ByteArray {
        val out = data.copyOfRange(pos, pos + count)
        pos += count
        return out
    }

    fun skip(count: Int) {
        pos += count
    }
}
