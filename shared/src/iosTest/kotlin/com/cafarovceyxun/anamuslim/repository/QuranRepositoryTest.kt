package com.cafarovceyxun.anamuslim.repository

import com.cafarovceyxun.anamuslim.compose.utils.AppLocale
import com.cafarovceyxun.anamuslim.compose.utils.preferences.TestDataStore
import com.cafarovceyxun.anamuslim.compose.utils.setAppLocale
import com.cafarovceyxun.anamuslim.db.TestQuranDatabase
import com.cafarovceyxun.anamuslim.db.entities.quran.MushafLineType
import com.cafarovceyxun.anamuslim.db.entities.quran.NavigationType
import com.cafarovceyxun.anamuslim.utils.reader.QuranScriptUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [QuranRepository] against a real (in-memory) Quran database — see [TestQuranDatabase] for the
 * fixture.
 *
 * These queries are the "silent failure" kind: when a juz range, a page→surah mapping or a name
 * fallback is wrong the app does not crash, it just shows the wrong surah, the wrong verse range,
 * or a blank name. They are also the queries the iOS build now runs unchanged, so this is the
 * regression net for the Room-KMP port as much as for the SQL.
 */
class QuranRepositoryTest {

    private val defaultLocale = AppLocale(
        rawLanguageTag = "app.locale.default",
        languageTag = "en",
        language = "en",
        numeralSystem = null,
    )

    private lateinit var repository: QuranRepository

    @BeforeTest
    fun setUp() {
        // `getFirstPageOf*` reads the script variant from preferences even when the caller passes a
        // script code explicitly.
        TestDataStore.ensureInitialized()
        // The locale is process-wide state, and every name lookup here starts from English. Setting
        // it on both sides of the test means neither test order nor a failed test can leak it.
        setAppLocale(defaultLocale)
    }

    @AfterTest
    fun tearDown() {
        setAppLocale(defaultLocale)
    }

    private suspend fun openFixture() {
        repository = TestQuranDatabase.shared().repository
    }

    // ==================== verse structure (the narrow port's real implementation) ==============

    /**
     * The counterpart of `RecitationVerseNavigationTest`, which drives the same two methods through
     * a hand-written fake. Here the real repository answers them, so the fake's table is pinned to
     * the shipped schema rather than to itself.
     */
    @Test
    fun verseStructureReadsTheSurahTableNotTheSeededAyahs() = runTest {
        openFixture()

        // Only 10 Baqara ayahs are seeded, but the surah row says 286 — a count that walked the
        // ayahs table would answer 10 here.
        assertEquals(286, repository.getChapterVerseCount(2))
        assertEquals(7, repository.getChapterVerseCount(1))
        assertEquals(6, repository.getChapterVerseCount(114))

        assertTrue(repository.isVerseValid4Chapter(2, 286))
        assertEquals(false, repository.isVerseValid4Chapter(2, 287))
        assertEquals(false, repository.isVerseValid4Chapter(1, 8))
        assertEquals(false, repository.isVerseValid4Chapter(1, 0))
    }

    @Test
    fun verseStructureRejectsChaptersOutsideTheQuran() = runTest {
        openFixture()

        assertEquals(0, repository.getChapterVerseCount(0))
        assertEquals(0, repository.getChapterVerseCount(115))
        // Valid chapter number, but absent from this fixture: still 0, never an exception.
        assertEquals(0, repository.getChapterVerseCount(3))
        assertEquals(false, repository.isVerseValid4Chapter(3, 1))
    }

    // ==================== juz / hizb verse ranges ==============================================

    @Test
    fun juzRangesAreGroupedPerSurahInOrder() = runTest {
        openFixture()

        // Juz 1 spans all of Al-Fatiha and the start of Al-Baqara.
        assertEquals(
            listOf(1 to 1..7, 2 to 1..10),
            repository.getChapterVerseRangesInJuz(1),
        )
        assertEquals(
            listOf(114 to 1..6),
            repository.getChapterVerseRangesInJuz(30),
        )
    }

    @Test
    fun hizbRangesSplitASurahAtTheHizbBoundary() = runTest {
        openFixture()

        // The fixture puts the hizb 1 → 2 boundary inside Al-Baqara, after verse 5.
        assertEquals(
            listOf(1 to 1..7, 2 to 1..5),
            repository.getChapterVerseRangesInHizb(1),
        )
        assertEquals(
            listOf(2 to 6..10),
            repository.getChapterVerseRangesInHizb(2),
        )
    }

    @Test
    fun unknownOrInvalidUnitsReturnEmptyRanges() = runTest {
        openFixture()

        assertEquals(emptyList(), repository.getChapterVerseRangesInJuz(0))
        assertEquals(emptyList(), repository.getChapterVerseRangesInJuz(-1))
        // Juz 15 is a real juz with no seeded ayahs — empty, not an error.
        assertEquals(emptyList(), repository.getChapterVerseRangesInJuz(15))
        assertEquals(emptyList(), repository.getChapterVerseRangesInHizb(0))
        assertEquals(emptyList(), repository.getChapterVerseRangesInHizb(59))
    }

    @Test
    fun juzNumbersAndSajdahSurahsComeFromTheAyahRows() = runTest {
        openFixture()

        assertEquals(listOf(1), repository.getJuzNosForChapter(1))
        assertEquals(listOf(30), repository.getJuzNosForChapter(114))
        assertEquals(emptyList(), repository.getJuzNosForChapter(3))

        // 32:15 is the only prostration verse seeded. 32:14/16 carry an explicit 0 and Al-Fatiha's
        // verses carry NULL — the query has to treat both as "no sajdah".
        assertEquals(setOf(32), repository.getSurahNosWithSajdah())
    }

    // ==================== mushaf pages =========================================================

    @Test
    fun pageSurahsResolveThroughStartAyahIdWhenTheLineHasNoSurahNo() = runTest {
        openFixture()

        // Page 1 is Al-Fatiha only — the surah-name line names it, the ayah lines do not.
        assertEquals(listOf(1), repository.getOrderedSurahNosOnMushafPage(1, 1))

        // Page 2 opens with Al-Fatiha's last verse on an ayah line whose surah_no is NULL, so the
        // only way to know surah 1 comes first is the start_ayah_id lookup. Order and de-dup both
        // matter: the surah-name and basmallah lines repeat surah 2.
        assertEquals(listOf(1, 2), repository.getOrderedSurahNosOnMushafPage(1, 2))
        assertEquals(listOf(2), repository.getOrderedSurahNosOnMushafPage(1, 3))
    }

    @Test
    fun pageSurahLookupRejectsUnusableArguments() = runTest {
        openFixture()

        assertEquals(emptyList(), repository.getOrderedSurahNosOnMushafPage(0, 1))
        assertEquals(emptyList(), repository.getOrderedSurahNosOnMushafPage(1, 0))
        // A page beyond the seeded map — empty, not an error.
        assertEquals(emptyList(), repository.getOrderedSurahNosOnMushafPage(1, 99))
    }

    @Test
    fun pageHeaderJoinsTheSurahNamesOnThatPage() = runTest {
        openFixture()

        assertEquals("Al-Fatihah", repository.getChapterNamesOnMushafPage(1, 1))
        assertEquals("Al-Fatihah, Al-Baqarah", repository.getChapterNamesOnMushafPage(1, 2))
        assertEquals("", repository.getChapterNamesOnMushafPage(1, 99))
    }

    @Test
    fun pageLookupsAnswerFromTheMushafMap() = runTest {
        openFixture()

        assertEquals(604, repository.getNumberOfPages(1))
        assertEquals(0, repository.getNumberOfPages(0))
        assertEquals(0, repository.getNumberOfPages(7))

        assertEquals(4, repository.getPageLines(1, 1).size)
        assertEquals(emptyList(), repository.getPageLines(1, 0))

        // Chapter → first page comes from the surah-name line.
        assertEquals(1, repository.getFirstPageOfChapter(1, QuranScriptUtils.SCRIPT_UTHMANI))
        assertEquals(2, repository.getFirstPageOfChapter(2, QuranScriptUtils.SCRIPT_UTHMANI))
        assertNull(repository.getFirstPageOfChapter(114, QuranScriptUtils.SCRIPT_UTHMANI))
        assertNull(repository.getFirstPageOfChapter(0, QuranScriptUtils.SCRIPT_UTHMANI))

        // Verse → page walks start_ayah_id..end_ayah_id, so a verse in the middle of a line counts.
        assertEquals(2, repository.getPageForVerse(surahNo = 2, ayahNo = 3, mushafId = 1))
        assertEquals(1, repository.getPageForVerse(surahNo = 1, ayahNo = 5, mushafId = 1))
        assertEquals(3, repository.getPageForVerse(surahNo = 2, ayahNo = 10, mushafId = 1))
        assertNull(repository.getPageForVerse(surahNo = 2, ayahNo = 200, mushafId = 1))
        assertNull(repository.getPageForVerse(surahNo = 2, ayahNo = 3, mushafId = 0))

        // First ayah on a page is the smallest start_ayah_id, ignoring the non-ayah lines.
        assertEquals(1001, repository.getFirstAyahIdOnPage(1, 1))
        assertEquals(1007, repository.getFirstAyahIdOnPage(1, 2))
        assertNull(repository.getFirstAyahIdOnPage(1, 99))
        assertNull(repository.getFirstAyahIdOnPage(0, 1))

        assertEquals(1, repository.getFirstPageOfJuz(1, QuranScriptUtils.SCRIPT_UTHMANI))
        assertNull(repository.getFirstPageOfJuz(30, QuranScriptUtils.SCRIPT_UTHMANI))
        assertEquals(1, repository.getFirstPageOfHizb(1, QuranScriptUtils.SCRIPT_UTHMANI))
        assertEquals(3, repository.getFirstPageOfHizb(2, QuranScriptUtils.SCRIPT_UTHMANI))
    }

    @Test
    fun juzAndHizbBadgesAreDerivedFromEachPagesFirstAyahLine() = runTest {
        openFixture()

        // Juz badge: the *first* ayah line of the page decides, so page 2's surah-name and
        // basmallah lines must not shift the answer.
        assertEquals(mapOf(1 to 1, 2 to 1, 3 to 1), repository.getJuzForMushafPages(1, listOf(1, 2, 3)))
        assertEquals(emptyMap(), repository.getJuzForMushafPages(1, emptyList()))
        assertEquals(emptyMap(), repository.getJuzForMushafPages(0, listOf(1)))

        // Hizb badge: every hizb present on the page, distinct and sorted.
        assertEquals(
            mapOf(1 to listOf(1), 2 to listOf(1), 3 to listOf(2)),
            repository.getHizbForMushafPages(1, listOf(1, 2, 3)),
        )
    }

    @Test
    fun ayahLineExpansionCoversTheMiddleVersesAndRefusesUnusableLines() = runTest {
        openFixture()

        val page1 = repository.getPageLines(1, 1)
        val page2 = repository.getPageLines(1, 2)

        // 1001..1003: endpoints plus the middle verse pulled from the ayahs table.
        val multiVerseLine = page1.first { it.lineType == MushafLineType.ayah }
        assertEquals(listOf(1001, 1002, 1003), repository.ayahIdsForMushafAyahLine(multiVerseLine))

        // A line holding a single verse never queries for a middle.
        val singleVerseLine = page2.first { it.lineType == MushafLineType.ayah }
        assertEquals(listOf(1007), repository.ayahIdsForMushafAyahLine(singleVerseLine))

        // Surah-name and basmallah lines carry no verses at all.
        val surahNameLine = page1.first { it.lineType == MushafLineType.surah_name }
        assertEquals(emptyList(), repository.ayahIdsForMushafAyahLine(surahNameLine))

        // A malformed ayah line (no word indices) degrades to empty instead of guessing.
        assertEquals(
            emptyList(),
            repository.ayahIdsForMushafAyahLine(
                multiVerseLine.copy(startWordIndex = null, endWordIndex = null)
            ),
        )
        // Reversed endpoints likewise.
        assertEquals(
            emptyList(),
            repository.ayahIdsForMushafAyahLine(
                multiVerseLine.copy(startAyahId = 1003, endAyahId = 1001)
            ),
        )
    }

    @Test
    fun pageLinesForSeveralPagesComeBackGroupedByPage() = runTest {
        openFixture()

        val grouped = repository.getPageLinesGroupedForPages(1, listOf(1, 2, 3))

        assertEquals(setOf(1, 2, 3), grouped.keys)
        assertEquals(4, grouped.getValue(1).size)
        assertEquals(1, grouped.getValue(3).size)
        // Line order is preserved inside each page.
        assertEquals(listOf(1, 2, 3, 4), grouped.getValue(2).map { it.lineNumber })
        assertEquals(emptyMap(), repository.getPageLinesGroupedForPages(1, emptyList()))
    }

    // ==================== names & localization fallback ========================================

    @Test
    fun chapterNamesWalkTheLanguageFallbackChain() = runTest {
        openFixture()

        // English locale: straight hit.
        assertEquals("Al-Fatihah", repository.getChapterName(1))
        assertEquals("Al-Baqarah", repository.getChapterName(2))

        // Turkish: Al-Fatiha has a tr row, Al-Baqara does not and falls through to en.
        setAppLocale(AppLocale(rawLanguageTag = "tr", languageTag = "tr", language = "tr", numeralSystem = null))
        assertEquals("Fâtiha", repository.getChapterName(1))
        assertEquals("Al-Baqarah", repository.getChapterName(2))

        assertEquals(
            mapOf(1 to "Fâtiha", 2 to "Al-Baqarah", 114 to "An-Nas"),
            repository.getChapterNames(listOf(1, 2, 114)),
        )
    }

    @Test
    fun missingNamesAreBlankRatherThanPlaceholders() = runTest {
        openFixture()

        assertEquals("", repository.getChapterName(0))
        // Surah 32 exists but has no localization row at all.
        assertEquals("", repository.getChapterName(32))
        assertEquals(emptyMap(), repository.getChapterNames(emptyList()))
        // Absent chapters are simply left out of the map — no blank entries to render.
        assertEquals(emptyMap(), repository.getChapterNames(listOf(32)))
    }

    /**
     * Azerbaijani names never come from the database — they live in a bundled table, and the DB
     * lookup is skipped entirely. Worth pinning: it means an `az` build cannot be debugged by
     * looking at `surah_localizations`.
     */
    @Test
    fun azerbaijaniNamesBypassTheDatabaseEntirely() = runTest {
        openFixture()

        setAppLocale(AppLocale(rawLanguageTag = "az", languageTag = "az", language = "az", numeralSystem = null))

        // Surah 32 has no localization row, yet an Azerbaijani name still comes back.
        assertTrue(repository.getChapterName(32).isNotBlank())
        // And the English row that *is* there is not what gets used for Al-Fatiha.
        assertTrue(repository.getChapterName(1) != "Al-Fatihah")
        assertTrue(repository.getChapterName(1).isNotBlank())
    }

    // ==================== surah rows & navigation units ========================================

    @Test
    fun surahRowsCarryTheirLocalizationsAndRevelationData() = runTest {
        openFixture()

        val fatiha = repository.getSurah(1)
        assertEquals(7, fatiha?.ayahCount)
        assertEquals(5, fatiha?.revelationOrder)
        assertNull(repository.getSurah(3))

        val withLocalizations = repository.getSurahWithLocalizations(1)
        assertEquals("Al-Fatihah", withLocalizations?.getCurrentName())
        assertEquals("The Opener", withLocalizations?.getCurrentMeaning())

        val byNos = repository.getSurahsWithLocalizationsByChapterNos(listOf(1, 2, 3))
        // Surah 3 is not in the fixture, so it is simply absent from the map.
        assertEquals(setOf(1, 2), byNos.keys)

        assertEquals(4, repository.getAllSurahs().first().size)
    }

    @Test
    fun navigationUnitsGroupRangesBySurahAndSortByUnitNumber() = runTest {
        openFixture()

        val juzs = repository.getJuzs().first()

        assertEquals(listOf(1, 30), juzs.map { it.unitNo })
        assertTrue(juzs.all { it.type == NavigationType.juz })

        val juz1 = juzs.first()
        assertEquals(listOf(1, 2), juz1.ranges.map { it.surah.surah.surahNo })
        assertEquals(1..7, juz1.ranges[0].startAyah..juz1.ranges[0].endAyah)
        assertEquals(141, juz1.ranges[1].endAyah)

        // Hizb ranges live in the same table, keyed by type — the juz query must not pick them up.
        val hizbs = repository.getHizbs().first()
        assertEquals(listOf(1), hizbs.map { it.unitNo })
        assertTrue(hizbs.all { it.type == NavigationType.hizb })
        // Types with no rows yield an empty list rather than a stray unit.
        assertEquals(emptyList(), repository.getRubs().first())
    }

    @Test
    fun verseLookupsUseTheAyahIdFormula() = runTest {
        openFixture()

        val ayah = repository.getAyah(2, 5)
        assertEquals(2005, ayah?.ayahId)
        assertEquals(1, ayah?.hizbNo)

        // Same row, reached by id — the surahNo * 1000 + ayahNo rule the whole mushaf map relies on.
        assertEquals(ayah, repository.getAyahById(2005))
        assertNull(repository.getAyah(2, 11))
        assertNull(repository.getAyahById(2011))
    }

}
