package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

/** Şəkil redaktorunun yerləşdirmə riyaziyyatı — ekransız (ImageBitmap-sız) sınanır. */
class StoryImageTransformTest {

    @Test
    fun `initial state is fit at zero degrees`() {
        assertEquals(StoryImageTransform.Initial, StoryImageTransform.fit(1080, 2400, 0f))
    }

    @Test
    fun `fill covers the 9x16 frame`() {
        // 1080×2400 ekran görüntüsü 9:16-dan hündürdür: sığdır hündürlüyə, doldur enə görədir.
        val fill = StoryImageTransform.fill(1080, 2400, 0f)
        assertEquals(2400f / 1920f, fill.scale, 1e-4f)
    }

    @Test
    fun `quarter turn fits the swapped dimensions`() {
        // Landşaft 1920×1080 fırlananda 1080×1920 olur — çərçivəyə tam düşür.
        val fit = StoryImageTransform.fit(1920, 1080, 90f)
        val base = minOf(9f / 16f / 1920f, 1f / 1080f)
        val target = minOf(9f / 16f / 1080f, 1f / 1920f)
        assertEquals(target / base, fit.scale, 1e-4f)
        assertEquals(90f, fit.rotation)
    }

    @Test
    fun `pinch keeps the point under the fingers in place`() {
        val start = StoryImageTransform(offsetX = 0.1f, offsetY = 0f)
        val frameWidth = 1000f
        val centroid = Offset(100f, 0f) // düz şəkil mərkəzinin üstündə
        val zoomed = start.applyGesture(centroid, Offset.Zero, zoom = 2f, rotation = 0f, frameWidth = frameWidth)
        assertEquals(2f, zoomed.scale, 1e-5f)
        assertEquals(0.1f, zoomed.offsetX, 1e-5f)
    }

    @Test
    fun `rotation around an off-center point moves the image`() {
        val rotated = StoryImageTransform.Initial.applyGesture(
            centroid = Offset(100f, 0f),
            pan = Offset.Zero,
            zoom = 1f,
            rotation = 90f,
            frameWidth = 1000f,
        )
        // Mərkəz (0,0) nöqtəsi (100,0) ətrafında 90° saat əqrəbi ilə → (100,-100).
        assertEquals(0.1f, rotated.offsetX, 1e-4f)
        assertEquals(-0.1f, rotated.offsetY, 1e-4f)
        assertEquals(90f, rotated.rotation)
    }

    @Test
    fun `scale is clamped`() {
        val huge = StoryImageTransform.Initial.applyGesture(Offset.Zero, Offset.Zero, 100f, 0f, 1000f)
        assertEquals(10f, huge.scale)
    }

    @Test
    fun `near right angle snaps and far does not`() {
        assertEquals(90f, StoryImageTransform(rotation = 86f).snapRotation().rotation)
        assertEquals(45f, StoryImageTransform(rotation = 45f).snapRotation().rotation)
        assertEquals(-180f, StoryImageTransform(rotation = -177f).snapRotation().rotation)
    }
}
