package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cafarovceyxun.anamuslim.db.entities.hadith.*
import com.cafarovceyxun.anamuslim.db.relations.HadithChildCount

import kotlinx.coroutines.flow.Flow

@Dao
interface HadithDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVolumes(volumes: List<HadithVolumeEntity>)

    @Query("SELECT * FROM hadith_volumes")
    suspend fun getAllVolumes(): List<HadithVolumeEntity>

    @Query("SELECT * FROM hadith_volumes")
    fun getAllVolumesFlow(): Flow<List<HadithVolumeEntity>>

    @Query("SELECT * FROM hadith_volumes WHERE slug = :slug")
    suspend fun getVolumeBySlug(slug: String): HadithVolumeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<HadithBookEntity>)

    @Query("SELECT * FROM hadith_books")
    suspend fun getAllBooks(): List<HadithBookEntity>

    @Query("SELECT * FROM hadith_books")
    fun getAllBooksFlow(): Flow<List<HadithBookEntity>>

    @Query("SELECT * FROM hadith_books WHERE volume_slug = :volumeSlug ORDER BY book_no ASC")
    suspend fun getBooksByVolume(volumeSlug: String): List<HadithBookEntity>

    @Query("SELECT * FROM hadith_books WHERE slug = :slug")
    suspend fun getBookBySlug(slug: String): HadithBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<HadithChapterEntity>)

    @Query("SELECT * FROM hadith_chapters WHERE book_slug = :bookSlug ORDER BY chapter_no ASC")
    suspend fun getChaptersByBook(bookSlug: String): List<HadithChapterEntity>

    @Query("SELECT * FROM hadith_chapters WHERE book_slug IN (:bookSlugs) ORDER BY chapter_no ASC")
    suspend fun getChaptersByBooks(bookSlugs: List<String>): List<HadithChapterEntity>

    @Query("SELECT * FROM hadith_chapters WHERE slug = :slug")
    suspend fun getChapterBySlug(slug: String): HadithChapterEntity?

    @Query("SELECT * FROM hadith_chapters WHERE slug IN (:slugs) ORDER BY chapter_no ASC")
    suspend fun getChaptersBySlugs(slugs: List<String>): List<HadithChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubChapters(subChapters: List<HadithSubChapterEntity>)

    @Query("SELECT * FROM hadith_sub_chapters WHERE chapter_slug = :chapterSlug ORDER BY sub_chapter_no ASC")
    suspend fun getSubChaptersByChapter(chapterSlug: String): List<HadithSubChapterEntity>

    @Query("SELECT * FROM hadith_sub_chapters WHERE chapter_slug IN (:chapterSlugs) ORDER BY sub_chapter_no ASC")
    suspend fun getSubChaptersByChapters(chapterSlugs: List<String>): List<HadithSubChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    @Query("SELECT * FROM hadiths WHERE chapter_slug = :chapterSlug ORDER BY hadith_no ASC")
    suspend fun getHadithsByChapter(chapterSlug: String): List<HadithEntity>

    @Query("SELECT * FROM hadiths WHERE chapter_slug IN (:chapterSlugs) ORDER BY hadith_no ASC")
    suspend fun getHadithsByChapters(chapterSlugs: List<String>): List<HadithEntity>

    @Query("SELECT * FROM hadiths WHERE chapter_slug = :chapterSlug AND sub_chapter_slug = :subChapterSlug ORDER BY hadith_no ASC")
    suspend fun getHadithsBySubChapter(chapterSlug: String, subChapterSlug: String): List<HadithEntity>

    @Query("SELECT * FROM hadiths WHERE id = :id")
    suspend fun getHadithById(id: Long): HadithEntity?

    @Query("DELETE FROM hadith_volumes")
    suspend fun clearVolumes()

    @Query("DELETE FROM hadith_books")
    suspend fun clearBooks()

    @Query("DELETE FROM hadith_chapters")
    suspend fun clearChapters()

    @Query("DELETE FROM hadith_sub_chapters")
    suspend fun clearSubChapters()

    @Query("DELETE FROM hadiths")
    suspend fun clearHadiths()

    @Query("DELETE FROM hadith_books WHERE volume_slug = :volumeSlug")
    suspend fun deleteBooksByVolume(volumeSlug: String)

    @Query("DELETE FROM hadith_volumes WHERE slug = :slug")
    suspend fun deleteVolumeBySlug(slug: String)

    @Query("DELETE FROM hadith_chapters WHERE book_slug IN (SELECT slug FROM hadith_books WHERE volume_slug = :volumeSlug)")
    suspend fun deleteChaptersByVolume(volumeSlug: String)

    @Query("DELETE FROM hadith_sub_chapters WHERE chapter_slug IN (SELECT slug FROM hadith_chapters WHERE book_slug IN (SELECT slug FROM hadith_books WHERE volume_slug = :volumeSlug))")
    suspend fun deleteSubChaptersByVolume(volumeSlug: String)

    @Query("DELETE FROM hadiths WHERE chapter_slug IN (SELECT slug FROM hadith_chapters WHERE book_slug IN (SELECT slug FROM hadith_books WHERE volume_slug = :volumeSlug))")
    suspend fun deleteHadithsByVolume(volumeSlug: String)

    @Transaction
    suspend fun deleteFullVolume(volumeSlug: String) {
        deleteHadithsByVolume(volumeSlug)
        deleteSubChaptersByVolume(volumeSlug)
        deleteChaptersByVolume(volumeSlug)
        deleteBooksByVolume(volumeSlug)
        deleteVolumeBySlug(volumeSlug)
    }

    /**
     * Hadith full-text search.
     *
     * [arabicQuery] is the caller's query reduced by `SearchNormalizer.arabicNormalize`, and the
     * `replace(...)` stack applies that same reduction to `text_ar` inside SQLite. It has to happen
     * on the column: unlike the Quran's `arabic_search` index, `text_ar` is stored fully
     * diacritised, so a plain `LIKE` only ever matched a query whose harakat were identical to the
     * stored ones — typing `الرحيم` found nothing at all. Pass an empty [arabicQuery] for a query
     * with no Arabic in it, which skips the branch instead of matching every row on `LIKE '%%'`.
     *
     * The `char()` codepoints are, in order: harakat and Quranic marks U+064B–U+0655, the
     * superscript alef U+0670 and the tatweel U+0640; then the alif variants and alif maqṣūra
     * folded to their plain forms.
     */
    @Query(
        """
        SELECT * FROM hadiths
        WHERE text_az LIKE '%' || :query || '%'
           OR (
                :arabicQuery != '' AND
                replace(replace(replace(replace(replace(
                replace(replace(replace(replace(replace(
                replace(replace(replace(replace(replace(
                replace(replace(replace(
                    text_ar,
                    char(1611), ''), char(1612), ''), char(1613), ''),
                    char(1614), ''), char(1615), ''), char(1616), ''),
                    char(1617), ''), char(1618), ''), char(1619), ''),
                    char(1620), ''), char(1621), ''), char(1648), ''),
                    char(1600), ''),
                    char(1571), char(1575)), char(1573), char(1575)),
                    char(1570), char(1575)), char(1649), char(1575)),
                    char(1609), char(1610))
                LIKE '%' || :arabicQuery || '%'
              )
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun searchHadiths(
        query: String,
        arabicQuery: String,
        limit: Int,
        offset: Int,
    ): List<HadithEntity>

    @Query("SELECT * FROM hadith_volumes WHERE name LIKE '%' || :query || '%'")
    suspend fun searchVolumes(query: String): List<HadithVolumeEntity>

    @Query("SELECT * FROM hadith_books WHERE name LIKE '%' || :query || '%'")
    suspend fun searchBooks(query: String): List<HadithBookEntity>

    @Query("SELECT * FROM hadith_chapters WHERE name LIKE '%' || :query || '%'")
    suspend fun searchChapters(query: String): List<HadithChapterEntity>

    @Query("SELECT * FROM hadith_sub_chapters WHERE name LIKE '%' || :query || '%'")
    suspend fun searchSubChapters(query: String): List<HadithSubChapterEntity>

    @Query("SELECT MAX(hadith_no) FROM hadiths WHERE chapter_slug = :chapterSlug")
    suspend fun getMaxHadithNoByChapter(chapterSlug: String): Int?

    @Query("SELECT MAX(hadith_no) FROM hadiths WHERE chapter_slug = :chapterSlug AND sub_chapter_slug = :subChapterSlug")
    suspend fun getMaxHadithNoBySubChapter(chapterSlug: String, subChapterSlug: String): Int?

    @Query("SELECT MAX(chapter_no) FROM hadith_chapters WHERE book_slug = :bookSlug")
    suspend fun getMaxChapterNoByBook(bookSlug: String): Int?

    @Query("SELECT MAX(sub_chapter_no) FROM hadith_sub_chapters WHERE chapter_slug = :chapterSlug")
    suspend fun getMaxSubChapterNoByChapter(chapterSlug: String): Int?

    @Query("SELECT MAX(book_no) FROM hadith_books WHERE volume_slug = :volumeSlug")
    suspend fun getMaxBookNoByVolume(volumeSlug: String): Int?

    @Query("SELECT COUNT(*) FROM hadith_volumes")
    suspend fun getVolumeCount(): Int

    // Counters for the index cards ("how much is inside this level"). Each returns one row per
    // parent that actually has children, so a parent missing from the result simply holds none.

    @Query(
        """
        SELECT book_slug AS parentSlug, COUNT(*) AS childCount FROM hadith_chapters
        WHERE book_slug IN (:bookSlugs)
        GROUP BY book_slug
        """
    )
    suspend fun getChapterCountsByBooks(bookSlugs: List<String>): List<HadithChildCount>

    @Query(
        """
        SELECT chapter_slug AS parentSlug, COUNT(*) AS childCount FROM hadith_sub_chapters
        WHERE chapter_slug IN (:chapterSlugs)
        GROUP BY chapter_slug
        """
    )
    suspend fun getSubChapterCountsByChapters(chapterSlugs: List<String>): List<HadithChildCount>

    @Query(
        """
        SELECT chapter_slug AS parentSlug, COUNT(*) AS childCount FROM hadiths
        WHERE chapter_slug IN (:chapterSlugs)
        GROUP BY chapter_slug
        """
    )
    suspend fun getHadithCountsByChapters(chapterSlugs: List<String>): List<HadithChildCount>

    @Query(
        """
        SELECT sub_chapter_slug AS parentSlug, COUNT(*) AS childCount FROM hadiths
        WHERE sub_chapter_slug IN (:subChapterSlugs)
        GROUP BY sub_chapter_slug
        """
    )
    suspend fun getHadithCountsBySubChapters(subChapterSlugs: List<String>): List<HadithChildCount>

    @Transaction
    suspend fun clearAll() {
        clearVolumes()
        clearBooks()
        clearChapters()
        clearSubChapters()
        clearHadiths()
    }

    /**
     * Atomic full-sync replace. Clearing the old data and inserting the freshly
     * fetched data happen in a single transaction, so a worker that is killed
     * mid-sync can never leave the user with an empty or half-populated database.
     */
    @Transaction
    suspend fun replaceAll(
        volumes: List<HadithVolumeEntity>,
        books: List<HadithBookEntity>,
        chapters: List<HadithChapterEntity>,
        subChapters: List<HadithSubChapterEntity>,
        hadiths: List<HadithEntity>
    ) {
        clearAll()
        insertVolumes(volumes)
        insertBooks(books)
        insertChapters(chapters)
        insertSubChapters(subChapters)
        insertHadiths(hadiths)
    }
}
