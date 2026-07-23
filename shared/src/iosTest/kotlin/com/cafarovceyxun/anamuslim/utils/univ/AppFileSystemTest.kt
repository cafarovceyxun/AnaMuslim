package com.cafarovceyxun.anamuslim.utils.univ

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runtime proof that the shared okio-backed [AppFileSystem] works on iOS/native: the app
 * Documents directory resolves, and the pure file operations (create dir, write/read text,
 * clone tree, recursive delete) round-trip. This is the iOS equivalent of the Android
 * `FileUtils.java` pure-I/O surface that AppFileSystem consolidates.
 */
class AppFileSystemTest {

    @Test
    fun documentsDirResolvesAndIsUsable() {
        val base = appFilesDirPath()
        assertTrue(base.isNotBlank())
        assertTrue(AppFileSystem.exists(base.toPath()), "Documents dir should exist: $base")
    }

    @Test
    fun createPathJoinsNonBlankSegments() {
        assertEquals("a/b/c", AppFileSystem.createPath("a", "", "b", "c"))
    }

    @Test
    fun writeReadCloneAndDeleteRoundTrip() {
        val root = AppFileSystem.makeAndGetAppResourceDir("appfs-test-${kotlin.random.Random.nextInt()}")
        try {
            // write + read
            val file = root / "sub" / "note.txt"
            AppFileSystem.writeText(file, "səlam okio")
            assertTrue(AppFileSystem.exists(file))
            assertEquals("səlam okio", AppFileSystem.readText(file))

            // createFile is idempotent on an existing file
            assertTrue(AppFileSystem.createFile(file))

            // clone the directory tree
            val clone = root / "clone"
            AppFileSystem.cloneDirectory(root / "sub", clone)
            assertEquals("səlam okio", AppFileSystem.readText(clone / "note.txt"))

            // recursive delete
            AppFileSystem.deleteRecursively(root)
            assertFalse(AppFileSystem.exists(root))
            // no-op on a missing path
            AppFileSystem.deleteRecursively(root)
        } finally {
            AppFileSystem.deleteRecursively(root)
        }
    }
}
