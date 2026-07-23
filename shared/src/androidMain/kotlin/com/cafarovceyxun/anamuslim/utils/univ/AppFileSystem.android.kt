package com.cafarovceyxun.anamuslim.utils.univ

import okio.FileSystem

/**
 * App-set holder for the Android private files directory. `QuranApp` assigns this from
 * `context.filesDir` at startup (same seam pattern as `DataStoreManager.init`), because
 * shared/androidMain has no direct access to a `Context`.
 */
object AndroidAppFiles {
    lateinit var filesDirPath: String
}

actual fun appFilesDirPath(): String = AndroidAppFiles.filesDirPath

internal actual fun platformFileSystem(): FileSystem = FileSystem.SYSTEM
