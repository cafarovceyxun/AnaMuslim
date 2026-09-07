package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable

/**
 * Pəncərəni çağıran kompozisiya yaşadığı müddətdə **portret**də saxlayır, dispose olanda əvvəlki
 * vəziyyəti geri qaytarır.
 *
 * Şəkil redaktoru üçün yazılıb: kart 9:16 / 4:5 / 1:1 nisbətindədir və alət paneli önizləmənin
 * altında dayanır — landşaftda önizləmə bir neçə santimetrə enir, panel isə kadrı yeyir. Yəni
 * fırlanma ekranı sadəcə «başqa cür» yox, **istifadəsiz** edir.
 *
 * ⚠️ Bu bir görünüş kilidi deyil, **platforma sorğusudur**: Android-də Activity-nin
 * `requestedOrientation`-u, iOS-da isə səhnənin dəstəklədiyi istiqamətlər dəyişir. Bölünmüş
 * ekranda / iPad çoxvəzifəliliyində sistem sorğunu nəzərə almaya bilər, ona görə redaktorun özü
 * landşaftda da düzgün düzülməlidir (bax `ShareImageEditorScreen`-in `isLandscape()` qolu) —
 * kilid rahatlıqdır, zəmanət deyil.
 */
@Composable
expect fun PortraitLockEffect()
