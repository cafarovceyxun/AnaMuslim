package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.material3.ColorScheme
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

actual fun isSystemInDarkMode(): Boolean =
    UIScreen.mainScreen.traitCollection.userInterfaceStyle ==
        UIUserInterfaceStyle.UIUserInterfaceStyleDark

/** iOS has no wallpaper-derived palette; callers fall back to the app's own. */
actual fun platformDynamicColorScheme(dark: Boolean): ColorScheme? = null
