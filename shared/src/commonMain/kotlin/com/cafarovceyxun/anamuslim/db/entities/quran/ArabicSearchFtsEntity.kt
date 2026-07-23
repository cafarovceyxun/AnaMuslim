package com.cafarovceyxun.anamuslim.db.entities.quran

import androidx.room.ColumnInfo

/**
 * Row from the standalone FTS4 table `arabic_search` shipped inside the pre-packaged
 * db/quranapp.db. Deliberately NOT a Room @Fts4 @Entity: a standalone FTS4 entity (one
 * without contentEntity) breaks the Room compiler on Kotlin/Native targets, and the table
 * is read-only static content, so it is queried via @RawQuery instead of being managed
 * (and schema-validated) by Room.
 */
data class ArabicSearchFtsEntity(
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,

    @ColumnInfo(name = "text")
    val text: String
)
