package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.repository.BookmarkAddResult
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

/**
 * «Günün ayəsi» hekayəsindəki **yadda saxlama** düyməsinin arxası.
 *
 * Ayrıca yerdədir, çünki iki fərqli əlfəcin növünü bir davranışa yığır: ayə aralığı üçün
 * `bookmarks`, hədis üçün isə `hadith_bookmarks` — sonuncusu kitab/cild slug-larını tələb edir və
 * onlar növbə sətrində yoxdur, cihazdakı hədis bazasından bərpa olunur.
 *
 * Hekayə oxucunun `LocalVerseActions` seam-inin altında **deyil** (o, `ReaderProvider`-ə bağlıdır),
 * ona görə repozitoriya birbaşa çağırılır — seam qurmaq bir düymə üçün artıq olardı.
 */
object DailyContentBookmarks {

    /** Element artıq yadda saxlanılıbmı. Dəstəklənməyən element üçün həmişə `false` axını. */
    fun isBookmarkedFlow(content: DailyContent): Flow<Boolean> {
        val repository = RepositoryProvider.userRepository

        return if (content.isHadith) {
            content.hadith_id?.let(repository::isHadithBookmarkedFlow) ?: flowOf(false)
        } else {
            val chapterNo = content.chapter_no
            val verses = content.verseNumbers

            if (chapterNo == null || verses.isEmpty()) {
                flowOf(false)
            } else {
                repository.isBookmarkedFlow(chapterNo, verses.first()..verses.last())
            }
        }
    }

    /**
     * Əlfəcini açıb-bağlayır və **yeni vəziyyəti** qaytarır (`true` = saxlanıldı).
     *
     * Çoxayəli element bir əlfəcin kimi saxlanılır — aralığın özü açardır, ona görə oxucudakı
     * «aralığı yadda saxla» ilə eyni sətir yaranır və ikisi bir-birini görür.
     */
    suspend fun toggle(content: DailyContent): Boolean = withContext(Dispatchers.IO) {
        if (content.isHadith) toggleHadith(content) else toggleVerses(content)
    }

    private suspend fun toggleVerses(content: DailyContent): Boolean {
        val repository = RepositoryProvider.userRepository
        val chapterNo = content.chapter_no ?: return false
        val verses = content.verseNumbers
        if (verses.isEmpty()) return false

        val range = verses.first()..verses.last()

        if (repository.isBookmarked(chapterNo, range)) {
            repository.removeFromBookmark(chapterNo, range.first, range.last)
            return false
        }

        return repository.addToBookmark(chapterNo, range, note = null) != BookmarkAddResult.Failed
    }

    private suspend fun toggleHadith(content: DailyContent): Boolean {
        val repository = RepositoryProvider.userRepository
        val hadithId = content.hadith_id ?: return false

        if (repository.isHadithBookmarked(hadithId)) {
            repository.removeHadithBookmark(hadithId)
            return false
        }

        val dao = RepositoryProvider.hadithDatabase.hadithDao()
        val hadith = dao.getHadithById(hadithId) ?: return false

        // Əlfəcin siyahısı hədisi struktur içində göstərir, növbə sətrində isə yalnız `hadith_id`
        // var — qalan slug-lar cihazdakı bazadan bərpa olunur.
        val chapter = hadith.chapter_slug?.let { dao.getChapterBySlug(it) }
        val book = chapter?.book_slug?.let { dao.getBookBySlug(it) }

        val result = repository.addHadithBookmark(
            hadithId = hadithId,
            volumeSlug = book?.volume_slug,
            bookSlug = chapter?.book_slug,
            chapterSlug = hadith.chapter_slug,
            subChapterSlug = hadith.sub_chapter_slug,
            hadithNo = hadith.hadith_no,
            title = chapter?.name.orEmpty(),
            preview = content.displayTextAz.take(160),
            note = null,
        )

        return result != BookmarkAddResult.Failed
    }
}
