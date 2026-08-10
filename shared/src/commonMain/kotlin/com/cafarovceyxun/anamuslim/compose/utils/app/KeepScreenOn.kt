package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences

/**
 * Keeps the screen awake while composed, for up to [timeoutMillis] (default 30 min), then lets it
 * sleep normally. On Android this sets/clears the window's keep-screen-on flag; iOS toggles the
 * idle timer. Used by the reader and long-form hadith screens.
 */
@Composable
expect fun KeepScreenOn(timeoutMillis: Long = 30 * 60 * 1000L)

/**
 * [KeepScreenOn] gated on [AppPreferences.KEY_KEEP_SCREEN_ON], for the reader and hadith screens.
 *
 * The gate is a conditional call rather than a parameter so that turning the preference off drops
 * [KeepScreenOn] out of the composition — its `onDispose` then releases the wake lock immediately,
 * and turning it back on starts a fresh timeout. Both readers call this instead of [KeepScreenOn]
 * so that the app-bar button and the settings switch drive one flag.
 */
@Composable
fun KeepScreenOnIfEnabled() {
    if (AppPreferences.observeKeepScreenOnEnabled()) {
        KeepScreenOn()
    }
}
