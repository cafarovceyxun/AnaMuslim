package com.cafarovceyxun.anamuslim.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.cafarovceyxun.anamuslim.db.entities.quran.NavigationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Proves the iOS equivalent of Android's `createFromAsset("db/quranapp.db")` end-to-end
 * against the real 24 MB pre-packaged asset: bundle→copy, Room open (schema/identity
 * validation of a prepackaged DB on native), real data queries incl. FTS4 MATCH, the
 * enum TypeConverters, and the version-mismatch re-copy semantics.
 *
 * The asset path is injected by shared/build.gradle.kts via SIMCTL_CHILD_QURANAPP_DB_ASSET.
 */
class QuranDatabaseTest {

    private val assetPath: String =
        NSProcessInfo.processInfo.environment["QURANAPP_DB_ASSET"] as? String
            ?: error("QURANAPP_DB_ASSET env var missing (set in shared/build.gradle.kts)")

    private fun tempTarget(): String =
        NSTemporaryDirectory() + "quranapp-test-" + Random.nextLong().toString(16)

    private fun userVersionOf(path: String): Long {
        val connection = BundledSQLiteDriver().open(path)
        return try {
            val statement = connection.prepare("PRAGMA user_version")
            try {
                assertTrue(statement.step())
                statement.getLong(0)
            } finally {
                statement.close()
            }
        } finally {
            connection.close()
        }
    }

    @Test
    fun realAssetCopyOpenAndQuery() = runBlocking {
        val target = tempTarget()
        prepareQuranDatabaseFile(target, assetPath)
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(target), "db file missing at $target (asset=$assetPath)")

        val db = Room.databaseBuilder<QuranDatabase>(name = target)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        try {
            // Full Quran: 114 surahs, with the @Relation localization mapping.
            val surahs = db.surahDao().getSurahsWithLocalizationsByNos((1..114).toList())
            assertEquals(114, surahs.size)
            val fatiha = db.surahDao().getSurah(1)
            assertNotNull(fatiha)
            assertEquals(7, fatiha.ayahCount)

            // Verse lookups (Ayat al-Kursi; Al-Fatiha via Flow query).
            val ayatAlKursi = db.ayahDao().getAyah(surahNo = 2, ayahNo = 255)
            assertNotNull(ayatAlKursi)
            assertEquals(7, db.ayahDao().getAyahsBySurah(1).first().size)

            // Enum-typed query parameter → QuranConverters binds on native.
            val juz1Ranges = db.navigationDao().getRangesByUnitNo(NavigationType.juz, 1)
            assertTrue(juz1Ranges.isNotEmpty(), "juz1Ranges empty")
            assertTrue(juz1Ranges.all { it.type == NavigationType.juz }, "juz1Ranges type mismatch")

            // FTS4 MATCH against the pre-packaged arabic_search index.
            val matches = db.arabicSearchDao().pageMatchedAyahs("الحمد*", limit = 10, offset = 0)
            assertTrue(matches.isNotEmpty(), "FTS matches empty")

            // Mushaf page mapping (mushaf_id 1 has both a mushafs row and mushaf_map lines).
            val mushaf = db.mushafDao().getMushaf(1)
            assertNotNull(mushaf)
            assertTrue(db.mushafDao().getPageLines(1, 1).isNotEmpty(), "mushaf page lines empty")
        } finally {
            db.close()
        }
    }

    @Test
    fun versionMismatchTriggersRecopy() {
        val target = tempTarget()

        // Fresh copy carries the asset's user_version (0 — Room stamps it on first open).
        prepareQuranDatabaseFile(target, assetPath)
        assertEquals(0L, userVersionOf(target))

        // Matching version → kept as-is (no re-copy back to 0).
        setUserVersion(target, 9L)
        prepareQuranDatabaseFile(target, assetPath)
        assertEquals(9L, userVersionOf(target))

        // Stale version → deleted and re-copied from the bundle asset.
        setUserVersion(target, 5L)
        prepareQuranDatabaseFile(target, assetPath)
        assertEquals(0L, userVersionOf(target))
    }

    private fun setUserVersion(path: String, version: Long) {
        val connection = BundledSQLiteDriver().open(path)
        try {
            connection.execSQL("PRAGMA user_version = $version")
        } finally {
            connection.close()
        }
    }
}
