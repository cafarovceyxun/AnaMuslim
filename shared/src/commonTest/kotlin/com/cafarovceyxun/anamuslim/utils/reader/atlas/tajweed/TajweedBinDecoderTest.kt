package com.cafarovceyxun.anamuslim.utils.reader.atlas.tajweed

import okio.Buffer
import okio.GzipSink
import okio.buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Unit tests for [TajweedBinDecoder], the `tools/tajweed/FORMAT.md` (schema v3) reader. Bodies are
 * assembled synthetically so the varint / nibble-unpack / override-merge logic is exercised without
 * the shipped resource, which is not reachable from a plain JVM unit test.
 */
class TajweedBinDecoderTest {

    private fun ByteArray.gzip(): ByteArray {
        val out = Buffer()
        // Explicit close rather than `use`: okio's `Closeable` is its own interface, not
        // `kotlin.AutoCloseable`, so the stdlib `use` only resolves on JVM (where it binds
        // `java.io.Closeable`). This file compiled green on `testDebugUnitTest` and failed on
        // `compileTestKotlinIosSimulatorArm64` for exactly that reason.
        val sink = GzipSink(out).buffer()
        sink.write(this)
        sink.close() // Writes the gzip trailer; the body is incomplete without it.
        return out.readByteArray()
    }

    /** Little-endian body builder mirroring the format contract. */
    private class BodyBuilder {
        private val buf = Buffer()
        fun u8(v: Int) = apply { buf.writeByte(v and 0xFF) }
        fun u16(v: Int) = apply { buf.writeByte(v and 0xFF).writeByte((v ushr 8) and 0xFF) }
        fun u32(v: Long) = apply {
            buf.writeByte((v and 0xFF).toInt())
                .writeByte(((v ushr 8) and 0xFF).toInt())
                .writeByte(((v ushr 16) and 0xFF).toInt())
                .writeByte(((v ushr 24) and 0xFF).toInt())
        }
        fun uvarint(value: Int) = apply {
            var v = value
            while (true) {
                val b = v and 0x7F
                v = v ushr 7
                if (v == 0) {
                    buf.writeByte(b)
                    break
                } else {
                    buf.writeByte(b or 0x80)
                }
            }
        }
        fun bytes(vararg b: Int) = apply { b.forEach { buf.writeByte(it and 0xFF) } }
        fun build(): ByteArray = buf.readByteArray()
    }

    private fun sampleBody(): ByteArray = BodyBuilder()
        // Header: magic, version, flags, num_classes, reserved.
        .bytes('T'.code, 'J'.code, 'W'.code, 'D'.code)
        .u8(TajweedBinDecoder.SCHEMA_VERSION).u8(0).u8(11).u8(0)
        // Palette: 11 ARGB u32 (class 0 sentinel + 10 colours matching FORMAT.md v3).
        .u32(0x00000000L).u32(0xFF999999L).u32(0xFFFFC1E0L).u32(0xFFFF8E3BL)
        .u32(0xFFFF5E8EL).u32(0xFFE30000L).u32(0xFFB5651DL).u32(0xFF26B55DL)
        .u32(0xFFC62828L).u32(0xFF9C27B0L).u32(0xFF1976D2L)
        // Rule-name table: empty (rn_len = 0).
        .u16(0)
        // Base section: 2 records (nibble-packed, unchanged from v1/v2).
        .u32(2L)
        //   record 0: 11 glyphs, all class 0 -> 6 nibble bytes of 0x00.
        .u8(11).bytes(0, 0, 0, 0, 0, 0)
        //   record 1: 6 glyphs, classes [2,0,0,0,0,1] -> bytes 0x02,0x00,0x10.
        .u8(6).bytes(0x02, 0x00, 0x10)
        // Override section (v3): 2 groups, sorted by ayah_id.
        .u32(2L)
        //   group ayah_id = 2 (first delta absolute), 1 word.
        .uvarint(2).u8(1)
        //     word_index 3, 1 diff: uvarint(glyph 5) + u8(class 10) -> 0x05, 0x0a.
        .u8(3).u8(1).uvarint(5).u8(10)
        //   group ayah_id = 2004 (delta 2002), 1 word.
        .uvarint(2002).u8(1)
        //     word_index 0, 2 diffs: (glyph 0, class 7) and (glyph 2, class 5).
        .u8(0).u8(2).uvarint(0).u8(7).uvarint(2).u8(5)
        .build()

    @Test
    fun decodesHeaderPaletteAndBaseRecords() {
        val data = TajweedBinDecoder.decodeBody(sampleBody())

        assertEquals(TajweedBinDecoder.SCHEMA_VERSION, data.version)
        assertEquals(11, data.numClasses)
        assertEquals(0x00000000, data.palette[0])
        assertEquals(0xFF999999.toInt(), data.palette[1])
        assertEquals(0xFFB5651D.toInt(), data.palette[6])
        assertEquals(0xFF1976D2.toInt(), data.palette[10])

        assertEquals(2, data.baseRecords.size)
        // 11 all-zero glyphs.
        assertContentEquals(ByteArray(11), data.baseRecords[0].classes)
        // Nibble-unpack low-nibble-first: 0x02,0x00,0x10 -> [2,0,0,0,0,1].
        assertContentEquals(byteArrayOf(2, 0, 0, 0, 0, 1), data.baseRecords[1].classes)
    }

    @Test
    fun decodesOverrideDeltaEncoding() {
        val data = TajweedBinDecoder.decodeBody(sampleBody())

        assertEquals(2, data.overrides.size)
        assertEquals(2, data.overrides[0].ayahId)
        // Second group's absolute ayah_id is prev + delta = 2 + 2002.
        assertEquals(2004, data.overrides[1].ayahId)

        val w = data.overrides[0].words.single()
        assertEquals(3, w.wordIndex)
        // Decoder stores diffs flattened: [glyph_index, class] = [5, 10].
        assertContentEquals(byteArrayOf(5, 10), w.packedDiffs)

        val w2 = data.overrides[1].words.single()
        assertEquals(0, w2.wordIndex)
        assertContentEquals(byteArrayOf(0, 7, 2, 5), w2.packedDiffs)
    }

    @Test
    fun appliesOverrideDiffsOntoBase() {
        // Base [2,0,0,0,0,1]; diff (glyph 5 -> class 10).
        val merged = TajweedBinDecoder.applyOverride(
            base = byteArrayOf(2, 0, 0, 0, 0, 1),
            packedDiffs = byteArrayOf(5, 10),
        )
        assertContentEquals(byteArrayOf(2, 0, 0, 0, 0, 10), merged)

        // Two diffs: glyph 0 -> class 7, glyph 2 -> class 5.
        val merged2 = TajweedBinDecoder.applyOverride(
            base = byteArrayOf(0, 0, 0, 0),
            packedDiffs = byteArrayOf(0, 7, 2, 5),
        )
        assertContentEquals(byteArrayOf(7, 0, 5, 0), merged2)

        // A class value > 7 (class 10) survives the full byte round-trip.
        val merged3 = TajweedBinDecoder.applyOverride(
            base = ByteArray(20),
            packedDiffs = byteArrayOf(17, 10),
        )
        assertEquals(10.toByte(), merged3[17])

        // Out-of-range glyph index is ignored, base is not mutated.
        val base = byteArrayOf(1, 1)
        val merged4 = TajweedBinDecoder.applyOverride(base, byteArrayOf(5, 3))
        assertContentEquals(byteArrayOf(1, 1), merged4)
        assertContentEquals(byteArrayOf(1, 1), base)
    }

    @Test
    fun gunzipRoundTripsThroughDecodeFile() {
        val body = sampleBody()
        val data = TajweedBinDecoder.decodeFile(body.gzip())
        assertEquals(TajweedBinDecoder.SCHEMA_VERSION, data.version)
        assertEquals(11, data.numClasses)
        assertEquals(2, data.baseRecords.size)
        assertEquals(2004, data.overrides[1].ayahId)
    }
}
