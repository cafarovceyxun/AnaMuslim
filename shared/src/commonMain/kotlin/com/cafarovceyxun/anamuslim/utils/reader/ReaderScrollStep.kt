package com.cafarovceyxun.anamuslim.utils.reader

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * How far one navigation key press (volume, S Pen, page keys) moves a scrolling reader, and how
 * that move is animated. Shared by the Quran reader and the hadith reader — one preference drives
 * both.
 *
 * The step is a share of the **visible viewport**, not a pixel count. A fixed pixel step is
 * device- and font-dependent: 800px is a third of a tablet screen but nearly all of a small phone,
 * and it covers fewer lines the moment the user enlarges the text. A percentage keeps one press
 * meaning the same thing everywhere, and it is a unit the slider can show without explaining.
 */
object ReaderScrollStep {

    const val MIN_PERCENT = 25
    const val MAX_PERCENT = 100

    /**
     * Three quarters of a screen: a full press forward, with a quarter left as visual overlap so
     * the reader does not lose the line they were on.
     */
    const val DEFAULT_PERCENT = 75

    /** The slider moves in 5% notches — finer than that is below the resolution of the eye here. */
    const val PERCENT_STEP = 5

    /**
     * Pixels one press should travel inside a viewport [viewportHeightPx] tall. Zero before the
     * viewport has been measured — callers should skip the scroll rather than jump by nothing.
     */
    fun stepPx(viewportHeightPx: Int, percent: Int): Float {
        if (viewportHeightPx <= 0) return 0f
        return viewportHeightPx * (percent.coerceIn(MIN_PERCENT, MAX_PERCENT) / 100f)
    }

    /**
     * Eased, rather than the `animateScrollBy` default spring. A spring covers most of a
     * three-quarter-screen move in its first frames and then creeps to a stop, which reads as a
     * snap; this accelerates and settles evenly, so the eye can follow the text across.
     */
    val animationSpec: AnimationSpec<Float> =
        tween(durationMillis = 450, easing = FastOutSlowInEasing)
}
