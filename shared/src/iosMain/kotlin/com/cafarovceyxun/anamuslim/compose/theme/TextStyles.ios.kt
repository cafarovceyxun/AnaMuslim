package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle

actual val tightTextStyle: TextStyle = TextStyle(
    lineHeightStyle = LineHeightStyle.Default.copy(
        alignment = LineHeightStyle.Alignment.Center,
    )
)

// Compose Multiplatform 1.8 iOS ui-text has no LineHeightStyle.Mode; use the closest supported config.
actual val arabicReaderLineHeightStyle: LineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

// iOS has no font-padding concept.
actual val translationPlatformTextStyle: PlatformTextStyle? = null
