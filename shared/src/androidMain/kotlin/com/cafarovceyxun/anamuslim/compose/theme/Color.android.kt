package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidColorSpace

/** Android-only bridge to the platform [android.graphics.Color] (uses the Android color space). */
fun Color.toAndroidColor(): android.graphics.Color =
    android.graphics.Color.valueOf(red, green, blue, alpha, colorSpace.toAndroidColorSpace())
