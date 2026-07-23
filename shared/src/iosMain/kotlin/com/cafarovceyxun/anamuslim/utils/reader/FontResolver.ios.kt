package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal actual fun loadFontFamilyFromFile(path: String): FontFamily? =
    try {
        val bytes = FileSystem.SYSTEM.read(path.toPath()) { readByteArray() }
        FontFamily(Font(identity = path, data = bytes))
    } catch (_: Exception) {
        null
    }

/**
 * The non-KFQPC reader scripts (default Uthmani Hafs, DK Indopak, PDMS Islamic) draw with a bundled
 * font rather than a downloaded one. Android resolves these from `res/font`; on iOS the same three
 * `.ttf` files ship in shared `composeResources/font`, extracted to disk once by
 * [BundledScriptFonts] so this synchronous resolver can read them (the resolver caches its result,
 * so it must not be given a `Res.readBytes` suspend point that could miss and cache the fallback).
 *
 * The script→file mapping mirrors `QuranApp`'s `getFontRes` hook exactly.
 */
internal actual fun loadBundledScriptFontFamily(script: String, isDark: Boolean): FontFamily? {
    val fileName = when (script) {
        QuranScriptUtils.SCRIPT_DK_INDOPAK -> "scheherazadenew_regular.ttf"
        QuranScriptUtils.SCRIPT_PDMS_ISLAMIC -> "quran_common.ttf"
        else -> "uthmanic_hafs.ttf"
    }

    val path = BundledScriptFonts.diskPath(fileName) ?: return null
    return loadFontFamilyFromFile(path)
}

/**
 * Extracts the bundled reader fonts to `Documents/fonts/` so [loadBundledScriptFontFamily] — which
 * must stay synchronous — can read them off disk. Called once from the iOS bootstrap before the UI
 * appears; after the first launch it is three `exists()` checks and no I/O.
 */
object BundledScriptFonts {

    private val fileNames = listOf(
        "uthmanic_hafs.ttf",
        "scheherazadenew_regular.ttf",
        "quran_common.ttf",
    )

    private fun dir(): Path = AppFileSystem.makeAndGetAppResourceDir("fonts")

    suspend fun ensureExtracted() {
        val dir = dir()
        for (name in fileNames) {
            val dest = dir / name
            if (AppFileSystem.exists(dest)) continue

            try {
                val bytes = Res.readBytes("font/$name")
                AppFileSystem.write(dest) { sink -> sink.write(bytes) }
            } catch (e: Exception) {
                AppLogger.saveError(e, "BundledScriptFonts.ensureExtracted($name)")
            }
        }
    }

    /** On-disk path of an already-extracted bundled font, or null if it is not present yet. */
    fun diskPath(name: String): String? {
        val path = dir() / name
        return if (AppFileSystem.exists(path)) path.toString() else null
    }
}
