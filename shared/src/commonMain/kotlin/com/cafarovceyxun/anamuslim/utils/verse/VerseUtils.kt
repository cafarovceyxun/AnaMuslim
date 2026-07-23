package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.compose.utils.preferences.VersePreferences
import com.cafarovceyxun.anamuslim.db.relations.VerseWithDetails
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.currentEpochMillis
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Picks and remembers the verse of the day.
 *
 * ⚠️ **This logic was accidentally emptied during the Faza 4 migration** (commit `93706bb`,
 * 2026-07-14): the body was replaced with delegation to `VerseUtilsHooks`, but nothing ever
 * registered those hooks — on either platform. `getVOTD()` therefore returned null everywhere,
 * which silently disabled the daily notification (`VerseOfTheDayWorker` bailed with
 * `Result.failure()`), the VOTD home card and the "verse of the day" badge in the reader. The
 * original logic is restored here, now in `commonMain`: every dependency it had (repository,
 * preferences, `QuranMeta`) is shared, and the one Android-only part — the launcher shortcut —
 * stays behind [VerseUtilsHooks].
 */
object VerseUtils {

    private const val VOTD_RESET_MILLIS = 24L * 60L * 60L * 1000L
    private const val MAX_VOTD_ATTEMPTS = 20

    private var votdChapNo: Int = -1
    private var votdVerseNo: Int = -1

    /**
     * The verse of the day: the stored one while it is less than 24h old, otherwise a freshly
     * drawn one. Null only if no suitable verse could be found.
     */
    suspend fun getVOTD(): VerseWithDetails? = withContext(Dispatchers.IO) {
        val repository = RepositoryProvider.quranRepository

        // 1) Always reuse the stored verse first — the "of the day" promise depends on it.
        val savedVerse = VersePreferences.getVotd()

        if (savedVerse != null && !isExpired(VersePreferences.getVotdTimestamp())) {
            val existing = buildVerseWithDetails(savedVerse.chapterNo, savedVerse.verseNo)

            if (existing != null) {
                votdChapNo = existing.chapterNo
                votdVerseNo = existing.verseNo
                return@withContext existing
            }

            // Stored verse no longer resolves (corrupt/invalid) — drop it and draw a new one.
            VersePreferences.removeVotd()
        }

        // 2) Draw a new one. Not every verse reads well standalone, hence the bounded retry.
        repeat(MAX_VOTD_ATTEMPTS) {
            val chapterNo = Random.nextInt(1, QuranMeta.chapterRange.last + 1)
            val verseCount = repository.getChapterVerseCount(chapterNo)
            if (verseCount <= 0 || verseCount == Int.MAX_VALUE) return@repeat

            val verseNo = Random.nextInt(1, verseCount + 1)
            val vwd = buildVerseWithDetails(chapterNo, verseNo)

            if (vwd != null && vwd.isIdealForVOTD()) {
                VersePreferences.saveVotd(
                    chapterNo = chapterNo,
                    verseNo = verseNo,
                    timestamp = currentEpochMillis(),
                )

                VerseUtilsHooks.pushVotdShortcut?.invoke(chapterNo, verseNo)

                votdChapNo = chapterNo
                votdVerseNo = verseNo

                return@withContext vwd
            }
        }

        null
    }

    /** True when [chapterNo]:[verseNo] is the current verse of the day (drives the reader badge). */
    fun isVOTD(chapterNo: Int, verseNo: Int): Boolean {
        if (votdChapNo <= 0 || votdVerseNo <= 0) {
            val votd = VersePreferences.getVotd() ?: return false
            votdChapNo = votd.chapterNo
            votdVerseNo = votd.verseNo
        }

        return chapterNo == votdChapNo && verseNo == votdVerseNo
    }

    fun getVotdSeed(): Long = currentEpochMillis() / (1000 * 60 * 60 * 24)

    private suspend fun buildVerseWithDetails(chapterNo: Int, verseNo: Int): VerseWithDetails? {
        val repository = RepositoryProvider.quranRepository

        if (!QuranMeta.isChapterValid(chapterNo)) return null
        if (!repository.isVerseValid4Chapter(chapterNo, verseNo)) return null

        return repository.getVerseWithDetails(
            chapterNo,
            verseNo,
            // Words are loaded so `isIdealForVOTD` can measure the verse; the UI still hides them.
            arabicEnabled = true,
        )
    }

    private fun isExpired(timestamp: Long): Boolean {
        if (timestamp <= 0L) return true

        val age = currentEpochMillis() - timestamp

        // A negative age means the clock moved backwards; treat that as expired rather than trust it.
        return age < 0L || age >= VOTD_RESET_MILLIS
    }
}
