package com.cafarovceyxun.anamuslim.compose.components.media

import androidx.compose.ui.geometry.Offset
import com.cafarovceyxun.anamuslim.utils.app.VideoCrop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Redaktorun kadr çərçivəsi və vaxt yazısı — jest riyaziyyatı ekransız sınanır. */
class VideoEditorGeometryTest {

    private val full = VideoCrop.Full

    @Test
    fun `top edge drag trims the status bar`() {
        val cropped = full.dragged(CropHandle.Top, dx = 0f, dy = 0.05f)
        assertEquals(VideoCrop(0f, 0.05f, 1f, 1f), cropped)
    }

    @Test
    fun `edge cannot pass the minimum span`() {
        val cropped = full.dragged(CropHandle.Left, dx = 0.95f, dy = 0f)
        assertEquals(1f - VideoCrop.MIN_SPAN, cropped.left, 1e-6f)
    }

    @Test
    fun `edge cannot leave the frame`() {
        val cropped = full.dragged(CropHandle.BottomRight, dx = 0.3f, dy = 0.3f)
        assertEquals(full, cropped)
    }

    @Test
    fun `move keeps the size and stays inside`() {
        val crop = VideoCrop(0.1f, 0.1f, 0.6f, 0.5f)
        val moved = crop.dragged(CropHandle.Move, dx = 0.9f, dy = -0.9f)
        assertEquals(0.5f, moved.right - moved.left, 1e-6f)
        assertEquals(0.4f, moved.bottom - moved.top, 1e-6f)
        assertEquals(1f, moved.right, 1e-6f)
        assertEquals(0f, moved.top, 1e-6f)
    }

    @Test
    fun `hit test picks corners before edges and inside as move`() {
        val crop = VideoCrop(0.1f, 0.1f, 0.9f, 0.9f)
        fun hit(x: Float, y: Float) = hitTestCrop(Offset(x, y), crop, 1000f, 1000f, touch = 24f)

        assertEquals(CropHandle.TopLeft, hit(105f, 110f))
        assertEquals(CropHandle.Right, hit(905f, 500f))
        assertEquals(CropHandle.Bottom, hit(500f, 890f))
        assertEquals(CropHandle.Move, hit(500f, 500f))
        assertEquals(CropHandle.None, hit(20f, 500f))
    }

    @Test
    fun `full crop is detected`() {
        assertTrue(VideoCrop.Full.isFull)
        assertTrue(!VideoCrop(top = 0.01f).isFull)
    }

    @Test
    fun `clock formats minutes and seconds`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:09", formatClock(9_400))
        assertEquals("2:00", formatClock(120_000))
        assertEquals("10:05", formatClock(605_000))
    }
}
