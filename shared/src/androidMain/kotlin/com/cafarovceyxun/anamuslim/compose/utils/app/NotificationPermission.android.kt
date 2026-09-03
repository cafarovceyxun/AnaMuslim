package com.cafarovceyxun.anamuslim.compose.utils.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences

/**
 * Built on the `RequestPermission` activity-result contract rather than Accompanist, so `shared`
 * needs no new dependency (activity-compose is already here for `rememberSystemBack`).
 *
 * Like Accompanist, the granted state is re-read on `ON_RESUME`: the user can flip the permission
 * in system settings and return to the app without the prompt callback ever firing.
 */
@Composable
actual fun rememberNotificationPermission(): NotificationPermissionState? {
    // Below Android 13 notifications need no runtime grant — nothing to ask for.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun currentlyGranted(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED

    // Only an Activity can answer this; without one the caller falls back to "send to Settings".
    fun currentlyShouldShowRationale(): Boolean {
        val activity = context as? Activity ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    }

    var granted by remember(context) { mutableStateOf(currentlyGranted()) }
    var rationale by remember(context) { mutableStateOf(currentlyShouldShowRationale()) }

    // «Bir dəfə soruşduq» bayrağının kompozisiya daxili nüsxəsi: `request()` çağırılan kimi sinxron
    // qalxır, ona görə istifadəçi dialoqa dərhal cavab versə də DataStore yazısı ilə yarış yoxdur.
    var asked by remember(context) { mutableStateOf(AppPreferences.getNotificationPermissionAsked()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted = isGranted
        // A denial is what flips this, so it must be re-read after the prompt closes.
        rationale = currentlyShouldShowRationale()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = currentlyGranted()
                rationale = currentlyShouldShowRationale()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(launcher) {
        object : NotificationPermissionState {
            override val isGranted: Boolean get() = granted

            // Heç soruşmamışıqsa sistem MÜTLƏQ dialoq göstərəcək — `shouldShowRequestPermissionRationale`
            // orada da `false`-dur, ona görə tək başına oxunsa istifadəçi dialoqu heç vaxt görmür.
            // Soruşmuşuqsa qərar həmin bayraqdadır: bir rəddən sonra `true`, daimi rəddən sonra `false`.
            override val canPrompt: Boolean get() = !asked || rationale

            override fun request() {
                asked = true
                AppPreferences.markNotificationPermissionAsked()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
