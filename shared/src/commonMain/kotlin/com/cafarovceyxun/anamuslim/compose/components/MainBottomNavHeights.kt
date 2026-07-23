package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape

/**
 * Bottom-nav metrics, split out of `MainBottomNavigationBar.kt` (`:app`) because shared screens need
 * the heights for their own bottom padding while the bar itself still depends on `NavController` and
 * the Android player state. The package is kept so existing `:app` callers resolve them unchanged.
 */
@Composable
fun mainBottomNavBarHeight(): Dp = if (isLandscape()) 52.dp else 64.dp

@Composable
fun mainBottomNavigationOuterHeight(): Dp {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return mainBottomNavBarHeight() + navBarBottom + 12.dp
}

/**
 * Bottom padding for a `Scaffold` floating action button so it clears the floating bottom nav.
 *
 * Scaffold already lifts its FAB slot by the window inset, so only the bar's own height plus the gap
 * under it is left to clear: padding with the full [mainBottomNavigationOuterHeight] counts the inset
 * twice and leaves a visible hole between the button and the bar.
 */
@Composable
fun mainBottomNavFabPadding(gap: Dp = 6.dp): Dp {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return (mainBottomNavigationOuterHeight() - navBarBottom - gap).coerceAtLeast(0.dp)
}

/**
 * The strip the mini player leaves free *below* itself in the reader, for the reader's own floating
 * tajweed/fullscreen buttons.
 *
 * Those buttons are laid out at `navigationBars + 12.dp` and are 32.dp tall, so the window inset has
 * to be part of this. Reserving a bare 40.dp — measured from the window bottom — put the player over
 * them on every device with a gesture bar or a home indicator.
 */
@Composable
fun readerFloatingControlsInset(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + READER_FLOATING_CONTROLS_STRIP

/** [readerFloatingControlsInset] without the window inset, for callers already padded for it. */
val READER_FLOATING_CONTROLS_STRIP = 40.dp
