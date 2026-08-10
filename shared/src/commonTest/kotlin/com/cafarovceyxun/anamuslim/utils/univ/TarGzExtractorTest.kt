package com.cafarovceyxun.anamuslim.utils.univ

import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.GzipSink
import okio.buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the hand-written tar reader that replaced Apache Commons Compress (JVM-only) when the
 * KFQPC font install moved to `commonMain`. The archives it must handle are plain ustar files, so
 * the fixtures here are built byte-by-byte rather than with a library — that is also what makes the
 * test cheap and deterministic.
 *
 * Runs on both JVM and native: fixtures live under the system temp directory, so the test never
 * touches [AppFileSystem]'s app-files root, which on Android would need a `Context`.
 */
class TarGzExtractorTest {

    @Test
    fun extractsRegularFilesWithExactContent() {
        val archive = tarGz(
            "qpc_page_001.ttf" to ByteArray(1000) { (it % 251).toByte() },
            "qpc_page_002.ttf" to "second".encodeToByteArray(),
        )

        val dir = writeArchiveAndExtract(archive, "regular")

        assertEquals(2, AppFileSystem.list(dir).size)
        assertEquals(1000L, AppFileSystem.size(dir / "qpc_page_001.ttf"))
        assertEquals("second", AppFileSystem.readText(dir / "qpc_page_002.ttf"))
    }

    /** Entry sizes that are not multiples of 512 must not bleed padding into the next entry. */
    @Test
    fun handlesUnpaddedEntrySizes() {
        val first = ByteArray(513) { 1 }
        val archive = tarGz("a.bin" to first, "b.bin" to byteArrayOf(9, 9, 9))

        val dir = writeArchiveAndExtract(archive, "padding")

        assertEquals(513L, AppFileSystem.size(dir / "a.bin"))
        assertEquals(3L, AppFileSystem.size(dir / "b.bin"))
        assertTrue(AppFileSystem.readBytes(dir / "b.bin").all { it == 9.toByte() })
    }

    /** A malicious archive must not be able to write outside the destination directory. */
    @Test
    fun sanitisesTraversalPaths() {
        val archive = tarGz("../../escaped.ttf" to "nope".encodeToByteArray())

        val dir = writeArchiveAndExtract(archive, "traversal")

        assertEquals("nope", AppFileSystem.readText(dir / "escaped.ttf"))
        assertFalse(AppFileSystem.exists(dir.parent!!.parent!! / "escaped.ttf"))
    }

    private fun writeArchiveAndExtract(archive: ByteArray, name: String): Path {
        val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "targz_test_$name"
        AppFileSystem.deleteRecursively(root)

        val archivePath = root / "in.tar.gz"
        AppFileSystem.write(archivePath) { sink -> sink.write(archive) }

        val outDir = root / "out"
        TarGzExtractor.extract(archivePath, outDir)
        return outDir
    }

    /** Builds a gzipped ustar archive: one 512-byte header per entry, then padded content. */
    private fun tarGz(vararg entries: Pair<String, ByteArray>): ByteArray {
        val tar = Buffer()

        for ((name, content) in entries) {
            tar.write(header(name, content.size.toLong()))
            tar.write(content)

            val padding = (512 - content.size % 512) % 512
            tar.write(ByteArray(padding))
        }

        // End-of-archive marker: two zero blocks.
        tar.write(ByteArray(1024))

        val gzipped = Buffer()
        val gzipSink = GzipSink(gzipped).buffer()
        gzipSink.write(tar, tar.size)
        gzipSink.close()
        return gzipped.readByteArray()
    }

    private fun header(name: String, size: Long): ByteArray {
        val block = ByteArray(512)
        name.encodeToByteArray().copyInto(block, 0)
        // Size is octal ASCII, NUL-terminated, in the 12 bytes at offset 124.
        size.toString(radix = 8).padStart(11, '0').encodeToByteArray().copyInto(block, 124)
        block[156] = '0'.code.toByte() // type flag: regular file
        return block
    }
}
