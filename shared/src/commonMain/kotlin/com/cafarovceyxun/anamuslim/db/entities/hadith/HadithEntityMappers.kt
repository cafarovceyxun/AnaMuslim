package com.cafarovceyxun.anamuslim.db.entities.hadith

import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume

// Mappers between the commonMain Room entities (`HadithEntities.kt` in :shared) and the
// Supabase network models. These stay in the app module because the Supabase models are
// Android `@Parcelize`/`Parcelable`; they will move to commonMain once Parcelable is abstracted.

fun HadithVolume.toEntity() = HadithVolumeEntity(slug, name, author, description, updated_at)
fun HadithVolumeEntity.toModel() = HadithVolume(slug, name, author, description, updated_at)

fun HadithBook.toEntity() = HadithBookEntity(slug, volume_slug, book_no, name, updated_at)
fun HadithBookEntity.toModel() = HadithBook(slug, volume_slug, book_no, name, updated_at)

fun HadithChapter.toEntity() = HadithChapterEntity(slug, book_slug, chapter_no, name, updated_at)
fun HadithChapterEntity.toModel() = HadithChapter(slug, book_slug, chapter_no, name, updated_at)

fun HadithSubChapter.toEntity() = HadithSubChapterEntity(slug, chapter_slug, sub_chapter_no, name, updated_at)
fun HadithSubChapterEntity.toModel() = HadithSubChapter(slug, chapter_slug, sub_chapter_no, name, updated_at)

fun Hadith.toEntity() = id?.let { HadithEntity(it, chapter_slug, sub_chapter_slug, hadith_no, text_ar, text_az, source, note, updated_at) }
fun HadithEntity.toModel() = Hadith(id, chapter_slug, sub_chapter_slug, hadith_no, text_ar, text_az, source, note, updated_at)
