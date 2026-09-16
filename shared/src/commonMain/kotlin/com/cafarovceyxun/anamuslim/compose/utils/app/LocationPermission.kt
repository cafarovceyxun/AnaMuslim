package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Namaz vaxtları üçün **kobud** yerləşmə icazəsi — tətbiqin ikinci (və sonuncu) runtime icazəsi.
 *
 * [NotificationPermissionState] ilə eyni formadadır ki, `PrayerSettingsSheet` `DailyReminderSheet`
 * naxışını olduğu kimi təkrarlaya bilsin.
 *
 * ℹ️ **Həm kobud, həm dəqiq istənilir (2026-09-16-dan).** Əvvəl yalnız kobud istənirdi — namaz
 * vaxtları üçün bu doğru qərardır (1 dəqiqə vaxt ≈ 25 km). Qiblə isə fərqlidir: Kəbəyə yaxın
 * yerlərdə kobud mövqenin ~2 km-lik xətası bucağı onlarla dərəcə oynadır, yəni funksiya
 * işləmir. Sistem dialoqunda istifadəçi yenə «Təxmini»ni seçə bilər — [isPrecise] həmin halı
 * bildirir və UI xəbərdarlıq göstərir.
 *
 * Fon icazəsi ümumiyyətlə istənmir — mövqe yalnız ön planda alınır.
 */
@Stable
interface LocationPermissionState {
    /** Mövqe hazırda oxuna bilirmi. */
    val isGranted: Boolean

    /**
     * Mövqe **dəqiqdir** (GPS), yoxsa sistem tərəfindən kobudlaşdırılıb.
     *
     * ⚠️ Bu fərq qiblə üçün həlledicidir. Android kobud mövqeni ~2 km-lik şəbəkəyə yuvarlaqlaşdırır
     * və dəyər oxunuşdan-oxunuşa dəyişir (2026-09-16-da Məkkədə ölçüldü: iki ardıcıl oxunuş
     * arasında **2008 m**, qiblə bucağında **62°** fərq). Kəbədən min kilometrlərlə uzaqda bunun
     * əhəmiyyəti yoxdur, bir-iki kilometr məsafədə isə bucağı tamamilə mənasız edir.
     */
    val isPrecise: Boolean get() = isGranted

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
