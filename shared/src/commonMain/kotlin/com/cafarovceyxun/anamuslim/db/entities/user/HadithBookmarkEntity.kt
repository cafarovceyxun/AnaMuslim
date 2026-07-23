package com.cafarovceyxun.anamuslim.db.entities.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis

/**
 * Yadda saxlanılan hədis. Hədisin özü ayrıca (hadith) bazasındadır, burada yalnız istinad və
 * istifadəçinin qeydi saxlanılır; [title] siyahıda hədisi açmadan göstərmək üçün lazımdır.
 */
@Entity(
    tableName = "hadith_bookmarks",
    indices = [Index(value = ["hadith_id"], unique = true)],
)
data class HadithBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "hadith_id")
    val hadithId: Long,

    @ColumnInfo(name = "volume_slug")
    val volumeSlug: String? = null,

    @ColumnInfo(name = "book_slug")
    val bookSlug: String? = null,

    @ColumnInfo(name = "chapter_slug")
    val chapterSlug: String? = null,

    @ColumnInfo(name = "sub_chapter_slug")
    val subChapterSlug: String? = null,

    @ColumnInfo(name = "hadith_no")
    val hadithNo: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "preview")
    val preview: String? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "date")
    val dateTime: Long = currentEpochMillis(),
)
