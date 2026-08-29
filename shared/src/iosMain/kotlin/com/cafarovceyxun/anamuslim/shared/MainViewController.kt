package com.cafarovceyxun.anamuslim.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.NavDestination
// The reified route matcher lives on NavDestination.Companion; without this import the member
// `hasRoute(route: String, arguments)` overload wins and the call does not compile.
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.AppReviewPromptHost
import com.cafarovceyxun.anamuslim.compose.components.MainBottomNavigationBar
import com.cafarovceyxun.anamuslim.compose.components.mainBottomNavigationOuterHeight
import com.cafarovceyxun.anamuslim.compose.components.readerFloatingControlsInset
import com.cafarovceyxun.anamuslim.compose.components.player.LocalPlayerActions
import com.cafarovceyxun.anamuslim.compose.components.player.MiniPlayerVisibility
import com.cafarovceyxun.anamuslim.compose.components.player.RecitationPlayerSheet
import com.cafarovceyxun.anamuslim.compose.components.player.rememberMiniPlayerVisibilityState
import com.cafarovceyxun.anamuslim.compose.components.rememberMainNavItems
import com.cafarovceyxun.anamuslim.compose.navigation.AppDestination
import com.cafarovceyxun.anamuslim.compose.navigation.AppNavHost
import com.cafarovceyxun.anamuslim.compose.navigation.BindReaderNavigationHooks
import com.cafarovceyxun.anamuslim.compose.navigation.MainTab
import com.cafarovceyxun.anamuslim.compose.navigation.TabReselectState
import com.cafarovceyxun.anamuslim.compose.navigation.rememberNavPlayerActions
import com.cafarovceyxun.anamuslim.compose.screens.GreetingSplash
import com.cafarovceyxun.anamuslim.compose.screens.onboarding.OnboardingGate
import com.cafarovceyxun.anamuslim.compose.screens.onboarding.OnboardingScreen
import com.cafarovceyxun.anamuslim.compose.screens.reader.ReaderChromeState
import com.cafarovceyxun.anamuslim.compose.theme.QuranAppTheme
import com.cafarovceyxun.anamuslim.compose.utils.BindMacBackGestures
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppViewModelStoreOwner
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.compose.utils.wrapForMacBack
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController

/** Mirrors `Assets.xcassets/LaunchBackground.colorset`, so the pre-bootstrap frame is invisible. */
private val LaunchBackground = Color(0xFF1D5333)

/**
 * iOS entry point: the shared Compose UI — [AppNavHost] over the `commonMain` screens, under the
 * shared bottom tab bar. This is the counterpart of Android's `MainScreen`, and stays here for the
 * same reason that one stays in `:app`: a composition root is where platform wiring belongs.
 *
 * [initSharedForIos] runs here for the same reason Android runs its wiring in `QuranApp.onCreate()`:
 * every repository, player and downloader seam is registered there, so nothing below may compose
 * before it — which this root gets by *awaiting* it rather than by blocking on it. (It used to be
 * reached through the migration-era `runSharedSmoke`, which also seeded test rows — a "Smoke cild"
 * volume among them — into the real databases on every launch.)
 */
fun MainViewController(): UIViewController = wrapForMacBack(composeRoot())

/**
 * The Compose controller itself. On Mac `wrapForMacBack` hosts it inside a controller that turns a
 * two-finger swipe and the ⌘ shortcuts into back navigation, because macOS delivers the trackpad as
 * scroll events and the system back gesture therefore never reaches Compose there. On iPhone and
 * iPad this *is* the returned controller — nothing is wrapped.
 */
private fun composeRoot(): UIViewController = ComposeUIViewController {
    // Awaited, not run inline. Bootstrap loads DataStore and extracts the bundled reader fonts, and
    // doing that inside composition parked the main thread on a background worker — the priority
    // inversion Xcode reports as "Thread running at User-interactive QoS waiting on a lower QoS
    // thread". Nothing below composes until it resolves, so no seam is still read too early.
    var bootstrapped by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching { initSharedForIos() }
            .onFailure { println("[ios-bootstrap] işə salma uğursuz: $it") }
        bootstrapped = true
    }

    if (!bootstrapped) {
        // Holds the launch screen's colour for the frame or two this takes, so the handover from
        // the launch storyboard to Compose has no flash. QuranAppTheme cannot be used yet: it reads
        // the stored theme, and the store is what bootstrap is still loading.
        Box(Modifier.fillMaxSize().background(LaunchBackground))
        return@ComposeUIViewController
    }

    // The iOS counterpart of Android's hosting Activity. Mandatory: without it
    // `appScopedViewModelStoreOwner()` falls back to the NavHost back-stack entry, and screens that
    // must share a view model (hadith index ↔ items) would silently get separate instances.
    val appViewModelStoreOwner = remember { AppViewModelStoreOwner() }

    val navController = rememberNavController()

    // Android registers these sinks in QuranApp.onCreate() to launch Activities; here they drive the
    // shared NavHost instead. Without them every in-app "open this verse/chapter/search" is a no-op.
    BindReaderNavigationHooks(navController)

    // Mac-only: publishes this host's back action to the swipe/⌘ handlers above Compose. A no-op on
    // iPhone and iPad, where nothing calls it.
    BindMacBackGestures(navController)

    // Compose Resources resolves strings against `Locale.current`, which it reads during
    // composition and caches per environment. Changing the language therefore only takes effect if
    // the tree is rebuilt — this key is the iOS stand-in for the activity recreation that
    // `AppCompatDelegate.setApplicationLocales` causes on Android. The nav controller is
    // deliberately remembered outside it, so the back stack survives a language change.
    val appLocale by appLocaleFlow.collectAsState()

    // Android reaches onboarding through a separate `ActivityOnboarding`, which iOS has no
    // counterpart for — that is why the first-run screens never appeared here at all. The flag is
    // read once rather than observed: `DataStoreManager.observe` emits its default first, which
    // would flash onboarding at every launch of an app that has already finished it. The read costs
    // nothing now — bootstrap has already loaded the store into memory.
    var showOnboarding by remember {
        mutableStateOf(OnboardingGate.shouldShow(AppPreferences.getOnboardingCompletedVersion()))
    }
    val scope = rememberCoroutineScope()

    key(appLocale) {
        CompositionLocalProvider(LocalAppViewModelStoreOwner provides appViewModelStoreOwner) {
            QuranAppTheme {
                if (showOnboarding) {
                    // Onboarding lives outside the nav graph, so nothing has provided the owner its
                    // `viewModel { … }` calls resolve against.
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides appViewModelStoreOwner
                    ) {
                        OnboardingScreen(
                            onComplete = {
                                OnboardingGate.markCompleted()
                                showOnboarding = false
                                scope.launch {
                                    AppPreferences.setOnboardingCompletedVersion(
                                        OnboardingGate.REQUIRED_VERSION,
                                    )
                                }
                            },
                            // No `onExit`: iOS has neither a back press to leave on nor an activity
                            // to finish, so the only way out is completing or skipping.
                        )
                    }

                    return@QuranAppTheme
                }

                Box(Modifier.fillMaxSize()) {
                    AppNavHost(
                        startDestination = AppDestination.Home,
                        navController = navController,
                        onOpenInReader = { chapterNo, fromVerse, _ ->
                            navController.navigate(
                                AppDestination.Reader(
                                    chapterNo = chapterNo,
                                    initialChapterNo = chapterNo,
                                    initialVerseNo = fromVerse,
                                ),
                            )
                        },
                    )

                    BottomTabBar(navController, Modifier.align(Alignment.BottomCenter))
                    RecitationMiniPlayerHost(navController)

                    // Hosted here rather than on a screen so the launch is counted once per app
                    // start; it decides for itself whether today is the day to ask.
                    AppReviewPromptHost()

                    // Overlays the app instead of gating it — the nav host above loads underneath
                    // while the greeting plays. It replays for nobody: `GreetingSplash` keeps a
                    // process-scoped flag, so the `key(appLocale)` rebuild above cannot restart it.
                    GreetingSplash()
                }
            }
        }
    }
}

/**
 * The recitation mini player, hosted above the nav graph so it survives moving between home, the
 * Quran index and the reader — exactly where Android's `MainScreen` puts it.
 *
 * It has to live at the composition root rather than inside `ReaderScreen`: the reader only
 * *reserves* the mini player's height in its padding and expects the host to draw it. Missing this
 * host is why iOS played recitation with no player on screen at all.
 */
@Composable
private fun RecitationMiniPlayerHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination ?: return

    // Same rule as Android: the player follows the Quran surfaces and stays out of hadith/search.
    val isReader = destination.hasRoute<AppDestination.Reader>()
    val showPlayer = isReader ||
            destination.hasRoute<AppDestination.Home>() ||
            destination.hasRoute<AppDestination.ReaderIndex>()

    if (!showPlayer) return

    val isTabDestination = tabDestinations.any { (matches, _) -> matches(destination) }
    val bottomNavHeight = if (isTabDestination) mainBottomNavigationOuterHeight() else 0.dp
    // The reader's own bottom controls sit under the player, inside this strip.
    val readerExtraPadding = if (isReader) readerFloatingControlsInset() else 0.dp

    var isPlayerExpanded by rememberSaveable { mutableStateOf(false) }

    // The reader goes chrome-free in fullscreen (and behind the tajweed legend); without this the
    // mini player keeps floating over the mushaf, since the reader's own HIDDEN visibility only
    // governs the padding it reserves, not this host.
    val hideForReaderChrome by ReaderChromeState.hidePlayer.collectAsState()

    // Verse-sync comes from the reader itself: it owns the `ReaderViewModel` this root cannot reach,
    // so it publishes the toggle while it is on screen. Null (no reader) hides the lock button.
    val verseSync by ReaderChromeState.verseSync.collectAsState()

    CompositionLocalProvider(LocalPlayerActions provides rememberNavPlayerActions(navController)) {
        RecitationPlayerSheet(
            collapsedBottomInset = bottomNavHeight + readerExtraPadding,
            playerVisibilityState = rememberMiniPlayerVisibilityState(
                if (hideForReaderChrome) MiniPlayerVisibility.HIDDEN
                else MiniPlayerVisibility.ALWAYS_SHOWN
            ),
            isExpanded = isPlayerExpanded,
            onExpandedChange = { isPlayerExpanded = it },
            isSyncing = verseSync?.isEnabled == true,
            onSyncRequest = verseSync?.onToggle,
            // Home and the Quran index get a quiet button to bring a swiped-away player back; the
            // reader has its own playback chrome, so it opts out.
            reopenAffordance = !isReader,
        )
    }
}

/**
 * The tab destinations, in bar order — indices line up with `rememberMainNavItems()`. Matching is a
 * predicate rather than a `KClass` because `NavDestination.hasRoute` is a reified extension.
 */
private val tabDestinations: List<Pair<(NavDestination) -> Boolean, () -> AppDestination>> = listOf(
    { d: NavDestination -> d.hasRoute<AppDestination.Home>() } to { AppDestination.Home },
    { d: NavDestination -> d.hasRoute<AppDestination.ReaderIndex>() } to { AppDestination.ReaderIndex },
    { d: NavDestination -> d.hasRoute<AppDestination.HadithIndex>() } to { AppDestination.HadithIndex },
    { d: NavDestination -> d.hasRoute<AppDestination.Search>() } to { AppDestination.Search },
    { d: NavDestination -> d.hasRoute<AppDestination.Settings>() } to { AppDestination.Settings() },
)

/**
 * Shown only while a tab destination is on top — pushed screens (reader, hadith items, a settings
 * sub-page) take the full height, matching Android, where `showBottomBar` is computed the same way.
 */
@Composable
private fun BottomTabBar(navController: NavHostController, modifier: Modifier) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination: NavDestination? = backStackEntry?.destination

    val current = destination ?: return
    val selectedIndex = tabDestinations.indexOfFirst { (matches, _) -> matches(current) }

    if (selectedIndex < 0) return

    Box(modifier) {
        MainBottomNavigationBar(
            items = rememberMainNavItems(),
            selectedIndex = selectedIndex,
            onSelect = { index ->
                if (index != selectedIndex) {
                    navController.navigate(tabDestinations[index].second()) {
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

/** Lives as long as the hosting view controller, mirroring an Android `ComponentActivity` scope. */
private class AppViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
