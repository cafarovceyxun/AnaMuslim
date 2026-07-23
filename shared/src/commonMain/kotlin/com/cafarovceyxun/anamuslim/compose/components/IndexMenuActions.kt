package com.cafarovceyxun.anamuslim.compose.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What the index overflow menu can do. The two store hand-offs are **nullable on purpose**: they
 * need an app-store listing to point at, which only exists on Android today. A null one hides its
 * menu entry rather than leaving a row that does nothing when tapped — the same rule
 * `rememberToggleScreenRotation()` follows for its button.
 */
data class IndexMenuActions(
    val onOpenBookmarks: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenStorageCleanup: () -> Unit = {},
    val onOpenPlayStore: (() -> Unit)? = null,
    val onOpenExportImport: () -> Unit = {},
    val onOpenAboutUs: () -> Unit = {},
    val onShareApp: () -> Unit = {},
    val onRateApp: (() -> Unit)? = null,
)

val LocalIndexMenuActions = staticCompositionLocalOf { IndexMenuActions() }
