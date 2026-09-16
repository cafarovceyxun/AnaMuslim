package com.cafarovceyxun.anamuslim.compose.components.qibla

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Web Mercator proyeksiyası.
 *
 * Testlər proyeksiyanın **öz tərifindən** çıxır (gediş-gəliş eyniliyi, ekvatorun və baş meridianın
 * mərkəzə düşməsi, zoom-un ikiqatlanması, ekvatorda məlum piksel miqyası) — kənar cədvəldən rəqəm
 * köçürülmür.
 */
class WebMercatorTest {

    private val zooms = listOf(0.0, 1.0, 5.5, 12.0, 18.0)

    @Test
    fun longitudeRoundTrips() {
        for (zoom in zooms) {
            for (step in -17..17) {
                val longitude = step * 10.0
                val back = WebMercator.worldXToLon(WebMercator.lonToWorldX(longitude, zoom), zoom)

                assertTrue(abs(back - longitude) < 1e-9, "zoom $zoom, $longitude → $back")
            }
        }
    }

    @Test
    fun latitudeRoundTrips() {
        for (zoom in zooms) {
            for (step in -8..8) {
                val latitude = step * 10.0
                val back = WebMercator.worldYToLat(WebMercator.latToWorldY(latitude, zoom), zoom)

                assertTrue(abs(back - latitude) < 1e-7, "zoom $zoom, $latitude → $back")
            }
        }
    }

    @Test
    fun originIsTheCentreOfTheWorldSquare() {
        for (zoom in zooms) {
            val half = WebMercator.worldSizePixels(zoom) / 2.0

            assertTrue(abs(WebMercator.lonToWorldX(0.0, zoom) - half) < 1e-6, "baş meridian, zoom $zoom")
            assertTrue(abs(WebMercator.latToWorldY(0.0, zoom) - half) < 1e-6, "ekvator, zoom $zoom")
        }
    }

    @Test
    fun worldDoublesWithEachZoomLevel() {
        assertTrue(abs(WebMercator.worldSizePixels(0.0) - 256.0) < 1e-9)

        for (zoom in 0..18) {
            val here = WebMercator.worldSizePixels(zoom.toDouble())
            val next = WebMercator.worldSizePixels(zoom + 1.0)

            assertTrue(abs(next - 2.0 * here) < 1e-6, "zoom $zoom: $here → $next")
        }
    }

    @Test
    fun northIsSmallerYThanSouth() {
        // Ekran koordinatı yuxarıdan aşağı artır: şimal kiçik Y, cənub böyük Y.
        val north = WebMercator.latToWorldY(60.0, 10.0)
        val south = WebMercator.latToWorldY(-60.0, 10.0)

        assertTrue(north < south, "şimal $north, cənub $south")
    }

    @Test
    fun metersPerPixelMatchesTheEquatorAtZoomZero() {
        // Ekvator çevrəsi / 256 piksel — proyeksiyanın tərifi.
        val expected = 2.0 * kotlin.math.PI * 6_378_137.0 / 256.0
        val actual = WebMercator.metersPerPixel(0.0, 0.0)

        assertTrue(abs(actual - expected) < 1e-6, "alındı $actual, gözlənilən $expected")
    }

    @Test
    fun metersPerPixelShrinksTowardsThePoles() {
        // cos(enlik) vurulmasa yüksək enlikdə radius bir neçə dəfə səhv çıxır.
        val equator = WebMercator.metersPerPixel(0.0, 16.0)
        val baku = WebMercator.metersPerPixel(40.4, 16.0)
        val tromso = WebMercator.metersPerPixel(69.6, 16.0)

        assertTrue(baku < equator, "Bakı $baku, ekvator $equator")
        assertTrue(tromso < baku, "Tromsø $tromso, Bakı $baku")
    }

    @Test
    fun latitudeIsClampedToTheProjectionLimit() {
        // Qütbdə proyeksiya sonsuzluğa gedir; sıxma olmasa NaN/Infinity çıxır və xəritə itir.
        val northPole = WebMercator.latToWorldY(90.0, 10.0)
        val southPole = WebMercator.latToWorldY(-90.0, 10.0)

        assertTrue(northPole.isFinite(), "şimal qütbü $northPole")
        assertTrue(southPole.isFinite(), "cənub qütbü $southPole")
        assertTrue(northPole >= 0.0, "şimal qütbü aralıqdan çıxdı: $northPole")
        assertTrue(southPole <= WebMercator.worldSizePixels(10.0), "cənub qütbü: $southPole")
    }

    @Test
    fun longitudeWrapsAroundTheAntimeridian() {
        assertTrue(abs(WebMercator.wrapLongitude(190.0) + 170.0) < 1e-9)
        assertTrue(abs(WebMercator.wrapLongitude(-190.0) - 170.0) < 1e-9)
        assertTrue(abs(WebMercator.wrapLongitude(0.0)) < 1e-9)
        // Antimeridian birmənalı deyil; tərif olaraq −180 seçilib.
        assertTrue(abs(WebMercator.wrapLongitude(180.0) + 180.0) < 1e-9)

        for (step in -40..40) {
            val wrapped = WebMercator.wrapLongitude(step * 17.0)

            assertTrue(wrapped >= -180.0 && wrapped < 180.0, "${step * 17.0} → $wrapped")
        }
    }
}
