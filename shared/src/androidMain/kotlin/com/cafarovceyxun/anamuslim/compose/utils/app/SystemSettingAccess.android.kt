package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * [check]-i indi və hər `ON_RESUME`-da yenidən oxuyur: istifadəçi sistem ayarından icazəni verib
 * qayıda bilər — xəbərdarlıq sətri dərhal itməlidir.
 */
@Composable
internal fun rememberCheckedOnResume(check: () -> Boolean): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestCheck by rememberUpdatedState(check)
    var value by remember { mutableStateOf(check()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) value = latestCheck()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return value
}
