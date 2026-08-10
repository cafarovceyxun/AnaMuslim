package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOn(timeoutMillis: Long) {
    DisposableEffect(Unit) {
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose {
            UIApplication.sharedApplication.idleTimerDisabled = false
        }
    }

    LaunchedEffect(Unit) {
        delay(timeoutMillis)
        UIApplication.sharedApplication.idleTimerDisabled = false
    }
}
