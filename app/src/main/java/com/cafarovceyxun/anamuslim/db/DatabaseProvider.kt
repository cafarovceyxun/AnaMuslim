package com.cafarovceyxun.anamuslim.db

import android.content.Context
import androidx.room.Room
import com.cafarovceyxun.anamuslim.db.migrations.ExternalQuranDatabaseMigrations
import com.cafarovceyxun.anamuslim.db.searchindex.SearchIndexDatabase
import com.cafarovceyxun.anamuslim.db.translation.QuranTranslationDatabase
import com.cafarovceyxun.anamuslim.db.translation.QuranTranslationStore
import com.cafarovceyxun.anamuslim.repository.QuranRepository
import com.cafarovceyxun.anamuslim.repository.UserRepository
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.reader.TranslUtilsAndroid
import com.cafarovceyxun.anamuslim.utils.univ.FileUtils
import java.io.File

object DatabaseProvider {

    @Volatile
    private var userDatabase: UserDatabase? = null

    @Volatile
    private var userRepository: UserRepository? = null

    @Volatile
    private var quranDatabase: QuranDatabase? = null

    @Volatile
    private var quranRepository: QuranRepository? = null

    @Volatile
    private var externalQuranDatabase: ExternalQuranDatabase? = null

    @Volatile
    private var searchIndexDatabase: SearchIndexDatabase? = null

    @Volatile
    private var quranTranslationStore: QuranTranslationStore? = null

    @Volatile
    private var hadithDatabase: HadithDatabase? = null

    fun getUserDatabase(context: Context): UserDatabase {
        return userDatabase ?: synchronized(this) {
            userDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                UserDatabase::class.java,
                "user_db"
            )
                .addMigrations(
                    UserDatabase.MIGRATION_1_2, 
                    UserDatabase.MIGRATION_2_3,
                    UserDatabase.MIGRATION_3_4,
                    UserDatabase.MIGRATION_4_5,
                    UserDatabase.MIGRATION_5_6
                )
                .fallbackToDestructiveMigration(false)
                .build()
                .also { userDatabase = it }
        }
    }

    fun getUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(this) {
            userRepository ?: UserRepository(
                getUserDatabase(context)
            ).also { userRepository = it }
        }
    }

    private fun getQuranDatabase(context: Context): QuranDatabase {
        return quranDatabase ?: synchronized(this) {
            quranDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                QuranDatabase::class.java,
                "quranapp"
            )
                .createFromAsset("db/quranapp.db")
                .fallbackToDestructiveMigration(true)
                .build()
                .also { quranDatabase = it }
        }
    }

    fun getExternalQuranDatabase(context: Context): ExternalQuranDatabase {
        return externalQuranDatabase ?: synchronized(this) {
            externalQuranDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                ExternalQuranDatabase::class.java,
                "quranapp_external"
            )
                .addMigrations(
                    ExternalQuranDatabaseMigrations.MIGRATION_1_2,
                    ExternalQuranDatabaseMigrations.MIGRATION_2_3,
                    ExternalQuranDatabaseMigrations.MIGRATION_3_4,
                    ExternalQuranDatabaseMigrations.MIGRATION_4_5,
                )
                .fallbackToDestructiveMigration(false)
                .build()
                .also { externalQuranDatabase = it }
        }
    }

    fun getQuranRepository(context: Context): QuranRepository {
        return quranRepository ?: synchronized(this) {
            quranRepository ?: QuranRepository(
                getQuranDatabase(context),
                getExternalQuranDatabase(context)
            ).also { quranRepository = it }
        }
    }

    fun getSearchIndexDatabase(context: Context): SearchIndexDatabase {
        return searchIndexDatabase ?: synchronized(this) {
            searchIndexDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                SearchIndexDatabase::class.java,
                "SearchIndex.db",
            )
                .fallbackToDestructiveMigration(true)
                .build()
                .also { searchIndexDatabase = it }
        }
    }

    fun getQuranTranslationStore(context: Context): QuranTranslationStore {
        return quranTranslationStore ?: synchronized(this) {
            quranTranslationStore ?: run {
                val appContext = context.applicationContext
                val dbFile = appContext.getDatabasePath(QuranTranslationStore.DB_NAME)
                dbFile.parentFile?.mkdirs()
                QuranTranslationDatabase.open(dbFile.absolutePath) { store ->
                    migrateFileBasedTranslations(appContext, store)
                }.also { quranTranslationStore = it }
            }
        }
    }

    /**
     * One-time legacy migration (formerly `QuranTranslDBHelper.onCreate`): imports pre-DB
     * file-based translations into the store, then deletes the on-disk translation directory.
     * Runs only on a freshly created DB; a no-op when there are no legacy files.
     */
    private fun migrateFileBasedTranslations(context: Context, store: QuranTranslationStore) {
        val fileUtils = FileUtils.newInstance(context)
        val translDir = File(fileUtils.appFilesDirectory, TranslUtilsAndroid.DIR_NAME)
        if (!translDir.exists()) return

        try {
            TranslUtilsAndroid.getTranslInfosAndFilesForMigration(fileUtils, translDir)?.let {
                for (pair in it) {
                    store.storeTranslation(pair.first, pair.second.readText())
                }
            }
        } catch (e: Exception) {
            Log.saveError(e, "DatabaseProvider.migrateFileBasedTranslations")
            e.printStackTrace()
        } finally {
            translDir.deleteRecursively()
            Log.d("Migration finished anyhow, deleting root translation directory: " + translDir.name)
        }
    }

    fun getHadithDatabase(context: Context): HadithDatabase {
        return hadithDatabase ?: synchronized(this) {
            hadithDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                HadithDatabase::class.java,
                "hadith_db"
            )
                .fallbackToDestructiveMigration(true)
                .build()
                .also { hadithDatabase = it }
        }
    }

    fun closeAll() {
        synchronized(this) {
            userDatabase?.close(); userDatabase = null
            quranDatabase?.close(); quranDatabase = null
            externalQuranDatabase?.close(); externalQuranDatabase = null
            searchIndexDatabase?.close(); searchIndexDatabase = null
            hadithDatabase?.close(); hadithDatabase = null
            userRepository = null
            quranRepository = null
            quranTranslationStore = null
        }
    }
}
