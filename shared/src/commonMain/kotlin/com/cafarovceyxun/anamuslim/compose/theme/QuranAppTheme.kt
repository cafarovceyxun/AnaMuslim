package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.SystemAppearance
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils

@Composable
fun QuranAppTheme(
    content: @Composable () -> Unit
) {
    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val colorScheme = ThemeUtils.observeColorScheme(isDarkTheme)
    val appLocale by appLocaleFlow.collectAsState()

    SystemAppearance(isDarkTheme)

    // Layout direction is derived from the app language rather than left to the platform. Android
    // would get there on its own (the per-app locale reaches the Configuration), but iOS would not:
    // Compose resolves the direction once from the UIKit view, while the language override lives in
    // `AppleLanguages` — so Arabic strings would render inside an LTR layout until the next launch.
    val layoutDirection = if (StringUtils.isRtlLanguage(appLocale.language)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalAppLocale provides appLocale,
        LocalLayoutDirection provides layoutDirection,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getAppTypography(),
            content = content
        )
    }
}
