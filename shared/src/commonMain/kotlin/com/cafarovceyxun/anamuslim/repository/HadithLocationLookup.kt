package com.cafarovceyxun.anamuslim.repository

import com.cafarovceyxun.anamuslim.db.entities.hadith.toModel
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Hədisin ağacdakı yeri — cild → kitab → bab → alt bab.
 *
 * Hədis yalnız bab (və alt bab) slug-ı daşıyır, ona görə zəncir bir-bir geri qurulur. Tapılmayan
 * səviyyə `null` qalır: qırıq slug çağıranı dayandırmamalıdır, həmin ad sadəcə iştirak etmir.
 *
 * Niyə ViewModel-də deyil: axtarış nəticəsinin vərəqi də bu məlumatı istəyir, `HadithViewModel`-in
 * `init`-i isə sinxron mənbəyini işə salıb üç flow-a abunə olur — bir ad sorğusu üçün onu qurmaq
 * baha başa gəlir. [com.cafarovceyxun.anamuslim.viewModels.HadithViewModel.getHadithLocation]
 * elə bu funksiyanı çağırır, yəni iki yol eyni nəticəni verir.
 */
suspend fun loadHadithLocation(hadith: Hadith): HadithLocation = withContext(Dispatchers.IO) {
    val dao = RepositoryProvider.hadithDatabase.hadithDao()
    val chapterSlug = hadith.chapter_slug ?: return@withContext HadithLocation()

    val chapter = dao.getChapterBySlug(chapterSlug)?.toModel()
    val book = chapter?.let { dao.getBookBySlug(it.book_slug)?.toModel() }
    val volume = book?.let { dao.getVolumeBySlug(it.volume_slug)?.toModel() }
    val subSlug = hadith.sub_chapter_slug?.takeIf { it != "DIRECT_VIEW" }
    val subChapter = subSlug?.let { slug ->
        dao.getSubChaptersByChapter(chapterSlug).firstOrNull { it.slug == slug }?.toModel()
    }

    return@withContext HadithLocation(
        volume = volume,
        book = book,
        chapter = chapter,
        subChapter = subChapter,
    )
}
