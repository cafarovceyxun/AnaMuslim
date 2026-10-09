package com.cafarovceyxun.anamuslim.utils.supabase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Müstəqil hekayənin bitmə vaxtı — PostgREST-in `timestamptz` formatları. */
class StoryAnnouncementTest {

    private val base = 1_791_417_600_000L // 2026-10-08T00:00:00Z

    @Test
    fun `round trip in utc`() {
        val iso = IsoInstant.fromEpochMillis(base + 15 * 3_600_000L + 30 * 60_000L)
        assertEquals("2026-10-08T15:30:00Z", iso)
        assertEquals(base + 15 * 3_600_000L + 30 * 60_000L, IsoInstant.toEpochMillis(iso))
    }

    @Test
    fun `postgrest offset and fraction formats`() {
        val expected = base + 15 * 3_600_000L + 30 * 60_000L
        assertEquals(expected, IsoInstant.toEpochMillis("2026-10-08T15:30:00+00:00"))
        assertEquals(expected, IsoInstant.toEpochMillis("2026-10-08T15:30:00.123456+00:00"))
        assertEquals(expected, IsoInstant.toEpochMillis("2026-10-08T19:30:00+04:00"))
        assertEquals(expected, IsoInstant.toEpochMillis("2026-10-08T15:30:00"))
    }

    @Test
    fun `garbage is rejected`() {
        assertNull(IsoInstant.toEpochMillis("2026-10-08"))
        assertNull(IsoInstant.toEpochMillis("not a date at all"))
    }

    @Test
    fun `activity follows expiry`() {
        val story = StoryAnnouncement(id = 1, note = "x", expires_at = "2026-10-08T15:30:00+00:00")
        assertTrue(story.isActive(base))
        assertFalse(story.isActive(base + 16 * 3_600_000L))
        assertTrue(story.copy(expires_at = null).isActive(Long.MAX_VALUE))
    }
}
