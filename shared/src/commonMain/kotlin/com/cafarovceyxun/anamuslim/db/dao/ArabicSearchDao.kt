package com.cafarovceyxun.anamuslim.db.dao

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import com.cafarovceyxun.anamuslim.db.entities.quran.ArabicSearchFtsEntity

@Dao
interface ArabicSearchDao {
    // Raw query because `arabic_search` is a standalone FTS4 table living only in the
    // pre-packaged asset; declaring it as a Room @Fts4 entity breaks native compilation.
    @RawQuery
    suspend fun pageMatchedAyahsRaw(query: RoomRawQuery): List<ArabicSearchFtsEntity>

    @RawQuery
    suspend fun countMatchedAyahsRaw(query: RoomRawQuery): List<Int>

    /**
     * Neçə ayə uyğun gəlir — sətirlərin özünü çəkmədən.
     *
     * Əsmaül Hüsnə siyahısı 99 adın **sayğacını** birdən göstərir; hər ad üçün 50-lik səhifə çəkib
     * saymaq həm yaddaş, həm də vaxt baxımından mənasız olardı («الله» ~2700 ayədə keçir).
     */
    suspend fun countMatchedAyahs(ftsQuery: String): Int = countMatchedAyahsRaw(
        RoomRawQuery(
            "SELECT COUNT(*) FROM arabic_search WHERE arabic_search MATCH ?",
        ) { statement ->
            statement.bindText(1, ftsQuery)
        }
    ).firstOrNull() ?: 0

    suspend fun pageMatchedAyahs(
        ftsQuery: String,
        limit: Int,
        offset: Int,
    ): List<ArabicSearchFtsEntity> = pageMatchedAyahsRaw(
        RoomRawQuery(
            """
            SELECT ayah_id, text FROM arabic_search
            WHERE arabic_search MATCH ?
            ORDER BY ayah_id
            LIMIT ? OFFSET ?
            """
        ) { statement ->
            statement.bindText(1, ftsQuery)
            statement.bindLong(2, limit.toLong())
            statement.bindLong(3, offset.toLong())
        }
    )
}
