package com.cafarovceyxun.anamuslim.compose.utils.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun PortraitLockEffect() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity()
        if (activity == null) {
            onDispose {}
        } else {
            // Əvvəlki dəyər yadda saxlanılır, `SCREEN_ORIENTATION_UNSPECIFIED` fərz edilmir:
            // oxucunun fırlatma düyməsi ([toggleScreenRotation]) Activity-ni artıq landşafta kilidləmiş
            // ola bilər və redaktordan çıxanda istifadəçi məhz oraya qayıtmalıdır.
            val previous = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onDispose { activity.requestedOrientation = previous }
        }
    }
}

/**
 * `LocalContext` adətən elə Activity-nin özüdür, amma redaktor `Dialog` içindədir və dialoq
 * pəncərəsinin konteksti sarğılana bilər — `as? Activity` belə halda səssizcə `null` verib kilidi
 * işləməz edərdi.
 */
private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
