package com.cafarovceyxun.anamuslim.search

import com.cafarovceyxun.anamuslim.db.searchindex.SEARCH_INDEX_DB_VERSION
import com.cafarovceyxun.anamuslim.db.searchindex.TranslationSearchContentEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.reader.factory.QuranTranslationFactory
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Builds the FTS search index over downloaded translations.
 *
 * This is the *work*; scheduling it is the platform's business. Android runs it from
 * `TranslationSearchIndexWorker` (WorkManager, so it survives backgrounding and can be expedited);
 * iOS calls it inline after a download and once at startup. Both share this code, so an index built
 * on either platform has identical contents — the same split the download seam uses.
 */
object TranslationSearchIndexer {

    private val dao get() = RepositoryProvider.searchIndexDatabase.searchIndexDao()

    /**
     * Indexes [slug] unless its fingerprint is unchanged, which makes repeated calls cheap: two
     * small queries and no rewrite. An undownloaded slug clears its rows instead.
     */
    suspend fun indexSlugIfNeeded(slug: String): Unit = withContext(Dispatchers.IO) {
        QuranTranslationFactory().use { factory ->
            if (!factory.isTranslationDownloaded(slug)) {
                dao.replaceSlugIndex(slug, emptyList(), "")
                return@use
            }

            val book = factory.getTranslationBookInfo(slug)
            val rowCount = factory.store.countRows(slug)
            val fingerprint = "${book.lastUpdated}|$rowCount|$SEARCH_INDEX_DB_VERSION"

            if (dao.getMeta(slug)?.fingerprint == fingerprint) return@use

            val rows = ArrayList<TranslationSearchContentEntity>(rowCount.coerceAtMost(7000))

            for (row in factory.store.getIndexRows(slug)) {
                val plain = StringUtils.removeHTML(row.text, false)
                val norm = SearchNormalizer.normalize(plain)
                if (norm.isBlank()) continue

                rows.add(
                    TranslationSearchContentEntity(
                        slug = slug,
                        surahNo = row.chapterNo,
                        ayahNo = row.verseNo,
                        text = norm,
                    )
                )
            }

            dao.replaceSlugIndex(slug, rows, fingerprint)
        }
    }

    /** Drops a translation's rows (and its meta row) from the index. */
    suspend fun removeSlug(slug: String): Unit = withContext(Dispatchers.IO) {
        dao.replaceSlugIndex(slug, emptyList(), "")
    }

    /**
     * Brings every known book in line with what is actually downloaded. [isStopped] lets a
     * cancellable host (Android's worker) bail out between books.
     */
    suspend fun syncAll(isStopped: () -> Boolean = { false }): Unit = withContext(Dispatchers.IO) {
        val slugs = QuranTranslationFactory().use { it.getAvailableTranslationBooksInfo().keys }

        for (slug in slugs) {
            if (isStopped()) break
            indexSlugIfNeeded(slug)
        }
    }
}
