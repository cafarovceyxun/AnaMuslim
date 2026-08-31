package com.cafarovceyxun.anamuslim.utils.prayer.location

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint

/**
 * Cihazın hazırkı mövqeyi (kobud dəqiqlik), və ya alınmadıqda **null**.
 *
 * Provider seam-i deyil, adi `expect/actual`: hər iki platformada tətbiq yalnız sistem çərçivəsinə
 * söykənir (`LocationManager` / `CLLocationManager`), `:app` modulundakı heç nəyə ehtiyac yoxdur —
 * yəni `DailyReminderScheduler`-i seam etməyə məcbur edən səbəb burada mövcud deyil.
 *
 * 🔴 **Yalnız ön plandan çağırılır.** Alarm receiver-i, worker və `IosPrayerReminder` mövqeyə
 * toxunmur — hamısı `PrayerPreferences`-də saxlanmış koordinatı oxuyur. Üç nəticəsi var:
 * `ACCESS_BACKGROUND_LOCATION` lazım deyil, receiver-də `Context` problemi yoxdur, və qapalı yerdə
 * bildiriş mövqe gözləyib gecikmir.
 *
 * ℹ️ Dəqiqlik qəsdən kobuddur: 1 dəqiqə vaxt ≈ 25 km, ona görə şəhər səviyyəsi kifayətdir.
 * `play-services-location` **əlavə edilmir** — yeni asılılıq versiya sürüşməsi riski gətirir
 * (CLAUDE.md), çərçivə API-si isə bu dəqiqlik üçün tamamilə yetərlidir.
 *
 * Hündürlük adətən şəbəkə mövqeyində gəlmir; boş qalanda `elevationMeters = 0.0` olur və çağıran
 * tərəf onu [CityCatalog.nearest] ilə tapılan şəhərin hündürlüyü ilə əvəz edir.
 */
expect suspend fun currentDeviceLocation(): GeoPoint?
