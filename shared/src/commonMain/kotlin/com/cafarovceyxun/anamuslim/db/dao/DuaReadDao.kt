package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cafarovceyxun.anamuslim.db.entities.user.DuaReadHistoryEntity
import com.cafarovceyxun.anamuslim.db.entities.user.DuaReadProgressEntity
import kotlinx.coroutines.flow.Flow

/** «Nəyi bitirdin» — bax [DuaReadProgressEntity]. */
@Dao
interface DuaReadProgressDao {

    /** Eyni mövzunu ikinci dəfə bitirmək sadəcə vaxtı yeniləyir. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DuaReadProgressEntity)

    @Query("SELECT * FROM dua_read_progress")
    fun getAllFlow(): Flow<List<DuaReadProgressEntity>>

    @Query("SELECT * FROM dua_read_progress")
    suspend fun getAll(): List<DuaReadProgressEntity>

    @Query("DELETE FROM dua_read_progress WHERE group_key = :groupKey")
    suspend fun delete(groupKey: String)

    @Query("DELETE FROM dua_read_progress")
    suspend fun deleteAll()
}

/** «Harada qaldın» — bax [DuaReadHistoryEntity]. */
@Dao
interface DuaReadHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DuaReadHistoryEntity)

    @Query("SELECT * FROM dua_read_history ORDER BY datetime DESC LIMIT 1")
    fun getLatestFlow(): Flow<DuaReadHistoryEntity?>

    @Query("SELECT * FROM dua_read_history ORDER BY datetime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DuaReadHistoryEntity>

    /**
     * Tavandan artıq sətirləri silir.
     *
     * Tarixçə **yığılan** cədvəldir: hər açılan dua sətir yazır, ona görə tavan olmasa siyahı
     * istifadə ilə birlikdə böyüyür. Silinən sətirlər ən köhnələridir.
     */
    @Query(
        "DELETE FROM dua_read_history WHERE dua_id NOT IN " +
            "(SELECT dua_id FROM dua_read_history ORDER BY datetime DESC LIMIT :limit)"
    )
    suspend fun trim(limit: Int)

    @Query("DELETE FROM dua_read_history")
    suspend fun deleteAll()
}
