package com.cafarovceyxun.anamuslim.utils.supabase

import kotlinx.serialization.Serializable

@Serializable
data class HadithVolume(
    val slug: String, // Primary Key
    val name: String,
    val name_ar: String? = null,
    val author: String? = null,
    val description: String? = null,
    val updated_at: String? = null
)

@Serializable
data class HadithBook(
    val slug: String, // Primary Key
    val volume_slug: String,
    val book_no: Int,
    val name: String,
    val name_ar: String? = null,
    val updated_at: String? = null
)

@Serializable
data class HadithChapter(
    val slug: String, // Primary Key
    val book_slug: String,
    val chapter_no: Int,
    val name: String,
    val name_ar: String? = null,
    val updated_at: String? = null
)

@Serializable
data class HadithSubChapter(
    val slug: String, // Primary Key
    val chapter_slug: String,
    val sub_chapter_no: Int,
    val name: String,
    val name_ar: String? = null,
    val updated_at: String? = null
)

@Serializable
data class Hadith(
    val id: Long? = null,
    val chapter_slug: String? = null,
    val sub_chapter_slug: String? = null,
    val hadith_no: Int,
    val text_ar: String,
    val text_az: String,
    val source: String? = null,
    val note: String? = null,
    val updated_at: String? = null
)
