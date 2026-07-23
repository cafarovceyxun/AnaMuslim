package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// iOS has no Material You dynamic color; theming always uses the static palette.
@Composable
internal actual fun rememberDynamicColorScheme(isDark: Boolean, enabled: Boolean): ColorScheme? = null

// iOS Compose reacts to ThemeUtils.observeDarkTheme(); no AppCompatDelegate equivalent to apply.
internal actual fun applyThemeModeToPlatform(themeMode: String) {}

actual fun isDynamicColorSupported(): Boolean = false
