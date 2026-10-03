package com.cafarovceyxun.anamuslim.search

import androidx.compose.ui.text.AnnotatedString
import androidx.paging.PagingSource
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import androidx.paging.PagingState
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
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
                    // Reduced to match the undiacritised `arabic_search` index — see
                    // [SearchNormalizer.quranNormalize]. The same reduced form is what gets
                    // highlighted with: [highlightMatches] folds both sides, so a query with no
                    // harakat still lands on the diacritised text below.
                    val normalized = SearchNormalizer.quranNormalize(SearchNormalizer.normalize(query))
                    val fts = FtsQueryBuilder.toPrefixAndQuery(normalized)
                    if (fts != null) {
                        val quranRepo = RepositoryProvider.quranRepository
                        val arabicRows = quranRepo.arabicTextSearch(fts, limit, offset)

                        // The index rows the match came from are undiacritised, so the preview is
                        // built from the muṣḥaf text instead — otherwise search was the one place in
                        // the app showing the Quran stripped of its harakat.
                        val verseTexts = quranRepo.getVerseTextsForAyahs(arabicRows.map { it.ayahId })

                        arabicRows.forEach { row ->
                            val (surahNo, ayahNo) = QuranMeta.getVerseNoFromAyahId(row.ayahId)
                            val text = verseTexts[row.ayahId]?.takeIf { it.isNotBlank() } ?: row.text
                            results.add(SearchResult(
                                chapterNo = surahNo,
                                verseNo = ayahNo,
                                matches = listOf(SearchResultMatch.QuranTextMatch(highlightMatches(text, normalized)))
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
                // Adjust offset for Hadith search if Quran search was performed
                // This is a naive implementation; in a real app you'd want a unified index.
                val hadithOffset = if (filters.searchQuran) 0 else offset // Simplified

                // A. Search Hadith Text — shared with the hadith index screens' search box.
                val hadithRows = if (filters.searchHadithText) {
                    HadithSearch.searchTexts(query, limit, hadithOffset)
                } else {
                    // Əhatə yalnız mövzulardadır — mətn sorğusu ümumiyyətlə getmir.
                    emptyList()
                }
                results.addAll(hadithRows)

                // B. Search Titles (if it's the first page or we want to merge)
                //
                // `offset`, `hadithOffset` yox: Quran axtarışı açıq olanda `hadithOffset` hər
                // səhifədə 0-a bərabərdir və başlıq uyğunluqları hər səhifənin başına təkrar
                // düşürdü. Tərs sıra köhnə `add(0, …)` döngüsünün nəticəsidir — qorunur.
                if (offset == 0 && filters.searchHadithTitles) {
                    results.addAll(0, HadithSearch.searchTitles(query).asReversed())
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
}
