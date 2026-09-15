package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cafarovceyxun.anamuslim.db.entities.user.HadithReadProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HadithReadProgressDao {
    /** Eyni babı ikinci dəfə bitirmək sadəcə vaxtı yeniləyir. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HadithReadProgressEntity)

    @Query("SELECT * FROM hadith_read_progress")
    fun getAllFlow(): Flow<List<HadithReadProgressEntity>>

    @Query("SELECT * FROM hadith_read_progress")
    suspend fun getAll(): List<HadithReadProgressEntity>

    @Query("DELETE FROM hadith_read_progress WHERE node_slug = :nodeSlug")
    suspend fun delete(nodeSlug: String)

    @Query("DELETE FROM hadith_read_progress")
    suspend fun deleteAll()
}
