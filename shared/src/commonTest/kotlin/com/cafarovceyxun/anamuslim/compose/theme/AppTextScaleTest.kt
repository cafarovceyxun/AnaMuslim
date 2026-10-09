package com.cafarovceyxun.anamuslim.compose.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class AppTextScaleTest {

    @Test
    fun systemScaleInsideRangeMapsToNearestStep() {
        assertEquals(100, AppTextScale.fromSystemFontScale(1.0f))
        assertEquals(80, AppTextScale.fromSystemFontScale(0.8f))
        assertEquals(130, AppTextScale.fromSystemFontScale(1.3f))
        // 1.33 → 133% → ən yaxın 5%-lik pillə 135.
        assertEquals(135, AppTextScale.fromSystemFontScale(1.33f))
    }

    @Test
    fun systemScaleOutsideRangeIsClamped() {
        // Samsung-un ən böyük şrifti 2.0×-dir, sürüşdürücü isə 150%-də bitir.
        assertEquals(AppTextScale.MAX_PERCENT, AppTextScale.fromSystemFontScale(2.0f))
        assertEquals(AppTextScale.MIN_PERCENT, AppTextScale.fromSystemFontScale(0.3f))
    }
}
