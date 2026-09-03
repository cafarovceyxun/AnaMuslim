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
     * Sistem [request] çağırışına **hələ də dialoqla** cavab verəcəkmi.
     *
     * `false` = «dialoq artıq işə yaramır, istifadəçini Ayarlara göndər».
     *
     * ⚠️ Android-də bu, `shouldShowRequestPermissionRationale`-ın birbaşa tərcüməsi **deyil**: o
     * bayraq həm «heç vaxt soruşulmayıb», həm də «daimi rədd» halında `false` verir. Actual onu
     * davamlı «bir dəfə soruşduq» bayrağı ilə birləşdirir — bax
     * [NotificationPermissionState.canPrompt].
     *
     * ℹ️ iOS burada onsuz da dəqiqdir: `CLLocationManager` statusu sinxron oxunur, ona görə «hələ
     * soruşulmayıb» halını bilirik və istifadəçini boş yerə Ayarlara göndərmirik.
     */
    val canPrompt: Boolean
}

/**
 * Yerləşmə icazəsinin vəziyyəti. Heç vaxt null qaytarmır — [rememberNotificationPermission]-dən
 * fərqli olaraq hər iki platformada, hər versiyada soruşulası bir şey var.
 */
@Composable
expect fun rememberLocationPermission(): LocationPermissionState
