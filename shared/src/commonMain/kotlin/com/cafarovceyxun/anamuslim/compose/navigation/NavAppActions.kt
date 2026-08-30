package com.cafarovceyxun.anamuslim.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeActions
import com.cafarovceyxun.anamuslim.compose.components.player.PlayerActions
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderActions
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithActions
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.utils.app.AppStoreReviewProvider
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.ReadType

/**
 * [HomeActions] over [AppNavHost] destinations — the shared counterpart of Android's
 * `rememberHomeActions`, which launches Activities instead.
 */
@Composable
fun rememberNavHomeActions(navController: NavHostController): HomeActions =
    remember(navController) {
        HomeActions(
            onOpenHadithReadHistory = {
                navController.navigate(AppDestination.HadithReadHistory)
            },
            onOpenHadithItem = { volume, book, chapter, sub, title ->
                navController.navigate(
                    AppDestination.HadithItems(title, volume, book, chapter, sub),
                )
            },
            // The hadith-of-the-day card's "read" button. A null id means the daily content has no
            // hadith attached, so there is nothing to open.
            onOpenHadithById = { hadithId ->
                hadithId?.let { navController.navigate(AppDestination.HadithDetail(it)) }
            },
            onOpenReadHistory = {
                navController.navigate(AppDestination.ReadHistory)
            },
            onOpenBookmarks = {
                navController.navigate(AppDestination.Bookmarks)
            },
            onOpenSuggestions = {
                // Detail route, not the tab root — see AppDestination.SettingsDetail.
                navController.navigate(
                    AppDestination.SettingsDetail(startRoute = SettingRoutes.SUGGESTIONS),
                )
            },
            onOpenReaderFromHistory = { history ->
                history.toReaderRoute()?.let(navController::navigate)
            },
            onOpenVerse = { chapterNo, verseNo ->
                navController.navigate(
                    AppDestination.Reader(
                        chapterNo = chapterNo,
                        initialChapterNo = chapterNo,
                        initialVerseNo = verseNo,
                    ),
                )
            },
        )
    }

/**
 * [IndexMenuActions] over [AppNavHost] destinations.
 *
 * `onRateApp` goes through [AppStoreReviewProvider] and is null until a platform registers it, so
 * the row stays hidden where there is nothing to open rather than doing nothing when tapped.
 *
 * `onOpenPlayStore`/`onShareApp` are still **null** here, which hides their entries: both want a
 * store link of their own and are a product decision rather than a missing seam. Sharing already
 * has its mechanism (`PlatformUtils.shareText`) and only wants the link. See the plan's 76th wave.
 */
@Composable
fun rememberNavIndexMenuActions(
    navController: NavHostController,
): IndexMenuActions = remember(navController) {
    IndexMenuActions(
        onOpenBookmarks = { navController.navigate(AppDestination.Bookmarks) },
        // Detail route, not the tab root — see AppDestination.SettingsDetail.
        onOpenSettings = { navController.navigate(AppDestination.SettingsDetail()) },
        onOpenStorageCleanup = { navController.navigate(AppDestination.StorageCleanup) },
        onOpenExportImport = { navController.navigate(AppDestination.ExportImport) },
        onOpenAboutUs = { navController.navigate(AppDestination.About) },
        onRateApp = if (AppStoreReviewProvider.isAvailable) {
            {
                val review = AppStoreReviewProvider.review
                // Deliberate order: the OS sheet is one tap and never leaves the app, and the
                // listing is the fallback for the platforms (and the rate-limited days) where it
                // does not appear. A written review needs the listing either way.
                if (!review.requestInAppRating()) review.openReviewPage()
            }
        } else {
            null
        },
    )
}

/**
 * [ReaderActions] over [AppNavHost] destinations — the shared counterpart of Android's
 * `rememberReaderActions`, which starts `ActivitySettings` with the same extra.
 *
 * Without this the reader's app-bar settings button falls back to the no-op default and does
 * nothing at all, which is what it did on iOS until now.
 */
@Composable
fun rememberNavReaderActions(navController: NavHostController): ReaderActions =
    remember(navController) {
        ReaderActions(
            onOpenReaderSettings = {
                navController.navigate(
                    // Detail route, not the tab root — see AppDestination.SettingsDetail.
                    AppDestination.SettingsDetail(startRoute = SettingRoutes.MAIN),
                )
            },
        )
    }

/**
 * [PlayerActions] over [AppNavHost] destinations — the counterpart of Android's
 * `rememberPlayerActions`. Same gap as [rememberNavReaderActions]: unprovided, the player's
 * "manage recitation downloads" button was inert.
 */
@Composable
fun rememberNavPlayerActions(navController: NavHostController): PlayerActions =
    remember(navController) {
        PlayerActions(
            onOpenRecitationDownloads = {
                navController.navigate(
                    // Detail route, not the tab root — see AppDestination.SettingsDetail.
                    AppDestination.SettingsDetail(startRoute = SettingRoutes.RECITATION_DOWNLOAD),
                )
            },
        )
    }

/**
 * [HadithActions] over [AppNavHost] destinations — the counterpart of Android's
 * `rememberHadithActions`, which starts `ActivitySettings` with no extras, i.e. the full settings
 * screen. Same gap as [rememberNavReaderActions]: unprovided, three hadith controls were inert on
 * iOS — the index app bar's settings button, the navigator sheet's settings button, and the
 * empty-state button that is the **only** way a fresh install reaches the hadith download.
 */
@Composable
fun rememberNavHadithActions(navController: NavHostController): HadithActions =
    remember(navController) {
        HadithActions(
            onOpenSettings = {
                // Detail route, not the tab root — see AppDestination.SettingsDetail.
                navController.navigate(AppDestination.SettingsDetail())
            },
        )
    }

/**
 * A read-history row → the reader route that resumes it, or null when the stored ids are no longer
 * valid. Mirrors Android's `ReaderFactory.prepareHistoryIntent`, minus two things the flat route
 * cannot carry: the reader mode, and mushaf-page resume (page numbers only mean something together
 * with a mushaf code). Both degrade to opening the same verse in whatever mode the reader is set
 * to — the same content, reached differently.
 */
internal fun ReadHistoryEntity.toReaderRoute(): AppDestination.Reader? =
    when (ReadType.fromValue(readType)) {
        ReadType.Chapter -> if (!QuranMeta.isChapterValid(chapterNo)) {
            null
        } else {
            AppDestination.Reader(
                chapterNo = chapterNo,
                initialChapterNo = chapterNo,
                initialVerseNo = fromVerseNo,
            )
        }

        ReadType.Juz -> if (!QuranMeta.isJuzValid(divisionNo)) {
            null
        } else {
            AppDestination.Reader(
                juzNo = divisionNo,
                initialChapterNo = chapterNo,
                initialVerseNo = fromVerseNo,
            )
        }

        ReadType.Hizb -> if (!QuranMeta.isHizbValid(divisionNo)) {
            null
        } else {
            AppDestination.Reader(
                hizbNo = divisionNo,
                initialChapterNo = chapterNo,
                initialVerseNo = fromVerseNo,
            )
        }
    }
