package com.cafarovceyxun.anamuslim.compose.utils.app

import androidx.compose.runtime.Composable

/**
 * Applies (and reverts) the reader's immersive-fullscreen chrome for the given [fullscreen] state:
 * on Android it hides/shows the system bars with a swipe-to-reveal behavior; iOS has no equivalent
 * system-bar control and no-ops (Faza 6 may revisit for the status bar).
 */
@Composable
expect fun ReaderFullscreenEffect(fullscreen: Boolean)

/**
 * A callback that flips the screen between portrait and landscape, or `null` where the platform
 * has no app-controlled orientation lock (iOS). Android returns a lambda bound to the current
 * Activity; the reader shows its rotation button only when this is non-null.
 */
@Composable
expect fun rememberToggleScreenRotation(): (() -> Unit)?

/**
 * Oxucu tərk ediləndə ekranı sistemin öz istiqamət seçiminə qaytarır.
 *
 * [rememberToggleScreenRotation] Android-də **Activity**-ni kilidləyir, oxucu isə `MainActivity`-nin
 * naviqasiya qrafikindəki adi bir ekrandır: kilid oxucudan çıxandan sonra da qalırdı və ana ekran,
 * ayarlar, axtarış — hamısı yan çevrilmiş açılırdı (hədis oxucusu və istinad ekranı bu tələyə
 * düşmür, onlar ayrıca Activity-dədir və kilid Activity ilə birgə ölür). iOS-da tətbiqin idarə etdiyi
 * fırlanma kilidi yoxdur (istiqamət cihazın öz vəziyyətindən gəlir), ona görə orada no-op-dur.
 */
@Composable
expect fun ReaderOrientationResetEffect()
