package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cafarovceyxun.anamuslim.compose.utils.LocalAppLocale
import com.cafarovceyxun.anamuslim.compose.utils.SystemAppearance
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.appLocaleFlow

@Composable
fun QuranAppTheme(
    content: @Composable () -> Unit
) {
    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val colorScheme = ThemeUtils.observeColorScheme(isDarkTheme)
    val appLocale by appLocaleFlow.collectAsState()

    SystemAppearance(isDarkTheme)

    CompositionLocalProvider(LocalAppLocale provides appLocale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getAppTypography(),
            content = content
        )
    }
}
