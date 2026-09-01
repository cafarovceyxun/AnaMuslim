package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable

/**
 * Android 12+-da «dəqiq siqnala icazə ver» sətri; iOS-da **heç nə çəkmir**.
 *
 * ⚠️ targetSdk 36 olduğu üçün `SCHEDULE_EXACT_ALARM` Android 14+-da default **verilmir** — yəni
 * istifadəçilərin çoxu qeyri-dəqiq yoldadır və bildiriş bir neçə dəqiqə sapa bilər. Bu sətir həmin
 * istifadəçiyə bir toxunuşluq həll təklif edir.
 *
 * Ton **xəta tonu deyil**: qeyri-dəqiq rejim də işləyir, sadəcə dəqiq deyil. `expect/actual`
 * seçildi, çünki alternativ (`Boolean` bayraq + UI-də şərt) platforma detalını paylaşılan ekrana
 * sızdırardı.
 */
@Composable
expect fun ExactAlarmPrompt()
