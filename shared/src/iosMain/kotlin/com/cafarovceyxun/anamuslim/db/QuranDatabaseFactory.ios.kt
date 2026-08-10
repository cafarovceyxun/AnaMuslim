@file:OptIn(ExperimentalForeignApi::class)

package com.cafarovceyxun.anamuslim.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Must match the version in [QuranDatabase]'s @Database annotation. The freshly copied asset
 * carries user_version 0 (Room stamps the real version on first open), so 0 never appears here
 * for a database that has been opened at least once.
 */
private const val QURAN_DB_VERSION = 9L

/**
 * iOS equivalent of the Android builder's `createFromAsset("db/quranapp.db")` +
 * `fallbackToDestructiveMigration(true)`: copies the pre-packaged database out of the app
 * bundle on first launch, and re-copies it whenever the on-disk copy's version no longer
 * matches [QURAN_DB_VERSION].
 */
fun createQuranDatabase(
    assetPath: String? = NSBundle.mainBundle.pathForResource("quranapp", "db"),
): QuranDatabase {
    val asset = requireNotNull(assetPath) { "quranapp.db is missing from the app bundle" }
    val targetPath = quranDatabaseTargetPath()
    prepareQuranDatabaseFile(targetPath, asset)
    return Room.databaseBuilder<QuranDatabase>(name = targetPath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

private fun quranDatabaseTargetPath(): String {
    val appSupport = NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory, NSUserDomainMask, true
    ).first() as String
    val dbDir = "$appSupport/databases"
    NSFileManager.defaultManager.createDirectoryAtPath(
        dbDir, withIntermediateDirectories = true, attributes = null, error = null
    )
    return "$dbDir/quranapp"
}

internal fun prepareQuranDatabaseFile(targetPath: String, assetPath: String) {
    val fileManager = NSFileManager.defaultManager
    if (fileManager.fileExistsAtPath(targetPath)) {
        val version = readUserVersion(targetPath)
        // 0 = copied but never opened by Room yet; keep it and let Room stamp the version.
        if (version == 0L || version == QURAN_DB_VERSION) return
        for (path in listOf(targetPath, "$targetPath-wal", "$targetPath-shm")) {
            if (fileManager.fileExistsAtPath(path)) {
                fileManager.removeItemAtPath(path, error = null)
            }
        }
    }
    memScoped {
        val errorVar = alloc<ObjCObjectVar<NSError?>>()
        val copied = fileManager.copyItemAtPath(assetPath, toPath = targetPath, error = errorVar.ptr)
        check(copied) {
            "Failed to copy pre-packaged quranapp.db from $assetPath to $targetPath: " +
                (errorVar.value?.localizedDescription ?: "unknown error")
        }
    }
}

private fun readUserVersion(path: String): Long {
    val connection = BundledSQLiteDriver().open(path)
    return try {
        val statement = connection.prepare("PRAGMA user_version")
        try {
            if (statement.step()) statement.getLong(0) else 0L
        } finally {
            statement.close()
        }
    } finally {
        connection.close()
    }
}
