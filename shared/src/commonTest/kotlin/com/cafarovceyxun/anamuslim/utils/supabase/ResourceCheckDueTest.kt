package com.cafarovceyxun.anamuslim.utils.supabase

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceCheckDueTest {
    private val now = 1_800_000_000_000L

    @Test
    fun neverCheckedIsDue() = assertTrue(isResourceCheckDue(0L, now))

    @Test
    fun checkedMinutesAgoIsNotDue() = assertFalse(isResourceCheckDue(now - 10 * 60_000L, now))

    @Test
    fun checkedAnHourAgoIsDue() = assertTrue(isResourceCheckDue(now - RESOURCE_CHECK_INTERVAL_MS, now))

    /** Gündəlik limit idi: səhər yoxlamış telefon axşamkı yeniliyi ertəsi günə qədər görmürdü. */
    @Test
    fun sameDayLaterCheckIsDue() = assertTrue(isResourceCheckDue(now - 6 * RESOURCE_CHECK_INTERVAL_MS, now))

    @Test
    fun clockMovedBackwardsIsDue() = assertTrue(isResourceCheckDue(now + 60_000L, now))
}
