package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.compose.utils.preferences.TestDataStore
import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.db.TestQuranDatabase
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.db.entities.quran.RevelationType
import com.cafarovceyxun.anamuslim.db.entities.quran.SurahEntity
import com.cafarovceyxun.anamuslim.db.relations.SurahWithLocalizations
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verse-of-the-day selection.
 *
 * This logic is worth a net specifically because it has failed silently before: during the Faza 4
 * migration `getVOTD()` was replaced with a delegation to hooks nobody registered, so it returned
 * null on both platforms and quietly switched off the daily notification, the home card and the
 * reader badge — with no crash and no log. The class doc on [VerseUtils] records it.
 *
 * ## Why the "draw a new verse" path always comes back null here
 *
 * A freshly drawn verse must pass `isIdealForVOTD()`, which measures the joined Arabic word text.
 * [TestQuranDatabase] seeds no `ayah_words` rows at all, so every candidate measures 0 characters
 * and is rejected. That makes the draw deterministic — it always fails — which is exactly what lets
 * these tests assert on it: `Random` picking a chapter out of 114 could otherwise make any
 * "did it redraw?" assertion flaky. The ideality rule itself is covered directly, below.
 */
class VerseUtilsTest {

    @BeforeTest
    fun setUp() {
        TestDataStore.ensureInitialized()
    }

    /** Builds the fixture and points [RepositoryProvider] at it, which is what [VerseUtils] reads. */
    private suspend fun openFixture() {
        TestQuranDatabase.shared()
    }

    @Test
    fun aStoredVerseIsReusedWhileItIsLessThanADayOld() = runTest {
        openFixture()
        VersePreferences.saveVotd(chapterNo = 2, verseNo = 5, timestamp = currentEpochMillis())

        val votd = VerseUtils.getVOTD()

        // The "of the day" promise: the same verse comes back, not a new draw.
        assertEquals(2, votd?.chapterNo)
        assertEquals(5, votd?.verseNo)
        // And it is returned even though it would never be *drawn* — the stored path deliberately
        // does not re-apply the ideality gate, otherwise today's verse could vanish mid-day.
        assertFalse(votd!!.isIdealForVOTD())

        // Asking again does not rotate it either.
        assertEquals(5, VerseUtils.getVOTD()?.verseNo)
    }

    @Test
    fun anExpiredVerseIsNotReturnedAgain() = runTest {
        openFixture()
        val yesterday = currentEpochMillis() - 25L * 60L * 60L * 1000L
        VersePreferences.saveVotd(chapterNo = 2, verseNo = 5, timestamp = yesterday)

        // 25 hours old → past the 24h reset, so the stored verse is abandoned and a draw happens
        // (which finds nothing in this fixture).
        assertNull(VerseUtils.getVOTD())
    }

    @Test
    fun aClockThatMovedBackwardsCountsAsExpired() = runTest {
        openFixture()
        // A timestamp in the future means the device clock went backwards; trusting it would pin
        // the same verse for however long the skew lasts.
        VersePreferences.saveVotd(
            chapterNo = 2,
            verseNo = 5,
            timestamp = currentEpochMillis() + 60L * 60L * 1000L,
        )

        assertNull(VerseUtils.getVOTD())
    }

    @Test
    fun aMissingTimestampCountsAsExpired() = runTest {
        openFixture()
        VersePreferences.saveVotd(chapterNo = 2, verseNo = 5, timestamp = 0L)

        assertNull(VerseUtils.getVOTD())
    }

    @Test
    fun aStoredVerseThatNoLongerResolvesIsDropped() = runTest {
        openFixture()
        // Surah 3 is not in the fixture — the same shape as a preference written by an older build
        // or corrupted on disk.
        VersePreferences.saveVotd(chapterNo = 3, verseNo = 1, timestamp = currentEpochMillis())

        assertNull(VerseUtils.getVOTD())
        // The unusable value must not be left behind to be retried forever.
        assertNotEquals(3, VersePreferences.getVotd()?.chapterNo)
    }

    @Test
    fun aVerseOutsideItsChaptersRangeIsDropped() = runTest {
        openFixture()
        // Al-Fatiha has 7 verses; verse 9 is what a truncated or mis-parsed preference looks like.
        VersePreferences.saveVotd(chapterNo = 1, verseNo = 9, timestamp = currentEpochMillis())

        assertNull(VerseUtils.getVOTD())
    }

    @Test
    fun theReaderBadgeFollowsTheStoredVerse() = runTest {
        openFixture()
        VersePreferences.saveVotd(chapterNo = 2, verseNo = 5, timestamp = currentEpochMillis())
        VerseUtils.getVOTD()

        assertTrue(VerseUtils.isVOTD(2, 5))
        assertFalse(VerseUtils.isVOTD(2, 6))
        assertFalse(VerseUtils.isVOTD(1, 5))
    }

    /**
     * The gate every drawn verse has to pass. Two sentences of Arabic is a good daily verse;
     * a two-word one says nothing on a notification, and a page-long one does not fit.
     */
    @Test
    fun idealityIsMeasuredOnTheJoinedArabicText() {
        // Boundaries: the rule is `length in 6..300`, measured after joining words with spaces.
        assertFalse(verseWithWords("بِس").isIdealForVOTD())      // 3 chars — too short
        assertTrue(verseWithWords("بِسْمِ", "ٱللَّهِ").isIdealForVOTD())
        assertFalse(verseWithWords().isIdealForVOTD())            // no words at all
        assertTrue(verseWithWords("ا".repeat(300)).isIdealForVOTD())
        assertFalse(verseWithWords("ا".repeat(301)).isIdealForVOTD())
        // The joining spaces count towards the length — two 3-char words clear the minimum only
        // because of the space between them.
        assertTrue(verseWithWords("بِس", "مِٱل").isIdealForVOTD())
    }

    private fun verseWithWords(vararg texts: String): VerseWithDetails = VerseWithDetails(
        words = texts.mapIndexed { index, text ->
            AyahWordEntity(
                ayahId = 2005,
                scriptId = 1,
                wordIndex = index,
                text = text,
            )
        },
        pageNo = 1,
        verse = AyahEntity(
            ayahId = 2005,
            surahNo = 2,
            ayahNo = 5,
            juzNo = 1,
            hizbNo = 1,
            rubNo = 2,
            manzilNo = 1,
            rukuNo = 1,
            sajdahType = null,
        ),
        chapter = SurahWithLocalizations(
            surah = SurahEntity(
                surahNo = 2,
                ayahCount = 286,
                revelationOrder = 87,
                rukusCount = 40,
                revelationType = RevelationType.medinan,
            ),
            localizations = emptyList(),
        ),
    )
}
