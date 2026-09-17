package com.cafarovceyxun.anamuslim.db.entities.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis

/**
 * Yadda saxlanılan dua. Duanın özü Supabase-dədir, burada yalnız istinad və istifadəçinin qeydi
 * qalır; [title] və [preview] siyahını **şəbəkəsiz** çəkmək üçündür — əlfəcinlər ekranı duanı
 * açmadan göstərilməlidir (hədis əlfəcini ilə eyni qayda, bax [HadithBookmarkEntity]).
 *
 * ⚠️ Başlıq/alt başlıq **slug** kimi saxlanılır, adın özü isə [title]-dədir: admin başlığın adını
 * dəyişəndə əlfəcin sətri köhnə adı göstərməyə davam edir, amma keçid yenə düz yerə aparır.
 */
@Entity(
    tableName = "dua_bookmarks",
    indices = [Index(value = ["dua_id"], unique = true)],
)
data class DuaBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "dua_id")
    val duaId: Long,

    @ColumnInfo(name = "category_slug")
    val categorySlug: String? = null,

    @ColumnInfo(name = "subcategory_slug")
    val subcategorySlug: String? = null,

    /** Mövzunun adı — siyahıda duanın üstündə duran sətir. */
    @ColumnInfo(name = "title")
    val title: String,

    /** Duanın tərcüməsindən (yoxdursa ərəbcəsindən) kəsilmiş önizləmə. */
    @ColumnInfo(name = "preview")
    val preview: String? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "date")
    val dateTime: Long = currentEpochMillis(),
)
