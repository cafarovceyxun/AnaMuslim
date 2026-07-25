package com.cafarovceyxun.anamuslim.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cafarovceyxun.anamuslim.db.migrations.ExternalQuranDatabaseMigrations
import com.cafarovceyxun.anamuslim.db.searchindex.SearchIndexDatabase
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlinx.coroutines.Dispatchers

/**
 * iOS counterpart of the Android [DatabaseProvider]: builds the Room databases with the bundled
 * SQLite driver at real `Documents/databases/` paths, mirroring the Android DB names, migrations
 * and destructive-fallback settings so the same schema/behaviour applies on both platforms.
 *
 * `QuranDatabase` (asset-backed) is intentionally omitted here — it needs the 24 MB `quranapp.db`
 * bundled into the iOS app and the `prepareQuranDatabaseFile()` copy step; that is wired when the
 * reader UI reaches iOS.
 */
object IosDatabaseProvider {

    private fun dbPath(name: String): String =
        (AppFileSystem.makeAndGetAppResourceDir("databases") / name).toString()

    val userDatabase: UserDatabase by lazy {
        Room.databaseBuilder<UserDatabase>(name = dbPath("user_db"))
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .addMigrations(
                UserDatabase.MIGRATION_1_2,
                UserDatabase.MIGRATION_2_3,
                UserDatabase.MIGRATION_3_4,
                UserDatabase.MIGRATION_4_5,
                UserDatabase.MIGRATION_5_6,
            )
            .build()
    }

    val externalQuranDatabase: ExternalQuranDatabase by lazy {
        Room.databaseBuilder<ExternalQuranDatabase>(name = dbPath("quranapp_external"))
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .addMigrations(
                ExternalQuranDatabaseMigrations.MIGRATION_1_2,
                ExternalQuranDatabaseMigrations.MIGRATION_2_3,
                ExternalQuranDatabaseMigrations.MIGRATION_3_4,
                ExternalQuranDatabaseMigrations.MIGRATION_4_5,
            )
            .build()
    }

    val searchIndexDatabase: SearchIndexDatabase by lazy {
        Room.databaseBuilder<SearchIndexDatabase>(name = dbPath("SearchIndex.db"))
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val hadithDatabase: HadithDatabase by lazy {
        Room.databaseBuilder<HadithDatabase>(name = dbPath("hadith_db"))
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /** Asset-backed read-only Quran DB, copied out of the app bundle on first launch. */
    val quranDatabase: QuranDatabase by lazy { createQuranDatabase() }
}
