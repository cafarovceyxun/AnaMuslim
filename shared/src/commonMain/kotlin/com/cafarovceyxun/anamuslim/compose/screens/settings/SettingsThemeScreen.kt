package com.cafarovceyxun.anamuslim.compose.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dynamic_color
import com.cafarovceyxun.anamuslim.resources.dynamic_color_description
import com.cafarovceyxun.anamuslim.resources.strTitleTheme
import com.cafarovceyxun.anamuslim.resources.theme_colors
import com.cafarovceyxun.anamuslim.resources.theme_mode
import com.cafarovceyxun.anamuslim.compose.components.common.AppBar
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsItem
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsThemeItem
import com.cafarovceyxun.anamuslim.compose.components.settings.ThemeSelectorSheet
import com.cafarovceyxun.anamuslim.compose.extensions.fullWidthColumn
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.isDynamicColorSupported
import com.cafarovceyxun.anamuslim.compose.utils.themeModeLabel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsThemeScreen() {
    val themeItems = themeColorItems()

    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val themeMode = ThemeUtils.observeThemeMode()
    val themeColor = ThemeUtils.observeThemeColor()
    val isDynamicColor = ThemeUtils.observeIsDynamicColor()
    var showThemeBottomSheet by rememberSaveable { mutableStateOf(false) }

    val span = 2

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { AppBar(title = stringResource(Res.string.strTitleTheme)) },
    ) { paddingValues ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(span),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 16.dp,
                bottom = 150.dp
            ),
        ) {
            fullWidthColumn(span) {
                SettingsItem(
                    title = Res.string.theme_mode,
                    subtitleStr = stringResource(themeModeLabel(themeMode)),
                ) { showThemeBottomSheet = true }
            }

            if (isDynamicColorSupported()) {
                fullWidthColumn(span) {
                    SwitchItem(
                        title = Res.string.dynamic_color,
                        subtitle = Res.string.dynamic_color_description,
                        checked = isDynamicColor,
                    ) {
                        coroutineScope.launch {
                            ThemeUtils.setDynamicColor(it)
                        }
                    }
                }
            }

            if (!isDynamicColorSupported() || !isDynamicColor) {
                fullWidthColumn(span) { ListItemCategoryLabel(title = stringResource(Res.string.theme_colors)) }
                items(themeItems.size) { index ->
                    SettingsThemeItem(
                        themeItem = themeItems[index],
                        isDarkTheme = isDarkTheme,
                        currentThemeColor = themeColor,
                    ) {
                        coroutineScope.launch {
                            ThemeUtils.setThemeColor(it)
                        }
                    }
                }
            }
        }

        ThemeSelectorSheet(showThemeBottomSheet) { showThemeBottomSheet = false }
    }
}
