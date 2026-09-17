package com.cafarovceyxun.anamuslim.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.cafarovceyxun.anamuslim.db.dao.BookmarkDao
import com.cafarovceyxun.anamuslim.db.dao.DuaBookmarkDao
import com.cafarovceyxun.anamuslim.db.dao.HadithBookmarkDao
import com.cafarovceyxun.anamuslim.db.dao.HadithReadHistoryDao
import com.cafarovceyxun.anamuslim.db.dao.HadithReadProgressDao
import com.cafarovceyxun.anamuslim.db.dao.QuranReadProgressDao
import com.cafarovceyxun.anamuslim.db.dao.ReadHistoryDao
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.DuaBookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithBookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadProgressEntity
import com.cafarovceyxun.anamuslim.db.entities.user.QuranReadProgressEntity
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity

@Database(
    entities = [
        BookmarkEntity::class,
        HadithBookmarkEntity::class,
        ReadHistoryEntity::class,
        HadithReadHistoryEntity::class,
        HadithReadProgressEntity::class,
        QuranReadProgressEntity::class,
        DuaBookmarkEntity::class
    ],
    version = 9,
    exportSchema = false,
)
@ConstructedBy(UserDatabaseConstructor::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun hadithBookmarkDao(): HadithBookmarkDao
    abstract fun readHistoryDao(): ReadHistoryDao
    abstract fun hadithReadHistoryDao(): HadithReadHistoryDao
    abstract fun hadithReadProgressDao(): HadithReadProgressDao
    abstract fun quranReadProgressDao(): QuranReadProgressDao
    abstract fun duaBookmarkDao(): DuaBookmarkDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `read_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `read_type` TEXT NOT NULL,
                        `reader_mode` TEXT NOT NULL,
                        `division_no` INTEGER NOT NULL DEFAULT 0,
                        `chapter_no` INTEGER NOT NULL DEFAULT 0,
                        `from_verse_no` INTEGER NOT NULL DEFAULT 0,
                        `to_verse_no` INTEGER NOT NULL DEFAULT 0,
                        `mushaf_id` INTEGER NOT NULL DEFAULT 0,
                        `page_no` INTEGER,
                        `datetime` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hadith_read_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `volume_slug` TEXT NOT NULL,
                        `book_slug` TEXT,
                        `chapter_slug` TEXT,
                        `sub_chapter_slug` TEXT,
                        `title` TEXT NOT NULL,
                        `datetime` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_user_activity` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Drops `pending_user_activity`. The activity telemetry it backed was removed outright,
         * so the table has no writer left. [MIGRATION_3_4] is deliberately left untouched — a
         * device coming from v3 still creates the table on the way through, then drops it here.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS `pending_user_activity`")
            }
        }

        /** Hədislərin yadda saxlanılması üçün `hadith_bookmarks`. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hadith_bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `hadith_id` INTEGER NOT NULL,
                        `volume_slug` TEXT,
                        `book_slug` TEXT,
                        `chapter_slug` TEXT,
                        `sub_chapter_slug` TEXT,
                        `hadith_no` INTEGER NOT NULL DEFAULT 0,
                        `title` TEXT NOT NULL,
                        `preview` TEXT,
                        `note` TEXT,
                        `date` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_hadith_bookmarks_hadith_id` " +
                        "ON `hadith_bookmarks` (`hadith_id`)"
                )
            }
        }

        /**
         * Oxunub qurtarılmış bab/alt-bablar üçün `hadith_read_progress`.
         *
         * Köhnə cihazda cədvəl boş başlayır: keçmiş oxunuşlar üçün «bitdi» məlumatı heç vaxt
         * saxlanılmayıb, ona görə geriyə doğru bərpa etmək mümkün deyil — nişanlar yenidən oxuduqca
         * dolur.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hadith_read_progress` (
                        `node_slug` TEXT PRIMARY KEY NOT NULL,
                        `volume_slug` TEXT NOT NULL,
                        `book_slug` TEXT,
                        `chapter_slug` TEXT,
                        `completed_at` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_hadith_read_progress_book_slug` " +
                        "ON `hadith_read_progress` (`book_slug`)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_hadith_read_progress_chapter_slug` " +
                        "ON `hadith_read_progress` (`chapter_slug`)"
                )
            }
        }

        /**
         * Oxunub qurtarılmış surə/cüz/hizb üçün `quran_read_progress`.
         *
         * Hədis tərəfindəki [MIGRATION_6_7] ilə eyni məntiq: köhnə cihazda cədvəl boş başlayır,
         * çünki keçmiş oxunuşlar üçün «bitdi» məlumatı heç vaxt saxlanılmayıb — nişanlar yenidən
         * oxuduqca dolur.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quran_read_progress` (
                        `node_key` TEXT PRIMARY KEY NOT NULL,
                        `read_type` TEXT NOT NULL,
                        `node_no` INTEGER NOT NULL DEFAULT 0,
                        `completed_at` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dua_bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dua_id` INTEGER NOT NULL,
                        `category_slug` TEXT,
                        `subcategory_slug` TEXT,
                        `title` TEXT NOT NULL,
                        `preview` TEXT,
                        `note` TEXT,
                        `date` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // Unikal indeks sxemdəki `@Index(unique = true)`-in qarşılığıdır: onsuz Room-un
                // sxem yoxlaması miqrasiyadan sonra uyğunsuzluq atır (`IllegalStateException`).
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_dua_bookmarks_dua_id` " +
                        "ON `dua_bookmarks` (`dua_id`)"
                )
            }
        }
    }
}

// The `actual` is generated by the Room compiler (KSP) for each target.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpectedDeclaration")
expect object UserDatabaseConstructor : RoomDatabaseConstructor<UserDatabase> {
    override fun initialize(): UserDatabase
}
