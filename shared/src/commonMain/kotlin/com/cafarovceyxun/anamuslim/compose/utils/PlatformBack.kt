package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Platform "navigate back" trigger for shared UI (e.g. [AppBar]/`BackButton` default action).
 *
 * Android returns a call into the current `OnBackPressedDispatcher`, which also drives the hardware
 * back button and any registered `BackHandler`. iOS has no such dispatcher, so its actual reads
 * [LocalSystemBack] — a pop action the navigation hosts provide. Callers should treat `null` as "no
 * system back available" and fall back to an explicit `onClick` where one is provided.
 */
@Composable
expect fun rememberSystemBack(): (() -> Unit)?

/**
 * The pop action for the nearest navigation host, provided by `AppNavHost` and (chained) by
 * `SettingsNavHost`. Only iOS's [rememberSystemBack] reads it; Android drives back through the
 * activity's `OnBackPressedDispatcher` and ignores this. Null when no host is above the caller.
 */
val LocalSystemBack = staticCompositionLocalOf<(() -> Unit)?> { null }
