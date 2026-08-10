package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Whether the window is wider than it is tall.
 *
 * Compose MP has no `LocalConfiguration`/`Configuration.ORIENTATION_LANDSCAPE`, so orientation is
 * derived from the window itself — which is also more correct for split-screen and iPad multitasking,
 * where device orientation and window shape disagree.
 */
@Composable
fun isLandscape(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return size.width > size.height
}

/**
 * The window's current width in dp — the Compose MP stand-in for `Configuration.screenWidthDp`,
 * derived from the window's pixel width and the current density.
 */
@Composable
fun screenWidthDp(): Dp {
    val widthPx = LocalWindowInfo.current.containerSize.width
    return with(LocalDensity.current) { widthPx.toDp() }
}

/** The window's current height in dp — the counterpart to [screenWidthDp]. */
@Composable
fun screenHeightDp(): Dp {
    val heightPx = LocalWindowInfo.current.containerSize.height
    return with(LocalDensity.current) { heightPx.toDp() }
}

/**
 * Whether the window is large enough for a two-pane reader layout.
 *
 * The thresholds are Material's expanded-width / medium-height breakpoints (840dp × 480dp), taken
 * from `androidx.window.core.layout.WindowSizeClass`. That artifact is Android-only, so the same
 * comparison is done here against [LocalWindowInfo] — the source `isAtLeastBreakpoint` reduces to
 * exactly this `width >= 840 && height >= 480`.
 */
@Composable
fun isExpandedWindow(): Boolean = screenWidthDp() >= 840.dp && screenHeightDp() >= 480.dp

/**
 * How many columns a full-window list of rows should use.
 *
 * The 800dp threshold is the same one the reader's navigator lists apply through
 * `BoxWithConstraints`; those measure a pane rather than the window, so they keep their local
 * check. This helper is for screens that fill the window, where the two are equivalent — it exists
 * so the threshold is stated once instead of being re-typed as a literal in each list.
 */
@Composable
fun listColumnCount(): Int = if (screenWidthDp() < 800.dp) 1 else 2
