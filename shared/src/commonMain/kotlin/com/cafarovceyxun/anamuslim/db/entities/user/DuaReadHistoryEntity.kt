package com.cafarovceyxun.anamuslim.db.entities.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis

/**
 * Oxunan son dua — «Oxumağa davam et» sətri bunu göstərir.
 *
 * [title] və [preview] burada saxlanılır, çünki sətir **şəbəkəsiz** çəkilməlidir: dualar
 * Supabase-dədir və siyahı hələ yüklənməmiş ola bilər (əlfəcin sətrindəki qayda ilə eyni,
 * bax [DuaBookmarkEntity]).
 *
 * Açar duanın id-sidir: eyni duaya qayıtmaq yeni sətir yaratmır, sadəcə vaxtı yeniləyir.
 */
@Entity(tableName = "dua_read_history")
data class DuaReadHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "dua_id") val duaId: Long,
    @ColumnInfo(name = "group_key") val groupKey: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "preview") val preview: String? = null,
    @ColumnInfo(name = "datetime") val datetime: Long = currentEpochMillis(),
)
