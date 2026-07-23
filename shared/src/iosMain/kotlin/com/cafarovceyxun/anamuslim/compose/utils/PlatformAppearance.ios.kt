package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.runtime.Composable

@Composable
actual fun SystemAppearance(isDark: Boolean) {
    // No-op on iOS. The status-bar tint is set via the host VC's trait; SwiftUI's only lever is
    // `.preferredColorScheme`, which pins that trait and would feed back into "system" theme mode
    // (see IosSystemChrome). The default style already follows the app's appearance, so this is fine.
}
