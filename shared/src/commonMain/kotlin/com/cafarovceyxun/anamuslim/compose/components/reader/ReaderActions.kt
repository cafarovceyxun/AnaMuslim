package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Navigation the reader chrome can trigger but cannot perform itself, since the destinations are
 * platform screens (an Android `Activity`, a SwiftUI route). The host provides the real
 * implementations; unprovided actions are no-ops.
 *
 * Same seam shape as [com.cafarovceyxun.anamuslim.compose.components.player.PlayerActions].
 */
data class ReaderActions(
    /** Opens the settings screen filtered to the reader's own settings. */
    val onOpenReaderSettings: () -> Unit = {},
)

val LocalReaderActions = staticCompositionLocalOf { ReaderActions() }
