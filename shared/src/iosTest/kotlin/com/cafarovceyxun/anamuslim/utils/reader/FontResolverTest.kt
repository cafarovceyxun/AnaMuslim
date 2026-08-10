package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.ui.text.font.FontFamily
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.ScriptFiles
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Runtime proof on iOS/native that the reader's KFQPC page-font lookup — the part of [FontResolver]
 * that used to live in Android's `FontResolver` on top of `java.io.File` — resolves the same
 * dark/light/legacy fallback chain against real iOS filesystem paths.
 */
class FontResolverTest {
    // KFQPC v4 is the script that ships dark page fonts; the others only ever have light ones.
    private val script = QuranScriptUtils.SCRIPT_KFQPC_V4
    private val pageNo = 7

    private val fontsDir get() = ScriptFiles.kfqpcScriptFontDir(script)
    private val darkFile get() = fontsDir / QuranScriptPlatformHooks.formatFontFilename(pageNo, true)
    private val lightFile get() = fontsDir / QuranScriptPlatformHooks.formatFontFilename(pageNo, false)
    private val oldFile get() = fontsDir / QuranScriptPlatformHooks.formatFontFilenameOld(pageNo)

    @BeforeTest
    @AfterTest
    fun clearFontDir() {
        AppFileSystem.deleteRecursively(fontsDir)
    }

    @Test
    fun resolvesNothingWhenNoFontIsDownloaded() {
        assertNull(resolveKfqpcFontPath(script, pageNo, isDark = true))
        assertNull(resolveKfqpcFontPath(script, pageNo, isDark = false))
    }

    @Test
    fun ignoresEmptyFontFiles() {
        AppFileSystem.createFile(lightFile)

        assertNull(resolveKfqpcFontPath(script, pageNo, isDark = false))
    }

    @Test
    fun prefersDarkFontInDarkModeAndLightOtherwise() {
        AppFileSystem.writeText(darkFile, "dark")
        AppFileSystem.writeText(lightFile, "light")

        assertEquals(darkFile, resolveKfqpcFontPath(script, pageNo, isDark = true))
        assertEquals(lightFile, resolveKfqpcFontPath(script, pageNo, isDark = false))
    }

    @Test
    fun fallsBackToLightFontWhenDarkIsMissing() {
        AppFileSystem.writeText(lightFile, "light")

        assertEquals(lightFile, resolveKfqpcFontPath(script, pageNo, isDark = true))
    }

    @Test
    fun fallsBackToLegacyFilenameWhenNothingElseExists() {
        AppFileSystem.writeText(oldFile, "legacy")

        // Asserted by content, not by name: the legacy filename differs from the light one only in
        // case (`.TTF` vs `.ttf`), and iOS' filesystem is case-insensitive, so either name resolves
        // to the same file here. Android's case-sensitive filesystem is what needs the fallback.
        for (isDark in listOf(true, false)) {
            val resolved = resolveKfqpcFontPath(script, pageNo, isDark)
            assertNotNull(resolved, "legacy font should resolve (isDark=$isDark)")
            assertEquals("legacy", AppFileSystem.readText(resolved))
        }
    }

    @Test
    fun atlasScriptResolvesToDefaultFontFamily() {
        // Atlas scripts render from raster images, so they must never resolve a font.
        assertEquals(
            FontFamily.Default,
            FontResolver.getInstance().fontFamily(QuranScriptUtils.SCRIPT_UTHMANI, pageNo, isDark = false),
        )
    }
}
