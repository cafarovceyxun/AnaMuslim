package com.cafarovceyxun.anamuslim.compose.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.cafarovceyxun.anamuslim.compose.screens.prayer.PrayerTimesScreen
import com.cafarovceyxun.anamuslim.compose.screens.ReadHistoryScreen
import com.cafarovceyxun.anamuslim.compose.screens.storageCleanup.StorageCleanupScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithIndexScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithItemsScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.LocalHadithActions
import com.cafarovceyxun.anamuslim.compose.screens.search.SearchScreen
import com.cafarovceyxun.anamuslim.compose.components.LocalIndexMenuActions
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
 * Reader hand-off stays a parameter rather than being resolved here: its implementation is still
 * platform-bound (the reader Activity), so the host stays free of platform seams until that screen
 * migrates too. Export/import used to be parameters for the same reason; it now reaches the file
 * picker through its own seam, because leaving them optional is precisely what made both buttons
 * inert on iOS — the host simply never passed them.
 */
@Composable
fun AppNavHost(
    startDestination: AppDestination = AppDestination.About,
    navController: NavHostController = rememberNavController(),
    onOpenInReader: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit = { _, _, _ -> },
) {
    // iOS reads this for its back-button action (Android uses the OnBackPressedDispatcher and
    // ignores it). Remembered so its identity is stable and the static local does not invalidate
    // every consumer on recomposition.
    val systemBack: () -> Unit = remember(navController) { { navController.popBackStack(); Unit } }

    // Ayarlar ekranı köhnə üst-bar menyusunun sətirlərini göstərir, ona görə bu seam bütün qrafın
    // üstündə verilir — yalnız ana səhifənin altında qalsaydı həmin sətirlər səssizcə heç nə edərdi.
    CompositionLocalProvider(
        LocalSystemBack provides systemBack,
        LocalIndexMenuActions provides rememberNavIndexMenuActions(navController),
    ) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Only tab -> tab moves are animated by direction; everything else keeps the library's fade,
        // so a push still reads as a push.
        enterTransition = {
            mainTabEnter(
                mainTabIndexOf(initialState.destination),
                mainTabIndexOf(targetState.destination),
            ) ?: fadeIn(animationSpec = tween(220))
        },
        exitTransition = {
            mainTabExit(
                mainTabIndexOf(initialState.destination),
                mainTabIndexOf(targetState.destination),
            ) ?: fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(180)) },
    ) {
        composable<AppDestination.About> { AboutScreen() }
        composable<AppDestination.Bookmarks> {
            BookmarksScreen(
                onOpenInReader = onOpenInReader,
                onOpenHadith = { title, volumeSlug, bookSlug, chapterSlug, subChapterSlug ->
                    navController.navigate(
                        AppDestination.HadithItems(
                            title = title,
                            volumeSlug = volumeSlug,
                            bookSlug = bookSlug,
                            chapterSlug = chapterSlug,
                            subChapterSlug = subChapterSlug,
                        ),
                    )
                },
            )
        }
        composable<AppDestination.ExportImport> {
            ExportImportScreen()
        }
        composable<AppDestination.SettingsTheme> { SettingsThemeScreen() }
        composable<AppDestination.Settings> { entry ->
            val startRoute = entry.toRoute<AppDestination.Settings>().startRoute
            SettingsNavHost(startDestination = startRoute ?: SettingRoutes.MAIN)
        }
        // Same screen as above, different route identity — see AppDestination.SettingsDetail.
        composable<AppDestination.SettingsDetail> { entry ->
            val startRoute = entry.toRoute<AppDestination.SettingsDetail>().startRoute
            SettingsNavHost(startDestination = startRoute ?: SettingRoutes.MAIN)
        }
        composable<AppDestination.ReadHistory> { ReadHistoryScreen() }
        composable<AppDestination.PrayerTimes> { PrayerTimesScreen() }
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
                onOpenHadith = { volume, book, chapter, sub, title ->
                    navController.navigate(
                        AppDestination.HadithItems(title, volume, book, chapter, sub),
                    )
                },
                // Voice search is an Android speech-recogniser hand-off with no shared seam yet.
                supportsVoiceSearch = false,
                voiceSearchFlow = remember { MutableSharedFlow() },
                onVoiceSearchClick = {},
            )
        }
        // Same screen as above, different route identity — see AppDestination.SettingsDetail.
        composable<AppDestination.SearchDetail> {
            SearchScreen(
                onOpenHadith = { volume, book, chapter, sub, title ->
                    navController.navigate(
                        AppDestination.HadithItems(title, volume, book, chapter, sub),
                    )
                },
                supportsVoiceSearch = false,
                voiceSearchFlow = remember { MutableSharedFlow() },
                onVoiceSearchClick = {},
            )
        }

        composable<AppDestination.HadithIndex> {
            // Mirrors Android's MainScreen/ActivityHadith, which provide this seam around both
            // hadith screens. Left unprovided it falls back to the no-op default, i.e. dead
            // buttons — including the empty state's, the only route to the hadith download.
            CompositionLocalProvider(
                LocalHadithActions provides rememberNavHadithActions(navController),
            ) {
                HadithIndexScreen(
                    onNavigateToItems = { volume, book, chapter, sub, title ->
                        navController.navigate(
                            AppDestination.HadithItems(title, volume, book, chapter, sub),
                        )
                    },
                )
            }
        }

        composable<AppDestination.HadithDetail> { entry ->
            val route = entry.toRoute<AppDestination.HadithDetail>()

            CompositionLocalProvider(
                LocalHadithActions provides rememberNavHadithActions(navController),
            ) {
                // No `onNavigateToItems`: pushed like this the screen drills through its own levels
                // in place, so back walks the hierarchy up and then leaves — the same shape
                // Android's ActivityHadith has.
                HadithIndexScreen(
                    initialHadithId = route.hadithId,
                    onExit = { navController.popBackStack() },
                )
            }
        }

        composable<AppDestination.HadithItems> { entry ->
            val route = entry.toRoute<AppDestination.HadithItems>()

            // The navigator sheet's settings button reads the same seam.
            CompositionLocalProvider(
                LocalHadithActions provides rememberNavHadithActions(navController),
            ) {
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
