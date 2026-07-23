package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.material3.ColorScheme

/**
 * The OS-level dark/light setting, read outside composition (`isSystemInDarkTheme()` is the
 * composable equivalent). Needed by non-composable callers such as `ChapterInfoViewModel`, which
 * renders an HTML document and has to pick its palette up front.
 */
expect fun isSystemInDarkMode(): Boolean

/**
 * The platform's dynamic (wallpaper-derived) color scheme, or null where the platform has none.
 * Android 12+ Material You supplies one; iOS has no equivalent, so callers fall back to the app's
 * own palette.
 */
expect fun platformDynamicColorScheme(dark: Boolean): ColorScheme?
