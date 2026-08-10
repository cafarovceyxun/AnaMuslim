package com.cafarovceyxun.anamuslim.repository

/**
 * How the Quran is divided into verses — the only thing verse-to-verse navigation needs to know.
 *
 * Deliberately **two methods, not a mirror of [QuranRepository]**. Callers like
 * `RecitationServiceState.getNextVerse` already took the repository as a parameter; narrowing that
 * parameter to what they actually read costs nothing at the call sites, states the dependency
 * honestly, and lets the navigation policy be exercised without a Room database behind it. A
 * 48-method interface over the whole repository would do none of those things better.
 *
 * The facts here are structural and identical for every user, which is why they can be answered by
 * a plain table in a test as faithfully as by the bundled Quran database.
 */
interface QuranVerseStructure {

    /** Number of verses in [chapterNo], or 0 when the chapter number is out of range. */
    suspend fun getChapterVerseCount(chapterNo: Int): Int

    suspend fun isVerseValid4Chapter(chapterNo: Int, verseNo: Int): Boolean
}
