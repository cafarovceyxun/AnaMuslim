package com.cafarovceyxun.anamuslim.compose.utils

import android.text.format.Formatter
import com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext

actual fun formatFileSize(bytes: Long): String =
    Formatter.formatFileSize(AndroidPlatformContext.context, bytes)
