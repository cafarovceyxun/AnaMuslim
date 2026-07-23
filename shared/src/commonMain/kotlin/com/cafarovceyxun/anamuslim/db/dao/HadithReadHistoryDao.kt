package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HadithReadHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HadithReadHistoryEntity): Long

    @Query("SELECT * FROM hadith_read_history ORDER BY datetime DESC LIMIT :limit")
    fun getFlow(limit: Int): Flow<List<HadithReadHistoryEntity>>

    // Offset paging: consumed by an app-side PagingSource (Room-KMP paging is not wired yet).
    @Query("SELECT * FROM hadith_read_history ORDER BY datetime DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllPaged(limit: Int, offset: Int): List<HadithReadHistoryEntity>

    @Query("SELECT COUNT(*) FROM hadith_read_history")
    suspend fun countHadithReadHistory(): Int

    @Query("DELETE FROM hadith_read_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM hadith_read_history WHERE volume_slug = :v AND (book_slug = :b OR (book_slug IS NULL AND :b IS NULL)) AND (chapter_slug = :c OR (chapter_slug IS NULL AND :c IS NULL)) AND (sub_chapter_slug = :s OR (sub_chapter_slug IS NULL AND :s IS NULL))")
    suspend fun deleteDuplicate(v: String, b: String?, c: String?, s: String?)

    @Query("DELETE FROM hadith_read_history WHERE id NOT IN (SELECT id FROM hadith_read_history ORDER BY datetime DESC LIMIT :limit)")
    suspend fun trimToSize(limit: Int)

    @Query("DELETE FROM hadith_read_history")
    suspend fun deleteAll()
}
