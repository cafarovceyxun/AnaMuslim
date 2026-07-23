package com.cafarovceyxun.anamuslim.compose.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dynamic_color
import com.cafarovceyxun.anamuslim.resources.dynamic_color_description
import com.cafarovceyxun.anamuslim.resources.theme_colors
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.resources.strLabelSystemDefault
import com.cafarovceyxun.anamuslim.resources.strLabelThemeDark
import com.cafarovceyxun.anamuslim.resources.strLabelThemeLight
import com.cafarovceyxun.anamuslim.resources.strMsgThemeDark
import com.cafarovceyxun.anamuslim.resources.strMsgThemeDefault
import com.cafarovceyxun.anamuslim.compose.components.common.SwitchItem
import com.cafarovceyxun.anamuslim.compose.components.settings.ListItemCategoryLabel
import com.cafarovceyxun.anamuslim.compose.components.settings.SettingsThemeItem
import com.cafarovceyxun.anamuslim.compose.screens.settings.themeColorItems
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.applyThemeModeToPlatform
import com.cafarovceyxun.anamuslim.compose.utils.isDynamicColorSupported
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.extensions.verticalFadingEdge

@Composable
fun OnboardingThemePage() {
    val themeMode = ThemeUtils.observeThemeMode()
    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val themeColor = ThemeUtils.observeThemeColor()
    val isDynamicColor = ThemeUtils.observeIsDynamicColor()
    val scope = rememberCoroutineScope()
    val themeItems = themeColorItems()

    val items = listOf(
        Triple(
            ThemeUtils.THEME_MODE_DEFAULT,
            Res.string.strLabelSystemDefault,
            Res.string.strMsgThemeDefault,
        ),
        Triple(
            ThemeUtils.THEME_MODE_DARK,
            Res.string.strLabelThemeDark,
            Res.string.strMsgThemeDark,
        ),
        Triple(
            ThemeUtils.THEME_MODE_LIGHT,
            Res.string.strLabelThemeLight,
            null,
        ),
    )

    val scrollState = rememberScrollState()
    val dynamicColorSupported = isDynamicColorSupported()

    Box(
        Modifier.verticalFadingEdge(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(8.dp),
        ) {
            items.forEach { (mode, title, description) ->
                RadioItem(
                    title = title,
                    subtitle = description,
                    selected = themeMode == mode,
                    onClick = {
                        scope.launch {
                            ThemeUtils.setThemeMode(mode)
                            applyThemeModeToPlatform(mode)
                        }
                    },
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (dynamicColorSupported) {
                    SwitchItem(
                        title = Res.string.dynamic_color,
                        subtitle = Res.string.dynamic_color_description,
                        checked = isDynamicColor,
                    ) {
                        scope.launch {
                            ThemeUtils.setDynamicColor(it)
                        }
                    }
                }

                if (!dynamicColorSupported || !isDynamicColor) {
                    ListItemCategoryLabel(title = stringResource(Res.string.theme_colors))

                    themeItems.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    SettingsThemeItem(
                                        themeItem = item,
                                        isDarkTheme = isDarkTheme,
                                        currentThemeColor = themeColor,
                                    ) {
                                        scope.launch {
                                            ThemeUtils.setThemeColor(it)
                                        }
                                    }
                                }
                            }

                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
