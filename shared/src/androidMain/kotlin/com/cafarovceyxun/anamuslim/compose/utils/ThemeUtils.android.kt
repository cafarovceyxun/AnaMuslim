package com.cafarovceyxun.anamuslim.compose.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberDynamicColorScheme(isDark: Boolean, enabled: Boolean): ColorScheme? {
    if (!enabled || !AndroidThemeUtils.isDynamicColorSupported()) return null
    val context = LocalContext.current
    return if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

internal actual fun applyThemeModeToPlatform(themeMode: String) {
    AppCompatDelegate.setDefaultNightMode(AndroidThemeUtils.resolveThemeModeForDelegate(themeMode))
}

actual fun isDynamicColorSupported(): Boolean = AndroidThemeUtils.isDynamicColorSupported()

/**
 * Android-only theme helpers that depend on [Context], `AppCompatDelegate` or Material You dynamic
 * color. The platform-neutral state (mode/color/dynamic preferences + palette) lives in [ThemeUtils].
 */
object AndroidThemeUtils {

    // Dynamic color (Material You) is available on Android 12+.
    fun isDynamicColorSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Non-composable color scheme for widgets. Delegates to [ThemeUtils.colorSchemeNow] so the
     * dynamic-vs-static decision lives in one place; [context] is kept for the existing Java/widget
     * call sites but is no longer read (the shared actual uses `AndroidPlatformContext`).
     */
    fun colorSchemeFromPreferences(context: Context, isDark: Boolean? = null): ColorScheme =
        ThemeUtils.colorSchemeNow(isDark)

    /** Dark/light resolution aligned with [ThemeUtils.observeDarkTheme] (theme mode + system night). */
    fun isDarkTheme(context: Context): Boolean = ThemeUtils.isDarkThemeNow()

    fun resolveThemeModeForDelegate(themeMode: String? = null): Int {
        return when (themeMode ?: ThemeUtils.getThemeMode()) {
            ThemeUtils.THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeUtils.THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeUtils.THEME_MODE_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
