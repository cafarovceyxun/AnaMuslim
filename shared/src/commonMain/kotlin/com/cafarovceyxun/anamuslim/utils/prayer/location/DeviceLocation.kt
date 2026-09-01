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
 * Hündürlük adətən şəbəkə mövqeyində gəlmir; boş qalanda `elevationMeters = 0.0` qalır. Bu,
 * praktikada əhəmiyyətsizdir, çünki hündürlük düzəlişi
 * ([com.cafarovceyxun.anamuslim.utils.prayer.PrayerParams.useElevation]) default **sönülüdür**.
 */
expect suspend fun currentDeviceLocation(): GeoPoint?

/**
 * [point] koordinatının insan oxuya biləcəyi adı, və ya tapılmadıqda **null**.
 *
 * ⚠️ Əvvəl bu iş daxildəki şəhər kataloqunun «ən yaxın şəhər» axtarışı ilə görülürdü və **səhv idi**:
 * kataloqda yalnız əhalisi 200 000+ olan şəhərlər var, ona görə Gədəbəydə (~10 min əhali) 30 km
 * uzaqdakı Şəmkir yapışdırılırdı. Platformanın öz geocoder-i həmin yeri düzgün adlandırır.
 *
 * 🔴 **Məxfilik:** bu, koordinatı cihazdan **çıxarır** — Android `Geocoder` Google-a, iOS
 * `CLGeocoder` Apple-a sorğu göndərir. Yalnız istifadəçi «Mövcud yerimi işlət» düyməsinə basanda,
 * yer başına bir dəfə baş verir; namaz **hesablaması** tamamilə cihazdadır və şəbəkə tələb etmir.
 * `PRIVACY.md` bunu açıq yazır.
 *
 * Uğursuzluq (şəbəkə yox, xidmət əlçatmaz, nəticə boş) **null** qaytarır — funksiya yıxılmır,
 * çağıran tərəf koordinatı etiket kimi göstərir.
 */
expect suspend fun reverseGeocode(point: GeoPoint): String?
