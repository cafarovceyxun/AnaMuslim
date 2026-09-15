package com.cafarovceyxun.anamuslim.db.entities.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Oxunub qurtarılmış **bab/alt-bab** — siyahılardakı ✓ nişanı bunu oxuyur.
 *
 * Yalnız **yarpaq** səviyyə saxlanılır: alt-babı olan babın özü yazılmır, onun tamamlanması
 * alt-bablarının sayından çıxarılır ([com.cafarovceyxun.anamuslim.viewModels.HadithProgress]).
 * Yuxarı səviyyələri də yazsaydıq iki həqiqət mənbəyi olardı: hədis əlavə edilən kimi alt-bab
 * yarımçıq qalır, amma «kitab bitdi» sətri yerində durardı.
 *
 * Tarixçədən ([HadithReadHistoryEntity]) fərqi: ora **harada qaldın**, bura **nəyi bitirdin**.
 * Biri son mövqedir və üzərinə yazılır, digəri yığılır.
 */
@Entity(
    tableName = "hadith_read_progress",
    indices = [Index(value = ["book_slug"]), Index(value = ["chapter_slug"])],
)
data class HadithReadProgressEntity(
    /** Alt-bab slug-ı, alt-babı olmayan babda isə babın öz slug-ı. */
    @PrimaryKey @ColumnInfo(name = "node_slug") val nodeSlug: String,
    @ColumnInfo(name = "volume_slug") val volumeSlug: String,
    @ColumnInfo(name = "book_slug") val bookSlug: String? = null,
    @ColumnInfo(name = "chapter_slug") val chapterSlug: String? = null,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
)
