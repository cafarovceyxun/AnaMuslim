package com.cafarovceyxun.anamuslim.compose.utils.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.cafarovceyxun.anamuslim.utils.AndroidPlatformContext
import com.cafarovceyxun.anamuslim.utils.AppLogger

actual fun openAppSettings() {
    val context = AndroidPlatformContext.context

    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        // Required: the application context has no task of its own to launch into.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        AppLogger.saveError(e, "openAppSettings")
    }
}
