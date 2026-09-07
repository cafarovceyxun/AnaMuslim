package com.cafarovceyxun.anamuslim.compose.screens.settings

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.cafarovceyxun.anamuslim.compose.components.LocalIndexMenuActions
import com.cafarovceyxun.anamuslim.compose.navigation.SettingRoutes
import com.cafarovceyxun.anamuslim.compose.utils.app.rememberIndexMenuActions
import com.cafarovceyxun.anamuslim.utils.univ.Keys

/**
 * Android host for the settings graph. Only two things keep it in `:app`: the `Intent` that carries
 * the start destination, and the `AppLogs` route (that screen reads crash-log files through the
 * Android-only `Log`). Everything else lives in `SettingsNavHost` in `commonMain`.
 */
@Composable
fun SettingsScreen(intent: Intent?, isNewIntent: Boolean) {
    val navController = rememberNavController()

    // ⚠️ `isNewIntent` şərti QƏSDƏN yoxdur. `rememberNavController()` prosesin öldürülməsindən
    // sonra öz yığınını bərpa edir və bərpa olunan yığın `startDestination`-u **üstələyir** — yəni
    // qısayol/jest «X route-unu aç» desə də, ekran istifadəçinin sonuncu baxdığı səhifədə qalırdı
    // (idarəetmə paneli əvəzinə tərcümə idxalı açılırdı). Extra varsa hər halda ora keçirik;
    // `launchSingleTop` təzə açılışda dublikat yaratmır, çünki həmin route onsuz da yığının başıdır.
    LaunchedEffect(intent, isNewIntent) {
        val requestedDestination = intent?.getStringExtra(Keys.NAV_DESTINATION) ?: return@LaunchedEffect

        navController.navigate(requestedDestination) {
            launchSingleTop = true
            restoreState = true
        }
    }

    val startDestination = intent?.getStringExtra(Keys.NAV_DESTINATION)
        ?: SettingRoutes.MAIN

    // Köhnə üst-bar menyusunun sətirləri artıq ayarlardadır — Android-də onlar Activity açır,
    // ona görə seam məhz burada verilir (paylaşılan host bunu AppNavHost-da edir).
    CompositionLocalProvider(LocalIndexMenuActions provides rememberIndexMenuActions()) {
        SettingsNavHost(
            navController = navController,
            startDestination = startDestination,
            extraRoutes = {
                route(SettingRoutes.APP_LOGS) { AppLogsScreen() }
            },
        )
    }
}
