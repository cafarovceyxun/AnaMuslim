package com.cafarovceyxun.anamuslim.db.entities.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Oxunub qurtarılmış **mövzu** — siyahılardakı ✓ nişanı bunu oxuyur.
 *
 * Vahid mövzudur (alt başlıq, alt başlıqsız başlıqda isə başlığın özü), ayrı-ayrı dualar yox:
 * istifadəçi «Namazdan sonra edilən zikr»i bitirir, «7 nömrəli dua»nı yox. Başlığın özü
 * **yazılmır** — onun tamamlanması alt mövzularından çıxarılır ([DuaReadProgress]), yoxsa yeni alt
 * mövzu əlavə olunanda «başlıq bitdi» sətri yalan danışardı (hədisdəki
 * [HadithReadProgressEntity] ilə eyni qayda).
 *
 * Tarixçədən ([DuaReadHistoryEntity]) fərqi: ora **harada qaldın**, bura **nəyi bitirdin**.
 */
@Entity(
    tableName = "dua_read_progress",
    indices = [Index(value = ["category_slug"])],
)
data class DuaReadProgressEntity(
    /** Alt başlığın slug-ı, alt başlıqsız başlıqda isə başlığın slug-ı (`DuaFlatEntry.groupKey`). */
    @PrimaryKey @ColumnInfo(name = "group_key") val groupKey: String,
    @ColumnInfo(name = "category_slug") val categorySlug: String,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
)
