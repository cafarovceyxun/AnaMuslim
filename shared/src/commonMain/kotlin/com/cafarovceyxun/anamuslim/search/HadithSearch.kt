package com.cafarovceyxun.anamuslim.search

import com.cafarovceyxun.anamuslim.compose.screens.hadith.hadithNameMatches
import com.cafarovceyxun.anamuslim.db.entities.hadith.toModel
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.arabicLabel
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.strLabelBab
import com.cafarovceyxun.anamuslim.resources.strLabelBook
import com.cafarovceyxun.anamuslim.resources.strLabelSubBab
import com.cafarovceyxun.anamuslim.resources.strLabelVolume
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume
import org.jetbrains.compose.resources.getString

/**
 * Hədis bazasında axtarış — Axtarış ekranı ([SearchPagingSource]) və hədis indeks ekranlarındakı
 * qutu eyni funksiyalardan keçir ki, iki yerdə iki cür nəticə çıxmasın.
 */
object HadithSearch {

    /**
     * Adı [query]-yə uyğun gələn cild / kitab / bab / alt bablar, bu sıra ilə, ən çox [limit] dənə.
     *
     * Dörd cədvəl bir dəfə oxunur və əcdadlar xəritədən tapılır: əvvəl hər uyğunluq üçün ayrıca
     * `getXBySlug` sorğuları gedirdi, qısa sorğu yüzlərlə baba dəyəndə bu, minlərlə sorğu demək idi.
     * Hər nəticə bütün əcdadlarını daşıyır — keçid `hadith_items` ünvanını onlardan qurur.
     */
    suspend fun searchTitles(
        query: String,
        limit: Int = Int.MAX_VALUE,
        exclude: Set<String> = emptySet(),
    ): List<SearchResult> {
        val dao = RepositoryProvider.hadithDatabase.hadithDao()
        val hits = matchTitles(
            query = query,
            volumes = dao.getAllVolumes().map { it.toModel() },
            books = dao.getAllBooks().map { it.toModel() },
            chapters = dao.getAllChapters().map { it.toModel() },
            subChapters = dao.getAllSubChapters().map { it.toModel() },
            limit = limit,
            exclude = exclude,
        )
        if (hits.isEmpty()) return emptyList()

        val labels = mapOf(
            HadithTitleLevel.VOLUME to getString(Res.string.strLabelVolume),
            HadithTitleLevel.BOOK to getString(Res.string.strLabelBook),
            HadithTitleLevel.CHAPTER to getString(Res.string.strLabelBab),
            HadithTitleLevel.SUB_CHAPTER to getString(Res.string.strLabelSubBab),
        )

        return hits.map { hit ->
            SearchResult(
                matches = listOf(
                    SearchResultMatch.HadithMatch(highlightMatches(hit.name, query), labels.getValue(hit.level)),
                ),
                volume = hit.volume,
                book = hit.book,
                chapter = hit.chapter,
                subChapter = hit.subChapter,
            )
        }
    }

    /**
     * Mətnində [query] keçən hədislər, əcdadları ilə.
     *
     * `text_ar` tam hərəkəli saxlanılır, ona görə ərəbcə sorğu sütunun reduksiya olunmuş nüsxəsi ilə
     * tutuşdurulur — bax `HadithDao.searchHadiths`.
     */
    suspend fun searchTexts(query: String, limit: Int, offset: Int): List<SearchResult> {
        val dao = RepositoryProvider.hadithDatabase.hadithDao()
        val arabicQuery = if (SearchNormalizer.containsArabic(query)) {
            SearchNormalizer.arabicNormalize(query)
        } else {
            ""
        }

        val rows = dao.searchHadiths(query, arabicQuery, limit, offset)
        if (rows.isEmpty()) return emptyList()

        val hadithLabel = getString(Res.string.hadith)
        val arabicLabel = getString(Res.string.arabicLabel)

        // Bir səhifədəki hədislər adətən bir neçə baba düşür — hər səviyyə bir dəfə oxunur.
        val chapters = HashMap<String, HadithChapter?>()
        val books = HashMap<String, HadithBook?>()
        val volumes = HashMap<String, HadithVolume?>()

        return rows.map { row ->
            val chap = row.chapter_slug?.let { slug ->
                chapters.getOrPut(slug) { dao.getChapterBySlug(slug)?.toModel() }
            }
            val bk = chap?.book_slug?.let { slug ->
                books.getOrPut(slug) { dao.getBookBySlug(slug)?.toModel() }
            }
            val vol = bk?.volume_slug?.let { slug ->
                volumes.getOrPut(slug) { dao.getVolumeBySlug(slug)?.toModel() }
            }

            SearchResult(
                matches = listOf(
                    SearchResultMatch.HadithMatch(
                        preview = highlightMatches(row.text_az, query),
                        source = hadithLabel,
                    ),
                    SearchResultMatch.HadithMatch(
                        preview = highlightMatches(row.text_ar, query),
                        source = arabicLabel,
                        isArabic = true,
                    ),
                ),
                hadith = row.toModel(),
                chapter = chap,
                book = bk,
                volume = vol,
            )
        }
    }

    /**
     * [searchTitles]-in təmiz hissəsi: süzgəc, sıra, limit və əcdad xəritəsi.
     *
     * Ad `hadithNameMatches` ilə yoxlanılır — indeks ekranlarındakı yerli süzgəclə **eyni**
     * funksiya. SQL `LIKE` bunu edə bilmirdi: SQLite yalnız ASCII hərflərinin böyük/kiçik fərqini
     * udur («iman» «İman»ı tapmırdı), `name_ar` isə sorğuya heç girmirdi.
     *
     * [exclude] — [hadithLevelKey] açarları; limitdən **əvvəl** çıxarılır, yoxsa ekranda artıq
     * görünən sətirlər limiti yeyib qlobal bölməni boş qoyardı.
     */
    internal fun matchTitles(
        query: String,
        volumes: List<HadithVolume>,
        books: List<HadithBook>,
        chapters: List<HadithChapter>,
        subChapters: List<HadithSubChapter>,
        limit: Int = Int.MAX_VALUE,
        exclude: Set<String> = emptySet(),
    ): List<HadithTitleHit> {
        if (query.isBlank() || limit <= 0) return emptyList()

        val volumeBySlug = volumes.associateBy { it.slug }
        val bookBySlug = books.associateBy { it.slug }
        val chapterBySlug = chapters.associateBy { it.slug }

        return buildList {
            fun full() = size >= limit
            fun offer(hit: HadithTitleHit) {
                if (hit.key !in exclude) add(hit)
            }

            for (vol in volumes) {
                if (full()) return@buildList
                if (hadithNameMatches(query, vol.name, vol.name_ar)) {
                    offer(HadithTitleHit(HadithTitleLevel.VOLUME, vol.name, vol, null, null, null))
                }
            }
            for (bk in books) {
                if (full()) return@buildList
                if (hadithNameMatches(query, bk.name, bk.name_ar)) {
                    offer(HadithTitleHit(HadithTitleLevel.BOOK, bk.name, volumeBySlug[bk.volume_slug], bk, null, null))
                }
            }
            for (chap in chapters) {
                if (full()) return@buildList
                if (hadithNameMatches(query, chap.name, chap.name_ar)) {
                    val bk = bookBySlug[chap.book_slug]
                    offer(
                        HadithTitleHit(
                            HadithTitleLevel.CHAPTER, chap.name,
                            bk?.let { volumeBySlug[it.volume_slug] }, bk, chap, null,
                        ),
                    )
                }
            }
            for (sub in subChapters) {
                if (full()) return@buildList
                if (hadithNameMatches(query, sub.name, sub.name_ar)) {
                    val chap = chapterBySlug[sub.chapter_slug]
                    val bk = chap?.let { bookBySlug[it.book_slug] }
                    offer(
                        HadithTitleHit(
                            HadithTitleLevel.SUB_CHAPTER, sub.name,
                            bk?.let { volumeBySlug[it.volume_slug] }, bk, chap, sub,
                        ),
                    )
                }
            }
        }
    }
}

enum class HadithTitleLevel { VOLUME, BOOK, CHAPTER, SUB_CHAPTER }

/** [HadithSearch.matchTitles]-in bir nəticəsi: uyğun gələn səviyyə və onun əcdadları. */
data class HadithTitleHit(
    val level: HadithTitleLevel,
    val name: String,
    val volume: HadithVolume?,
    val book: HadithBook?,
    val chapter: HadithChapter?,
    val subChapter: HadithSubChapter?,
) {
    val key: String
        get() = hadithLevelKey(
            level,
            when (level) {
                HadithTitleLevel.VOLUME -> volume?.slug
                HadithTitleLevel.BOOK -> book?.slug
                HadithTitleLevel.CHAPTER -> chapter?.slug
                HadithTitleLevel.SUB_CHAPTER -> subChapter?.slug
            }.orEmpty(),
        )
}

/**
 * Nəticənin göstərdiyi səviyyənin açarı — `v:`/`b:`/`c:`/`s:` + slug.
 *
 * İndeks ekranları artıq göstərdikləri sətirləri bu açarla bildirir ki, qlobal bölmə onları ikinci
 * dəfə sadalamasın. Prefiks ona görədir ki, slug-lar səviyyələr arasında təkrarlana bilər; forma
 * `OutlineMatch.key` ilə eynidir. Mətn nəticəsində (hədis) null.
 */
val SearchResult.hadithLevelKey: String?
    get() = when {
        hadith != null -> null
        subChapter != null -> hadithLevelKey(HadithTitleLevel.SUB_CHAPTER, subChapter.slug)
        chapter != null -> hadithLevelKey(HadithTitleLevel.CHAPTER, chapter.slug)
        book != null -> hadithLevelKey(HadithTitleLevel.BOOK, book.slug)
        volume != null -> hadithLevelKey(HadithTitleLevel.VOLUME, volume.slug)
        else -> null
    }

fun hadithLevelKey(level: HadithTitleLevel, slug: String): String = when (level) {
    HadithTitleLevel.VOLUME -> "v:"
    HadithTitleLevel.BOOK -> "b:"
    HadithTitleLevel.CHAPTER -> "c:"
    HadithTitleLevel.SUB_CHAPTER -> "s:"
} + slug
