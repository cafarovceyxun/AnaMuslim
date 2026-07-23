package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle

/**
 * Centered, tight line-height style used across list/title UI.
 *
 * `LineHeightStyle.Mode.Tight` only exists in androidx.compose ui-text 1.10+, which the Android
 * target resolves; the Compose Multiplatform 1.8.2 iOS artifacts still ship ui-text 1.8's API,
 * where `Mode` has only `Fixed`/`Minimum`. The actuals keep Android byte-for-byte identical and
 * give iOS the closest supported configuration.
 */
expect val tightTextStyle: TextStyle

/**
 * Line-height style for the Arabic Quran text (centered, trimmed both ends, tight mode on Android).
 * Same `Mode.Tight`-only-on-1.10+ seam as [tightTextStyle]: Android keeps the tight mode, iOS drops it.
 */
expect val arabicReaderLineHeightStyle: LineHeightStyle

/**
 * Platform text style for translation body text, opting into legacy font padding on Android
 * (`includeFontPadding = true`, which differs from Compose's `false` default); `null` on iOS,
 * which has no font-padding concept.
 */
expect val translationPlatformTextStyle: PlatformTextStyle?
