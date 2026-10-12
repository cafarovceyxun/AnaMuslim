package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable

/**
 * Android-də «pil optimizasyonundan çıxarılıb» vəziyyəti; iOS-da `null`.
 *
 * Namaz siqnalı `setAlarmClock` olduğu üçün Doze onu onsuz da gecikdirmir. Bu, aqressiv OEM-lər
 * (Xiaomi, Huawei, Samsung) üçündür: onların təmizləyiciləri tətbiqi arxa fonda dayandırır və
 * siqnal **səssizcə** düşür — log təmiz, kompilyator sakit, istifadəçi isə tətbiqi sınıq sayır.
 * Əlavə fayda: pil istisnası olan tətbiq üçün `canScheduleExactAlarms()` da `true` qaytarır.
 *
 * ⚠️ İcazə İSTƏNMİR. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + birbaşa dialoq bir toxunuşluq olardı,
 * amma Play həmin icazəni məhdud siyahı ilə idarə edir — manifestdə `USE_EXACT_ALARM`-ın olmama
 * səbəbi ilə eyni. Əvəzinə sistemin **siyahısı** açılır və istifadəçi tətbiqi özü seçir.
 */
@Composable
expect fun rememberBatteryOptimizationAccess(): SystemSettingAccess?
