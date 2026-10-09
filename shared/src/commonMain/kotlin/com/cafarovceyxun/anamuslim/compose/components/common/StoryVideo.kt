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
 * [playbackSpeed] — `1` normal, `2` irəli sarınma (sağ tərəf basılı), **mənfi** dəyər geri sarınma
 * (sol tərəf, `-2`). Pleyerlər tərsinə oynamır (ExoPlayer heç, AVPlayer yalnız bəzi fayllarda), ona
 * görə geri sarınma video dayandırılıb mövqe addım-addım geri çəkilərək edilir ([STORY_REWIND_TICK_MILLIS]).
 * Zolaq videonun öz mövqeyi ilə getdiyi üçün o da sürətlənir və ya geri qayıdır.
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
    playbackSpeed: Float,
    onProgress: (Float) -> Unit,
    onFinished: () -> Unit,
)

/** Geri sarınmada mövqenin çəkilmə intervalı; addım `interval × |sürət|`-dür (2× = 240 ms). */
internal const val STORY_REWIND_TICK_MILLIS = 120L
