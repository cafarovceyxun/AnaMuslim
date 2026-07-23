package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Navigation the hadith UI can trigger but cannot perform itself, since the destinations are
 * platform screens (an Android `Activity`, a SwiftUI route). The host provides the real
 * implementations; unprovided actions are no-ops.
 *
 * Same seam shape as [com.cafarovceyxun.anamuslim.compose.components.reader.ReaderActions].
 */
data class HadithActions(
    /** Opens the app's settings screen. */
    val onOpenSettings: () -> Unit = {},
)

val LocalHadithActions = staticCompositionLocalOf { HadithActions() }
