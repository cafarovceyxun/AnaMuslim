package com.cafarovceyxun.anamuslim.compose.utils.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
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
 * `NotificationPermission.android.kt`-nin birə-bir güzgüsü — eyni `RequestPermission` kontraktı,
 * eyni `ON_RESUME` yenidən oxuması (istifadəçi Ayarlarda icazəni dəyişib qayıda bilər).
 */
@Composable
actual fun rememberLocationPermission(): LocationPermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun hasPermission(name: String): Boolean =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED

    fun currentlyGranted(): Boolean = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun currentlyPrecise(): Boolean = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    fun currentlyShouldShowRationale(): Boolean {
        val activity = context as? Activity ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    var granted by remember(context) { mutableStateOf(currentlyGranted()) }
    var precise by remember(context) { mutableStateOf(currentlyPrecise()) }
    var rationale by remember(context) { mutableStateOf(currentlyShouldShowRationale()) }

    // Bax `NotificationPermission.android.kt` — eyni «bir dəfə soruşduq» nüsxəsi.
    var asked by remember(context) { mutableStateOf(AppPreferences.getLocationPermissionAsked()) }

    // ⚠️ `RequestMultiplePermissions`: Android 12+ dəqiq icazəni **tək başına** verməyi qəbul
    // etmir — FINE həmişə COARSE ilə birlikdə istənilməlidir, yoxsa dialoq açılmır və sorğu
    // səssizcə rədd olunur.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        granted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || currentlyGranted()
        precise = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || currentlyPrecise()
        rationale = currentlyShouldShowRationale()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = currentlyGranted()
                precise = currentlyPrecise()
                rationale = currentlyShouldShowRationale()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(launcher) {
        object : LocationPermissionState {
            override val isGranted: Boolean get() = granted

            override val isPrecise: Boolean get() = precise

            // Heç soruşmamışıqsa sistem mütləq dialoq göstərəcək; soruşmuşuqsa qərar
            // `shouldShowRequestPermissionRationale`-dadır. Bax [NotificationPermissionState.canPrompt].
            override val canPrompt: Boolean get() = !asked || rationale

            // Hər ikisi birlikdə istənilir — səbəb yuxarıdakı launcher şərhindədir. İstifadəçi
            // dialoqda yenə «Təxmini»ni seçə bilər; o zaman [isPrecise] false qalır və qiblə
            // ekranı xəbərdarlıq göstərir.
            override fun request() {
                asked = true
                AppPreferences.markLocationPermissionAsked()
                launcher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }
}
