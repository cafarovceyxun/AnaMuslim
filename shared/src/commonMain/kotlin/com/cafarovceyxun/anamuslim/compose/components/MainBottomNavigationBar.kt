package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
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
 *
 * A horizontal drag across the bar walks the tabs the same way a tap does — it only calls [onSelect]
 * with the neighbouring index, so both hosts get it without knowing the gesture exists.
 */
@Composable
fun MainBottomNavigationBar(
    items: List<MainNavItem>,
    selectedIndex: Int,
    onSelect: (index: Int) -> Unit,
) {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Read through `rememberUpdatedState` so the gesture below can key its `pointerInput` on the tab
    // count alone: keying it on `selectedIndex` would restart the pointer handler on the very first
    // tab change and cancel the drag that caused it, ending the swipe after one tab.
    val currentIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val layoutDirection = LocalLayoutDirection.current
    val gestureScope = rememberCoroutineScope()
    // How far the finger has carried the pill past the tab it currently sits on, in pixels. Kept as
    // an `Animatable` so the release can spring it back to 0 while the pill's own spring is still
    // travelling to the newly selected tab.
    val dragOffset = remember { Animatable(0f) }

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

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight)
                    // On the bar itself, not on the screen below it: this is a chrome gesture, and
                    // the screens under it own their own horizontal scrolling (the reader's pages).
                    .pointerInput(items.size, layoutDirection) {
                        if (items.isEmpty()) return@pointerInput
                        val horizontalPadding = NAV_BAR_HORIZONTAL_PADDING.toPx()
                        val tabWidthPx = (size.width - horizontalPadding * 2) / items.size
                        if (tabWidthPx <= 0f) return@pointerInput
                        // In an Arabic (RTL) layout the tabs are mirrored, so a leftward finger means
                        // the *next* tab. Everything below works in tab order, not screen order.
                        val towardsNextTab = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                        // How far past the first/last tab the pill may be pulled before it stops —
                        // the gesture's only feedback that there is nothing further that way.
                        val edgeResistance = tabWidthPx * 0.3f

                        awaitEachGesture {
                            // `requireUnconsumed = false`: each tab is `clickable` and consumes the
                            // down, so a strict wait here would never see a press that starts on a
                            // tab — that is to say, anywhere on the bar.
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var travel = 0f
                            var index = currentIndex

                            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                                change.consume()
                                travel = overSlop * towardsNextTab
                            } ?: return@awaitEachGesture

                            horizontalDrag(drag.id) { change ->
                                // Consuming the movement is also what cancels the pressed tab's
                                // ripple, so a swipe that started on a tab does not also tap it.
                                travel += change.positionChange().x * towardsNextTab
                                change.consume()

                                // A full tab width per tab, so the pill tracks the finger 1:1; the
                                // loop lets one fast swipe cross several tabs.
                                while (travel >= tabWidthPx && index < items.lastIndex) {
                                    index++
                                    travel -= tabWidthPx
                                    currentOnSelect(index)
                                }
                                while (travel <= -tabWidthPx && index > 0) {
                                    index--
                                    travel += tabWidthPx
                                    currentOnSelect(index)
                                }
                                if (index == items.lastIndex) travel = travel.coerceAtMost(edgeResistance)
                                if (index == 0) travel = travel.coerceAtLeast(-edgeResistance)

                                gestureScope.launch { dragOffset.snapTo(travel) }
                            }

                            gestureScope.launch {
                                dragOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    }
            ) {
                // Every tab carries weight(1f), so SpaceAround has no slack left to distribute and each
                // tab is exactly this wide — which is what lets the pill be positioned by arithmetic
                // instead of by measuring the selected child.
                val tabWidth = (maxWidth - NAV_BAR_HORIZONTAL_PADDING * 2) / items.size
                val pillOffset by animateDpAsState(
                    targetValue = NAV_BAR_HORIZONTAL_PADDING + tabWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "pillOffset"
                )

                Box(
                    modifier = Modifier
                        // Lambda offset (still RTL-aware, unlike `absoluteOffset`) so the per-frame
                        // drag value is read at layout time instead of recomposing the whole bar.
                        .offset { IntOffset((pillOffset.toPx() + dragOffset.value).roundToInt(), 0) }
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary.alpha(0.12f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = NAV_BAR_HORIZONTAL_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    items.forEachIndexed { index, item ->
                        val isActive = selectedIndex == index
                        val contentColor by animateColorAsState(
                            targetValue = if (isActive) colorScheme.primary else colorScheme.onSurface.alpha(0.6f),
                            label = "color"
                        )
                        // Bouncy spring on a two-state target: settling at 1.12f overshoots on the way
                        // in, which reads as a tap acknowledgement without needing a one-shot animation.
                        val iconScale by animateFloatAsState(
                            targetValue = if (isActive) 1.12f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "iconScale"
                        )
                        val iconModifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }

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
                                        modifier = iconModifier
                                    )
                                } else if (item.iconRes != null) {
                                    Icon(
                                        painter = painterResource(item.iconRes),
                                        contentDescription = stringResource(item.title),
                                        tint = contentColor,
                                        modifier = iconModifier
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
}

/** Shared by the tab row's own padding and the pill's offset arithmetic — they must not drift apart. */
private val NAV_BAR_HORIZONTAL_PADDING = 8.dp
