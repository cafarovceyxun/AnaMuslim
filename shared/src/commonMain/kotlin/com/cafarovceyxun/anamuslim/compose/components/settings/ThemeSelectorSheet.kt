package com.cafarovceyxun.anamuslim.compose.components.settings

import com.cafarovceyxun.anamuslim.resources.dr_icon_theme
import com.cafarovceyxun.anamuslim.resources.Res
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.common.RadioItem
import com.cafarovceyxun.anamuslim.resources.strLabelSystemDefault
import com.cafarovceyxun.anamuslim.resources.strLabelThemeDark
import com.cafarovceyxun.anamuslim.resources.strLabelThemeLight
import com.cafarovceyxun.anamuslim.resources.strMsgThemeDark
import com.cafarovceyxun.anamuslim.resources.strMsgThemeDefault
import com.cafarovceyxun.anamuslim.resources.strTitleTheme
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils
import com.cafarovceyxun.anamuslim.compose.utils.applyThemeModeToPlatform
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ThemeSelectorSheet(isOpen: Boolean, onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val themeMode = ThemeUtils.observeThemeMode()

    val items = listOf(
        Triple(
            ThemeUtils.THEME_MODE_DEFAULT,
            Res.string.strLabelSystemDefault,
            Res.string.strMsgThemeDefault
        ),
        Triple(ThemeUtils.THEME_MODE_DARK, Res.string.strLabelThemeDark, Res.string.strMsgThemeDark),
        Triple(ThemeUtils.THEME_MODE_LIGHT, Res.string.strLabelThemeLight, null),
    )

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.dr_icon_theme,
        title = stringResource(Res.string.strTitleTheme),
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            items.forEach { (theme, title, description) ->
                RadioItem(
                    title = title,
                    subtitle = description,
                    selected = themeMode == theme,
                    onClick = {
                        onDismiss()

                        coroutineScope.launch {
                            ThemeUtils.setThemeMode(theme)
                            applyThemeModeToPlatform(theme)
                        }
                    }
                )
            }
        }
    }
}
