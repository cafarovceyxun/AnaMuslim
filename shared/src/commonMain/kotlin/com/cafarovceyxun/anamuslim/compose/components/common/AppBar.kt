package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.utils.rememberSystemBack
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_arrow_left
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
    shadowElevation: Dp = 1.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val systemBack = rememberSystemBack()
    var searchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val onSearch = onSearchQueryChange

    val windowInfo = LocalWindowInfo.current
    val isLandscape = windowInfo.containerSize.width > windowInfo.containerSize.height

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
        TopAppBar(
            modifier = if (isLandscape) Modifier.height(48.dp) else Modifier,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                navigationIconContentColor = color,
                titleContentColor = color,
                actionIconContentColor = color,
            ),
            title = {
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
                        style = if (isLandscape) typography.titleSmall else typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            navigationIcon = {
                BackButton() {
                    if (searchEnabled && searchExpanded) {
                        searchExpanded = false
                    } else {
                        systemBack?.invoke()
                    }
                }
            },
            actions = {
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
            },
        )
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
                painter = painterResource(Res.drawable.dr_icon_arrow_left),
                contentDescription = backLabel,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
