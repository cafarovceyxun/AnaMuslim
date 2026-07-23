package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.cafarovceyxun.anamuslim.compose.theme.AppIcons
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.quran
import com.cafarovceyxun.anamuslim.resources.strLabelNavHadith
import com.cafarovceyxun.anamuslim.resources.strLabelNavHome
import com.cafarovceyxun.anamuslim.resources.strLabelNavSearch
import com.cafarovceyxun.anamuslim.resources.strTitleQuran
import com.cafarovceyxun.anamuslim.resources.strTitleSettings

/** One tab. Exactly one of [icon]/[iconRes] is set — the Quran tab is the only drawable-backed one. */
data class MainNavItem(
    val title: StringResource,
    val icon: ImageVector? = null,
    val iconRes: DrawableResource? = null,
)

/** The five top-level tabs, identical on both platforms; only the routing behind them differs. */
@Composable
fun rememberMainNavItems(): List<MainNavItem> = remember {
    listOf(
        MainNavItem(Res.string.strLabelNavHome, AppIcons.Home),
        MainNavItem(Res.string.strTitleQuran, iconRes = Res.drawable.quran),
        MainNavItem(Res.string.strLabelNavHadith, AppIcons.Hadith),
        MainNavItem(Res.string.strLabelNavSearch, AppIcons.Search),
        MainNavItem(Res.string.strTitleSettings, AppIcons.Settings),
    )
}

/**
 * The bottom tab bar, deliberately **route-agnostic**: it takes a selected index and reports taps by
 * index rather than owning a `NavController`. Android matches its string `MainRoutes` against the
 * back stack, iOS matches typed `AppDestination`s — neither routing model leaks in here.
 */
@Composable
fun MainBottomNavigationBar(
    items: List<MainNavItem>,
    selectedIndex: Int,
    onSelect: (index: Int) -> Unit,
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = navBarBottom + 12.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surfaceContainer,
            shadowElevation = 0.dp,
            shape = CircleShape,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.3f))
        ) {
            val navBarHeight = mainBottomNavBarHeight()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEachIndexed { index, item ->
                    val isActive = selectedIndex == index
                    val contentColor by animateColorAsState(
                        targetValue = if (isActive) colorScheme.primary else colorScheme.onSurface.alpha(0.6f),
                        label = "color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.title),
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else if (item.iconRes != null) {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = stringResource(item.title),
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Text(
                                text = stringResource(item.title),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1,
                                fontSize = 10.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
