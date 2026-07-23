package com.cafarovceyxun.anamuslim.compose.utils

import platform.Foundation.NSByteCountFormatter
import platform.Foundation.NSByteCountFormatterCountStyleFile

actual fun formatFileSize(bytes: Long): String =
    NSByteCountFormatter.stringFromByteCount(bytes, NSByteCountFormatterCountStyleFile)
