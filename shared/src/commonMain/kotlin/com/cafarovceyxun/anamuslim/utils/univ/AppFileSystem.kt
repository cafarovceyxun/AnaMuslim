package com.cafarovceyxun.anamuslim.utils.univ

import okio.BufferedSink
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * Multiplatform file I/O layer (okio-backed) for the app's private files directory.
 *
 * This holds the platform-neutral file operations previously living only in the Android
 * `FileUtils.java`. Android's `FileUtils` now delegates its pure-I/O methods here (keeping its
 * `java.io.File` API for the 22 existing Android callers), while iOS/shared code can use this
 * directly. Android-only concerns (Uri/MediaStore/Bitmap/FileProvider) stay in `FileUtils.java`.
 *
 * The base directory (`appFilesDirPath()`) is an `expect` — androidMain returns
 * `context.filesDir`, iosMain returns the app's Documents directory.
 */
object AppFileSystem {
    /** Internal (not private) so the inline [write] helper below can reach it. */
    @PublishedApi
    internal val fileSystem: FileSystem = platformFileSystem()
    private val fs: FileSystem get() = fileSystem

    /** Absolute path of the app's private files directory (Android `filesDir` / iOS Documents). */
    fun appFilesDir(): Path = appFilesDirPath().toPath()

    /** Joins non-blank segments with the platform separator (mirror of Java `FileUtils.createPath`). */
    fun createPath(vararg subPaths: String): String =
        subPaths.filter { it.isNotEmpty() }.joinToString(Path.DIRECTORY_SEPARATOR)

    /** Returns (creating if needed) a resource subdirectory under the app files dir. */
    fun makeAndGetAppResourceDir(resourceDirName: String): Path {
        val dir = appFilesDir() / resourceDirName
        fs.createDirectories(dir)
        return dir
    }

    fun exists(path: Path): Boolean = fs.exists(path)

    /** Size of [path] in bytes; null if it does not exist or the size is unknown. */
    fun size(path: Path): Long? = fs.metadataOrNull(path)?.size

    /** Creates an empty file (and parent dirs) if it does not already exist. Returns true if present after. */
    fun createFile(path: Path): Boolean {
        if (fs.exists(path)) return true
        path.parent?.let { fs.createDirectories(it) }
        return try {
            fs.write(path) { }
            true
        } catch (e: IOException) {
            false
        }
    }

    fun writeText(path: Path, text: String) {
        path.parent?.let { fs.createDirectories(it) }
        fs.write(path) { writeUtf8(text) }
    }

    fun readText(path: Path): String = fs.read(path) { readUtf8() }

    /** Whole-file bytes — for payloads that may be compressed and are decoded in memory. */
    fun readBytes(path: Path): ByteArray = fs.read(path) { readByteArray() }

    /**
     * Streams into [path], creating parent directories first. Unlike [writeText] this never holds
     * the whole payload in memory, which is what large downloads (recitation audio) need.
     *
     * Inline so callers may suspend inside [block] — a download reports progress between chunks.
     */
    internal inline fun <T> write(path: Path, block: (BufferedSink) -> T): T {
        path.parent?.let { fileSystem.createDirectories(it) }
        return fileSystem.sink(path).buffer().use(block)
    }

    /** Deletes a single file. No-op if it does not exist. Returns true if it was deleted. */
    fun delete(path: Path): Boolean {
        if (!fs.exists(path)) return false
        return try {
            fs.delete(path)
            true
        } catch (e: IOException) {
            false
        }
    }

    /** Immediate subdirectories of [path]; empty if it does not exist or is not a directory. */
    fun listDirectories(path: Path): List<Path> {
        if (!fs.exists(path)) return emptyList()
        return fs.listOrNull(path)?.filter { fs.metadataOrNull(it)?.isDirectory == true } ?: emptyList()
    }

    /** Immediate children (files and directories) of [path]; empty if it does not exist. */
    fun list(path: Path): List<Path> {
        if (!fs.exists(path)) return emptyList()
        return fs.listOrNull(path) ?: emptyList()
    }

    /** Every regular file under [path], recursively; empty if it does not exist. */
    fun listFilesRecursively(path: Path): List<Path> {
        val metadata = fs.metadataOrNull(path) ?: return emptyList()
        if (!metadata.isDirectory) return listOf(path)
        return fs.listOrNull(path)?.flatMap { listFilesRecursively(it) } ?: emptyList()
    }

    /**
     * Moves [source] onto [target], replacing it. Used to publish a fully written temp file so
     * readers never observe a half-written one.
     */
    fun atomicMove(source: Path, target: Path): Boolean {
        target.parent?.let { fs.createDirectories(it) }
        return try {
            fs.atomicMove(source, target)
            true
        } catch (e: IOException) {
            false
        }
    }

    /** Recursively deletes a file or directory tree. No-op if it does not exist. */
    fun deleteRecursively(path: Path) {
        if (!fs.exists(path)) return
        fs.deleteRecursively(path)
    }

    /** Copies a file or directory tree from [source] to [target]. */
    fun cloneDirectory(source: Path, target: Path) {
        val metadata = fs.metadataOrNull(source)
        if (metadata?.isDirectory == true) {
            fs.createDirectories(target)
            for (child in fs.list(source)) {
                cloneDirectory(child, target / child.name)
            }
        } else {
            target.parent?.let { fs.createDirectories(it) }
            fs.copy(source, target)
        }
    }
}

/** App's private files directory absolute path (androidMain: filesDir, iosMain: Documents). */
expect fun appFilesDirPath(): String

/** The platform's real (disk-backed) okio [FileSystem]. */
internal expect fun platformFileSystem(): FileSystem
