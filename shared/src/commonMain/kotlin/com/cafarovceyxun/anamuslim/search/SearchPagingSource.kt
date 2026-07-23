package com.cafarovceyxun.anamuslim.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.paging.PagingSource
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.arabicLabel
import com.cafarovceyxun.anamuslim.resources.hadith
import com.cafarovceyxun.anamuslim.resources.strLabelBab
import com.cafarovceyxun.anamuslim.resources.strLabelBook
import com.cafarovceyxun.anamuslim.resources.strLabelVolume
import org.jetbrains.compose.resources.getString
import androidx.paging.PagingState
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.db.entities.hadith.toModel
import com.cafarovceyxun.anamuslim.utils.supabase.Hadith
import com.cafarovceyxun.anamuslim.utils.supabase.HadithBook
import com.cafarovceyxun.anamuslim.utils.supabase.HadithChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithSubChapter
import com.cafarovceyxun.anamuslim.utils.supabase.HadithVolume
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils

data class SearchResult(
    val chapterNo: Int? = null,
    val verseNo: Int? = null,
    val matches: List<SearchResultMatch>,
    val hadith: Hadith? = null,
    val volume: HadithVolume? = null,
    val book: HadithBook? = null,
    val chapter: HadithChapter? = null,
    val subChapter: HadithSubChapter? = null
)

sealed class SearchResultMatch {
    data class TranslationMatch(
        val slug: String,
        val displayName: String,
        val preview: AnnotatedString,
    ) : SearchResultMatch()

    data class QuranTextMatch(
        val preview: AnnotatedString,
    ) : SearchResultMatch()

    data class HadithMatch(
        val preview: AnnotatedString,
        val source: String,
        val isArabic: Boolean = false
    ) : SearchResultMatch()
}

class SearchPagingSource(
    private val query: String,
    private val sourceQuran: Boolean, // Note: This is now partially redundant with filters.searchQuran
    private val filters: SearchFilters = SearchFilters(),
) : PagingSource<Int, SearchResult>() {

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, SearchResult> {
        return try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            
            val results = mutableListOf<SearchResult>()
            var nextKey: Int? = null

            // 1. Quran Search (if enabled)
            if (filters.searchQuran) {
                if (sourceQuran) { // Arabic text search
                    val normalized = SearchNormalizer.normalize(query)
                    val fts = FtsQueryBuilder.toPrefixAndQuery(normalized)
                    if (fts != null) {
                        val quranRepo = RepositoryProvider.quranRepository
                        val arabicRows = quranRepo.arabicTextSearch(fts, limit, offset)
                        
                        arabicRows.forEach { row ->
                            val (surahNo, ayahNo) = QuranMeta.getVerseNoFromAyahId(row.ayahId)
                            results.add(SearchResult(
                                chapterNo = surahNo,
                                verseNo = ayahNo,
                                matches = listOf(SearchResultMatch.QuranTextMatch(highlightMatches(row.text, query)))
                            ))
                        }
                        if (arabicRows.size == limit) nextKey = offset + limit
                    }
                } else { // Translation search
                    val fts = FtsQueryBuilder.toTranslationTextQuery(query)
                    if (fts != null) {
                        QuranTranslationFactory().use { factory ->
                            val dao = RepositoryProvider.searchIndexDatabase.searchIndexDao()
                            val slugFilter = filters.selectedSlugs?.takeIf { it.isNotEmpty() }
                            
                            val versePage = dao.pageMatchedVersesFiltered(fts, slugFilter, null, limit, offset)
                            if (versePage.isNotEmpty()) {
                                val verseKeys = versePage.map { it.surahNo to it.ayahNo }
                                val rows = dao.rowsForPagedVersesFiltered(fts, verseKeys.map { "${it.first}:${it.second}" }, slugFilter)
                                val slugs = rows.map { it.slug }.toSet()
                                val bulkTranslations = factory.getTranslationsBulkForSearch(slugs, verseKeys)
                                val books = factory.getAvailableTranslationBooksInfo()

                                val grouped = rows.groupBy { it.surahNo to it.ayahNo }
                                grouped.forEach { (coord, matches) ->
                                    results.add(SearchResult(
                                        chapterNo = coord.first,
                                        verseNo = coord.second,
                                        matches = matches.sortedBy { it.slug }.map { row ->
                                            val text = bulkTranslations[row.slug]?.get(coord)?.text ?: row.text
                                            SearchResultMatch.TranslationMatch(
                                                slug = row.slug,
                                                displayName = books[row.slug]?.displayName ?: row.slug,
                                                preview = highlightMatches(StringUtils.removeHTML(text, false), query)
                                            )
                                        }
                                    ))
                                }
                                if (versePage.size == limit) nextKey = offset + limit
                            }
                        }
                    }
                }
            }

            // 2. Hadith Search (if enabled and we still have space or Quran is disabled)
            // For simplicity, if both are enabled, we show Quran first, then Hadith.
            // Paging for two sources is hard without a total count, so we'll just check Hadith if Quran gave no more results.
            if (filters.searchHadith && nextKey == null) {
                val hadithDao = RepositoryProvider.hadithDatabase.hadithDao()
                
                // Adjust offset for Hadith search if Quran search was performed
                // This is a naive implementation; in a real app you'd want a unified index.
                val hadithOffset = if (filters.searchQuran) 0 else offset // Simplified
                
                // A. Search Hadith Text
                val hadithRows = hadithDao.searchHadiths(query, limit, hadithOffset)
                
                // For each hadith, we ideally want its hierarchy for better navigation.
                // For now, we'll just pass what we have.
                hadithRows.forEach { row ->
                    val chap = row.chapter_slug?.let { hadithDao.getChapterBySlug(it)?.toModel() }
                    val bk = chap?.book_slug?.let { hadithDao.getBookBySlug(it)?.toModel() }
                    
                    results.add(SearchResult(
                        matches = listOf(
                            SearchResultMatch.HadithMatch(
                                preview = highlightMatches(row.text_az, query),
                                source = getString(Res.string.hadith)
                            ),
                            SearchResultMatch.HadithMatch(
                                preview = highlightMatches(row.text_ar, query),
                                source = getString(Res.string.arabicLabel),
                                isArabic = true
                            )
                        ),
                        hadith = row.toModel(),
                        chapter = chap,
                        book = bk,
                        volume = bk?.volume_slug?.let { hadithDao.getVolumeBySlug(it)?.toModel() }
                    ))
                }

                // B. Search Titles (if it's the first page or we want to merge)
                if (hadithOffset == 0) {
                    val volumeMatches = hadithDao.searchVolumes(query)
                    volumeMatches.forEach {
                        results.add(0, SearchResult(
                            matches = listOf(SearchResultMatch.HadithMatch(highlightMatches(it.name, query), getString(Res.string.strLabelVolume))),
                            volume = it.toModel()
                        ))
                    }
                    val bookMatches = hadithDao.searchBooks(query)
                    bookMatches.forEach {
                        results.add(0, SearchResult(
                            matches = listOf(SearchResultMatch.HadithMatch(highlightMatches(it.name, query), getString(Res.string.strLabelBook))),
                            book = it.toModel()
                        ))
                    }
                    val chapterMatches = hadithDao.searchChapters(query)
                    chapterMatches.forEach {
                        results.add(0, SearchResult(
                            matches = listOf(SearchResultMatch.HadithMatch(highlightMatches(it.name, query), getString(Res.string.strLabelBab))),
                            chapter = it.toModel()
                        ))
                    }
                }

                if (hadithRows.size == limit) nextKey = offset + limit
            }

            return LoadResult.Page(
                data = results,
                prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                nextKey = nextKey
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(
        state: PagingState<Int, SearchResult>
    ): Int? {

        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor)
            ?: return null

        return page.prevKey?.plus(state.config.pageSize)
            ?: page.nextKey?.minus(state.config.pageSize)
    }

    private fun highlightMatches(text: String, rawQuery: String): AnnotatedString {
        val contextWindow = 180
        val sidePadding = 48
        val ellipsis = "…"

        val tokens = rawQuery
            .trim()
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinctBy { it.lowercase() }

        if (tokens.isEmpty()) {
            if (text.length <= contextWindow) return buildAnnotatedString { append(text) }

            return buildAnnotatedString {
                append(text.take(contextWindow).trimEnd())
                append(ellipsis)
            }
        }

        val source = text
        val lower = source.lowercase()
        val spans = mutableListOf<IntRange>()
        for (token in tokens.sortedByDescending { it.length }) {
            val q = token.lowercase()
            var idx = 0

            while (idx < lower.length) {
                val at = lower.indexOf(q, idx)
                if (at < 0) break
                spans += at until (at + q.length)
                idx = at + q.length
            }
        }

        if (spans.isEmpty()) {
            if (text.length <= contextWindow) return buildAnnotatedString { append(text) }
            return buildAnnotatedString {
                append(text.take(contextWindow).trimEnd())
                append(ellipsis)
            }
        }

        val merged = spans
            .sortedBy { it.first }
            .fold(mutableListOf<IntRange>()) { acc, range ->
                val last = acc.lastOrNull()
                if (last == null || range.first > last.last + 1) {
                    acc.add(range)
                } else {
                    acc[acc.lastIndex] = last.first..maxOf(last.last, range.last)
                }
                acc
            }

        val firstHit = merged.first()
        val sliceStart = maxOf(0, firstHit.first - sidePadding)
        val sliceEndExclusive = minOf(source.length, sliceStart + contextWindow)

        val prefix = if (sliceStart > 0) ellipsis else ""
        val suffix = if (sliceEndExclusive < source.length) ellipsis else ""

        val rawSlice = source.substring(sliceStart, sliceEndExclusive)
        val leadingTrimCount = rawSlice.length - rawSlice.trimStart().length
        val visibleText = rawSlice.trimStart().trimEnd()
        val contentStartInSource = sliceStart + leadingTrimCount

        val highlightStyle = SpanStyle(background = Color(0x66FFD858))

        return buildAnnotatedString {
            append(prefix)
            append(visibleText)
            append(suffix)

            val textOffset = prefix.length

            for (range in merged) {
                val clippedStart = maxOf(range.first, sliceStart)
                val clippedEndExclusive = minOf(range.last + 1, sliceEndExclusive)
                if (clippedStart >= clippedEndExclusive) continue

                val startInVisible = clippedStart - contentStartInSource
                val endInVisible = clippedEndExclusive - contentStartInSource
                val styleStart = textOffset + maxOf(0, startInVisible)
                val styleEnd = textOffset + minOf(visibleText.length, endInVisible)
                if (styleStart >= styleEnd) continue

                addStyle(
                    style = highlightStyle,
                    start = styleStart,
                    end = styleEnd,
                )
            }
        }
    }
}
