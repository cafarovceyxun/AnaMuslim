package com.cafarovceyxun.anamuslim.db

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runtime proof that the real UserDatabase works on iOS/native: entity CRUD, the offset
 * paged-list queries that back the app-side PagingSource, and the migrations ported from
 * `SupportSQLiteDatabase` to `SQLiteConnection`. Third real database migrated to commonMain,
 * and the first one carrying manual Room migrations.
 */
class UserDatabaseTest {

    private fun db() = Room.inMemoryDatabaseBuilder<UserDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

    @Test
    fun crudAndOffsetPagingRoundTrip() = runBlocking {
        val db = db()
        val bookmarks = db.bookmarkDao()
        val readHistory = db.readHistoryDao()
        val hadithHistory = db.hadithReadHistoryDao()

        // Bookmarks: insert + offset paging window.
        repeat(25) { i ->
            bookmarks.insert(
                BookmarkEntity(chapterNo = 1, fromVerseNo = i, toVerseNo = i, note = "n$i")
            )
        }
        assertEquals(25, bookmarks.countBookmarks())
        val firstPage = bookmarks.getBookmarksPaged(limit = 20, offset = 0)
        val secondPage = bookmarks.getBookmarksPaged(limit = 20, offset = 20)
        assertEquals(20, firstPage.size)
        assertEquals(5, secondPage.size)
        // ORDER BY id DESC → newest first, no overlap between pages.
        assertTrue(firstPage.first().id > secondPage.first().id)

        // Read history paged query.
        readHistory.insert(ReadHistoryEntity(readType = "Chapter", readerMode = "Reading", chapterNo = 2))
        readHistory.insert(ReadHistoryEntity(readType = "Juz", readerMode = "Reading", divisionNo = 3))
        assertEquals(2, readHistory.countReadHistory())
        assertEquals(2, readHistory.getAllPaged(limit = 20, offset = 0).size)

        // Hadith read history + dedup.
        hadithHistory.insert(HadithReadHistoryEntity(volumeSlug = "bukhari", title = "Iman", datetime = 1L))
        hadithHistory.insert(HadithReadHistoryEntity(volumeSlug = "bukhari", title = "Iman", datetime = 2L))
        assertEquals(2, hadithHistory.countHadithReadHistory())
        hadithHistory.deleteDuplicate("bukhari", null, null, null)
        assertEquals(0, hadithHistory.countHadithReadHistory())

        db.close()
    }

    @Test
    fun portedMigrationsExecuteOnNative() {
        // Directly exercise the SQLiteConnection-based migration bodies on the bundled native
        // driver, proving the SupportSQLiteDatabase→SQLiteConnection port runs correctly.
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            UserDatabase.MIGRATION_1_2.migrate(connection)
            UserDatabase.MIGRATION_2_3.migrate(connection)
            UserDatabase.MIGRATION_3_4.migrate(connection)

            for (table in listOf("read_history", "hadith_read_history", "pending_user_activity")) {
                assertTrue(connection.hasTable(table), "migration did not create table $table")
            }

            // 4→5 drops the activity-telemetry table. Running it after 3→4 mirrors what a device
            // upgrading from v3 does: create on the way through, then drop.
            UserDatabase.MIGRATION_4_5.migrate(connection)

            assertFalse(
                connection.hasTable("pending_user_activity"),
                "MIGRATION_4_5 did not drop pending_user_activity",
            )
            for (table in listOf("read_history", "hadith_read_history")) {
                assertTrue(connection.hasTable(table), "MIGRATION_4_5 dropped $table by mistake")
            }
        } finally {
            connection.close()
        }
    }
}

private fun SQLiteConnection.hasTable(name: String): Boolean {
    val stmt = prepare("SELECT name FROM sqlite_master WHERE type='table' AND name = ?")
    return try {
        stmt.bindText(1, name)
        stmt.step()
    } finally {
        stmt.close()
    }
}
