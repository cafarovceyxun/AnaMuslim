package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cafarovceyxun.anamuslim.db.entities.user.QuranReadProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranReadProgressDao {
    /** Eyni surəni ikinci dəfə bitirmək sadəcə vaxtı yeniləyir. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuranReadProgressEntity)

    @Query("SELECT * FROM quran_read_progress")
    fun getAllFlow(): Flow<List<QuranReadProgressEntity>>

    @Query("SELECT * FROM quran_read_progress")
    suspend fun getAll(): List<QuranReadProgressEntity>

    @Query("DELETE FROM quran_read_progress WHERE node_key = :nodeKey")
    suspend fun delete(nodeKey: String)

    /** Xətm bitəndə yalnız surə nişanları sıfırlanır — cüz/hizb izi öz yerində qalır. */
    @Query("DELETE FROM quran_read_progress WHERE read_type = :readType")
    suspend fun deleteByType(readType: String)

    @Query("DELETE FROM quran_read_progress")
    suspend fun deleteAll()
}
