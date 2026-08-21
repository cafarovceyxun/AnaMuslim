package com.cafarovceyxun.anamuslim.compose.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

/**
 * Bar-order index of a tab destination, or `-1` for anything else.
 *
 * The iOS counterpart of `MainRoutes.BOTTOM_NAV_ROUTES.indexOf(route)`: Android navigates by string
 * route, the shared host by typed [AppDestination], so the same ordering has to be expressible both
 * ways. Reordering here without [MainTab] and `rememberMainNavItems()` sends the slide the wrong way.
 */
fun mainTabIndexOf(destination: NavDestination?): Int = when {
    destination == null -> -1
    destination.hasRoute<AppDestination.Home>() -> 0
    destination.hasRoute<AppDestination.ReaderIndex>() -> 1
    destination.hasRoute<AppDestination.HadithIndex>() -> 2
    destination.hasRoute<AppDestination.Search>() -> 3
    destination.hasRoute<AppDestination.Settings>() -> 4
    else -> -1
}

/**
 * Enter/exit for a move **between two bottom-bar tabs**, or `null` when this transition is not one
 * (a push, a pop, or anything with a non-tab end) — the caller then keeps its own animation.
 *
 * The direction comes from the tab indices, so swiping the bar rightwards carries the screens
 * rightwards too; `SlideDirection.Start/End` (not Left/Right) keeps that true under an Arabic RTL
 * layout, where the tabs themselves are mirrored. The slide is a fraction of the width rather than a
 * full page: the tabs are siblings, not a stack, and a full-width push would read as a drill-down.
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabEnter(
    fromIndex: Int,
    toIndex: Int,
): EnterTransition? {
    if (!isTabMove(fromIndex, toIndex)) return null
    val towards = if (toIndex > fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideIntoContainer(
        towards = towards,
        animationSpec = tween(TAB_SLIDE_MILLIS, easing = EaseOutExpo),
        initialOffset = { it / 3 },
    ) + fadeIn(animationSpec = tween(TAB_FADE_IN_MILLIS, easing = EaseOutExpo))
}

/** Exit half of [mainTabEnter] — same direction, so the two screens travel together. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabExit(
    fromIndex: Int,
    toIndex: Int,
): ExitTransition? {
    if (!isTabMove(fromIndex, toIndex)) return null
    val towards = if (toIndex > fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideOutOfContainer(
        towards = towards,
        animationSpec = tween(TAB_SLIDE_MILLIS, easing = EaseOutExpo),
        targetOffset = { it / 3 },
    ) + fadeOut(animationSpec = tween(TAB_FADE_OUT_MILLIS, easing = LinearEasing))
}

private fun isTabMove(fromIndex: Int, toIndex: Int) =
    fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex

private const val TAB_SLIDE_MILLIS = 300
private const val TAB_FADE_IN_MILLIS = 220
private const val TAB_FADE_OUT_MILLIS = 180
