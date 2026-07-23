package com.cafarovceyxun.anamuslim.compose.components.player

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Navigation the player UI can trigger but cannot perform itself, since the destinations are
 * platform screens (an Android `Activity`, a SwiftUI route). The host provides the real
 * implementations; unprovided actions are no-ops.
 *
 * Same seam shape as [com.cafarovceyxun.anamuslim.compose.components.IndexMenuActions].
 */
data class PlayerActions(
    /** Opens the screen where recitation audio is downloaded and managed. */
    val onOpenRecitationDownloads: () -> Unit = {},
)

val LocalPlayerActions = staticCompositionLocalOf { PlayerActions() }
