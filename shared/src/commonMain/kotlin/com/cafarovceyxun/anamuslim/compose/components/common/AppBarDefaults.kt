package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.utils.isLandscape

/**
 * Single source of truth for the app's top-bar chrome so every screen — the Quran reader, search,
 * settings, hadith, and the rest — reads as one bar that changes contents, not as five bars that
 * happen to sit in the same place.
 *
 * Four things have to match across bars, not just one:
 *  - **Height:** the primary row is [BarHeightPortrait] and collapses to [BarHeightLandscape] in
 *    landscape. This is *content* height only; system-inset padding is added around it via
 *    [appBarInsetsPadding], never baked into the number (baking it in squished the row on devices
 *    whose status bar stays visible in landscape).
 *  - **Insets:** [appBarInsetsPadding] clears the status bar *and the horizontal display cutout*
 *    (the landscape camera notch) with one shared rule. Bars that only used `statusBarsPadding()`
 *    let content slide under the side camera cutout, which is what read as "broken on one screen".
 *  - **Collapse state:** [CollapseOnLandscapeEffect] is the only place that force-collapses a
 *    scrolling bar. Hand-rolled copies of that effect collapsed on rotation into landscape but never
 *    restored on the way back, so screens re-entered portrait stuck shut.
 *  - **Title placement:** every bar is start-aligned (Material 3), so a title never jumps sideways
 *    when navigating between screens.
 *
 * The chosen 48dp compact height sits in the shared zone of both platforms' conventions — iOS's
 * landscape navigation bar (~44pt) and Material 3's compact top-app-bar row — so it reads as native
 * on Android and iOS alike. Point new bars here instead of adding fresh magic numbers.
 */
object AppBarDefaults {
    /** Height of the primary bar row (content only) when the window is taller than it is wide. */
    val BarHeightPortrait: Dp = 64.dp

    /** Compact height of the primary bar row (content only) in landscape / short windows. */
    val BarHeightLandscape: Dp = 48.dp

    /**
     * Height of the hero area a collapsing bar shows *above* its primary row when fully expanded —
     * the logo-and-large-title block on the Quran and hadith index screens. Total expanded height is
     * [barHeight] + [heroHeight]; collapsing that hero away lands exactly on [barHeight], which is
     * why a collapsed index bar is pixel-identical to a plain screen's bar.
     */
    val HeroHeightPortrait: Dp = 156.dp

    /** Compact hero height in landscape, keeping total expanded height reasonable on short windows. */
    val HeroHeightLandscape: Dp = 92.dp

    /** Shadow under the bar, applied once the bar sits over scrolled content. */
    val ShadowElevation: Dp = 1.dp

    /** Logo box inside an expanded collapsing hero. Sized to leave room for the title beneath it. */
    val heroLogoWidth: Dp
        @Composable get() = if (isLandscape()) 120.dp else 180.dp

    /** Logo box height inside an expanded collapsing hero. */
    val heroLogoHeight: Dp
        @Composable get() = if (isLandscape()) 44.dp else 84.dp

    /** The primary bar row content height for the current window orientation. */
    val barHeight: Dp
        @Composable get() = if (isLandscape()) BarHeightLandscape else BarHeightPortrait

    /** The collapsing hero height for the current window orientation. */
    val heroHeight: Dp
        @Composable get() = if (isLandscape()) HeroHeightLandscape else HeroHeightPortrait

    /** Total height of a fully expanded collapsing bar, hero included. */
    val expandedBarHeight: Dp
        @Composable get() = barHeight + heroHeight

    /** Title text style, tightened by one step in landscape to match the shorter bar. */
    val titleStyle: TextStyle
        @Composable get() = if (isLandscape()) typography.titleSmall else typography.titleMedium

    /** Large title style used inside an expanded collapsing hero. */
    val heroTitleStyle: TextStyle
        @Composable get() = if (isLandscape()) typography.titleMedium else typography.titleLarge
}

/**
 * Padding that clears the status bar and the horizontal display cutout for a top bar's *content*,
 * while the enclosing `Surface` still paints its background behind those areas. Apply this to the
 * content wrapper (a bar row, or the `Column` holding several rows), then set the row height with
 * [appBarRowHeight] so the fixed height is measured *inside* the inset padding — total bar height is
 * therefore `insets + rowHeight`, and the row itself never gets squished.
 */
@Composable
fun Modifier.appBarInsetsPadding(): Modifier = this.windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
)

/**
 * Fixes a bar row to the standard content height for the current orientation. Chain it *after*
 * [appBarInsetsPadding] so the height applies to the content, not the inset-padded outer box.
 */
@Composable
fun Modifier.appBarRowHeight(): Modifier = this.height(AppBarDefaults.barHeight)

/**
 * Collapses a scrolling top bar when the window turns landscape, where vertical space is scarce, and
 * expands it again on the way back to portrait.
 *
 * The restore half is the point. Every screen used to inline `if (isLandscape) heightOffset =
 * heightOffsetLimit`, which pins the bar shut on rotation and then leaves it pinned: returning to
 * portrait re-runs the effect, takes the `else` branch that does not exist, and the bar stays
 * collapsed — so the same screen opened at different times looked collapsed or expanded at random.
 *
 * Only a collapse this effect performed is undone, so a bar the *user* scrolled shut in portrait
 * stays shut.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapseOnLandscapeEffect(state: TopAppBarState) {
    val isLandscape = isLandscape()
    var collapsedByRotation by remember { mutableStateOf(false) }

    LaunchedEffect(isLandscape, state.heightOffsetLimit) {
        // A non-negative limit means the bar has no room to collapse yet (not measured).
        if (state.heightOffsetLimit >= 0f) return@LaunchedEffect

        if (isLandscape) {
            if (state.heightOffset > state.heightOffsetLimit) {
                collapsedByRotation = true
                state.heightOffset = state.heightOffsetLimit
            }
        } else if (collapsedByRotation) {
            collapsedByRotation = false
            state.heightOffset = 0f
        }
    }
}
