package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.utils.screenWidthDp

/**
 * The widest a single column of list rows or form controls should get.
 *
 * Chosen to sit above every phone width in portrait, so on phones this whole file is a no-op and
 * nothing about the existing layouts changes.
 */
val ReadableMaxWidth: Dp = 640.dp

/**
 * Centres [content] and caps it at [maxWidth].
 *
 * Without this, a row built as `fillMaxWidth()` with a leading label and a trailing control turns
 * into two unrelated columns on a tablet — on a 13" iPad the label sits at the far left and its own
 * radio button roughly a thousand dp away, with nothing in between. Capping the column keeps the
 * label and its control visually paired at any window size.
 *
 * Wrap the *content*, not the scroll container: the list should still scroll and draw its
 * background edge to edge, only the rows are constrained.
 */
@Composable
fun ReadableWidthColumn(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ReadableMaxWidth,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.widthIn(max = maxWidth)) {
            content()
        }
    }
}

/**
 * The same cap for a lazy list, expressed as a horizontal inset to fold into its `contentPadding`.
 *
 * A `LazyColumn` cannot be wrapped the way [ReadableWidthColumn] wraps a column: constraining the
 * list itself would take the scroll gesture and the row backgrounds off the window edges. Padding
 * the content keeps the list full width and only narrows what it draws.
 *
 * Returns 0.dp on every phone width, so phone layouts are untouched.
 */
@Composable
fun readableWidthInset(maxWidth: Dp = ReadableMaxWidth): Dp =
    ((screenWidthDp() - maxWidth) / 2).coerceAtLeast(0.dp)
