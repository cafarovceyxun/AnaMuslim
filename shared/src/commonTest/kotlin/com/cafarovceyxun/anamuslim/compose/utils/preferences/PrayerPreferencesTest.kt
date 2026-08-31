package com.cafarovceyxun.anamuslim.compose.utils.preferences

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import com.cafarovceyxun.anamuslim.utils.prayer.PrayerParams
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ⚠️ [TestDataStore] proses üzrə **tək** store-dur (öz KDoc-unda yazılıb), ona görə hər test
 * asılı olduğu dəyəri özü yazır — təmiz store fərz edilmir.
 */
class PrayerPreferencesTest {

    @BeforeTest
    fun setUp() = TestDataStore.ensureInitialized()

    // region — serializasiya (saf, store lazım deyil)

    @Test
    fun blankNotifyMeansTheFiveDailyPrayers() {
        val parsed = PrayerPreferences.parseNotify("")

        assertEquals(PrayerPreferences.DEFAULT_NOTIFY, parsed)
        assertTrue(Prayer.SUNRISE !in parsed, "Günəş default olaraq xatırladılmır")
    }

    @Test
    fun notifyRoundTripsThroughTheSingleLine() {
        val chosen = setOf(Prayer.FAJR, Prayer.MAGHRIB, Prayer.SUNRISE)
        val raw = PrayerPreferences.serializeNotify(chosen)

        assertEquals(chosen, PrayerPreferences.parseNotify(raw))
    }

    @Test
    fun anUnlistedPrayerFallsBackToItsDefault() {
        // `home.layout` naxışı: yalnız Fəcr söndürülüb, qalanları sadalanmayıb → default qalır.
        val parsed = PrayerPreferences.parseNotify("!fajr")

        assertTrue(Prayer.FAJR !in parsed)
        assertTrue(Prayer.DHUHR in parsed, "sadalanmayan vaxt defaultunu saxlayır")
        assertTrue(Prayer.SUNRISE !in parsed, "Günəşin defaultu sönülüdür")
    }

    @Test
    fun notifyIgnoresGarbageTokens() {
        val parsed = PrayerPreferences.parseNotify("fajr,,belə-namaz-yoxdur, !isha ,")

        assertTrue(Prayer.FAJR in parsed)
        assertTrue(Prayer.ISHA !in parsed)
        assertTrue(Prayer.DHUHR in parsed)
    }

    @Test
    fun offsetsRoundTripAndDropZeroes() {
        val offsets = mapOf(Prayer.FAJR to -5, Prayer.ISHA to 12)
        val raw = PrayerPreferences.serializeOffsets(offsets)

        assertEquals("-5,0,0,0,0,12", raw)
        assertEquals(offsets, PrayerPreferences.parseOffsets(raw))
        assertEquals(emptyMap(), PrayerPreferences.parseOffsets(""))
    }

    @Test
    fun offsetsAreClampedToTheAllowedRange() {
        val raw = PrayerPreferences.serializeOffsets(mapOf(Prayer.FAJR to 900))
        val parsed = PrayerPreferences.parseOffsets(raw)

        assertEquals(PrayerParams.OFFSET_RANGE.last, parsed[Prayer.FAJR])
    }

    @Test
    fun offsetsSurviveATruncatedLine() {
        // Köhnə versiyadan qalan qısa sətir (məsələn Günəş əlavə olunmazdan əvvəl) partlamamalıdır.
        val parsed = PrayerPreferences.parseOffsets("3,4")

        assertEquals(3, parsed[Prayer.FAJR])
        assertEquals(4, parsed[Prayer.SUNRISE])
        assertNull(parsed[Prayer.ISHA])
    }

    // endregion

    // region — store

    @Test
    fun anglesAreClampedWhenWritten() = runTest {
        PrayerPreferences.setAngles(fajrAngle = 3.0, ishaAngle = 40.0)

        val params = PrayerPreferences.getParams()
        assertTrue(abs(params.fajrAngle - PrayerParams.ANGLE_RANGE.start) < 1e-9, "${params.fajrAngle}")
        assertTrue(
            abs(params.ishaAngle - PrayerParams.ANGLE_RANGE.endInclusive) < 1e-9,
            "${params.ishaAngle}",
        )
    }

    @Test
    fun asrShadowFactorOnlyAcceptsOneOrTwo() = runTest {
        PrayerPreferences.setAsrShadowFactor(2)
        assertEquals(2, PrayerPreferences.getParams().asrShadowFactor)

        PrayerPreferences.setAsrShadowFactor(9)
        assertEquals(1, PrayerPreferences.getParams().asrShadowFactor, "naməlum dəyər Şafiiyə düşür")
    }

    @Test
    fun locationIsWrittenAndClearedAtomically() = runTest {
        PrayerPreferences.setLocation(
            point = GeoPoint(40.4093, 49.8671),
            placeName = "Bakı",
            mode = PrayerPreferences.MODE_MANUAL,
            atMillis = 1_788_220_800_000L,
        )

        val point = PrayerPreferences.getPoint()
        assertTrue(point != null && abs(point.latitude - 40.4093) < 1e-9)
        assertEquals(PrayerPreferences.MODE_MANUAL, PrayerPreferences.getLocationMode())
        assertEquals(1_788_220_800_000L, PrayerPreferences.getLocationUpdatedAt())

        PrayerPreferences.clearLocation()
        assertNull(PrayerPreferences.getPoint(), "bayraq düşəndə koordinat oxunmur")
    }

    @Test
    fun settingsReflectEveryStoredPiece() = runTest {
        PrayerPreferences.setEnabled(true)
        PrayerPreferences.setAngles(12.0, 15.0)
        PrayerPreferences.setNotify(setOf(Prayer.FAJR, Prayer.MAGHRIB))
        PrayerPreferences.setOffsets(mapOf(Prayer.MAGHRIB to 2))
        PrayerPreferences.setLocation(GeoPoint(41.0, 29.0), "İstanbul", PrayerPreferences.MODE_GPS, 1L)

        val settings = PrayerPreferences.getSettings()

        assertTrue(settings.enabled)
        assertTrue(settings.canSchedule)
        assertEquals("İstanbul", settings.placeName)
        assertEquals(setOf(Prayer.FAJR, Prayer.MAGHRIB), settings.notify)
        assertEquals(2, settings.params.offsetOf(Prayer.MAGHRIB))
        assertTrue(abs(settings.params.ishaAngle - 15.0) < 1e-9)
    }

    @Test
    fun settingsCannotScheduleWithoutALocation() = runTest {
        PrayerPreferences.setEnabled(true)
        PrayerPreferences.setNotify(setOf(Prayer.FAJR))
        PrayerPreferences.clearLocation()

        assertTrue(!PrayerPreferences.getSettings().canSchedule, "yer yoxdursa plan qurulmur")
    }

    @Test
    fun markDeliveredStoresTheKeyAndPrunesOldOnes() = runTest {
        val now = 1_788_220_800_000L // 2026-09-01

        PrayerPreferences.markDelivered("2026-07-01#FAJR", now)
        PrayerPreferences.markDelivered("2026-09-01#DHUHR", now)

        val delivered = PrayerPreferences.getDelivered()
        assertTrue("2026-09-01#DHUHR" in delivered)
        assertTrue("2026-07-01#FAJR" !in delivered, "köhnə açar təmizlənməlidir")
    }

    // endregion
}
