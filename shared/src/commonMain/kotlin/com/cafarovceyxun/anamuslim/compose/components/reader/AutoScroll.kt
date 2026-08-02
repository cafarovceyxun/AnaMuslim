package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos

/**
 * Avtomatik sürüşdürmənin yeganə "səviyyə → piksel" mənbəyi.
 *
 * Əvvəl bu çevirmə altı yerdə əl ilə yazılmışdı (`speed / 2f`, `step * 0.4f` və bir yerdə `0.5f`),
 * ona görə vərəqdən başlayanda və jest rejimində eyni "1x" fərqli sürət verirdi. Sürət vahidi
 * **60 fps kadrına düşən piksel**-dir; [AutoScrollEffect] onu real kadr müddətinə uyğunlaşdırır.
 */
object AutoScroll {

    /** Vərəqdəki sürət sürüşdürücüsünün aralığı (1x–15x). */
    const val MIN_LEVEL = 1f
    const val MAX_LEVEL = 15f

    /** `Slider` uc nöqtələr arasındakı dayanacaqları sayır, ona görə tam ədəd sayı − 2. */
    const val LEVEL_STEPS = (MAX_LEVEL - MIN_LEVEL).toInt() - 1

    /** Jest rejimində yuxarı/aşağı sürüşdürmə ilə dəyişən addım aralığı. */
    const val MIN_STEP = 1
    const val MAX_STEP = 30

    fun speedOfLevel(level: Float): Float = level / 2f

    fun speedOfStep(step: Int): Float = step * 0.4f
}

/** 60 fps-də bir kadrın uzunluğu — sürət vahidinin təməli. */
private const val FRAME_NANOS = 16_666_666f

/**
 * Bütün avtomatik sürüşdürmə ekranlarının (ayə oxuyucusu, şaquli tərcümə, hədis siyahısı) ortaq
 * sürücüsü.
 *
 * İki səbəbdən `withFrameNanos`, `delay(16)` yox:
 * 1. `delay` kadr saatına bağlı deyil — 120 Hz ekranda sürüşmə kadrların arasına düşür və mətn
 *    titrəyir; kadr gecikəndə isə sürət səssizcə aşağı düşür.
 * 2. Sürət keçən vaxta vurulduğu üçün eyni "5x" bütün cihazlarda eyni sürətdir.
 *
 * Siyahının sonuna çatanda [onFinished] çağırılır — əvvəl döngə boş-boşuna fırlanır və düymə
 * "işləyir" görünürdü.
 */
@Composable
fun AutoScrollEffect(
    state: ScrollableState,
    speed: Float?,
    enabled: Boolean = true,
    onFinished: () -> Unit,
) {
    LaunchedEffect(state, speed, enabled) {
        if (!enabled || speed == null || speed <= 0f) return@LaunchedEffect

        var previousFrame = 0L
        while (true) {
            val frame = withFrameNanos { it }
            val elapsed = if (previousFrame == 0L) FRAME_NANOS else (frame - previousFrame).toFloat()
            previousFrame = frame

            // Ekran sönüb yanandan və ya GC fasiləsindən sonra bir kadrda səhifə boyu sıçramasın.
            val frames = (elapsed / FRAME_NANOS).coerceIn(0.5f, 3f)

            if (state.scrollBy(speed * frames) <= 0f && !state.canScrollForward) {
                onFinished()
                return@LaunchedEffect
            }
        }
    }
}
