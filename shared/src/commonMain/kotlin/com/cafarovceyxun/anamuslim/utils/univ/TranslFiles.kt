package com.cafarovceyxun.anamuslim.utils.univ

import com.cafarovceyxun.anamuslim.utils.reader.TranslUtils
import okio.Path

/**
 * Translation locations under the app files dir, derived from [AppFileSystem] and the shared
 * [TranslUtils] directory names — no Context involved. Same approach as [ScriptFiles]; the app's
 * `FileUtils` delegates its manifest method here so both resolve one definition.
 */
object TranslFiles {

    fun translationDir(): Path = AppFileSystem.makeAndGetAppResourceDir(TranslUtils.DIR_NAME)

    /** The `available_downloads.json` manifest listing every downloadable translation. */
    fun translsManifestFile(): Path =
        AppFileSystem.makeAndGetAppResourceDir(TranslUtils.DIR_NAME_4_AVAILABLE_DOWNLOADS) /
            TranslUtils.TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME

    // String-returning accessor for the Java `FileUtils` call site.
    fun translsManifestFilePathString(): String = translsManifestFile().toString()
}
