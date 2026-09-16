package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.cafarovceyxun.anamuslim.compose.utils.IosSystemChrome

@Composable
actual fun ReaderFullscreenEffect(fullscreen: Boolean) {
    // Publish to the SwiftUI host, which hides the status bar with `.statusBarHidden`. Reset on
    // dispose so leaving the reader always restores the bar, whatever state it was left in.
    DisposableEffect(fullscreen) {
        IosSystemChrome.setStatusBarHidden(fullscreen)
        onDispose { IosSystemChrome.setStatusBarHidden(false) }
    }
}

@Composable
actual fun rememberToggleScreenRotation(): (() -> Unit)? = null

// iOS-da oxucunun fırlatma düyməsi yoxdur və tətbiq istiqaməti yalnız `PortraitLockEffect` üçün
// məhdudlaşdırır; yan çevrilmiş ekran birbaşa cihazın öz vəziyyətidir, ona görə oxucudan çıxanda
// geri çevirmək istifadəçinin telefonunu «düzəltməyə» çalışmaq olardı.
@Composable
actual fun ReaderOrientationResetEffect() = Unit
