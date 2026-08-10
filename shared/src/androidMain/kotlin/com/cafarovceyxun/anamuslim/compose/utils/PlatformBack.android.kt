package com.cafarovceyxun.anamuslim.compose.utils

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable

@Composable
actual fun rememberSystemBack(): (() -> Unit)? {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    return dispatcher?.let { d -> { d.onBackPressed() } }
}
