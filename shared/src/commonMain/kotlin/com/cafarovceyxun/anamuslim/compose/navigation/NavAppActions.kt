package com.cafarovceyxun.anamuslim.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions
import com.cafarovceyxun.anamuslim.compose.components.homepage.HomeActions
import com.cafarovceyxun.anamuslim.compose.components.player.PlayerActions
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderActions
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithActions
import com.cafarovceyxun.anamuslim.compose.utils.PlatformUtils
import com.cafarovceyxun.anamuslim.db.entities.user.ReadHistoryEntity
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strMsgShareApp
import com.cafarovceyxun.anamuslim.resources.strTitleShareApp
import com.cafarovceyxun.anamuslim.utils.app.AppStoreReviewProvider
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.ReadType
import org.jetbrains.compose.resources.stringResource

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
 * All three store rows — rate, update, share — hang off [AppStoreReviewProvider] and are null while
 * no platform has registered it, so each row stays hidden where there is nothing to open rather
 * than doing nothing when tapped (the seam rule in `CLAUDE.md`).
 *
 * They were held back while the App Store listing was still unpublished, on the grounds that a row
 * sharing a dead URL is worse than no row; the listing went live on 2026-08-15, so the seam now
 * carries the link ([AppStoreReview.listingUrl]) and the rows are shown.
 */
@Composable
fun rememberNavIndexMenuActions(
    navController: NavHostController,
): IndexMenuActions {
    val storeAvailable = AppStoreReviewProvider.isAvailable

    // Read outside the lambda: `stringResource` is a composable and the share text is a template
    // that has to be resolved in the app's language, not at the moment of the tap.
    val listingUrl = if (storeAvailable) AppStoreReviewProvider.review.listingUrl else ""
    val shareMessage = stringResource(Res.string.strMsgShareApp, listingUrl)
    val shareTitle = stringResource(Res.string.strTitleShareApp)

    return remember(navController, storeAvailable, shareMessage, shareTitle) {
        IndexMenuActions(
            onOpenBookmarks = { navController.navigate(AppDestination.Bookmarks) },
            // Detail route, not the tab root — see AppDestination.SettingsDetail.
            onOpenSettings = { navController.navigate(AppDestination.SettingsDetail()) },
            onOpenStorageCleanup = { navController.navigate(AppDestination.StorageCleanup) },
            onOpenExportImport = { navController.navigate(AppDestination.ExportImport) },
            onOpenAboutUs = { navController.navigate(AppDestination.About) },
            onOpenPlayStore = if (storeAvailable) {
                { AppStoreReviewProvider.review.openListing() }
            } else {
                null
            },
            onShareApp = if (storeAvailable) {
                { PlatformUtils.shareText(shareMessage, shareTitle) }
            } else {
                null
            },
            onRateApp = if (storeAvailable) {
                {
                    val review = AppStoreReviewProvider.review
                    // Deliberate order: the OS sheet is one tap and never leaves the app, and the
                    // listing is the fallback for the platforms (and the rate-limited days) where
                    // it does not appear. A written review needs the listing either way.
                    if (!review.requestInAppRating()) review.openReviewPage()
                }
            } else {
                null
            },
        )
    }
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
