package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Namaz vaxtları üçün **kobud** yerləşmə icazəsi — tətbiqin ikinci (və sonuncu) runtime icazəsi.
 *
 * [NotificationPermissionState] ilə eyni formadadır ki, `PrayerSettingsSheet` `DailyReminderSheet`
 * naxışını olduğu kimi təkrarlaya bilsin.
 *
 * ⚠️ Yalnız **kobud** dəqiqlik istənilir (`ACCESS_COARSE_LOCATION` / `WhenInUse`): 1 dəqiqə vaxt
 * ≈ 25 km, ona görə dəqiq mövqe həm lazımsızdır, həm də dialoqu sərtləşdirir və mağaza
 * bəyannaməsini ağırlaşdırır. Fon icazəsi ümumiyyətlə istənmir — mövqe yalnız ön planda alınır.
 */
@Stable
interface LocationPermissionState {
    /** Mövqe hazırda oxuna bilirmi. */
    val isGranted: Boolean

    /** Sistem dialoqunu göstərir. Cavab [isGranted] vasitəsilə gəlir, dərhal deyil. */
    fun request()

    /**
     * Sistem hələ də dialoq göstərəcəkmi.
     *
     * `false` = «dialoq artıq işə yaramır, istifadəçini Ayarlara göndər». Android-də bu, bir dəfə
     * rədd edildikdən sonrakı vəziyyətdir; iOS-da isə status `notDetermined` olmayanda —
     * orada sistem quraşdırma başına yalnız bir dəfə soruşur.
     *
     * ℹ️ [NotificationPermissionState.shouldShowRationale]-dən fərqli olaraq iOS burada həmişə
     * `false` qaytarmır: `CLLocationManager` statusu sinxron oxunur, ona görə «hələ soruşulmayıb»
     * halını dəqiq bilirik və istifadəçini boş yerə Ayarlara göndərmirik.
     */
    val shouldShowRationale: Boolean
}

/**
 * Yerləşmə icazəsinin vəziyyəti. Heç vaxt null qaytarmır — [rememberNotificationPermission]-dən
 * fərqli olaraq hər iki platformada, hər versiyada soruşulası bir şey var.
 */
@Composable
expect fun rememberLocationPermission(): LocationPermissionState
