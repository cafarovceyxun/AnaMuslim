package com.cafarovceyxun.anamuslim.db.translation

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.cafarovceyxun.anamuslim.api.models.translation.TranslationBookInfoModel
import platform.Foundation.NSTemporaryDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Runtime proof on iOS/native that the KMP [QuranTranslationStore] replacement for the Android
 * `QuranTranslDBHelper` works: dynamic per-slug tables via raw `androidx.sqlite`, JSON parsing of
 * both translation payload formats via kotlinx.serialization (replacing the JVM-only org.json), the
 * full reader/search query surface the `QuranTranslationFactory` relies on, and the
 * `user_version`-based open/migration lifecycle in [QuranTranslationDatabase].
 */
class QuranTranslationStoreTest {

    private val standardSlug = "en_sahih-international"
    private val azSlug = "az_vasim-mammadaliyev"

    private fun bookInfo(slug: String) = TranslationBookInfoModel(slug).apply {
        langCode = "en"
        langName = "English"
        bookName = "Sahih International"
        authorName = "Saheeh International"
        displayName = "Sahih International"
        lastUpdated = 42L
        downloadPath = "path/$slug.json"
    }

    private val standardJson = """
        {
          "suras": [
            { "index": 1, "ayas": [
              { "index": 1, "translation": "In the name of Allah", "footnotes": [] },
              { "index": 2, "translation": "All praise is due to Allah",
                "footnotes": [ { "index": 1, "text": "A note on praise" } ] }
            ] },
            { "index": 2, "ayas": [
              { "index": 255, "translation": "Allah - there is no deity except Him" }
            ] }
          ]
        }
    """.trimIndent()

    private val azJson = """
        { "1:1": { "t": "Mərhəmətli Allahın adı ilə", "n": "" },
          "2:255": { "t": "Ayət əl-Kürsü", "n": "qeyd" } }
    """.trimIndent()

    private fun seededStore(connection: SQLiteConnection): QuranTranslationStore {
        val store = QuranTranslationStore(connection)
        store.storeTranslation(bookInfo(standardSlug), standardJson)
        store.storeTranslation(bookInfo(azSlug), azJson)
        return store
    }

    @Test
    fun dynamicTablesAndBothJsonFormatsRoundTripOnNative() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            val store = seededStore(connection)

            // Info table holds both slugs.
            assertEquals(listOf(azSlug, standardSlug), store.getStoredSlugs())

            // Verses come back chapter/verse ordered, per dynamic table.
            val standardTexts = store.getVerseTexts(standardSlug)
            assertEquals(3, standardTexts.size)
            assertEquals("In the name of Allah", standardTexts.first())
            assertEquals("Allah - there is no deity except Him", standardTexts.last())

            val azTexts = store.getVerseTexts(azSlug)
            assertEquals(2, azTexts.size)
            assertTrue(azTexts.first().startsWith("Mərhəmətli"))

            // Re-storing the same slug replaces cleanly (INSERT OR REPLACE, no duplicate rows).
            store.storeTranslation(bookInfo(azSlug), azJson)
            assertEquals(2, store.getVerseTexts(azSlug).size)
        } finally {
            connection.close()
        }
    }

    @Test
    fun readerQuerySurfaceMatchesFactoryExpectations() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            val store = seededStore(connection)

            // Book info: all + filtered subset, keyed and ordered by slug.
            assertEquals(setOf(azSlug, standardSlug), store.getBooksInfo().keys)
            val single = store.getBooksInfo(setOf(standardSlug))
            assertEquals(1, single.size)
            assertEquals("Sahih International", single[standardSlug]?.bookName)

            assertTrue(store.isTranslationDownloaded(standardSlug))
            assertFalse(store.isTranslationDownloaded("nonexistent_slug"))

            // Single verse.
            val v = store.getVersesSingle(standardSlug, 1, 2)
            assertEquals(1, v.size)
            assertEquals("All praise is due to Allah", v[0].text)
            assertEquals(standardSlug, v[0].bookSlug)

            // Range + distinct queries return verse-ordered results.
            val range = store.getVersesRange(standardSlug, 1, 1, 2)
            assertEquals(listOf(1, 2), range.map { it.verseNo })
            val distinct = store.getVersesDistinct(standardSlug, 1, intArrayOf(2, 1))
            assertEquals(listOf(1, 2), distinct.map { it.verseNo })

            // az note column survives the compact format.
            assertEquals("qeyd", store.getVersesSingle(azSlug, 2, 255).first().note)

            // Bulk search across both books.
            val bulk = store.getTranslationsBulkForSearch(
                setOf(standardSlug, azSlug),
                listOf(2 to 255),
            )
            assertEquals("Allah - there is no deity except Him", bulk[standardSlug]?.get(2 to 255)?.text)
            assertEquals("Ayət əl-Kürsü", bulk[azSlug]?.get(2 to 255)?.text)

            // Index-building helpers used by the search worker.
            assertEquals(3, store.countRows(standardSlug))
            val indexRows = store.getIndexRows(standardSlug)
            assertEquals(3, indexRows.size)
            assertEquals(1 to 1, indexRows.first().chapterNo to indexRows.first().verseNo)
        } finally {
            connection.close()
        }
    }

    @Test
    fun updateAndDeleteMutateInPlace() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            val store = seededStore(connection)

            store.updateTranslation(standardSlug, 1, 1, "Updated text", "Updated note")
            val updated = store.getVersesSingle(standardSlug, 1, 1).first()
            assertEquals("Updated text", updated.text)
            assertEquals("Updated note", updated.note)

            store.deleteTranslation(standardSlug)
            assertFalse(store.isTranslationDownloaded(standardSlug))
            assertEquals(listOf(azSlug), store.getStoredSlugs())
        } finally {
            connection.close()
        }
    }

    @Test
    fun openStampsFreshVersionAndRunsSeed() {
        val store = QuranTranslationDatabase.open(":memory:") { seeded ->
            seeded.storeTranslation(bookInfo(azSlug), azJson)
        }
        // onFreshCreate ran, info table exists, and version stamped to current.
        assertEquals(listOf(azSlug), store.getStoredSlugs())
        assertEquals(QuranTranslationStore.DB_VERSION, store.userVersion())
    }

    @Test
    fun openMigratesV1DatabaseByAddingNoteColumn() {
        val path = NSTemporaryDirectory() + "qtransl-v1-" + Random.nextLong().toString(16) + ".db"

        // Build a v1-shaped DB by hand: a per-slug table WITHOUT the `note` column, version 1.
        val setup = BundledSQLiteDriver().open(path)
        try {
            val store = QuranTranslationStore(setup)
            store.createInfoTable()
            setup.execSQL(
                "INSERT INTO ${QuranTranslationStore.INFO_TABLE} " +
                    "(${QuranTranslationStore.COL_SLUG}) VALUES ('$standardSlug')"
            )
            setup.execSQL(
                "CREATE TABLE ${QuranTranslationStore.escapeTableName(standardSlug)} (" +
                    "${QuranTranslationStore.COL_ID} TEXT PRIMARY KEY," +
                    "${QuranTranslationStore.COL_CHAPTER_NO} INTEGER," +
                    "${QuranTranslationStore.COL_VERSE_NO} INTEGER," +
                    "${QuranTranslationStore.COL_TEXT} TEXT," +
                    // Köhnə (v1) sxem hələ də haşiyə sütunu ilə gəlir; kod artıq onu oxumur.
                    "footnotes TEXT)"
            )
            setup.execSQL(
                "INSERT INTO ${QuranTranslationStore.escapeTableName(standardSlug)} VALUES " +
                    "('1:1', 1, 1, 'legacy text', '[]')"
            )
            store.setUserVersion(1)
        } finally {
            setup.close()
        }

        // Reopening runs the v1→v2 upgrade path.
        val migrated = QuranTranslationDatabase.open(path)
        assertEquals(QuranTranslationStore.DB_VERSION, migrated.userVersion())

        // The `note` column now exists: legacy row reads back with an empty note, and updates stick.
        val legacy = migrated.getVersesSingle(standardSlug, 1, 1).first()
        assertEquals("legacy text", legacy.text)
        assertEquals("", legacy.note)
        migrated.updateTranslation(standardSlug, 1, 1, "legacy text", "added note")
        val afterUpdate = migrated.getVersesSingle(standardSlug, 1, 1).first()
        assertEquals("added note", afterUpdate.note)
        assertNotNull(migrated.getBooksInfo(setOf(standardSlug))[standardSlug])
    }
}
