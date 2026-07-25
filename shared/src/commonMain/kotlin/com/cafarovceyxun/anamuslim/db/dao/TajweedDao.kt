package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cafarovceyxun.anamuslim.db.entities.atlas.TajweedMetaEntity
import com.cafarovceyxun.anamuslim.db.entities.atlas.TajweedOverrideEntity
import com.cafarovceyxun.anamuslim.db.entities.atlas.TajweedWordColorEntity

@Dao
interface TajweedDao {

    @Query("SELECT * FROM tajweed_meta WHERE bundle_key = :bundleKey LIMIT 1")
    suspend fun getMeta(bundleKey: String): TajweedMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(row: TajweedMetaEntity)

    @Query("SELECT COUNT(*) FROM tajweed_word_colors WHERE bundle_key = :bundleKey")
    suspend fun countWordColors(bundleKey: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordColors(rows: List<TajweedWordColorEntity>)

    @Query(
        """
        SELECT * FROM tajweed_word_colors
        WHERE bundle_key = :bundleKey AND word IN (:words)
        """,
    )
    suspend fun getWordColors(bundleKey: String, words: List<String>): List<TajweedWordColorEntity>

    @Query("DELETE FROM tajweed_word_colors WHERE bundle_key = :bundleKey")
    suspend fun deleteWordColors(bundleKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverrides(rows: List<TajweedOverrideEntity>)

    @Query("SELECT * FROM tajweed_overrides WHERE ayah_id IN (:ayahIds)")
    suspend fun getOverridesForAyahs(ayahIds: List<Int>): List<TajweedOverrideEntity>

    @Query("DELETE FROM tajweed_overrides")
    suspend fun deleteAllOverrides()
}
