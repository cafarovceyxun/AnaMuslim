package com.cafarovceyxun.anamuslim.compose.utils

import com.cafarovceyxun.anamuslim.R

/**
 * App-module theme label lookup. Returns an app `R.string` id (consumed by the `Int`-based settings
 * item wrappers), so it stays here rather than in the shared module's [ThemeUtils]/`AndroidThemeUtils`.
 */
object AndroidThemeLabels {
    fun resolveThemeModeLabel(themeMode: String): Int {
        return when (themeMode) {
            ThemeUtils.THEME_MODE_LIGHT -> R.string.strLabelThemeLight
            ThemeUtils.THEME_MODE_DARK -> R.string.strLabelThemeDark
            else -> R.string.strLabelSystemDefault
        }
    }
}
