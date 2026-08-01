package com.cafarovceyxun.anamuslim.compose.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe routes for screens that already live in `commonMain`.
 *
 * On Android each of these is still reachable through its own `Activity`; the shared [AppNavHost]
 * is what iOS uses instead, and what those Activities collapse into as Faza 5 proceeds.
 */
sealed interface AppDestination {

    @Serializable
    data object About : AppDestination

    @Serializable
    data object Bookmarks : AppDestination

    @Serializable
    data object ExportImport : AppDestination


    @Serializable
    data object SettingsTheme : AppDestination

    /**
     * The whole settings graph, not a single screen: it hosts its own nested NavHost over the
     * string-based [SettingRoutes], which is what the settings screens navigate through.
     *
     * [startRoute] is one of those string routes; null opens the settings hub. It is the route
     * counterpart of the `NAV_DESTINATION` intent extra Android's `SettingsScreen` reads.
     */
    @Serializable
    data class Settings(val startRoute: String? = null) : AppDestination

    /**
     * Settings opened **on top of** whatever is on screen (the reader's gear, the player's download
     * button, the index menu, the word-by-word sheet) — never through the bottom bar.
     *
     * It exists only so those pushes do not land the tab-root [Settings] inside another tab's back
     * stack: `BottomTabBar` saves that stack with `saveState` and restores it later, so a pushed
     * [Settings] made the Quran tab reopen on a settings page with "Settings" lit in the bar. Android
     * cannot hit this because there the same buttons start `ActivitySettings`, a separate stack; this
     * is that separation, expressed as a route. Same screen, same argument — only the identity differs.
     */
    @Serializable
    data class SettingsDetail(val startRoute: String? = null) : AppDestination

    /** Search pushed over the current screen (the Quran index's search icon). See [SettingsDetail]. */
    @Serializable
    data object SearchDetail : AppDestination

    @Serializable
    data object ReadHistory : AppDestination

    /**
     * The reader. Exactly one of the four ids selects what to open, mirroring the four
     * [com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData] variants.
     *
     * A flat DTO rather than the sealed type itself: type-safe routes only carry primitives without
     * a custom `NavType`, and this hop is the only place the two shapes meet.
     *
     * `LocalReaderActions`/`LocalPlayerActions` fall back to their no-op defaults where the host
     * provides none (iOS today).
     */
    @Serializable
    data class Reader(
        val chapterNo: Int? = null,
        val juzNo: Int? = null,
        val hizbNo: Int? = null,
        val pageNo: Int? = null,
        /** Verse to scroll to on open, as `chapter:verse`; null opens at the top. */
        val initialChapterNo: Int? = null,
        val initialVerseNo: Int? = null,
    ) : AppDestination

    @Serializable
    data object ReaderIndex : AppDestination

    /** Chapter-info page (about the surah), rendered from HTML by the platform web view. */
    @Serializable
    data class ChapterInfo(val chapterNo: Int) : AppDestination

    @Serializable
    data object EditsManagement : AppDestination

    @Serializable
    data object LanguageSelection : AppDestination

    @Serializable
    data object TranslationSelection : AppDestination

    @Serializable
    data object Scripts : AppDestination

    @Serializable
    data object SettingsWbw : AppDestination

    @Serializable
    data object RecitationDownload : AppDestination

    @Serializable
    data object Search : AppDestination

    @Serializable
    data object StorageCleanup : AppDestination

    /** The homepage — the shared host's real entry point, not just another reachable screen. */
    @Serializable
    data object Home : AppDestination

    @Serializable
    data object HadithIndex : AppDestination

    @Serializable
    data object HadithReadHistory : AppDestination

    /**
     * Hadith items for one slug path. Shares its [HadithViewModel] with [HadithIndex] through
     * `LocalAppViewModelStoreOwner`, so the host must provide that owner above the NavHost.
     */
    @Serializable
    data class HadithItems(
        val title: String,
        val volumeSlug: String? = null,
        val bookSlug: String? = null,
        val chapterSlug: String? = null,
        val subChapterSlug: String? = null,
    ) : AppDestination
}
