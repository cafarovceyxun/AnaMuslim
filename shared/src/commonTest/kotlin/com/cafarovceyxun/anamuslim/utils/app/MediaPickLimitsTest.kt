package com.cafarovceyxun.anamuslim.utils.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Hekayə sıxışdırmasının ölçü/bitrate hesabı — Android və iOS eyni funksiyanı işlədir, ona görə
 * burada sınanır. 2026-10-08-ə qədər 720 **uzun** kənar idi (405×720) və ekran yazısının mətni
 * bulanırdı; bu testlər qısa kənar qaydasını saxlayır.
 */
class MediaPickLimitsTest {

    @Test
    fun `portrait screen recording keeps 720 on the short side`() {
        assertEquals(720 to 1600, MediaPickLimits.scaledSize(1080, 2400, 720))
    }

    @Test
    fun `landscape video is scaled by its short side too`() {
        assertEquals(1280 to 720, MediaPickLimits.scaledSize(1920, 1080, 720))
    }

    @Test
    fun `small source is not upscaled`() {
        assertNull(MediaPickLimits.scaledSize(540, 960, 720))
        assertNull(MediaPickLimits.scaledSize(720, 1280, 720))
    }

    @Test
    fun `scaled dimensions are even`() {
        val (width, height) = MediaPickLimits.scaledSize(1179, 2556, 720)!!
        assertEquals(0, width % 2)
        assertEquals(0, height % 2)
    }

    @Test
    fun `invalid dimensions give no scaling`() {
        assertNull(MediaPickLimits.scaledSize(0, 1920, 720))
    }

    @Test
    fun `short video is capped at the max bitrate`() {
        assertEquals(MediaPickLimits.MAX_VIDEO_BITRATE, MediaPickLimits.targetVideoBitrate(10_000))
    }

    @Test
    fun `long video stays within the size budget`() {
        val millis = MediaPickLimits.MAX_VIDEO_MILLIS
        val bitrate = MediaPickLimits.targetVideoBitrate(millis)
        val bytes = (bitrate + MediaPickLimits.AUDIO_BITRATE).toLong() * (millis / 1000) / 8
        assertTrue(bytes <= MediaPickLimits.TARGET_VIDEO_BYTES, "bytes=$bytes")
        assertTrue(bitrate >= MediaPickLimits.MIN_VIDEO_BITRATE)
    }

    @Test
    fun `unknown duration falls back to the cap`() {
        assertEquals(MediaPickLimits.MAX_VIDEO_BITRATE, MediaPickLimits.targetVideoBitrate(null))
    }
}
