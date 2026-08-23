package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.cafarovceyxun.anamuslim.compose.utils.LocalSystemBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cafarovceyxun.anamuslim.compose.navigation.MainTab
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.compose.navigation.TabReselectState

private val enterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(durationMillis = 100),
)
private val exitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(durationMillis = 100),
)
private val popEnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(durationMillis = 100),
)
private val popExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(durationMillis = 100),
)

fun NavGraphBuilder.route(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition },
        content = content
    )
}

/** Provided by whichever host renders the settings graph; the settings screens navigate through it. */
val LocalSettingsNavController = compositionLocalOf<NavHostController> {
    error("NavHostController is not provided")
}

/**
 * The settings graph over the `commonMain` settings screens.
 *
 * Android's `SettingsScreen` wraps this to add its `Intent` start-destination handling and the
 * `AppLogs` route (that screen reads crash-log files through the Android-only `Log`), passing them
 * as [extraRoutes] so the route table itself is not duplicated per platform.
 */
@Composable
fun SettingsNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = SettingRoutes.MAIN,
    extraRoutes: NavGraphBuilder.() -> Unit = {},
) {
    // Chain back: pop the settings graph, and when it is already at its root fall through to the
    // host that opened settings (iOS back-button support; Android ignores LocalSystemBack).
    val parentBack = LocalSystemBack.current
    val settingsBack: () -> Unit = remember(navController, parentBack) {
        { if (!navController.popBackStack()) parentBack?.invoke() }
    }

    // Re-tapping the Settings tab returns to the settings root in one move rather than unwinding a
    // page at a time. Only reachable on Android, where the outer route stays `settings` while a
    // sub-page is open so the bar keeps showing; on iOS sub-pages are their own destinations and
    // hide the bar, which makes this inert there rather than wrong.
    TabReselectState.OnTabReselect(MainTab.SETTINGS) {
        navController.popBackStack(startDestination, inclusive = false)
    }

    CompositionLocalProvider(
        LocalSettingsNavController provides navController,
        LocalSystemBack provides settingsBack,
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = startDestination,
            ) {
                route(SettingRoutes.MAIN) { SettingsMainScreen() }
                route(SettingRoutes.LANGUAGE) { LanguageSelectionScreen() }
                route(SettingRoutes.THEME) { SettingsThemeScreen() }
                route(SettingRoutes.TRANSLATIONS) { TranslationSelectionScreen() }
                route(SettingRoutes.SCRIPT) { ScriptsScreen() }
                route(SettingRoutes.WWB) { SettingsWbwScreen() }
                route(SettingRoutes.RECITATION_DOWNLOAD) { RecitationDownloadScreen() }
                route(SettingRoutes.EDITS_MANAGEMENT) { EditsManagementScreen() }
                route(SettingRoutes.REPORTS_MANAGEMENT) { ReportsManagementScreen() }
                route(SettingRoutes.APP_RELEASE_MANAGEMENT) { AppReleaseManagementScreen() }

                extraRoutes()
            }
        }
    }
}
