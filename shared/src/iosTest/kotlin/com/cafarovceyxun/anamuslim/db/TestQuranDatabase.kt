package com.cafarovceyxun.anamuslim.db

import androidx.room.Room
import androidx.room.execSQL
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cafarovceyxun.anamuslim.repository.QuranRepository
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers

/**
 * An in-memory [QuranDatabase] + [ExternalQuranDatabase] pair, seeded with a miniature but
 * structurally faithful slice of the shipped 24 MB asset DB, so [QuranRepository] can be exercised
 * in a plain unit test.
 *
 * Why raw SQL instead of DAO inserts: the Quran DAOs are read-only by design (the database ships
 * as an asset and the app never writes to it), so there is no `@Insert` to call. Room still creates
 * the real schema for us — the fixture only has to fill it.
 *
 * **Why `iosTest` and not `commonTest`:** Room's `Context`-free `inMemoryDatabaseBuilder` is
 * declared in the library's `nativeMain` only; on the Android target the name resolves to the Java
 * overload that wants a `Context` and a `Class`, which a plain JVM unit test cannot supply without
 * Robolectric. The code under test is `commonMain` either way, and the risk this covers is the
 * Room-KMP port, so native is the right place to run it — the same reason the other database tests
 * live here.
 *
 * ## The fixture
 *
 * Real numbers throughout, so an off-by-one in a query shows up as a wrong *Quranic* answer rather
 * than a wrong test number:
 *
 * - **Surahs** 1 (Fatiha, 7 ayahs), 2 (Baqara, 286), 32 (Sajdah, 30), 114 (Nas, 6).
 * - **Ayahs** only where a test needs them — 1:1-7 and 2:1-10 (juz 1; hizb 1 up to 2:5, hizb 2
 *   after), 32:14-16, 114:1-6 (juz 30). `ayah_id = surahNo * 1000 + ayahNo`, the asset DB's rule.
 *   Note the surahs table still claims 286 ayahs for Baqara while only 10 rows exist — that gap is
 *   deliberate, it catches a verse count that starts counting rows instead of reading `ayah_count`.
 * - **Sajdah** on 32:15 only (a real sajdah verse), with 32:14 carrying an explicit `0` and 1:1 a
 *   `NULL` — the two ways "no sajdah" is spelled in the asset.
 * - **Mushaf** id 1 (`uthmani`), 3 pages of lines including surah-name and basmallah lines, and
 *   ayah lines whose `surah_no` is NULL so the page→surah resolution has to go through
 *   `start_ayah_id`, exactly as the real data does.
 * - **Localizations** en for 1/2/114, tr for 1 only — so a Turkish app locale has to fall back to
 *   English for Baqara but not for Fatiha.
 */
class TestQuranDatabase private constructor(
    val quranDatabase: QuranDatabase,
    val externalDatabase: ExternalQuranDatabase,
) {
    val repository = QuranRepository(quranDatabase, externalDatabase)

    companion object {
        private var instance: TestQuranDatabase? = null

        /**
         * The fixture, built and seeded once per test process.
         *
         * Deliberately never closed, and deliberately shared: [RepositoryProvider] is a
         * process-wide sink, so a per-test database that gets closed in a teardown would leave
         * every later test holding a closed connection. Nothing writes to this database — it stands
         * in for a read-only asset — so sharing it costs no isolation.
         */
        suspend fun shared(): TestQuranDatabase {
            instance?.let { return it }

            val quran = Room.inMemoryDatabaseBuilder<QuranDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()

            val external = Room.inMemoryDatabaseBuilder<ExternalQuranDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Default)
                .build()

            quran.seed()

            return TestQuranDatabase(quran, external).also {
                instance = it
                RepositoryProvider.setQuranRepositoryProvider { it.repository }
            }
        }
    }
}

/** `ayah_id` as the asset database computes it. */
private fun ayahId(surahNo: Int, ayahNo: Int) = surahNo * 1000 + ayahNo

private suspend fun QuranDatabase.seed() = useWriterConnection { connection ->
    // --- surahs -------------------------------------------------------------------------------
    // (surah_no, ayah_count, revelation_order, rukus_count, revelation_type)
    // Enums are stored as their `name`, per QuranConverters.
    listOf(
        "(1, 7, 5, 1, 'meccan')",
        "(2, 286, 87, 40, 'medinan')",
        "(32, 30, 75, 3, 'meccan')",
        "(114, 6, 21, 1, 'meccan')",
    ).forEach {
        connection.execSQL(
            "INSERT INTO surahs (surah_no, ayah_count, revelation_order, rukus_count, revelation_type) VALUES $it"
        )
    }

    // --- ayahs --------------------------------------------------------------------------------
    // (ayah_id, surah_no, ayah_no, juz_no, hizb_no, rub_no, manzil_no, ruku_no, sajdah_type)
    val ayahRows = buildList {
        // Al-Fatiha, all of juz 1 / hizb 1. Verse 1 leaves sajdah_type NULL.
        for (ayahNo in 1..7) {
            add(Ayah(1, ayahNo, juz = 1, hizb = 1, rub = 1, manzil = 1, ruku = 1, sajdah = null))
        }
        // Al-Baqara's opening, still juz 1, but crossing the hizb 1 → 2 boundary after verse 5.
        for (ayahNo in 1..10) {
            add(
                Ayah(
                    surahNo = 2,
                    ayahNo = ayahNo,
                    juz = 1,
                    hizb = if (ayahNo <= 5) 1 else 2,
                    rub = if (ayahNo <= 5) 2 else 3,
                    manzil = 1,
                    ruku = if (ayahNo <= 7) 1 else 2,
                    sajdah = null,
                )
            )
        }
        // As-Sajdah around its prostration verse: 14 says "no sajdah" with a 0, 15 is the real one.
        add(Ayah(32, 14, juz = 21, hizb = 41, rub = 4, manzil = 6, ruku = 2, sajdah = 0))
        add(Ayah(32, 15, juz = 21, hizb = 41, rub = 4, manzil = 6, ruku = 2, sajdah = 1))
        add(Ayah(32, 16, juz = 21, hizb = 41, rub = 4, manzil = 6, ruku = 2, sajdah = 0))
        // An-Nas closes the Quran in juz 30.
        for (ayahNo in 1..6) {
            add(Ayah(114, ayahNo, juz = 30, hizb = 60, rub = 4, manzil = 7, ruku = 1, sajdah = null))
        }
    }

    ayahRows.forEach { ayah ->
        connection.execSQL(
            "INSERT INTO ayahs " +
                    "(ayah_id, surah_no, ayah_no, juz_no, hizb_no, rub_no, manzil_no, ruku_no, sajdah_type) " +
                    "VALUES (${ayahId(ayah.surahNo, ayah.ayahNo)}, ${ayah.surahNo}, ${ayah.ayahNo}, " +
                    "${ayah.juz}, ${ayah.hizb}, ${ayah.rub}, ${ayah.manzil}, ${ayah.ruku}, " +
                    "${ayah.sajdah?.toString() ?: "NULL"})"
        )
    }

    // --- surah localizations ------------------------------------------------------------------
    // Turkish exists for Al-Fatiha only; everything else has to fall back to English.
    listOf(
        "(1, 'en', 'Al-Fatihah', 'The Opener')",
        "(1, 'tr', 'Fâtiha', 'Açış')",
        "(2, 'en', 'Al-Baqarah', 'The Cow')",
        "(114, 'en', 'An-Nas', 'Mankind')",
    ).forEach {
        connection.execSQL(
            "INSERT INTO surah_localizations (surah_no, lang_code, name, meaning) VALUES $it"
        )
    }

    // --- navigation ranges --------------------------------------------------------------------
    // (type, unit_no, surah_no, start_ayah, end_ayah)
    listOf(
        "('juz', 1, 1, 1, 7)",
        "('juz', 1, 2, 1, 141)",
        "('juz', 30, 114, 1, 6)",
        "('hizb', 1, 1, 1, 7)",
        "('hizb', 1, 2, 1, 74)",
    ).forEach {
        connection.execSQL(
            "INSERT INTO navigation_ranges (type, unit_no, surah_no, start_ayah, end_ayah) VALUES $it"
        )
    }

    // --- mushaf + page map --------------------------------------------------------------------
    connection.execSQL(
        "INSERT INTO mushafs (mushaf_id, mushaf_code, no_of_pages, lines_per_page) " +
                "VALUES (1, 'uthmani', 604, 15)"
    )

    // (mushaf_id, page_number, line_number, line_type, is_centered,
    //  start_ayah_id, start_word_index, end_ayah_id, end_word_index, surah_no)
    //
    // Only the surah-name and basmallah lines carry `surah_no`; ayah lines leave it NULL, which is
    // what forces getOrderedSurahNosOnMushafPage through the start_ayah_id lookup.
    listOf(
        // Page 1 — Al-Fatiha 1-6.
        "(1, 1, 1, 'surah_name', 1, NULL, NULL, NULL, NULL, 1)",
        "(1, 1, 2, 'basmallah', 1, NULL, NULL, NULL, NULL, 1)",
        "(1, 1, 3, 'ayah', 0, 1001, 0, 1003, 4, NULL)",
        "(1, 1, 4, 'ayah', 0, 1004, 0, 1006, 3, NULL)",
        // Page 2 — Al-Fatiha's last verse, then Al-Baqara starts.
        "(1, 2, 1, 'ayah', 0, 1007, 0, 1007, 5, NULL)",
        "(1, 2, 2, 'surah_name', 1, NULL, NULL, NULL, NULL, 2)",
        "(1, 2, 3, 'basmallah', 1, NULL, NULL, NULL, NULL, 2)",
        "(1, 2, 4, 'ayah', 0, 2001, 0, 2005, 7, NULL)",
        // Page 3 — Al-Baqara 6-10 (hizb 2).
        "(1, 3, 1, 'ayah', 0, 2006, 0, 2010, 3, NULL)",
    ).forEach {
        connection.execSQL(
            "INSERT INTO mushaf_map (mushaf_id, page_number, line_number, line_type, is_centered, " +
                    "start_ayah_id, start_word_index, end_ayah_id, end_word_index, surah_no) VALUES $it"
        )
    }
}

private data class Ayah(
    val surahNo: Int,
    val ayahNo: Int,
    val juz: Int,
    val hizb: Int,
    val rub: Int,
    val manzil: Int,
    val ruku: Int,
    val sajdah: Int?,
)
