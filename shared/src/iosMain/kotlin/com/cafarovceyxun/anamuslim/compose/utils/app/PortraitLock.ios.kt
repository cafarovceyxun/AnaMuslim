package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.cafarovceyxun.anamuslim.compose.utils.IosSystemChrome

@Composable
actual fun PortraitLockEffect() {
    // Səhnənin dəstəklədiyi istiqamətlər `UIApplicationDelegate`-dədir, yəni Compose-dan əlçatmaz —
    // status zolağı ilə eyni səbəb, ona görə eyni körpü işlədilir. Swift tərəfi maskanı dəyişib
    // `requestGeometryUpdate` çağırır.
    DisposableEffect(Unit) {
        IosSystemChrome.setPortraitLocked(true)
        onDispose { IosSystemChrome.setPortraitLocked(false) }
    }
}
