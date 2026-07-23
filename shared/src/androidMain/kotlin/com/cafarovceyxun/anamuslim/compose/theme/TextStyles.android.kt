package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle

actual val tightTextStyle: TextStyle = TextStyle(
    lineHeightStyle = LineHeightStyle.Default.copy(
        mode = LineHeightStyle.Mode.Tight,
        alignment = LineHeightStyle.Alignment.Center,
    )
)

actual val arabicReaderLineHeightStyle: LineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
    mode = LineHeightStyle.Mode.Tight,
)

actual val translationPlatformTextStyle: PlatformTextStyle? =
    PlatformTextStyle(includeFontPadding = true)
