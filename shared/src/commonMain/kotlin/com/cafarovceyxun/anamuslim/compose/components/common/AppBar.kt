package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.utils.rememberSystemBack
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_chevron_left
import com.cafarovceyxun.anamuslim.resources.dr_icon_search
import com.cafarovceyxun.anamuslim.resources.strDescGoBack
import com.cafarovceyxun.anamuslim.resources.strHintSearch
import com.cafarovceyxun.anamuslim.resources.strLabelNavSearch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppBar(
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    bgColor: Color = colorScheme.surfaceContainer,
    color: Color = colorScheme.onSurface,
    searchQuery: String = "",
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholder: String? = null,
    shadowElevation: Dp = AppBarDefaults.ShadowElevation,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val systemBack = rememberSystemBack()
    var searchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val onSearch = onSearchQueryChange

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    val searchEnabled = onSearch != null
    if (searchEnabled) {
        BackHandler(enabled = searchExpanded) {
            searchExpanded = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = shadowElevation,
        color = bgColor
    ) {
        // A plain Row rather than Material's `TopAppBar`: that composable positions its title from
        // its own `expandedHeight` token instead of the height it is actually given, so forcing our
        // shared row height onto it left the title floating above the back button and the actions.
        // Laying the row out here keeps every slot on one baseline and matches CollapsingAppBar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appBarInsetsPadding()
                .appBarRowHeight()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton {
                if (searchEnabled && searchExpanded) {
                    searchExpanded = false
                } else if (onBack != null) {
                    onBack()
                } else {
                    systemBack?.invoke()
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (searchEnabled && searchExpanded) {
                    SearchTextField(
                        value = searchQuery,
                        onValueChange = onSearch,
                        placeholder = searchPlaceholder ?: stringResource(Res.string.strHintSearch),
                        modifier = Modifier.focusRequester(searchFocusRequester),
                    )
                } else if (titleContent != null) {
                    titleContent()
                } else if (title != null) {
                    Text(
                        text = title,
                        style = AppBarDefaults.titleStyle,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!searchExpanded) {
                if (searchEnabled) {
                    val searchLabel = stringResource(Res.string.strLabelNavSearch)
                    SimpleTooltip(text = searchLabel) {
                        IconButton(
                            onClick = { searchExpanded = true },
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_search),
                                contentDescription = searchLabel,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                actions()
            }
        }
    }
}

@Composable
fun BackButton(
    onClick: (() -> Unit)? = null
) {
    val backLabel = stringResource(Res.string.strDescGoBack)
    val systemBack = rememberSystemBack()

    fun handleClick() {
        if (onClick != null) onClick()
        else systemBack?.invoke()
    }

    SimpleTooltip(text = backLabel) {
        IconButton(
            onClick = ::handleClick,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_left),
                contentDescription = backLabel,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
