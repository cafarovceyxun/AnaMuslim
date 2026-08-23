package com.cafarovceyxun.anamuslim.compose.screens.settings

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.utils.univ.Keys

/**
 * Android host for the settings graph. Only two things keep it in `:app`: the `Intent` that carries
 * the start destination, and the `AppLogs` route (that screen reads crash-log files through the
 * Android-only `Log`). Everything else lives in `SettingsNavHost` in `commonMain`.
 */
@Composable
fun SettingsScreen(intent: Intent?, isNewIntent: Boolean) {
    val navController = rememberNavController()

    LaunchedEffect(intent, isNewIntent) {
        if (!isNewIntent) return@LaunchedEffect

        val startDestination = intent?.getStringExtra(Keys.NAV_DESTINATION)

        if (startDestination != null) {
            navController.navigate(startDestination) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val startDestination = intent?.getStringExtra(Keys.NAV_DESTINATION)
        ?: SettingRoutes.MAIN

    SettingsNavHost(
        navController = navController,
        startDestination = startDestination,
        extraRoutes = {
            route(SettingRoutes.APP_LOGS) { AppLogsScreen() }
        },
    )
}
