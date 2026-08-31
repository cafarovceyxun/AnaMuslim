package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cafarovceyxun.anamuslim.utils.prayer.location.IosLocationAuthorization

/**
 * `CLLocationManager` statusu sinxron oxunduğu üçün burada bildiriş icazəsindəki boşluq yoxdur:
 * status delegate vasitəsilə **canlı** izlənir, yəni istifadəçi dialoqa cavab verən kimi ekran
 * yenilənir və Ayarlardan qayıdanda da doğru vəziyyət görünür.
 */
@Composable
actual fun rememberLocationPermission(): LocationPermissionState {
    var granted by remember { mutableStateOf(IosLocationAuthorization.isGranted()) }
    var canPrompt by remember { mutableStateOf(IosLocationAuthorization.canPrompt()) }

    DisposableEffect(Unit) {
        IosLocationAuthorization.observeAuthorization {
            granted = IosLocationAuthorization.isGranted()
            canPrompt = IosLocationAuthorization.canPrompt()
        }
        onDispose { IosLocationAuthorization.observeAuthorization(null) }
    }

    return remember {
        object : LocationPermissionState {
            override val isGranted: Boolean get() = granted

            // iOS quraşdırma başına bir dəfə soruşur: status `notDetermined` deyilsə yeganə yol
            // Ayarlardır, çağıran tərəf də məhz bunu oxuyub istifadəçini ora yönləndirir.
            override val shouldShowRationale: Boolean get() = canPrompt

            override fun request() = IosLocationAuthorization.requestAuthorization()
        }
    }
}
