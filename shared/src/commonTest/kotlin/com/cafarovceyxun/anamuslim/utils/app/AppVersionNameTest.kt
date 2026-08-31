package com.cafarovceyxun.anamuslim.utils.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Hekayənin görünmə həddi buradan keçir: sürüm adları **ədəd** kimi müqayisə olunmalıdır.
 * Sətir müqayisəsi `2026.08.9 > 2026.08.31` kimi yanlış nəticə verərdi və 31-ci buraxılışın
 * funksiyası köhnə cihazda görünərdi.
 */
class AppVersionNameTest {

    @Test
    fun aNewerVersionPassesTheGate() {
        assertTrue(AppVersionName.isAtLeast("2026.08.31", "2026.08.30"))
        assertTrue(AppVersionName.isAtLeast("2026.09.01", "2026.08.31"))
        assertTrue(AppVersionName.isAtLeast("2027.01.01", "2026.12.31"))
    }

    @Test
    fun theExactVersionPassesTheGate() {
        assertTrue(AppVersionName.isAtLeast("2026.08.31", "2026.08.31"))
    }

    @Test
    fun anOlderVersionIsBlocked() {
        assertFalse(AppVersionName.isAtLeast("2026.08.30", "2026.08.31"))
        assertFalse(AppVersionName.isAtLeast("2026.07.31", "2026.08.01"))
    }

    /** Sətir müqayisəsi burada tərsinə düşərdi: "9" > "31". */
    @Test
    fun partsAreComparedAsNumbersNotText() {
        assertFalse(AppVersionName.isAtLeast("2026.08.9", "2026.08.31"))
        assertTrue(AppVersionName.isAtLeast("2026.08.31", "2026.08.9"))
    }

    @Test
    fun aMissingPartCountsAsZero() {
        assertTrue(AppVersionName.isAtLeast("2026.09", "2026.09.0"))
        assertFalse(AppVersionName.isAtLeast("2026.09", "2026.09.1"))
    }

    /** Debug qurmalarında ad `2026.08.31-debug` ola bilər — hərflər müqayisəni pozmamalıdır. */
    @Test
    fun nonNumericSuffixesAreIgnored() {
        assertTrue(AppVersionName.isAtLeast("2026.08.31-debug", "2026.08.31"))
        assertTrue(AppVersionName.isAtLeast("2026.08.14 (13)", "2026.08.14"))
    }

    /** Hədd yoxdursa hamı görür; sürüm oxunmursa da gizlətmirik. */
    @Test
    fun anUnknownVersionOrNoLimitIsAllowed() {
        assertTrue(AppVersionName.isAtLeast("2026.01.01", null))
        assertTrue(AppVersionName.isAtLeast("2026.01.01", ""))
        assertTrue(AppVersionName.isAtLeast("", "2026.08.31"))
        assertTrue(AppVersionName.isAtLeast(null, "2026.08.31"))
    }
}
