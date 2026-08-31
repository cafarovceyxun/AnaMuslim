package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cafarovceyxun.anamuslim.db.entities.user.HadithBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HadithBookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: HadithBookmarkEntity): Long

    @Update
    suspend fun update(bookmark: HadithBookmarkEntity): Int

    @Query("DELETE FROM hadith_bookmarks WHERE hadith_id = :hadithId")
    suspend fun removeByHadithId(hadithId: Long): Int

    @Query("DELETE FROM hadith_bookmarks WHERE id IN (:ids)")
    suspend fun removeBulk(ids: List<Long>): Int

    @Query("DELETE FROM hadith_bookmarks")
    suspend fun removeAll()

    @Query("SELECT COUNT(*) FROM hadith_bookmarks WHERE hadith_id = :hadithId")
    suspend fun count(hadithId: Long): Int

    @Query("SELECT hadith_id FROM hadith_bookmarks")
    fun getBookmarkedIdsFlow(): Flow<List<Long>>

    @Query("SELECT * FROM hadith_bookmarks WHERE hadith_id = :hadithId LIMIT 1")
    suspend fun get(hadithId: Long): HadithBookmarkEntity?

    @Query("SELECT * FROM hadith_bookmarks WHERE hadith_id = :hadithId LIMIT 1")
    fun getFlow(hadithId: Long): Flow<HadithBookmarkEntity?>

    @Query("SELECT * FROM hadith_bookmarks ORDER BY id DESC")
    fun getAllFlow(): Flow<List<HadithBookmarkEntity>>

    /** Ehtiyat nüsxə üçün birdəfəlik oxu — [getAllFlow] axını eksporta uyğun deyil. */
    @Query("SELECT * FROM hadith_bookmarks ORDER BY id DESC")
    suspend fun getAll(): List<HadithBookmarkEntity>

    @Query("SELECT COUNT(*) FROM hadith_bookmarks")
    suspend fun countAll(): Int
}
