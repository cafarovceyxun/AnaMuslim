package com.cafarovceyxun.anamuslim.compose.utils.app

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun ReaderFullscreenEffect(fullscreen: Boolean) {
    val view = LocalView.current
    LaunchedEffect(fullscreen) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
actual fun rememberToggleScreenRotation(): (() -> Unit)? {
    val activity = LocalContext.current as? Activity ?: return null
    return { toggleScreenRotation(activity) }
}

@Composable
actual fun ReaderOrientationResetEffect() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity()
        onDispose {
            if (activity == null) return@onDispose

            // Fırlanmanın özü artıq Activity-ni yenidən qurmur (manifestdəki `configChanges`),
            // amma dil və qaranlıq rejim kimi dəyişikliklər hələ də qurur — orada dispose «oxucudan
            // çıxış» demək deyil, kilidi açmaq isə ekranı gözlənilmədən geri çevirərdi. Bağlanan
            // Activity-də kilid onsuz da ölür. Ona görə yalnız həqiqi çıxış sıfırlanır.
            if (activity.isChangingConfigurations || activity.isFinishing) return@onDispose

            // Girişdəki dəyər yox, **manifest defoltu** geri qaytarılır: fırlanma Activity-ni
            // yenidən qurduğu üçün ikinci kompozisiya artıq kilidlənmiş vəziyyəti «əvvəlki» kimi
            // görür və onu bərpa etmək kilidi əbədi edərdi. `MainActivity` heç bir
            // `screenOrientation` elan etmir, yəni sistem seçimi elə budur.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
