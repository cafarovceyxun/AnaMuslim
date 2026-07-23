package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable

/**
 * Keeps the screen awake while composed, for up to [timeoutMillis] (default 30 min), then lets it
 * sleep normally. On Android this sets/clears the window's keep-screen-on flag; iOS toggles the
 * idle timer. Used by the reader and long-form hadith screens.
 */
@Composable
expect fun KeepScreenOn(timeoutMillis: Long = 30 * 60 * 1000L)
