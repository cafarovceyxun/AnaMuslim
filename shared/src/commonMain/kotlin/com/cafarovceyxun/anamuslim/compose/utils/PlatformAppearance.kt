package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic side effect to handle system-level appearance such as status bar
 * and navigation bar colors/icons based on the theme.
 */
@Composable
expect fun SystemAppearance(isDark: Boolean)
