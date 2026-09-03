package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable

/**
 * Android-də «pil optimizasyonundan çıxar» sətri; iOS-da **heç nə çəkmir**.
 *
 * [ExactAlarmPrompt] ilə eyni forma və eyni səbəb: aqressiv OEM-lərdə (Xiaomi, Huawei, Samsung)
 * Doze və tətbiq təmizləyiciləri namaz siqnalını **səssizcə** öldürür — log təmiz, kompilyator
 * sakit, istifadəçi isə tətbiqi sınıq sayır.
 *
 * ⚠️ İcazə İSTƏNMİR. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + birbaşa dialoq bir toxunuşluq olardı,
 * amma Play həmin icazəni məhdud siyahı ilə idarə edir — manifestdə `USE_EXACT_ALARM`-ın olmama
 * səbəbi ilə eyni. Əvəzinə sistemin **siyahısı** açılır və istifadəçi tətbiqi özü seçir.
 *
 * Ton **xəta tonu deyil**: optimizasyon aktikən də bildirişlər gəlir, sadəcə Doze-da gecikə bilər.
 */
@Composable
expect fun BatteryOptimizationPrompt()
