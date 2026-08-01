package com.cafarovceyxun.anamuslim.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.cafarovceyxun.anamuslim.compose.utils.LocalSystemBack
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cafarovceyxun.anamuslim.compose.screens.AboutScreen
import com.cafarovceyxun.anamuslim.compose.screens.BookmarksScreen
import com.cafarovceyxun.anamuslim.compose.screens.ExportImportScreen
import com.cafarovceyxun.anamuslim.compose.screens.HadithReadHistoryScreen
import com.cafarovceyxun.anamuslim.compose.screens.HomeScreen
import com.cafarovceyxun.anamuslim.compose.screens.ReadHistoryScreen
import com.cafarovceyxun.anamuslim.compose.screens.storageCleanup.StorageCleanupScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithIndexScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithItemsScreen
import com.cafarovceyxun.anamuslim.compose.screens.search.SearchScreen
import com.cafarovceyxun.anamuslim.compose.components.player.LocalPlayerActions
import com.cafarovceyxun.anamuslim.compose.components.reader.LocalReaderActions
import com.cafarovceyxun.anamuslim.compose.screens.reader.ReaderScreen
import com.cafarovceyxun.anamuslim.compose.screens.reader.ReaderIndexScreen
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData
import com.cafarovceyxun.anamuslim.utils.reader.ReaderUiHooks
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams
import com.cafarovceyxun.anamuslim.compose.screens.chapterInfo.ChapterInfoScreen
import com.cafarovceyxun.anamuslim.compose.screens.settings.RecitationDownloadScreen
import com.cafarovceyxun.anamuslim.compose.screens.settings.EditsManagementScreen
import com.cafarovceyxun.anamuslim.compose.screens.settings.LanguageSelectionScreen
import com.cafarovceyxun.anamuslim.compose.screens.settings.ScriptsScreen
import com.cafarovceyxun.anamuslim.compose.screens.settings.TranslationSelectionScreen
import com.cafarovceyxun.anamuslim.compose.screens.settings.SettingsNavHost
import com.cafarovceyxun.anamuslim.compose.screens.settings.SettingsThemeScreen
import com.cafarovceyxun.anamuslim.compose.screens.settings.SettingsWbwScreen
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Shared NavHost over the `commonMain` screens.
 *
 * Export/import and reader hand-off stay as parameters rather than being resolved here: their
 * implementations are still platform-bound (SAF document pickers on Android, the reader Activity),
 * so the host stays free of platform seams until those screens migrate too.
 */
@Composable
fun AppNavHost(
    startDestination: AppDestination = AppDestination.About,
    navController: NavHostController = rememberNavController(),
    onOpenInReader: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit = { _, _, _ -> },
    onExport: (scopes: Map<String, Boolean>) -> Unit = {},
    onImport: (scopes: Map<String, Boolean>) -> Unit = {},
) {
    // iOS reads this for its back-button action (Android uses the OnBackPressedDispatcher and
    // ignores it). Remembered so its identity is stable and the static local does not invalidate
    // every consumer on recomposition.
    val systemBack: () -> Unit = remember(navController) { { navController.popBackStack(); Unit } }

    CompositionLocalProvider(LocalSystemBack provides systemBack) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<AppDestination.About> { AboutScreen() }
        composable<AppDestination.Bookmarks> {
            BookmarksScreen(
                onOpenInReader = onOpenInReader,
                onOpenHadith = { title, chapterSlug, subChapterSlug ->
                    navController.navigate(
                        AppDestination.HadithItems(
                            title = title,
                            chapterSlug = chapterSlug,
                            subChapterSlug = subChapterSlug,
                        ),
                    )
                },
            )
        }
        composable<AppDestination.ExportImport> {
            ExportImportScreen(exportCallback = onExport, importCallback = onImport)
        }
        composable<AppDestination.SettingsTheme> { SettingsThemeScreen() }
        composable<AppDestination.Settings> { entry ->
            val startRoute = entry.toRoute<AppDestination.Settings>().startRoute
            SettingsNavHost(startDestination = startRoute ?: SettingRoutes.MAIN.arg(false))
        }
        // Same screen as above, different route identity — see AppDestination.SettingsDetail.
        composable<AppDestination.SettingsDetail> { entry ->
            val startRoute = entry.toRoute<AppDestination.SettingsDetail>().startRoute
            SettingsNavHost(startDestination = startRoute ?: SettingRoutes.MAIN.arg(false))
        }
        composable<AppDestination.ReadHistory> { ReadHistoryScreen() }
        composable<AppDestination.HadithReadHistory> {
            HadithReadHistoryScreen(
                onOpenHistory = { history ->
                    navController.navigate(
                        AppDestination.HadithItems(
                            title = history.title,
                            volumeSlug = history.volumeSlug,
                            bookSlug = history.bookSlug,
                            chapterSlug = history.chapterSlug,
                            subChapterSlug = history.subChapterSlug,
                        ),
                    )
                },
            )
        }
        composable<AppDestination.StorageCleanup> { StorageCleanupScreen() }
        composable<AppDestination.Home> {
            HomeScreen(
                modifier = Modifier.fillMaxSize(),
                homeActions = rememberNavHomeActions(navController),
                indexMenuActions = rememberNavIndexMenuActions(navController),
            )
        }
        composable<AppDestination.Reader> { entry ->
            // Mirrors Android's MainScreen, which provides the same two seams around the reader.
            // Left unprovided they fall back to no-op defaults, i.e. dead buttons.
            CompositionLocalProvider(
                LocalReaderActions provides rememberNavReaderActions(navController),
                LocalPlayerActions provides rememberNavPlayerActions(navController),
            ) {
                ReaderScreen(entry.toRoute<AppDestination.Reader>().toLaunchParams())
            }
        }
        composable<AppDestination.ReaderIndex> {
            ReaderIndexScreen(
                onNavigateToReader = { params ->
                    navController.navigate(params.toReaderRoute())
                },
            )
        }
        composable<AppDestination.Scripts> { ScriptsScreen() }
        composable<AppDestination.SettingsWbw> { SettingsWbwScreen() }
        composable<AppDestination.RecitationDownload> { RecitationDownloadScreen() }
        composable<AppDestination.ChapterInfo> { entry ->
            ChapterInfoScreen(
                initialChapterNo = entry.toRoute<AppDestination.ChapterInfo>().chapterNo,
                initialLanguage = null,
            )
        }

        composable<AppDestination.EditsManagement> { EditsManagementScreen() }
        composable<AppDestination.LanguageSelection> { LanguageSelectionScreen() }
        composable<AppDestination.TranslationSelection> { TranslationSelectionScreen() }

        composable<AppDestination.Search> {
            SearchScreen(
                navController = navController,
                // Voice search is an Android speech-recogniser hand-off with no shared seam yet.
                supportsVoiceSearch = false,
                voiceSearchFlow = remember { MutableSharedFlow() },
                onVoiceSearchClick = {},
            )
        }
        // Same screen as above, different route identity — see AppDestination.SettingsDetail.
        composable<AppDestination.SearchDetail> {
            SearchScreen(
                navController = navController,
                supportsVoiceSearch = false,
                voiceSearchFlow = remember { MutableSharedFlow() },
                onVoiceSearchClick = {},
            )
        }

        composable<AppDestination.HadithIndex> {
            HadithIndexScreen(
                onNavigateToItems = { volume, book, chapter, sub, title ->
                    navController.navigate(
                        AppDestination.HadithItems(title, volume, book, chapter, sub),
                    )
                },
            )
        }

        composable<AppDestination.HadithItems> { entry ->
            val route = entry.toRoute<AppDestination.HadithItems>()

            HadithItemsScreen(
                title = route.title,
                volumeSlug = route.volumeSlug,
                bookSlug = route.bookSlug,
                chapterSlug = route.chapterSlug,
                subChapterSlug = route.subChapterSlug,
                onBack = { navController.popBackStack() },
                onNavigate = { volume, book, chapter, sub, title ->
                    navController.navigate(
                        AppDestination.HadithItems(title, volume, book, chapter, sub),
                    )
                },
            )
        }
    }
    }
}

/**
 * Route DTO → the sealed launch params the reader consumes. Falls back to chapter 1 when a route
 * carries no id, which only happens if one is constructed by hand.
 */
private fun AppDestination.Reader.toLaunchParams(): ReaderLaunchParams {
    val initialVerse = if (initialChapterNo != null && initialVerseNo != null) {
        ChapterVersePair(initialChapterNo, initialVerseNo)
    } else {
        null
    }

    val data = when {
        juzNo != null -> ReaderIntentData.FullJuz(juzNo, initialVerse)
        hizbNo != null -> ReaderIntentData.FullHizb(hizbNo, initialVerse)
        // Null script/variant means "whatever the reader is configured for".
        pageNo != null -> ReaderIntentData.MushafPage(
            mushafCode = null,
            mushafVariant = null,
            pageNo = pageNo,
            initialVerse = initialVerse,
        )
        else -> ReaderIntentData.FullChapter(chapterNo ?: 1, initialVerse)
    }

    return ReaderLaunchParams(data)
}

/** The reverse hop, for screens that navigate with full launch params (the reader index). */
private fun ReaderLaunchParams.toReaderRoute(): AppDestination.Reader = when (val d = data) {
    is ReaderIntentData.FullChapter -> AppDestination.Reader(chapterNo = d.chapterNo)
    is ReaderIntentData.FullJuz -> AppDestination.Reader(juzNo = d.juzNo)
    is ReaderIntentData.FullHizb -> AppDestination.Reader(hizbNo = d.hizbNo)
    is ReaderIntentData.MushafPage -> AppDestination.Reader(pageNo = d.pageNo)
}

/**
 * Points the global [ReaderUiHooks] navigation sinks at [navController], for hosts whose whole UI is
 * [AppNavHost] (iOS today).
 *
 * Opt-in rather than done inside [AppNavHost]: Android registers these same sinks in
 * `QuranApp.onCreate()` to launch Activities, and must not have them overwritten by a nested host.
 *
 * Left unset deliberately: `openChapterInfo` has no shared destination yet (the WebView-backed
 * chapter-info screen), so it keeps no-oping until that lands.
 */
@Composable
fun BindReaderNavigationHooks(navController: NavHostController) {
    LaunchedEffect(navController) {
        ReaderUiHooks.openChapter = { chapterNo ->
            navController.navigate(AppDestination.Reader(chapterNo = chapterNo))
        }
        ReaderUiHooks.openJuz = { juzNo ->
            navController.navigate(AppDestination.Reader(juzNo = juzNo))
        }
        ReaderUiHooks.openHizb = { hizbNo ->
            navController.navigate(AppDestination.Reader(hizbNo = hizbNo))
        }
        ReaderUiHooks.openVerse = { chapterNo, verseNo ->
            navController.navigate(
                AppDestination.Reader(
                    chapterNo = chapterNo,
                    initialChapterNo = chapterNo,
                    initialVerseNo = verseNo,
                ),
            )
        }
        ReaderUiHooks.openVerseRange = { chapterNo, fromVerse, _ ->
            navController.navigate(
                AppDestination.Reader(
                    chapterNo = chapterNo,
                    initialChapterNo = chapterNo,
                    initialVerseNo = fromVerse,
                ),
            )
        }
        // The translation-pinning variant degrades to a plain range open: pinning rides on Android
        // Intent extras that have no route equivalent yet, so the reader uses the saved selection.
        ReaderUiHooks.openVerseRangeWithTranslations = { chapterNo, fromVerse, _, _ ->
            navController.navigate(
                AppDestination.Reader(
                    chapterNo = chapterNo,
                    initialChapterNo = chapterNo,
                    initialVerseNo = fromVerse,
                ),
            )
        }
        // Both push over the caller rather than switching tabs, so they use the detail routes —
        // pushing the tab roots here poisons the calling tab's saved back stack.
        ReaderUiHooks.openSearch = {
            navController.navigate(AppDestination.SearchDetail)
        }
        ReaderUiHooks.openSettingsRoute = { route ->
            navController.navigate(AppDestination.SettingsDetail(startRoute = route))
        }
        ReaderUiHooks.openChapterInfo = { chapterNo ->
            navController.navigate(AppDestination.ChapterInfo(chapterNo))
        }
        ReaderUiHooks.openReaderFromHistory = { history ->
            history.toReaderRoute()?.let(navController::navigate)
        }
    }
}
