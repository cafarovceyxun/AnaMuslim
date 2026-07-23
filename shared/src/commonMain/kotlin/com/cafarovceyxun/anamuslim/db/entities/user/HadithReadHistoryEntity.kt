package com.cafarovceyxun.anamuslim.db.entities.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hadith_read_history")
data class HadithReadHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "volume_slug") val volumeSlug: String,
    @ColumnInfo(name = "book_slug") val bookSlug: String? = null,
    @ColumnInfo(name = "chapter_slug") val chapterSlug: String? = null,
    @ColumnInfo(name = "sub_chapter_slug") val subChapterSlug: String? = null,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "datetime") val datetime: Long,
)
