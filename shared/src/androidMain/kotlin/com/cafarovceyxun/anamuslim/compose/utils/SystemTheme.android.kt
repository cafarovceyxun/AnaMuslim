package com.cafarovceyxun.anamuslim.compose.utils

import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext

actual fun isSystemInDarkMode(): Boolean {
    val uiMode = AndroidPlatformContext.context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK
    return uiMode == Configuration.UI_MODE_NIGHT_YES
}

actual fun platformDynamicColorScheme(dark: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = AndroidPlatformContext.context
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
