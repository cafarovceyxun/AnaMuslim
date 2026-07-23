package com.cafarovceyxun.anamuslim.utils.reader.atlas

import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import okio.Path

/**
 * On-disk layout of imported atlas bundles, derived from [AppFileSystem] — the shared counterpart
 * of the Android `AtlasManager` directory helpers, with the same file names so an already-imported
 * Android install keeps working.
 */
object AtlasFiles {

    private const val DIR_NAME = "atlas"

    fun rootDir(): Path = AppFileSystem.makeAndGetAppResourceDir(DIR_NAME)

    /** One texture page (`atlas.json` `textures[]` entry). */
    fun textureFile(bundleKey: String, textureIndex: Int): Path =
        rootDir() / "${bundleKey}_tex$textureIndex.png"

    fun tempFile(name: String): Path = rootDir() / name

    /** Drops every texture page of [bundleKey] (import replaces them wholesale). */
    fun deleteTextures(bundleKey: String) {
        AppFileSystem.delete(rootDir() / "$bundleKey.png")

        AppFileSystem.list(rootDir())
            .filter { it.name.startsWith("${bundleKey}_tex") }
            .forEach { AppFileSystem.delete(it) }
    }
}
