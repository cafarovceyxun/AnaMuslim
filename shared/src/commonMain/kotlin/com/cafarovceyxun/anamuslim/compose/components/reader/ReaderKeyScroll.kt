package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.utils.reader.ReaderScrollStep
import kotlinx.coroutines.flow.Flow

/**
 * Drives a [LazyListState] from a reader's direction-only key events (volume / S Pen / page keys).
 *
 * The step is resolved here, at the point of scrolling, rather than in the ViewModel: it is a share
 * of *this* list's viewport, which only the layout knows, and it must track the live preference and
 * text size without re-emitting anything. The move is eased ([ReaderScrollStep.animationSpec]) so a
 * three-quarter-screen jump reads as a glide, not a snap.
 *
 * [directions] carries 1 for forward/down and -1 for backward/up.
 */
@Composable
fun ReaderKeyScrollEffect(
    listState: LazyListState,
    directions: Flow<Int>,
) {
    val stepPercent = AppPreferences.observeReaderScrollStepPercent()

    LaunchedEffect(listState, stepPercent) {
        directions.collect { direction ->
            val viewportPx = listState.layoutInfo.viewportSize.height
            val step = ReaderScrollStep.stepPx(viewportPx, stepPercent)
            if (step <= 0f) return@collect
            listState.animateScrollBy(
                value = direction * step,
                animationSpec = ReaderScrollStep.animationSpec,
            )
        }
    }
}
