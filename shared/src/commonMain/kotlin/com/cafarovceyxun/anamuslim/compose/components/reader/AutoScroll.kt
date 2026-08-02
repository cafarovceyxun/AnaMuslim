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

    /**
     * Sürət aralığı: 0.5x–15x. Aşağı ucu köhnə 1x-in yarısıdır (uzun ayələri oxuyarkən mətn
     * gözdən qaçmasın), yuxarı ucu isə köhnə 10x-dən 50% böyükdür.
     */
    const val MIN_LEVEL = 0.5f
    const val MAX_LEVEL = 15f
    const val LEVEL_INCREMENT = 0.5f

    /** `Slider` uc nöqtələr arasındakı dayanacaqları sayır. */
    const val LEVEL_STEPS = ((MAX_LEVEL - MIN_LEVEL) / LEVEL_INCREMENT).toInt() - 1

    /** Jest rejimi eyni nərdivanı tam ədəd indekslə gəzir: addım 1 = 0.5x, addım 30 = 15x. */
    const val MIN_STEP = 1
    const val MAX_STEP = ((MAX_LEVEL / LEVEL_INCREMENT).toInt())

    fun levelOfStep(step: Int): Float =
        (step * LEVEL_INCREMENT).coerceIn(MIN_LEVEL, MAX_LEVEL)

    /**
     * Səviyyə → 60 fps kadrına düşən piksel. Bölən köhnə `2f` deyil, `8f`-dir: bütün nərdivan
     * 4 dəfə yavaşladıldı, etiketlər ("1x", "15x") isə olduğu kimi qaldı.
     */
    fun speedOfLevel(level: Float): Float = level / 8f

    fun speedOfStep(step: Int): Float = speedOfLevel(levelOfStep(step))

    /** `7f` → "7x", `2.5f` → "2.5x" — sürüşdürücüdə və HUD-da eyni yazılış. */
    fun levelLabel(level: Float): String {
        val whole = level.toInt()
        return if (level == whole.toFloat()) "${whole}x" else "${level}x"
    }
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
    onFinished: () -> Unit,
) {
    LaunchedEffect(state, speed) {
        if (speed == null || speed <= 0f) return@LaunchedEffect

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
