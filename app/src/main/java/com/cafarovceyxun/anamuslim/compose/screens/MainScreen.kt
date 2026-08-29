package com.cafarovceyxun.anamuslim.compose.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.compose.components.AppReviewPromptHost
import com.cafarovceyxun.anamuslim.compose.components.MainBottomNavigationBar
import com.cafarovceyxun.anamuslim.compose.components.rememberMainNavItems
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.readerFloatingControlsInset
import androidx.compose.runtime.CompositionLocalProvider
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberHomeActions
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberIndexMenuActions
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberNotificationPermission
import com.cafarovceyxun.anamuslim.compose.components.player.LocalPlayerActions
import com.cafarovceyxun.anamuslim.compose.components.player.RecitationPlayerSheet
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberPlayerActions
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberReaderActions
import com.cafarovceyxun.anamuslim.compose.components.reader.LocalReaderActions
import com.cafarovceyxun.anamuslim.compose.screens.hadith.LocalHadithActions
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppViewModelStoreOwner
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberHadithActions
import com.cafarovceyxun.anamuslim.compose.components.player.rememberMiniPlayerVisibilityState
import com.cafarovceyxun.anamuslim.compose.components.player.MiniPlayerVisibility
import androidx.navigation.NavBackStackEntry
import com.cafarovceyxun.anamuslim.compose.navigation.MainRoutes
import com.cafarovceyxun.anamuslim.compose.navigation.MainTab
import com.cafarovceyxun.anamuslim.compose.navigation.mainTabEnter
import com.cafarovceyxun.anamuslim.compose.navigation.mainTabExit
import com.cafarovceyxun.anamuslim.compose.navigation.TabReselectState
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithIndexScreen
import com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithItemsScreen
import com.cafarovceyxun.anamuslim.compose.screens.reader.ReaderIndexScreen
import com.cafarovceyxun.anamuslim.compose.screens.reader.ReaderChromeState
import com.cafarovceyxun.anamuslim.compose.screens.reader.ReaderScreen
import com.cafarovceyxun.anamuslim.compose.screens.search.SearchScreen
import com.cafarovceyxun.anamuslim.compose.screens.settings.SettingsScreen
import com.cafarovceyxun.anamuslim.viewModels.ReaderIndexViewModel
import com.cafarovceyxun.anamuslim.utils.reader.ReaderLaunchParams
import com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData
import com.cafarovceyxun.anamuslim.utils.univ.EditEvent
import com.cafarovceyxun.anamuslim.utils.univ.EventBus
import com.cafarovceyxun.anamuslim.utils.univ.SortEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    externalReaderParamsFlow: MutableStateFlow<ReaderLaunchParams?> = remember { MutableStateFlow(null) }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as androidx.activity.ComponentActivity
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainRoutes.HOME
    val scope = rememberCoroutineScope()
    
    // Notification permission (null below Android 13, where none is needed).
    val notificationPermission = rememberNotificationPermission()
    LaunchedEffect(notificationPermission) {
        if (notificationPermission != null && !notificationPermission.isGranted) {
            notificationPermission.request()
        }
    }

    val quranViewModel = viewModel<ReaderIndexViewModel>()

    // Internal state for reader params to pass to ReaderScreen.
    // `rememberSaveable`, not `remember`: the reader's rotation button restarts the Activity (the
    // manifest handles no config changes), and a plain `remember` dropped these — the READER route
    // then fell back to chapter 1, so rotating out of Al-Baqara landed the reader on Al-Fatiha.
    var readerParams by rememberSaveable { mutableStateOf<ReaderLaunchParams?>(null) }

    // The reader hides its chrome in fullscreen (and behind the tajweed legend) and publishes that
    // here — its own HIDDEN visibility only zeroes the padding it reserves, not this host's player.
    val hideForReaderChrome by ReaderChromeState.hidePlayer.collectAsState()
    val verseSync by ReaderChromeState.verseSync.collectAsState()

    val playerVisibilityState = rememberMiniPlayerVisibilityState(
        if (hideForReaderChrome) MiniPlayerVisibility.HIDDEN else MiniPlayerVisibility.ALWAYS_SHOWN
    )
    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(externalReaderParamsFlow) {
        // A reader-open intent (widget/notification) is consumed by `handleIntent` *before*
        // `setContent`, so the flow can already hold params when this effect first runs — before the
        // NavHost below has registered its graph. Navigating then crashes with "graph has not been
        // set", so wait for the first destination (the start route) to be in place first.
        navController.currentBackStackEntryFlow.first()
        externalReaderParamsFlow.collect { params ->
            if (params != null) {
                readerParams = params
                navController.navigate(MainRoutes.READER) {
                    // Avoid multiple copies of reader in backstack if needed
                    launchSingleTop = true
                }
                externalReaderParamsFlow.value = null // consume
            }
        }
    }

    // UI Visibility Logic
    val isHome = currentRoute == MainRoutes.HOME
    val isQuranIndex = currentRoute == MainRoutes.QURAN
    val isHadithIndex = currentRoute == MainRoutes.HADITH
    val isSearch = currentRoute == MainRoutes.SEARCH
    val isSettings = currentRoute == MainRoutes.SETTINGS
    val isReader = currentRoute == MainRoutes.READER

    val showBottomBar = isHome || isQuranIndex || isHadithIndex || isSearch || isSettings
    val showPlayer = (isHome || isQuranIndex || isReader) && !currentRoute.contains("hadith")


    val playerVisible = playerVisibilityState.isVisible

    CompositionLocalProvider(
        // `activity` (not the nav back-stack entry) so the hadith index and items screens keep
        // sharing one HadithViewModel, as `viewModel<HadithViewModel>(activity)` did before.
        LocalAppViewModelStoreOwner provides activity,
        LocalHadithActions provides rememberHadithActions(),
    ) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {

            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = MainRoutes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                // Tab -> tab slides in the direction the bar moved (tap or swipe); pushes keep the
                // zoom-fade, which is what tells a drill-down apart from a sideways move.
                enterTransition = {
                    mainTabEnter(initialState.mainTabIndex(), targetState.mainTabIndex())
                        ?: (fadeIn(animationSpec = tween(300, easing = EaseOutExpo)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(300, easing = EaseOutExpo)))
                },
                exitTransition = {
                    mainTabExit(initialState.mainTabIndex(), targetState.mainTabIndex())
                        ?: (fadeOut(animationSpec = tween(250, easing = LinearEasing)) +
                            scaleOut(targetScale = 0.94f, animationSpec = tween(250, easing = LinearEasing)))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300, easing = EaseOutExpo)) + 
                    scaleIn(initialScale = 1.06f, animationSpec = tween(300, easing = EaseOutExpo))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(250, easing = LinearEasing)) + 
                    scaleOut(targetScale = 1.06f, animationSpec = tween(250, easing = LinearEasing))
                }
            ) {
                composable(MainRoutes.HOME) {
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        homeActions = rememberHomeActions(navController),
                        indexMenuActions = rememberIndexMenuActions(),
                    )
                }
                composable(MainRoutes.QURAN) {
                    ReaderIndexScreen(viewModel = quranViewModel) { params ->
                        readerParams = params
                        navController.navigate(MainRoutes.READER) {
                            launchSingleTop = true
                        }
                    }
                }
                composable(MainRoutes.HADITH) {
                    HadithIndexScreen(
                        onNavigateToItems = { volume, book, chapter, sub, title ->
                            navController.navigate(
                                MainRoutes.HADITH_ITEMS
                                    .replace("{volumeSlug}", volume ?: "null")
                                    .replace("{bookSlug}", book ?: "null")
                                    .replace("{chapterSlug}", chapter ?: "null")
                                    .replace("{subChapterSlug}", sub ?: "null")
                                    .replace("{title}", title)
                            )
                        }
                    )
                }
                composable(
                    route = MainRoutes.HADITH_ITEMS,
                    arguments = listOf(
                        navArgument("volumeSlug") { type = NavType.StringType; nullable = true },
                        navArgument("bookSlug") { type = NavType.StringType; nullable = true },
                        navArgument("chapterSlug") { type = NavType.StringType; nullable = true },
                        navArgument("subChapterSlug") { type = NavType.StringType; nullable = true },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val volumeSlug = backStackEntry.arguments?.getString("volumeSlug")?.takeIf { it != "null" }
                    val bookSlug = backStackEntry.arguments?.getString("bookSlug")?.takeIf { it != "null" }
                    val chapterSlug = backStackEntry.arguments?.getString("chapterSlug")?.takeIf { it != "null" }
                    val subChapterSlug = backStackEntry.arguments?.getString("subChapterSlug")?.takeIf { it != "null" }
                    val title = backStackEntry.arguments?.getString("title") ?: ""
                    
                    HadithItemsScreen(
                        title = title,
                        volumeSlug = volumeSlug,
                        bookSlug = bookSlug,
                        chapterSlug = chapterSlug,
                        subChapterSlug = subChapterSlug,
                        onBack = { navController.popBackStack() },
                        onNavigate = { v, b, c, s, newTitle ->
                            navController.navigate(
                                MainRoutes.HADITH_ITEMS
                                    .replace("{volumeSlug}", v ?: "null")
                                    .replace("{bookSlug}", b ?: "null")
                                    .replace("{chapterSlug}", c ?: "null")
                                    .replace("{subChapterSlug}", s ?: "null")
                                    .replace("{title}", newTitle)
                            ) {
                                popUpTo(MainRoutes.HADITH_ITEMS) { inclusive = true }
                            }
                        }
                    )
                }
                composable(MainRoutes.SEARCH) {
                    SearchScreen(
                        onOpenHadith = { volume, book, chapter, sub, title ->
                            navController.navigate(
                                MainRoutes.HADITH_ITEMS
                                    .replace("{volumeSlug}", volume ?: "null")
                                    .replace("{bookSlug}", book ?: "null")
                                    .replace("{chapterSlug}", chapter ?: "null")
                                    .replace("{subChapterSlug}", sub ?: "null")
                                    .replace("{title}", Uri.encode(title))
                            )
                        },
                        supportsVoiceSearch = false,
                        voiceSearchFlow = MutableSharedFlow(),
                        onVoiceSearchClick = {}
                    )
                }
                composable(MainRoutes.SETTINGS) {
                    SettingsScreen(intent = null, isNewIntent = false)
                }
                composable(MainRoutes.READER) {
                    val params = readerParams ?: ReaderLaunchParams(ReaderIntentData.FullChapter(1))
                    CompositionLocalProvider(
                        LocalReaderActions provides rememberReaderActions(),
                        LocalPlayerActions provides rememberPlayerActions(),
                    ) {
                        ReaderScreen(params)
                    }
                }
            }
        }

        if (showBottomBar) {
            Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {
                // Route matching stays here: the shared bar is route-agnostic, and these are the
                // string `MainRoutes` this host navigates by.
                val tabRoutes = MainRoutes.BOTTOM_NAV_ROUTES
                val selectedIndex = tabRoutes.indexOfFirst {
                    if (it == MainRoutes.HOME) currentRoute == MainRoutes.HOME
                    else currentRoute.startsWith(it)
                }.coerceAtLeast(0)

                MainBottomNavigationBar(
                    items = rememberMainNavItems(),
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        val route = tabRoutes[index]
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            TabReselectState.reselect(MainTab.entries[index])
                        }
                    },
                )
            }
        }

        if (showPlayer) {
            val bottomNavHeight = if (showBottomBar) mainBottomNavigationOuterHeight() else 0.dp
            // The reader's floating tajweed/fullscreen buttons live in this strip, under the player.
            val readerExtraPadding = if (isReader) readerFloatingControlsInset() else 0.dp
            
            CompositionLocalProvider(LocalPlayerActions provides rememberPlayerActions()) {
                RecitationPlayerSheet(
                    collapsedBottomInset = bottomNavHeight + readerExtraPadding,
                    playerVisibilityState = playerVisibilityState,
                    isExpanded = isPlayerExpanded,
                    onExpandedChange = { isPlayerExpanded = it },
                    // Published by the reader while it is on screen, rather than read off a
                    // `ReaderViewModel` resolved here: this host outlives the reader destination,
                    // so it has no reader session of its own to ask.
                    isSyncing = verseSync?.isEnabled == true,
                    onSyncRequest = verseSync?.onToggle,
                    // Home and the Quran index get a quiet button to bring a swiped-away player
                    // back; the reader has its own playback chrome, so it opts out.
                    reopenAffordance = !isReader,
                )
            }
        }

        // Hosted here rather than on a screen so the launch is counted once per app start; it
        // decides for itself whether today is the day to ask.
        AppReviewPromptHost()
    }
    }
}

/**
 * Bar-order index of the tab this entry is, or `-1` for the reader, hadith items and the rest.
 *
 * The string-route counterpart of `mainTabIndexOf` (typed routes, shared host) — same order, because
 * both feed the same slide direction.
 */
private fun NavBackStackEntry.mainTabIndex(): Int =
    MainRoutes.BOTTOM_NAV_ROUTES.indexOf(destination.route)
