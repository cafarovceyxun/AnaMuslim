package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable

/**
 * Android 12+-da «Siqnallar və xatırlatmalar» (`SCHEDULE_EXACT_ALARM`) icazəsi; Android 12−-də və
 * iOS-da `null` (anlayış yoxdur, ya da icazə avtomatikdir).
 *
 * ⚠️ targetSdk 36 olduğu üçün icazə Android 14+-da yeni quraşdırmaya default **verilmir** — yəni
 * istifadəçilərin çoxu qeyri-dəqiq yoldadır və bildiriş **bir saata qədər** gecikə bilər. İcazə
 * verilən kimi namaz siqnalı `setAlarmClock`-a keçir (Android: `PrayerAlarmScheduler`).
 */
@Composable
expect fun rememberExactAlarmAccess(): SystemSettingAccess?
