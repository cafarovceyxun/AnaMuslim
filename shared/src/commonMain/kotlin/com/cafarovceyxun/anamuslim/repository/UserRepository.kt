package com.cafarovceyxun.anamuslim.repository

import com.cafarovceyxun.anamuslim.db.UserDatabase
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.BookmarkKey
import com.cafarovceyxun.anamuslim.db.entities.user.HadithBookmarkEntity
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking

/** Outcome of a bookmark-insert; the UI layer maps this to a user-facing message. */
enum class BookmarkAddResult { AlreadyBookmarked, Added, Failed }

class UserRepository(
    private val database: UserDatabase
) {
    /** Exposed for the Android-only paging extensions (androidx.paging lives in `:app`). */
    val userDatabase: UserDatabase get() = database

    private val bookmarkDao get() = database.bookmarkDao()
    private val hadithBookmarkDao get() = database.hadithBookmarkDao()
    private val readHistoryDao get() = database.readHistoryDao()
    private val hadithReadHistoryDao get() = database.hadithReadHistoryDao()

    companion object {
        private const val HISTORY_LIMIT = 40
        private const val HADITH_HISTORY_LIMIT = 40
    }

    suspend fun addMultipleBookmarks(bookmarks: List<BookmarkEntity>) {
        bookmarkDao.insertAll(bookmarks)
    }

    /**
     * Ehtiyat nüsxədən gələn əlfəcinlərdən **yalnız cihazda olmayanları** əlavə edir və neçəsinin
     * əlavə olunduğunu qaytarır.
     *
     * `insertAll` ilə fərqi budur ki, o hər sətri yeni id ilə yazır: eyni faylı iki dəfə import edən
     * istifadəçi əlfəcinlərinin **ikiqat** siyahısını alırdı. Açar sətrin id-si deyil — id yerli
     * bazanın işidir — ayə aralığının özüdür.
     */
    suspend fun addMissingBookmarks(bookmarks: List<BookmarkEntity>): Int {
        if (bookmarks.isEmpty()) return 0

        val existing = bookmarkDao.getBookmarks()
            .map { BookmarkKey(it.chapterNo, it.fromVerseNo, it.toVerseNo) }
            .toHashSet()

        val missing = bookmarks.filter {
            existing.add(BookmarkKey(it.chapterNo, it.fromVerseNo, it.toVerseNo))
        }

        if (missing.isEmpty()) return 0

        bookmarkDao.insertAll(missing.map { it.copy(id = 0) })
        return missing.size
    }

    suspend fun addToBookmark(
        chapterNo: Int,
        verseRange: IntRange,
        note: String?,
    ): BookmarkAddResult {
        if (isBookmarked(chapterNo, verseRange)) {
            return BookmarkAddResult.AlreadyBookmarked
        }

        val entity = BookmarkEntity(
            chapterNo = chapterNo,
            fromVerseNo = verseRange.first,
            toVerseNo = verseRange.last,
            note = note
        )

        val rowId = bookmarkDao.insert(entity)
        val inserted = rowId != -1L

        return if (inserted) BookmarkAddResult.Added else BookmarkAddResult.Failed
    }

    fun addToBookmarkBlocking(
        chapterNo: Int,
        verseRange: IntRange,
        note: String?,
    ): BookmarkAddResult = runBlocking {
        addToBookmark(chapterNo, verseRange, note)
    }

    suspend fun updateBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        note: String?,
    ) {
        val existing = bookmarkDao.getBookmark(chapterNo, fromVerse, toVerse) ?: return
        bookmarkDao.updateBookmark(existing.copy(note = note))
    }

    suspend fun removeFromBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): Boolean {
        val rowsAffected = bookmarkDao.removeBookmark(chapterNo, fromVerse, toVerse)
        return rowsAffected >= 1
    }

    fun removeFromBookmarkBlocking(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): Boolean = runBlocking {
        removeFromBookmark(chapterNo, fromVerse, toVerse)
    }

    suspend fun removeBookmarksBulk(
        ids: LongArray,
    ): Boolean {
        val rowsAffected = bookmarkDao.removeBookmarksBulk(ids.toList())
        return rowsAffected >= 1
    }

    suspend fun isBookmarked(
        chapterNo: Int,
        verseRange: IntRange
    ): Boolean {
        return bookmarkDao.countBookmark(chapterNo, verseRange.first, verseRange.last) > 0
    }

    fun isBookmarkedBlocking(
        chapterNo: Int,
        verseRange: IntRange
    ): Boolean = runBlocking {
        isBookmarked(chapterNo, verseRange)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isBookmarkedFlow(
        chapterNo: Int,
        verseRange: IntRange
    ): Flow<Boolean> {
        return bookmarkDao.countBookmarkFlow(chapterNo, verseRange.first, verseRange.last)
            .mapLatest { it > 0 }
    }

    suspend fun removeAllBookmarks() {
        bookmarkDao.removeAllBookmarks()
    }

    suspend fun getBookmark(
        chapNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): BookmarkEntity? {
        return bookmarkDao.getBookmark(chapNo, fromVerse, toVerse)
    }

    suspend fun getBookmarks(): ArrayList<BookmarkEntity> {
        return ArrayList(bookmarkDao.getBookmarks())
    }

    fun getBookmarksFlow(): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksFlow()
    }

    fun getBookmarkFlow(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Flow<BookmarkEntity?> {
        return bookmarkDao.getBookmarkFlow(chapterNo, fromVerse, toVerse)
    }

    // ── Read History ──
    suspend fun saveReadHistory(entity: ReadHistoryEntity) {
        readHistoryDao.deleteDuplicate(
            readType = entity.readType,
            readerMode = entity.readerMode,
            divisionNo = entity.divisionNo,
            chapterNo = entity.chapterNo,
            fromVerseNo = entity.fromVerseNo,
            toVerseNo = entity.toVerseNo,
        )
        readHistoryDao.insert(entity)
        readHistoryDao.trimToSize(HISTORY_LIMIT)
    }

    fun getHistoriesFlow(limit: Int): Flow<List<ReadHistoryEntity>> {
        return readHistoryDao.getFlow(limit)
    }

    suspend fun deleteHistory(id: Long) {
        readHistoryDao.deleteById(id)
    }

    suspend fun deleteAllHistories() {
        readHistoryDao.deleteAll()
    }

    /** Ehtiyat nüsxə üçün oxuma tarixçəsi; cədvəl onsuz da [HISTORY_LIMIT] sətirdə saxlanılır. */
    suspend fun getReadHistories(): List<ReadHistoryEntity> =
        readHistoryDao.getAllPaged(HISTORY_LIMIT, 0)

    suspend fun saveHadithReadHistory(entity: HadithReadHistoryEntity) {
        hadithReadHistoryDao.deleteDuplicate(entity.volumeSlug, entity.bookSlug, entity.chapterSlug, entity.subChapterSlug)
        hadithReadHistoryDao.insert(entity)
        hadithReadHistoryDao.trimToSize(HADITH_HISTORY_LIMIT)
    }

    fun getHadithHistoriesFlow(limit: Int): Flow<List<HadithReadHistoryEntity>> {
        return hadithReadHistoryDao.getFlow(limit)
    }

    /** Cild slug-ı → həmin cilddə ən son oxunan yer. Boş cildlər xəritədə olmur. */
    fun getLatestHadithHistoryPerVolumeFlow(): Flow<Map<String, HadithReadHistoryEntity>> {
        return hadithReadHistoryDao.getLatestPerVolumeFlow()
            .map { list -> list.associateBy { it.volumeSlug } }
    }

    suspend fun deleteHadithHistory(id: Long) {
        hadithReadHistoryDao.deleteById(id)
    }

    suspend fun deleteAllHadithHistories() {
        hadithReadHistoryDao.deleteAll()
    }

    /** Ehtiyat nüsxə üçün hədis oxuma tarixçəsi. */
    suspend fun getHadithReadHistories(): List<HadithReadHistoryEntity> =
        hadithReadHistoryDao.getAllPaged(HADITH_HISTORY_LIMIT, 0)

    // region hədis yadda saxlama

    /**
     * Hədisi yadda saxlayır. Eyni hədis artıq saxlanılıbsa [BookmarkAddResult.AlreadyBookmarked]
     * qaytarır — `hadith_id` unikal indeksdir, ona görə təkrar yazma cəhdi edilmir.
     */
    suspend fun addHadithBookmark(
        hadithId: Long,
        volumeSlug: String?,
        bookSlug: String?,
        chapterSlug: String?,
        subChapterSlug: String?,
        hadithNo: Int,
        title: String,
        preview: String?,
        note: String?,
    ): BookmarkAddResult {
        if (isHadithBookmarked(hadithId)) {
            return BookmarkAddResult.AlreadyBookmarked
        }

        val rowId = hadithBookmarkDao.insert(
            HadithBookmarkEntity(
                hadithId = hadithId,
                volumeSlug = volumeSlug,
                bookSlug = bookSlug,
                chapterSlug = chapterSlug,
                subChapterSlug = subChapterSlug,
                hadithNo = hadithNo,
                title = title,
                preview = preview,
                note = note,
            )
        )

        return if (rowId != -1L) BookmarkAddResult.Added else BookmarkAddResult.Failed
    }

    suspend fun isHadithBookmarked(hadithId: Long): Boolean =
        hadithBookmarkDao.count(hadithId) > 0

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isHadithBookmarkedFlow(hadithId: Long): Flow<Boolean> =
        hadithBookmarkDao.getFlow(hadithId).mapLatest { it != null }

    fun getHadithBookmarkFlow(hadithId: Long): Flow<HadithBookmarkEntity?> =
        hadithBookmarkDao.getFlow(hadithId)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getBookmarkedHadithIdsFlow(): Flow<Set<Long>> =
        hadithBookmarkDao.getBookmarkedIdsFlow().mapLatest { it.toSet() }

    fun getHadithBookmarksFlow(): Flow<List<HadithBookmarkEntity>> =
        hadithBookmarkDao.getAllFlow()

    suspend fun updateHadithBookmarkNote(hadithId: Long, note: String?) {
        val existing = hadithBookmarkDao.get(hadithId) ?: return
        hadithBookmarkDao.update(existing.copy(note = note))
    }

    suspend fun removeHadithBookmark(hadithId: Long): Boolean =
        hadithBookmarkDao.removeByHadithId(hadithId) >= 1

    suspend fun removeHadithBookmarksBulk(ids: List<Long>): Int =
        hadithBookmarkDao.removeBulk(ids)

    suspend fun removeAllHadithBookmarks() {
        hadithBookmarkDao.removeAll()
    }

    suspend fun countHadithBookmarks(): Int = hadithBookmarkDao.countAll()

    /** Ehtiyat nüsxə üçün bütün hədis əlfəcinləri. */
    suspend fun getHadithBookmarks(): List<HadithBookmarkEntity> = hadithBookmarkDao.getAll()

    /**
     * [addMissingBookmarks]-in hədis qarşılığı; açar `hadith_id`-dir. Mövcud sətir **əvəzlənmir**:
     * cihazdakı qeyd faylın qeydindən daha yeni ola bilər.
     */
    suspend fun addMissingHadithBookmarks(bookmarks: List<HadithBookmarkEntity>): Int {
        if (bookmarks.isEmpty()) return 0

        val existing = hadithBookmarkDao.getAll().map { it.hadithId }.toHashSet()
        var added = 0

        bookmarks.forEach { bookmark ->
            if (!existing.add(bookmark.hadithId)) return@forEach
            if (hadithBookmarkDao.insert(bookmark.copy(id = 0)) != -1L) added++
        }

        return added
    }

    // endregion

}
