package com.cafarovceyxun.anamuslim.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cafarovceyxun.anamuslim.db.entities.atlas.AtlasBundleEntity
import com.cafarovceyxun.anamuslim.db.entities.atlas.AtlasWordShapeEntity
import com.cafarovceyxun.anamuslim.db.entities.wbw.WbwAudioTimingEntity
import com.cafarovceyxun.anamuslim.db.entities.wbw.WbwWordEntity
import com.cafarovceyxun.anamuslim.db.migrations.ExternalQuranDatabaseMigrations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Runtime proof that the real ExternalQuranDatabase works on iOS/native: word-by-word +
 * audio-timing + atlas-shape CRUD, and the migrations ported to `SQLiteConnection` (including
 * MIGRATION_3_4's platform hook, which defaults to a no-op here). Fourth real database migrated
 * to commonMain.
 */
class ExternalQuranDatabaseTest {

    private fun db() = Room.inMemoryDatabaseBuilder<ExternalQuranDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

    @Test
    fun wbwAndAtlasCrudRoundTrip() = runBlocking {
        val db = db()
        val wbw = db.wbwDao()
        val atlas = db.atlasWordShapeDao()

        // Word-by-word words, replaced transactionally by wbw_id.
        wbw.replaceByWbwId(
            "en", listOf(
                WbwWordEntity(ayahId = 1, wordIndex = 0, wbwId = "en", translation = "In", transliteration = "bi"),
                WbwWordEntity(ayahId = 1, wordIndex = 1, wbwId = "en", translation = "the name", transliteration = "smi"),
            )
        )
        assertEquals(2, wbw.getWordsForAyah(1, "en").size)
        assertEquals(listOf("en"), wbw.getDownloadedWbwIds(listOf("en", "az")))

        // Audio timings, chunked replace.
        wbw.replaceTimingByAudioId(
            "recA", listOf(
                WbwAudioTimingEntity("recA", ayahId = 1, wordIndex = 0, startMillis = 0, endMillis = 500),
                WbwAudioTimingEntity("recA", ayahId = 1, wordIndex = 1, startMillis = 500, endMillis = 900),
            )
        )
        assertEquals(2, wbw.getTimingCount("recA"))
        assertEquals(900L, wbw.getWordTiming("recA", 1, 1)?.endMillis)

        // Atlas bundle + page-scoped word shapes.
        atlas.upsertBundle(AtlasBundleEntity(bundleKey = "b1", metaJson = "{}", layerJson = "[]"))
        assertNotNull(atlas.getBundleByKey("b1"))
        atlas.insertShapes(
            listOf(
                AtlasWordShapeEntity(bundleKey = "b1", word = "الله", page = 1, placementsJson = "[1]"),
                AtlasWordShapeEntity(bundleKey = "b1", word = "الله", page = 2, placementsJson = "[2]"),
            )
        )
        assertEquals(2, atlas.countShapesForBundle("b1"))
        assertEquals("[2]", atlas.getShape("b1", "الله", 2)?.placementsJson)

        db.close()
    }

    @Test
    fun portedMigrationsExecuteOnNative() {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            ExternalQuranDatabaseMigrations.MIGRATION_1_2.migrate(connection)
            ExternalQuranDatabaseMigrations.MIGRATION_2_3.migrate(connection)
            // Exercises DELETE/DROP/CREATE plus the (no-op by default) reset hook.
            ExternalQuranDatabaseMigrations.MIGRATION_3_4.migrate(connection)

            for (table in listOf("atlas_bundles", "atlas_word_shapes", "wbw_audio_timing")) {
                val stmt = connection.prepare(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name = ?"
                )
                try {
                    stmt.bindText(1, table)
                    assertTrue(stmt.step(), "migration did not create table $table")
                } finally {
                    stmt.close()
                }
            }

            // atlas_word_shapes must be the v4 shape (has the `page` column).
            val cols = connection.prepare("SELECT COUNT(*) FROM pragma_table_info('atlas_word_shapes') WHERE name = 'page'")
            try {
                assertTrue(cols.step())
                assertEquals(1L, cols.getLong(0))
            } finally {
                cols.close()
            }
        } finally {
            connection.close()
        }
    }
}
