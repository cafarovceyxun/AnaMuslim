package com.cafarovceyxun.anamuslim.db.entities.hadith

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// NOTE: the `toEntity()` / `toModel()` mappers between these entities and the Supabase
// (`utils.supabase`) models live in the app module (`HadithEntityMappers.kt`, same package),
// because those models are Android `@Parcelize`/`Parcelable` and not yet in commonMain.

@Serializable
@Entity(tableName = "hadith_volumes")
data class HadithVolumeEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val author: String? = null,
    val description: String? = null,
    val updated_at: String? = null
)

@Serializable
@Entity(
    tableName = "hadith_books",
    indices = [Index(value = ["volume_slug", "book_no"])]
)
data class HadithBookEntity(
    @PrimaryKey val slug: String,
    val volume_slug: String,
    val book_no: Int,
    val name: String,
    val updated_at: String? = null
)

@Serializable
@Entity(
    tableName = "hadith_chapters",
    indices = [Index(value = ["book_slug", "chapter_no"])]
)
data class HadithChapterEntity(
    @PrimaryKey val slug: String,
    val book_slug: String,
    val chapter_no: Int,
    val name: String,
    val updated_at: String? = null
)

@Serializable
@Entity(
    tableName = "hadith_sub_chapters",
    indices = [Index(value = ["chapter_slug", "sub_chapter_no"])]
)
data class HadithSubChapterEntity(
    @PrimaryKey val slug: String,
    val chapter_slug: String,
    val sub_chapter_no: Int,
    val name: String,
    val updated_at: String? = null
)

@Serializable
@Entity(
    tableName = "hadiths",
    indices = [
        Index(value = ["chapter_slug", "hadith_no"]),
        Index(value = ["chapter_slug", "sub_chapter_slug", "hadith_no"])
    ]
)
data class HadithEntity(
    @PrimaryKey val id: Long,
    val chapter_slug: String? = null,
    val sub_chapter_slug: String? = null,
    val hadith_no: Int,
    val text_ar: String,
    val text_az: String,
    val source: String? = null,
    val note: String? = null,
    val updated_at: String? = null
)
