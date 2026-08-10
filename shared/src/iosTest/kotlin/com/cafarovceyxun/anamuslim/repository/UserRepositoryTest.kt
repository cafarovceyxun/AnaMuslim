package com.cafarovceyxun.anamuslim.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cafarovceyxun.anamuslim.db.UserDatabase
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [UserRepository] against a real (in-memory) user database.
 *
 * This is the data a user would actually miss: their bookmarks and their reading history. None of
 * it fails loudly — a broken de-duplication just fills the history list with repeats, a broken
 * trim lets it grow without bound, and a bookmark that silently fails to register looks like a
 * missed tap. All of it is `commonMain` now, so iOS runs exactly these paths.
 *
 * Unlike [com.cafarovceyxun.anamuslim.db.TestQuranDatabase] this fixture is built per test: the
 * user database is written to, so tests must not see each other's rows. Nothing registers it with
 * `RepositoryProvider`, which is what makes a short-lived instance safe here.
 */
class UserRepositoryTest {

    private fun newRepository(): UserRepository = UserRepository(
        Room.inMemoryDatabaseBuilder<UserDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    )

    // ==================== bookmarks ============================================================

    @Test
    fun bookmarkingTheSameRangeTwiceIsReportedRatherThanDuplicated() = runTest {
        val repository = newRepository()

        assertEquals(BookmarkAddResult.Added, repository.addToBookmark(2, 255..255, "Ayat al-Kursi"))
        // The second attempt must be distinguishable from a failure — the UI shows a different
        // message for each.
        assertEquals(BookmarkAddResult.AlreadyBookmarked, repository.addToBookmark(2, 255..255, null))

        assertEquals(1, repository.getBookmarks().size)
        // The note of the existing bookmark is left alone; a re-add is not an edit.
        assertEquals("Ayat al-Kursi", repository.getBookmark(2, 255, 255)?.note)
    }

    /**
     * Bookmarks are keyed by the *exact* range, not by coverage. Bookmarking 2:255 does not make
     * 2:255-256 already-bookmarked, and vice versa — the reader's bookmark icon reflects the range
     * currently selected, so anything looser would light it up for a selection nobody saved.
     */
    @Test
    fun bookmarksAreKeyedByTheExactVerseRange() = runTest {
        val repository = newRepository()

        repository.addToBookmark(2, 255..255, null)
        assertEquals(BookmarkAddResult.Added, repository.addToBookmark(2, 255..256, null))

        assertEquals(2, repository.getBookmarks().size)
        assertTrue(repository.isBookmarked(2, 255..255))
        assertTrue(repository.isBookmarked(2, 255..256))
        // A range that merely overlaps or contains a saved one is not bookmarked.
        assertFalse(repository.isBookmarked(2, 254..255))
        assertFalse(repository.isBookmarked(2, 256..256))
        // Nor is the same range in another chapter.
        assertFalse(repository.isBookmarked(3, 255..255))
    }

    @Test
    fun removingABookmarkReportsWhetherAnythingMatched() = runTest {
        val repository = newRepository()
        repository.addToBookmark(1, 1..7, null)

        assertTrue(repository.removeFromBookmark(1, 1, 7))
        // Removing it again matches nothing — the caller needs to tell that apart from a success.
        assertFalse(repository.removeFromBookmark(1, 1, 7))
        assertFalse(repository.removeFromBookmark(1, 1, 6))
        assertEquals(0, repository.getBookmarks().size)
    }

    @Test
    fun bulkRemovalDeletesExactlyTheGivenIds() = runTest {
        val repository = newRepository()
        repository.addMultipleBookmarks(
            listOf(
                BookmarkEntity(chapterNo = 1, fromVerseNo = 1, toVerseNo = 1, note = null),
                BookmarkEntity(chapterNo = 2, fromVerseNo = 2, toVerseNo = 2, note = null),
                BookmarkEntity(chapterNo = 3, fromVerseNo = 3, toVerseNo = 3, note = null),
            )
        )

        val all = repository.getBookmarks()
        assertEquals(3, all.size)

        assertTrue(repository.removeBookmarksBulk(longArrayOf(all[0].id, all[1].id)))
        assertEquals(1, repository.getBookmarks().size)

        // Ids that match nothing, and an empty selection, both report "nothing removed".
        assertFalse(repository.removeBookmarksBulk(longArrayOf(9999L)))
        assertFalse(repository.removeBookmarksBulk(longArrayOf()))

        repository.removeAllBookmarks()
        assertEquals(0, repository.getBookmarks().size)
    }

    @Test
    fun editingANoteLeavesAMissingBookmarkAlone() = runTest {
        val repository = newRepository()
        repository.addToBookmark(2, 255..255, "first")

        repository.updateBookmark(2, 255, 255, "second")
        assertEquals("second", repository.getBookmark(2, 255, 255)?.note)

        // Clearing a note is a legitimate edit, not a delete.
        repository.updateBookmark(2, 255, 255, null)
        assertNull(repository.getBookmark(2, 255, 255)?.note)
        assertEquals(1, repository.getBookmarks().size)

        // Editing a bookmark that does not exist must not create one.
        repository.updateBookmark(9, 1, 1, "ghost")
        assertEquals(1, repository.getBookmarks().size)
        assertNull(repository.getBookmark(9, 1, 1))
    }

    @Test
    fun theBookmarkFlowFollowsInsertsAndRemovals() = runTest {
        val repository = newRepository()
        val flow = repository.isBookmarkedFlow(2, 255..255)

        assertFalse(flow.first())

        repository.addToBookmark(2, 255..255, null)
        assertTrue(flow.first())

        repository.removeFromBookmark(2, 255, 255)
        assertFalse(flow.first())
    }

    // ==================== read history =========================================================

    @Test
    fun rereadingTheSamePlaceMovesItUpInsteadOfDuplicating() = runTest {
        val repository = newRepository()

        repository.saveReadHistory(chapterHistory(chapterNo = 1, datetime = 1_000L))
        repository.saveReadHistory(chapterHistory(chapterNo = 2, datetime = 2_000L))
        repository.saveReadHistory(chapterHistory(chapterNo = 1, datetime = 3_000L))

        val histories = repository.getHistoriesFlow(10).first()

        // Three saves, two places — and the re-read one is now the most recent.
        assertEquals(2, histories.size)
        assertEquals(listOf(1, 2), histories.map { it.chapterNo })
        assertEquals(3_000L, histories.first().datetime)
    }

    /**
     * The de-duplication key is the *place* — read type, reader mode, division, chapter and verse
     * range. The mushaf page and script are carried along but are not part of it, so re-opening
     * the same verses in another script replaces the entry rather than adding a second one.
     */
    @Test
    fun historyDeduplicationIgnoresThePageAndTheScript() = runTest {
        val repository = newRepository()

        repository.saveReadHistory(
            chapterHistory(chapterNo = 1, datetime = 1_000L).copy(mushafCode = "uthmani", pageNo = 1)
        )
        repository.saveReadHistory(
            chapterHistory(chapterNo = 1, datetime = 2_000L).copy(mushafCode = "indopak", pageNo = 5)
        )

        val histories = repository.getHistoriesFlow(10).first()

        assertEquals(1, histories.size)
        // The surviving row is the newer one, with its own page and script.
        assertEquals("indopak", histories.first().mushafCode)
        assertEquals(5, histories.first().pageNo)
    }

    @Test
    fun aDifferentReaderModeOrVerseRangeIsADifferentEntry() = runTest {
        val repository = newRepository()

        repository.saveReadHistory(chapterHistory(chapterNo = 1, datetime = 1_000L))
        repository.saveReadHistory(
            chapterHistory(chapterNo = 1, datetime = 2_000L).copy(readerMode = "Reading")
        )
        repository.saveReadHistory(
            chapterHistory(chapterNo = 1, datetime = 3_000L).copy(fromVerseNo = 2, toVerseNo = 3)
        )
        repository.saveReadHistory(
            chapterHistory(chapterNo = 1, datetime = 4_000L).copy(readType = "Juz", divisionNo = 1)
        )

        assertEquals(4, repository.getHistoriesFlow(10).first().size)
    }

    /**
     * The history is capped at 40 entries, newest kept.
     *
     * Note the explicit, strictly increasing timestamps: the trim orders by `datetime`, and the
     * entity defaults that column to "now", so 45 saves inside one millisecond would tie and the
     * survivors would be arbitrary. Real use never inserts two entries in the same millisecond,
     * but a test that relied on the default would be flaky for a reason that looks like a bug.
     */
    @Test
    fun theHistoryIsCappedAtFortyEntries() = runTest {
        val repository = newRepository()

        for (chapterNo in 1..45) {
            repository.saveReadHistory(
                chapterHistory(chapterNo = chapterNo, datetime = 1_000L + chapterNo)
            )
        }

        val histories = repository.getHistoriesFlow(100).first()

        assertEquals(40, histories.size)
        // Newest first, and the five oldest chapters are the ones that fell off.
        assertEquals(45, histories.first().chapterNo)
        assertEquals(6, histories.last().chapterNo)
    }

    @Test
    fun historyEntriesCanBeRemovedOneByOneOrAllAtOnce() = runTest {
        val repository = newRepository()
        repository.saveReadHistory(chapterHistory(chapterNo = 1, datetime = 1_000L))
        repository.saveReadHistory(chapterHistory(chapterNo = 2, datetime = 2_000L))

        val first = repository.getHistoriesFlow(10).first().first()
        repository.deleteHistory(first.id)
        assertEquals(listOf(1), repository.getHistoriesFlow(10).first().map { it.chapterNo })

        repository.deleteAllHistories()
        assertEquals(0, repository.getHistoriesFlow(10).first().size)
    }

    // ==================== hadith read history ==================================================

    /**
     * Hadith history is keyed by four slugs, three of them nullable — and in SQL `x = NULL` is
     * never true. The DAO spells the comparison out as `(x = :p OR (x IS NULL AND :p IS NULL))`
     * for exactly that reason; simplifying it back to `=` would silently stop de-duplicating
     * volume-level entries, which are precisely the ones with null book/chapter slugs.
     */
    @Test
    fun hadithDeduplicationMatchesNullSlugsAsWell() = runTest {
        val repository = newRepository()

        repository.saveHadithReadHistory(
            HadithReadHistoryEntity(volumeSlug = "bukhari", title = "Iman", datetime = 1_000L)
        )
        repository.saveHadithReadHistory(
            HadithReadHistoryEntity(volumeSlug = "bukhari", title = "Iman", datetime = 2_000L)
        )

        val histories = repository.getHadithHistoriesFlow(10).first()
        assertEquals(1, histories.size)
        assertEquals(2_000L, histories.first().datetime)
    }

    @Test
    fun aDifferentHadithSlugIsADifferentEntry() = runTest {
        val repository = newRepository()

        repository.saveHadithReadHistory(
            HadithReadHistoryEntity(volumeSlug = "bukhari", title = "Iman", datetime = 1_000L)
        )
        // Same volume, but now down at book level — a null slug and a set slug must not collapse.
        repository.saveHadithReadHistory(
            HadithReadHistoryEntity(
                volumeSlug = "bukhari",
                bookSlug = "revelation",
                title = "Revelation",
                datetime = 2_000L,
            )
        )
        repository.saveHadithReadHistory(
            HadithReadHistoryEntity(volumeSlug = "muslim", title = "Iman", datetime = 3_000L)
        )

        assertEquals(3, repository.getHadithHistoriesFlow(10).first().size)
    }

    @Test
    fun theHadithHistoryIsCappedAtFortyEntriesToo() = runTest {
        val repository = newRepository()

        for (index in 1..45) {
            repository.saveHadithReadHistory(
                HadithReadHistoryEntity(
                    volumeSlug = "bukhari",
                    bookSlug = "book_$index",
                    title = "Book $index",
                    datetime = 1_000L + index,
                )
            )
        }

        val histories = repository.getHadithHistoriesFlow(100).first()

        assertEquals(40, histories.size)
        assertEquals("book_45", histories.first().bookSlug)
        assertEquals("book_6", histories.last().bookSlug)

        repository.deleteHadithHistory(histories.first().id)
        assertEquals(39, repository.getHadithHistoriesFlow(100).first().size)

        repository.deleteAllHadithHistories()
        assertEquals(0, repository.getHadithHistoriesFlow(100).first().size)
    }

    /** A verse-list reading entry, the shape the reader saves as the user scrolls. */
    private fun chapterHistory(chapterNo: Int, datetime: Long) = ReadHistoryEntity(
        readType = "Chapter",
        readerMode = "VerseByVerse",
        chapterNo = chapterNo,
        fromVerseNo = 1,
        toVerseNo = 7,
        datetime = datetime,
    )
}
