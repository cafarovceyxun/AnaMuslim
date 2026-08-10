package com.cafarovceyxun.anamuslim.compose.screens.settings

import com.cafarovceyxun.anamuslim.compose.theme.colors.BaseColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeBlueColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeDefaultColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeMonoColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemePurpleColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeRedColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeVioletColors
import com.cafarovceyxun.anamuslim.compose.theme.colors.ThemeYellowColors
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.theme_blue
import com.cafarovceyxun.anamuslim.resources.theme_default
import com.cafarovceyxun.anamuslim.resources.theme_mono
import com.cafarovceyxun.anamuslim.resources.theme_purple
import com.cafarovceyxun.anamuslim.resources.theme_red
import com.cafarovceyxun.anamuslim.resources.theme_violet
import com.cafarovceyxun.anamuslim.resources.theme_yellow
import org.jetbrains.compose.resources.StringResource

/** A selectable app-theme colour option. `title` is a Compose-MP [StringResource] (not an Android
 *  `R.string` Int), so this model and its consumers ([SettingsThemeItem]) live in commonMain. */
data class ThemeItem(
    val id: String,
    val title: StringResource,
    val color: BaseColors,
)

fun themeColorItems(): List<ThemeItem> = listOf(
    ThemeItem(ThemeUtils.THEME_COLOR_DEFAULT, Res.string.theme_default, ThemeDefaultColors()),
    ThemeItem(ThemeUtils.THEME_COLOR_BLUE, Res.string.theme_blue, ThemeBlueColors()),
    ThemeItem(ThemeUtils.THEME_COLOR_RED, Res.string.theme_red, ThemeRedColors()),
    ThemeItem(ThemeUtils.THEME_COLOR_PURPLE, Res.string.theme_purple, ThemePurpleColors()),
    ThemeItem(ThemeUtils.THEME_COLOR_VIOLET, Res.string.theme_violet, ThemeVioletColors()),
    ThemeItem(ThemeUtils.THEME_COLOR_YELLOW, Res.string.theme_yellow, ThemeYellowColors()),
    ThemeItem(ThemeUtils.THEME_COLOR_MONO, Res.string.theme_mono, ThemeMonoColors()),
)
