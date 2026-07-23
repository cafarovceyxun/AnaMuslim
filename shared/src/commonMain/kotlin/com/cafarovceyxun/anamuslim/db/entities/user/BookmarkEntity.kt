package com.cafarovceyxun.anamuslim.db.entities.user

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis

@Entity(tableName = "user_bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "chapter_no")
    val chapterNo: Int,

    @ColumnInfo(name = "from_verse_no")
    val fromVerseNo: Int,

    @ColumnInfo(name = "to_verse_no")
    val toVerseNo: Int,

    @ColumnInfo(name = "note")
    val note: String?,

    // Epoch milliseconds. Historically a `java.util.Date` stored via a Room TypeConverter as an
    // INTEGER column; now a plain Long so the entity is KMP-safe. The column type is unchanged.
    @ColumnInfo(name = "date")
    val dateTime: Long = currentEpochMillis(),
)


@Immutable
data class BookmarkKey(
    val chapterNo: Int,
    val fromVerse: Int,
    val toVerse: Int
)
