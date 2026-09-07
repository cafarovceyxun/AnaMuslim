package com.cafarovceyxun.anamuslim.compose.utils

/**
 * Bridges Compose-driven system-chrome state to the SwiftUI host, which applies it with
 * `.statusBarHidden` (and, for [portraitLocked], with the app delegate's orientation mask).
 *
 * Compose Multiplatform 1.8 exposes no common API for the iOS status bar (verified against the
 * ui-uikit klibs), and the bar is owned by the `UIViewController` that `ComposeUIViewController`
 * manages — unreachable from common code. So `ReaderFullscreenEffect` publishes here and the Swift
 * `ContentView` observes [listener] and reads [statusBarHidden] back. The supported interface
 * orientations have the same problem for the same reason: they are answered by
 * `UIApplicationDelegate`, so [portraitLocked] rides the same bridge.
 *
 * Only fullscreen is bridged, not the light/dark status-bar tint: the SwiftUI way to set the tint is
 * `.preferredColorScheme`, which pins the trait Compose reads for "system" theme mode — that both
 * feeds back on the first frame and blocks live system-theme changes. iOS's default status-bar style
 * already follows that trait, so the tint needs no help; `SystemAppearance` stays a no-op on iOS.
 */
object IosSystemChrome {

    /**
     * Set by the Swift host; invoked (on an arbitrary thread) whenever any published value changes.
     *
     * Tək callback-dir və hansı sahənin dəyişdiyini demir — Swift tərəfi hər çağırışda hamısını
     * yenidən oxuyur. Sahə sayı azdır, ona görə ayrıca siqnal əlavə etmək faydasızdır.
     */
    var listener: (() -> Unit)? = null

    var statusBarHidden: Boolean = false
        private set

    /**
     * Pəncərənin portretdə saxlanması tələb olunurmu — bax `PortraitLockEffect`.
     *
     * Swift `AppDelegate.orientationLock`-u buna görə qurur və `requestGeometryUpdate` ilə səhnəni
     * dərhal çevirir; sistem tələbi rədd edə bilər (bölünmüş ekran, iPad çoxvəzifəliliyi), ona görə
     * bu bayraq zəmanət yox, sorğudur.
     */
    var portraitLocked: Boolean = false
        private set

    fun setStatusBarHidden(value: Boolean) {
        if (value == statusBarHidden) return
        statusBarHidden = value
        listener?.invoke()
    }

    fun setPortraitLocked(value: Boolean) {
        if (value == portraitLocked) return
        portraitLocked = value
        listener?.invoke()
    }
}
