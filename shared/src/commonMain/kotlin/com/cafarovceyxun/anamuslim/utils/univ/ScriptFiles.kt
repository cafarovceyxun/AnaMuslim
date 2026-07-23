package com.cafarovceyxun.anamuslim.utils.univ

import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import okio.Path

/**
 * Script/font locations under the app files dir, derived from [AppFileSystem] and the shared
 * [QuranScriptUtils] directory names — no Context involved.
 *
 * The app's `FileUtils` delegates its script methods here (via the `*PathString` accessors) so both
 * platforms resolve the exact same directories from one definition.
 */
object ScriptFiles {

    fun scriptDir(): Path = AppFileSystem.makeAndGetAppResourceDir(QuranScriptUtils.SCRIPT_DIR_NAME)

    fun scriptFile(scriptKey: String): Path = scriptDir() / "$scriptKey.json"

    fun scriptFontDir(): Path = AppFileSystem.makeAndGetAppResourceDir(QuranScriptUtils.FONTS_DIR_NAME)

    fun kfqpcScriptFontDir(scriptSlug: String): Path = scriptFontDir() / scriptSlug

    /** Script slugs that have a downloaded font directory. */
    fun downloadedScriptFontSlugs(): List<String> =
        AppFileSystem.listDirectories(scriptFontDir()).map { it.name }

    // String-returning accessors for the Java `FileUtils` call sites.
    fun scriptDirPathString(): String = scriptDir().toString()

    fun scriptFilePathString(scriptKey: String): String = scriptFile(scriptKey).toString()

    fun scriptFontDirPathString(): String = scriptFontDir().toString()

    fun kfqpcScriptFontDirPathString(scriptSlug: String): String = kfqpcScriptFontDir(scriptSlug).toString()
}
