package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.player.MINI_PLAYER_BOTTOM_GAP
import com.cafarovceyxun.anamuslim.compose.components.player.MINI_PLAYER_HEIGHT
import com.cafarovceyxun.anamuslim.compose.components.player.rememberMiniPlayerVisibilityState
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
 * Bottom `contentPadding` for a list or scroll column that runs *under* the floating bottom nav.
 *
 * The tab screens used to hardcode 100–128.dp here. That is short of the bar's real reach on any
 * window with a home indicator or gesture bar — [mainBottomNavigationOuterHeight] is 64 + 34 + 12 =
 * 110.dp on a current iPhone — so the last row stayed half-covered by the bar. Nothing catches this:
 * the row is drawn, just behind an opaque pill.
 *
 * [extra] is the breathing room left above the bar. Screens with a FAB pass its height, so the last
 * row clears the button too.
 */
@Composable
fun mainBottomNavContentPadding(extra: Dp = 16.dp): Dp = mainBottomNavigationOuterHeight() + extra

/**
 * [mainBottomNavContentPadding] plus the mini player, for the two tab screens that host it (home and
 * the Quran index).
 *
 * The player floats *above* the bar (`collapsedBottomInset = mainBottomNavigationOuterHeight()`), so
 * its height adds to the bar's instead of overlapping it. Reserved only while a recitation is
 * actually loaded — otherwise every fresh install would open with 80.dp of dead space under the
 * fold. Asking [rememberMiniPlayerVisibilityState] again is the documented way to reserve room for
 * the player without owning it.
 */
@Composable
fun mainBottomNavContentPaddingWithPlayer(extra: Dp = 16.dp): Dp {
    val playerVisible = rememberMiniPlayerVisibilityState().isVisible
    return mainBottomNavContentPadding(extra) +
            if (playerVisible) MINI_PLAYER_HEIGHT + MINI_PLAYER_BOTTOM_GAP else 0.dp
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
