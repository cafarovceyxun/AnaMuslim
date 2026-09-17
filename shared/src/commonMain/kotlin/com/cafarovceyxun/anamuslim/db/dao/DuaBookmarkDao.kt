package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cafarovceyxun.anamuslim.db.entities.user.DuaBookmarkEntity
import kotlinx.coroutines.flow.Flow

/** Bax [HadithBookmarkDao] — imzalar qəsdən eynidir, əlfəcinlər ekranı hər ikisini eyni idarə edir. */
@Dao
interface DuaBookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: DuaBookmarkEntity): Long

    @Update
    suspend fun update(bookmark: DuaBookmarkEntity): Int

    @Query("DELETE FROM dua_bookmarks WHERE dua_id = :duaId")
    suspend fun removeByDuaId(duaId: Long): Int

    /**
     * ⚠️ Açar **`dua_id`-dir**, sətrin öz `id`-si yox: siyahıdakı seçim də duanın id-si ilə aparılır
     * (`DuaBookmarkItemCard`). Sətir id-si ilə silmək kompilyasiyada da, testdə də sağlam görünür,
     * amma ekranda «silmək heç nə etmir» kimi çıxır.
     */
    @Query("DELETE FROM dua_bookmarks WHERE dua_id IN (:duaIds)")
    suspend fun removeBulk(duaIds: List<Long>): Int

    @Query("DELETE FROM dua_bookmarks")
    suspend fun removeAll()

    @Query("SELECT COUNT(*) FROM dua_bookmarks WHERE dua_id = :duaId")
    suspend fun count(duaId: Long): Int

    @Query("SELECT dua_id FROM dua_bookmarks")
    fun getBookmarkedIdsFlow(): Flow<List<Long>>

    @Query("SELECT * FROM dua_bookmarks WHERE dua_id = :duaId LIMIT 1")
    suspend fun get(duaId: Long): DuaBookmarkEntity?

    @Query("SELECT * FROM dua_bookmarks WHERE dua_id = :duaId LIMIT 1")
    fun getFlow(duaId: Long): Flow<DuaBookmarkEntity?>

    @Query("SELECT * FROM dua_bookmarks ORDER BY id DESC")
    fun getAllFlow(): Flow<List<DuaBookmarkEntity>>

    /** Ehtiyat nüsxə üçün birdəfəlik oxu — [getAllFlow] axını eksporta uyğun deyil. */
    @Query("SELECT * FROM dua_bookmarks ORDER BY id DESC")
    suspend fun getAll(): List<DuaBookmarkEntity>

    @Query("SELECT COUNT(*) FROM dua_bookmarks")
    suspend fun countAll(): Int
}
