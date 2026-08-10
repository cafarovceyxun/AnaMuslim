package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.cafarovceyxun.anamuslim.compose.theme.tightTextStyle

/**
 * State for a [CollapsingAppBar], with the collapse range already set to the hero height and kept in
 * sync across rotation.
 *
 * Screens used to compute `-(expandedHeight - collapsedHeight).toPx()` themselves; some then forgot
 * to refresh it when the orientation changed, leaving the bar with a stale collapse range and a
 * hero that could not fully scroll away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberCollapsingAppBarState(): TopAppBarState {
    val density = LocalDensity.current
    val heroHeight = AppBarDefaults.heroHeight
    val state = rememberTopAppBarState(
        initialHeightOffsetLimit = with(density) { -heroHeight.toPx() }
    )

    LaunchedEffect(heroHeight, density) {
        state.heightOffsetLimit = with(density) { -heroHeight.toPx() }
    }

    CollapseOnLandscapeEffect(state)

    return state
}

/**
 * The app's one collapsing top bar: a hero block (logo + large title) that scrolls away to leave the
 * ordinary [AppBar] row behind.
 *
 * Both index screens — Quran and hadith — used to hand-roll this, each lerping its own logo offsets
 * against its own height pair. The two drifted, and neither collapsed onto the standard bar height,
 * so navigating from an index into a plain screen visibly shifted the title.
 *
 * Here the geometry is fixed by [AppBarDefaults]: expanded is `barHeight + heroHeight`, collapsed is
 * exactly `barHeight`. The hero and the row title cross-fade rather than sliding past each other, so
 * the title lands in the same start-aligned slot every other bar uses and simply stays there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    logo: Painter? = null,
    logoContentDescription: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val fraction = scrollBehavior.state.collapsedFraction.coerceIn(0f, 1f)
    val barHeight = AppBarDefaults.barHeight
    val totalHeight = lerp(AppBarDefaults.expandedBarHeight, barHeight, fraction)

    // The hero clears out early and the row title arrives late, so the two never read as one title
    // sliding diagonally across the bar — the effect the old offset-lerp produced.
    val heroAlpha = (1f - fraction * 1.4f).coerceIn(0f, 1f)
    val rowTitleAlpha = ((fraction - 0.6f) / 0.4f).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorScheme.surfaceContainer,
        shadowElevation = AppBarDefaults.ShadowElevation * fraction,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appBarInsetsPadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
                if (logo != null && heroAlpha > 0f) {
                    HeroContent(
                        title = title,
                        logo = logo,
                        logoContentDescription = logoContentDescription,
                        fraction = fraction,
                        alpha = heroAlpha,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(AppBarDefaults.heroHeight),
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .height(barHeight)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    BackButton(onClick = onBack)

                    Text(
                        text = title,
                        style = AppBarDefaults.titleStyle,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .alpha(rowTitleAlpha),
                    )

                    actions()
                }
            }
        }
    }
}

@Composable
private fun HeroContent(
    title: String,
    logo: Painter,
    logoContentDescription: String?,
    fraction: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    // Shrinking slightly on the way out keeps the hero from looking like it simply switched off.
    val scale = lerp(1f, 0.88f, fraction)
    val logoWidth = AppBarDefaults.heroLogoWidth * scale
    val logoHeight = AppBarDefaults.heroLogoHeight * scale

    Column(
        modifier = modifier.alpha(alpha).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = logo,
            contentDescription = logoContentDescription,
            modifier = Modifier.size(logoWidth, logoHeight),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(colorScheme.primary),
        )

        Text(
            text = title,
            style = AppBarDefaults.heroTitleStyle.merge(tightTextStyle),
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            // The row title below is the canonical one; both are always in the tree and only differ
            // by alpha, so leaving this readable makes a screen reader announce the title twice.
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}
