package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.cafarovceyxun.anamuslim.concurrent.ReentrantLock
import com.cafarovceyxun.anamuslim.concurrent.withLock
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.ScriptFiles
import okio.Path

private const val KFQPC_FONT_CACHE_SIZE = 24

private data class ScriptFontKey(
    val script: String,
    val isDark: Boolean,
)

private data class KfqpcFontKey(
    val script: String,
    val pageNo: Int,
    val isDark: Boolean,
)

/**
 * Resolves the reader's Arabic [FontFamily] for a script/page, caching what it loads.
 *
 * Cache policy and font-file resolution are platform-neutral; only the final "font file/resource ->
 * FontFamily" step is an `expect` ([loadFontFamilyFromFile] / [loadBundledScriptFontFamily]).
 * Atlas scripts render from raster images instead of fonts, so they resolve to `null` here.
 */
class FontResolver private constructor() {
    private val cacheLock = ReentrantLock()

    // non-KFQPC: one font per script
    private val scriptFontCache = mutableMapOf<ScriptFontKey, FontFamily>()

    // KFQPC has hundreds of page fonts, so keep only the recent reader window resident.
    // An access-ordered LinkedHashMap has no common equivalent, so LRU order is kept by
    // re-inserting on hit and dropping the eldest key on overflow.
    private val kfqpcFontCache = LinkedHashMap<KfqpcFontKey, FontFamily>()

    fun fontFamily(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): FontFamily {
        return resolve(script, pageNo, isDark) ?: FontFamily.Default
    }

    private fun resolve(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): FontFamily? {
        if (script.isQuranAtlasScript()) return null

        return if (script.isKFQPCScript()) {
            getKfqpcFont(KfqpcFontKey(script, pageNo, isDark))
        } else {
            getScriptFont(ScriptFontKey(script, isDark))
        }
    }

    private fun getScriptFont(key: ScriptFontKey): FontFamily {
        cacheLock.withLock { scriptFontCache[key] }?.let { return it }

        val loaded = loadBundledScriptFontFamily(key.script, key.isDark) ?: FontFamily.Default

        return cacheLock.withLock {
            scriptFontCache.getOrPut(key) { loaded }
        }
    }

    private fun getKfqpcFont(key: KfqpcFontKey): FontFamily {
        cacheLock.withLock {
            // Re-insert on hit so the eldest entry stays the least recently used one.
            kfqpcFontCache.remove(key)?.also { kfqpcFontCache[key] = it }
        }?.let { return it }

        val loaded = resolveKfqpcFontPath(key.script, key.pageNo, key.isDark)
            ?.let { loadFontFamilyFromFile(it.toString()) }
            ?: FontFamily.Default

        return cacheLock.withLock {
            kfqpcFontCache[key]?.let { return@withLock it }

            kfqpcFontCache[key] = loaded
            if (kfqpcFontCache.size > KFQPC_FONT_CACHE_SIZE) {
                kfqpcFontCache.keys.firstOrNull()?.let { kfqpcFontCache.remove(it) }
            }

            loaded
        }
    }

    /**
     * Drops every loaded [FontFamily].
     *
     * KFQPC keeps one font file per mushaf page and this holds [KFQPC_FONT_CACHE_SIZE] of them
     * resident, which is worth giving back when the UI is gone; a `FontFamily` already handed to a
     * live composition keeps working, and the next resolve simply loads the file again.
     */
    fun clearCache() {
        cacheLock.withLock {
            scriptFontCache.clear()
            kfqpcFontCache.clear()
        }
    }

    fun prefetch(script: String, pages: List<Int>, isDark: Boolean) {
        if (script.isQuranAtlasScript()) return

        if (!script.isKFQPCScript()) {
            getScriptFont(ScriptFontKey(script, isDark))
            return
        }

        pages
            .distinct()
            .take(KFQPC_FONT_CACHE_SIZE)
            .forEach { pageNo ->
                getKfqpcFont(KfqpcFontKey(script, pageNo, isDark))
            }
    }

    companion object {
        private val instanceLock = ReentrantLock()

        private var instance: FontResolver? = null

        fun getInstance(): FontResolver {
            return instance ?: instanceLock.withLock {
                instance ?: FontResolver().also { instance = it }
            }
        }

        @Composable
        fun remember(): FontResolver = remember { getInstance() }
    }
}

/**
 * The KFQPC page-font file for [pageNo], or null when nothing usable is downloaded.
 *
 * Prefers the dark variant when the script ships one, falling back to the light file and then to
 * the legacy (pre-dark) filename.
 */
internal fun resolveKfqpcFontPath(
    script: String,
    pageNo: Int,
    isDark: Boolean,
): Path? {
    return try {
        val fontsDir = ScriptFiles.kfqpcScriptFontDir(script)
        val useDark = isDark && script.getQuranScriptFontHasDark()
        val darkFile = fontsDir / QuranScriptPlatformHooks.formatFontFilename(pageNo, true)
        val lightFile = fontsDir / QuranScriptPlatformHooks.formatFontFilename(pageNo, false)
        val oldFile = fontsDir / QuranScriptPlatformHooks.formatFontFilenameOld(pageNo)

        val primary = if (useDark) darkFile else lightFile
        val fallbackTtf = if (useDark) lightFile else null

        when {
            primary.isNonEmptyFile() -> primary
            fallbackTtf?.isNonEmptyFile() == true -> fallbackTtf
            oldFile.isNonEmptyFile() -> oldFile
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun Path.isNonEmptyFile(): Boolean = (AppFileSystem.size(this) ?: 0L) > 0L

/** Loads a font file from disk into a [FontFamily]; null when it cannot be loaded. */
internal expect fun loadFontFamilyFromFile(path: String): FontFamily?

/** Loads the platform's built-in font for a non-KFQPC [script]; null when the platform ships none. */
internal expect fun loadBundledScriptFontFamily(script: String, isDark: Boolean): FontFamily?
