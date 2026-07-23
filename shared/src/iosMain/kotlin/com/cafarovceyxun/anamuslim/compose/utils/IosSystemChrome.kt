package com.cafarovceyxun.anamuslim.compose.utils

/**
 * Bridges Compose-driven system-chrome state to the SwiftUI host, which applies it with
 * `.statusBarHidden`.
 *
 * Compose Multiplatform 1.8 exposes no common API for the iOS status bar (verified against the
 * ui-uikit klibs), and the bar is owned by the `UIViewController` that `ComposeUIViewController`
 * manages — unreachable from common code. So `ReaderFullscreenEffect` publishes here and the Swift
 * `ContentView` observes [listener] and reads [statusBarHidden] back.
 *
 * Only fullscreen is bridged, not the light/dark status-bar tint: the SwiftUI way to set the tint is
 * `.preferredColorScheme`, which pins the trait Compose reads for "system" theme mode — that both
 * feeds back on the first frame and blocks live system-theme changes. iOS's default status-bar style
 * already follows that trait, so the tint needs no help; `SystemAppearance` stays a no-op on iOS.
 */
object IosSystemChrome {

    /** Set by the Swift host; invoked (on an arbitrary thread) whenever [statusBarHidden] changes. */
    var listener: (() -> Unit)? = null

    var statusBarHidden: Boolean = false
        private set

    fun setStatusBarHidden(value: Boolean) {
        if (value == statusBarHidden) return
        statusBarHidden = value
        listener?.invoke()
    }
}
