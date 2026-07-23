package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cafarovceyxun.anamuslim.compose.theme.colors.BaseColors
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strLabelSystemDefault
import com.cafarovceyxun.anamuslim.resources.strLabelThemeDark
import com.cafarovceyxun.anamuslim.resources.strLabelThemeLight
import org.jetbrains.compose.resources.StringResource
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeBlueColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeDefaultColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeMonoColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemePurpleColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeRedColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeVioletColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeYellowColors
import com.cafarovceyxun.anamuslim.compose.utils.preferences.DataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Platform-neutral theme state (mode / color / dynamic-color preferences + static palette
 * selection). Android-only pieces (Material You dynamic color, `AppCompatDelegate` night mode,
 * `Context`-based widget color schemes) live in `AndroidThemeUtils`; the app-resource label lookup
 * (`resolveThemeModeLabel`) stays in the app module.
 */
object ThemeUtils {
    const val THEME_MODE_DEFAULT = "app.theme.default"
    const val THEME_MODE_LIGHT = "app.theme.light"
    const val THEME_MODE_DARK = "app.theme.dark"

    internal val KEY_THEME_MODE = stringPreferencesKey("v2.theme_mode")
    internal val KEY_THEME_COLOR = stringPreferencesKey("v2.theme_color")
    internal val KEY_THEME_DYNAMIC_COLOR = booleanPreferencesKey("v2.theme_dynamic_color")

    const val THEME_COLOR_DEFAULT = "default"
    const val THEME_COLOR_BLUE = "blue"
    const val THEME_COLOR_RED = "red"
    const val THEME_COLOR_PURPLE = "purple"
    const val THEME_COLOR_MONO = "mono"
    const val THEME_COLOR_VIOLET = "violet"
    const val THEME_COLOR_YELLOW = "yellow"

    const val DEFAULT_DYNAMIC_COLOR = false

    @Composable
    fun observeDarkTheme(): Boolean {
        return when (observeThemeMode()) {
            THEME_MODE_LIGHT -> false
            THEME_MODE_DARK -> true
            else -> isSystemInDarkTheme()
        }
    }

    /**
     * Non-composable counterpart of [observeDarkTheme], for callers outside composition (widgets,
     * the chapter-info HTML renderer). Same resolution: explicit theme mode, else the OS setting.
     */
    fun isDarkThemeNow(): Boolean = when (getThemeMode()) {
        THEME_MODE_LIGHT -> false
        THEME_MODE_DARK -> true
        else -> isSystemInDarkMode()
    }

    /**
     * Non-composable color scheme: the platform's dynamic palette when the user enabled it and the
     * platform has one, else the app's static palette.
     */
    fun colorSchemeNow(isDark: Boolean? = null): ColorScheme {
        val dark = isDark ?: isDarkThemeNow()
        if (getIsDynamicColor()) {
            platformDynamicColorScheme(dark)?.let { return it }
        }
        return paletteColorScheme(getThemeColor(), dark)
    }

    @Composable
    fun observeThemeMode(): String {
        return DataStoreManager.observe(KEY_THEME_MODE, THEME_MODE_DEFAULT)
    }

    fun getThemeMode(): String {
        return DataStoreManager.read(KEY_THEME_MODE, THEME_MODE_DEFAULT)
    }

    suspend fun setThemeMode(themeMode: String) {
        DataStoreManager.write(KEY_THEME_MODE, themeMode)
    }

    @Composable
    fun observeThemeColor(): String {
        return DataStoreManager.observe(KEY_THEME_COLOR, THEME_COLOR_DEFAULT)
    }

    fun getThemeColor(): String {
        return DataStoreManager.read(KEY_THEME_COLOR, THEME_COLOR_DEFAULT)
    }

    suspend fun setThemeColor(themeColor: String) {
        DataStoreManager.write(KEY_THEME_COLOR, themeColor)
    }

    @Composable
    fun observeIsDynamicColor(): Boolean {
        return DataStoreManager.observe(KEY_THEME_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR)
    }

    fun getIsDynamicColor(): Boolean {
        return DataStoreManager.read(KEY_THEME_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR)
    }

    suspend fun setDynamicColor(isDynamicColor: Boolean) {
        DataStoreManager.write(KEY_THEME_DYNAMIC_COLOR, isDynamicColor)
    }

    @Composable
    fun observeColorScheme(
        isDarkTheme: Boolean = observeDarkTheme()
    ): ColorScheme {
        val themeColor = observeThemeColor()
        val isDynamicColor = observeIsDynamicColor()
        val dynamic = rememberDynamicColorScheme(isDarkTheme, isDynamicColor)
        return dynamic ?: paletteColorScheme(themeColor, isDarkTheme)
    }

    /**
     * Static (non-dynamic) palette selection, shared by Compose theming and the Android home-screen
     * widgets. Material You dynamic color is layered on top by the platform (see
     * [rememberDynamicColorScheme] / `AndroidThemeUtils.colorSchemeFromPreferences`).
     */
    fun paletteColorScheme(themeColor: String, isDarkTheme: Boolean): ColorScheme {
        val preferredColor: BaseColors = when (themeColor) {
            THEME_COLOR_BLUE -> ThemeBlueColors()
            THEME_COLOR_RED -> ThemeRedColors()
            THEME_COLOR_PURPLE -> ThemePurpleColors()
            THEME_COLOR_MONO -> ThemeMonoColors()
            THEME_COLOR_VIOLET -> ThemeVioletColors()
            THEME_COLOR_YELLOW -> ThemeYellowColors()
            else -> ThemeDefaultColors()
        }

        return if (isDarkTheme) preferredColor.darkColors() else preferredColor.lightColors()
    }

    fun widgetAppearancePreferencesFlow(): Flow<Triple<String, String, Boolean>> {
        return combine(
            DataStoreManager.flow(KEY_THEME_MODE, THEME_MODE_DEFAULT),
            DataStoreManager.flow(KEY_THEME_COLOR, THEME_COLOR_DEFAULT),
            DataStoreManager.flow(KEY_THEME_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR),
        ) { mode, color, dynamicColor ->
            Triple(mode, color, dynamicColor)
        }.distinctUntilChanged()
    }
}

/**
 * Material You dynamic color scheme for the current platform, or `null` when unavailable/disabled.
 * Android resolves it from the device wallpaper (API 31+); iOS has no equivalent and returns `null`.
 */
@Composable
internal expect fun rememberDynamicColorScheme(isDark: Boolean, enabled: Boolean): ColorScheme?

/**
 * Applies the selected [themeMode] to the running platform UI after it has been persisted via
 * [ThemeUtils.setThemeMode]. Android drives `AppCompatDelegate` night mode; iOS is a no-op because
 * Compose reacts to [ThemeUtils.observeDarkTheme] directly.
 */
internal expect fun applyThemeModeToPlatform(themeMode: String)

/**
 * Whether the platform can derive a Material You palette (Android 12+); `false` on iOS. Settings UI
 * uses it to decide whether to offer the dynamic-color switch at all.
 */
expect fun isDynamicColorSupported(): Boolean

/** Label for a [ThemeUtils] theme-mode constant. Platform-neutral counterpart of `AndroidThemeLabels`. */
fun themeModeLabel(themeMode: String): StringResource = when (themeMode) {
    ThemeUtils.THEME_MODE_LIGHT -> Res.string.strLabelThemeLight
    ThemeUtils.THEME_MODE_DARK -> Res.string.strLabelThemeDark
    else -> Res.string.strLabelSystemDefault
}
