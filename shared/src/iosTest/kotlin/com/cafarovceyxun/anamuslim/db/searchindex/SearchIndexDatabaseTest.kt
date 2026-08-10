package com.cafarovceyxun.anamuslim.db.searchindex

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Runtime proof that the real SearchIndexDatabase works on iOS/native: FTS4 tokenization,
 * the RoomRawQuery-based filtered queries, and the meta upsert all run against the bundled
 * SQLite driver. This is the first real (non-probe) database migrated to commonMain.
 */
class SearchIndexDatabaseTest {

    private fun db() = Room.inMemoryDatabaseBuilder<SearchIndexDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

    @Test
    fun ftsMatchAndRawQueriesRoundTrip() = runBlocking {
        val db = db()
        val dao = db.searchIndexDao()

        dao.replaceSlugIndex(
            slug = "az.ferimet",
            rows = listOf(
                TranslationSearchContentEntity(slug = "az.ferimet", surahNo = 1, ayahNo = 1, text = "Rahman ve Rahim olan Allahin adi ile"),
                TranslationSearchContentEntity(slug = "az.ferimet", surahNo = 2, ayahNo = 3, text = "Onlar qeybe iman getirenlerdir"),
            ),
            fingerprint = "fp1",
        )
        dao.replaceSlugIndex(
            slug = "en.sahih",
            rows = listOf(
                TranslationSearchContentEntity(slug = "en.sahih", surahNo = 5, ayahNo = 6, text = "In the name of Allah the Entirely Merciful"),
            ),
            fingerprint = "fp2",
        )

        // Meta upserted for both slugs.
        assertEquals("fp1", dao.getMeta("az.ferimet")?.fingerprint)
        assertEquals("fp2", dao.getMeta("en.sahih")?.fingerprint)

        // FTS MATCH across all slugs — two distinct verses (az 1:1, en 5:6).
        val allAllah = dao.pageMatchedVersesFiltered(
            ftsQuery = "Allah*",
            slugs = null,
            surahNo = null,
            limit = 50,
            offset = 0,
        )
        assertEquals(2, allAllah.size)

        // Slug filter narrows results.
        val onlyEn = dao.pageMatchedVersesFiltered(
            ftsQuery = "Allah*",
            slugs = listOf("en.sahih"),
            surahNo = null,
            limit = 50,
            offset = 0,
        )
        assertEquals(1, onlyEn.size)
        assertEquals(5, onlyEn.first().surahNo)

        // Surah filter.
        val surah2 = dao.pageMatchedVersesFiltered(
            ftsQuery = "iman*",
            slugs = null,
            surahNo = 2,
            limit = 50,
            offset = 0,
        )
        assertEquals(1, surah2.size)
        assertEquals(3, surah2.first().ayahNo)

        // rowsForPagedVersesFiltered resolves the per-verse detail rows by key.
        val rows = dao.rowsForPagedVersesFiltered(
            ftsQuery = "Allah*",
            keys = listOf("1:1"),
            slugs = null,
        )
        assertEquals(1, rows.size)
        assertTrue(rows.all { it.surahNo == 1 && it.ayahNo == 1 })

        // Removing a slug clears its rows and meta.
        dao.replaceSlugIndex("az.ferimet", emptyList(), "ignored")
        assertNotNull(dao.getMeta("en.sahih"))
        assertEquals(null, dao.getMeta("az.ferimet"))
        val afterRemoval = dao.pageMatchedVersesFiltered("Allah*", null, null, 50, 0)
        assertEquals(1, afterRemoval.size)

        db.close()
    }
}
