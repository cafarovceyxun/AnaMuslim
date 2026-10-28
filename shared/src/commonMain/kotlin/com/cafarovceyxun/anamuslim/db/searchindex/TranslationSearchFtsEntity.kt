package com.cafarovceyxun.anamuslim.db.searchindex

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * ⚠️ `remove_diacritics=1`, **2 yox**: ikinci rejim SQLite 3.27-də gəlib, Android isə sistemin öz
 * SQLite-ını işlədir — API 24–29-da (Android 7–10) 3.9–3.22-dir və tanımadığı seçimdə
 * `CREATE VIRTUAL TABLE` xəta verir. Yəni o telefonlarda indeks bazası ümumiyyətlə yaradılmır və
 * tərcümə axtarışı heç nə tapmırdı. Azərbaycan hərfləri (ö ü ç ş ğ) üçün iki rejim eynidir.
 * Hədis indeksi də eyni tokenizer-i işlədir ([com.cafarovceyxun.anamuslim.search.HadithSearchIndex]).
 */
@Fts4(
    contentEntity = TranslationSearchContentEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=1"]
)
@Entity(tableName = "translation_search_fts")
data class TranslationSearchFtsEntity(
    @ColumnInfo(name = "slug")
    val slug: String,

    @ColumnInfo(name = "surahNo")
    val surahNo: Int,

    @ColumnInfo(name = "ayahNo")
    val ayahNo: Int,

    @ColumnInfo(name = "text")
    val text: String,
)
