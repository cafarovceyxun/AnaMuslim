package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity

/**
 * Platform hooks for reader UI needs that have no common-code equivalent, following the
 * settable-sink convention (`AppLogger`, `TranslationPlatformHooks`). Registered at startup
 * (Android `QuranApp.onCreate()`); unset hooks fall back to a common-code default.
 */
object ReaderUiHooks {

    /**
     * Opens the platform's chapter-info screen for the given chapter (Android:
     * `ActivityChapInfo`). No-op while unset (iOS until reader navigation lands in Faza 6).
     */
    var openChapterInfo: ((chapterNo: Int) -> Unit)? = null

    /**
     * Opens the reader at the given verse (Android: `ReaderFactory.startVerse` →
     * `MainActivity`). No-op while unset (iOS until reader navigation lands in Faza 6).
     */
    var openVerse: ((chapterNo: Int, verseNo: Int) -> Unit)? = null

    /**
     * Opens the settings screen at the given `SettingRoutes` destination (Android:
     * `ActivitySettings` with `Keys.NAV_DESTINATION`). No-op while unset (iOS Faza 6).
     */
    var openSettingsRoute: ((route: String) -> Unit)? = null

    /** Opens the search screen (Android: `ActivitySearch`). No-op while unset (iOS Faza 6). */
    var openSearch: (() -> Unit)? = null

    /**
     * Opens the reader at the given verse range (Android: `ReaderFactory.startVerseRange`).
     * No-op while unset (iOS Faza 6).
     */
    var openVerseRange: ((chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit)? = null

    /**
     * Opens the reader at the given verse range, pinned to [translationSlugs] for that visit only
     * (the user's saved translation selection is left untouched). Used by `ReferenceScreen`, which
     * shows a curated translation set. No-op while unset (iOS Faza 6).
     */
    var openVerseRangeWithTranslations: ((
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        translationSlugs: Set<String>,
    ) -> Unit)? = null

    /**
     * Opens the reader at a saved [ReadHistoryEntity] position (Android:
     * `ReaderFactory.prepareHistoryIntent`). A global sink rather than a CompositionLocal because
     * the read-history screen is hosted outside the homepage's `LocalHomeActions` scope. No-op
     * while unset (iOS Faza 6).
     */
    var openReaderFromHistory: ((history: ReadHistoryEntity) -> Unit)? = null

    /** Opens the reader at a whole chapter (Android: `ReaderFactory.startChapter`). */
    var openChapter: ((chapterNo: Int) -> Unit)? = null

    /** Opens the reader at a juz (Android: `ReaderFactory.startJuz`). */
    var openJuz: ((juzNo: Int) -> Unit)? = null

    /** Opens the reader at a hizb (Android: `ReaderFactory.startHizb`). */
    var openHizb: ((hizbNo: Int) -> Unit)? = null

}
