package com.cafarovceyxun.anamuslim.compose.components.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Hekayədəki videonu oynadan səth — səssiz deyil, tam ekran, idarəetmə düymələri olmadan.
 *
 * Compose Multiplatform-da hazır video komponenti yoxdur, ona görə expect/actual: Android-də
 * media3 `ExoPlayer` (tətbiq onsuz da media3 işlədir, yeni versiya gətirilmir), iOS-da isə
 * `AVPlayerViewController`.
 *
 * [paused] barmaq ekranda saxlananda (və ya hekayə ortadan toxunuşla dayandırılanda) `true` olur:
 * zolaq dayanırsa video da dayanmalıdır, yoxsa davam edən səs donmuş zolaqla uyuşmur.
 *
 * [onFinished] video bitəndə çağırılır — hekayə zolağı növbəti slayda məhz bununla keçir, sabit
 * taymerlə yox. [onProgress] isə 0..1 aralığında oynatma mövqeyidir: yuxarıdakı zolaq videonun öz
 * vaxtını göstərsin deyə lazımdır (şəkil slaydında zolağı animasiya doldurur).
 */
@Composable
expect fun StoryVideo(
    url: String,
    modifier: Modifier,
    paused: Boolean,
    onProgress: (Float) -> Unit,
    onFinished: () -> Unit,
)
