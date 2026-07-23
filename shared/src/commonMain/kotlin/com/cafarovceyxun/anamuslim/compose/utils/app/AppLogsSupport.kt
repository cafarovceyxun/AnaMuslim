package com.cafarovceyxun.anamuslim.compose.utils.app

/**
 * Whether the platform hosts the `settings.app_logs` screen.
 *
 * `AppLogsScreen` reads crash-log files through the Android-only `Log` helper, so it is registered
 * from `:app`'s `SettingsScreen` as an extra route rather than from the shared `SettingsNavHost`.
 * On iOS that route does not exist, and navigating to a route the graph never declared throws — so
 * the admin entry hides instead of crashing.
 *
 * A plain constant for the same reason [supportsVolumeKeyNavigation] is one: both platforms have a
 * definite answer at build time.
 */
expect val supportsAppLogs: Boolean
