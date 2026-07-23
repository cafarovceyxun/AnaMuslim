package com.cafarovceyxun.anamuslim.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.cafarovceyxun.anamuslim.db.dao.BookmarkDao
import com.cafarovceyxun.anamuslim.db.dao.HadithBookmarkDao
import com.cafarovceyxun.anamuslim.db.dao.HadithReadHistoryDao
import com.cafarovceyxun.anamuslim.db.dao.ReadHistoryDao
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithBookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity

@Database(
    entities = [
        BookmarkEntity::class,
        HadithBookmarkEntity::class,
        ReadHistoryEntity::class,
        HadithReadHistoryEntity::class
    ],
    version = 6,
    exportSchema = false,
)
@ConstructedBy(UserDatabaseConstructor::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun hadithBookmarkDao(): HadithBookmarkDao
    abstract fun readHistoryDao(): ReadHistoryDao
    abstract fun hadithReadHistoryDao(): HadithReadHistoryDao

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
    }
}

// The `actual` is generated by the Room compiler (KSP) for each target.
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpectedDeclaration")
expect object UserDatabaseConstructor : RoomDatabaseConstructor<UserDatabase> {
    override fun initialize(): UserDatabase
}
