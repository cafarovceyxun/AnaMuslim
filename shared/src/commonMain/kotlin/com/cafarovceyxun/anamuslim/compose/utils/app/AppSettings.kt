package com.cafarovceyxun.anamuslim.compose.utils.app

/**
 * Opens this app's entry in the system settings, where the user can grant a permission they
 * previously denied. Both platforms have exactly this concept, so it needs no provider indirection.
 *
 * Best-effort: failures are logged rather than surfaced, since there is nothing useful the UI can
 * do when the OS refuses to open its own settings screen.
 */
expect fun openAppSettings()
