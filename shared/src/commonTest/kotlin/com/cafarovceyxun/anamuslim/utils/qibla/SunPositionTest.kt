package com.cafarovceyxun.anamuslim.utils.qibla

import com.cafarovceyxun.anamuslim.utils.prayer.GeoPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Günəş kompası.
 *
 * Testin ağırlığı **astronomik invariantlardadır**, kənar cədvəldən köçürülmüş rəqəmlərdə yox:
 * bahar bərabərliyində yerli günorta günəşi dəqiq cənubdadır və hündürlüyü `90° − enlik`-dir.
 * Bu ikisi eyni anda düzgün çıxırsa, saat bucağı, düz qalxma və horizontal çevirmə — üçü də
 * düzdür; birində işarə səhvi olsa hər iki yoxlama sınar.
 *
 * ⚠️ Tolerantlıq 1.5°-dir və qəsdən boşdur: «yerli günorta» dəqiqə dəqiqliyində seçilmiş andır,
 * vaxt tənliyi isə saniyələrlə sürüşür. Ekranda nişan gözlə günəşlə tutuşdurulur, yəni bir
 * dərəcəlik fərq onsuz da görünmür.
 */
class SunPositionTest {

    /** Qrinviç rəsədxanası — bərabərlik günündə yoxlamanı ən sadə formaya salır. */
    private val greenwich = GeoPoint(latitude = 51.4769, longitude = -0.0005)

    /** 2026-03-20 12:07 UT — bahar bərabərliyində Qrinviçdə yerli günəş günortası. */
    private val equinoxNoonMillis = 1_774_008_420_000L

    @Test
    fun noonSunIsDueSouthAtTheEquinox() {
        val sun = SunCompass.at(greenwich, equinoxNoonMillis)

        assertTrue(abs(sun.azimuthDeg - 180.0) < 1.5, "azimut ${sun.azimuthDeg}")
    }

    @Test
    fun noonAltitudeAtTheEquinoxIsNinetyMinusLatitude() {
        val sun = SunCompass.at(greenwich, equinoxNoonMillis)
        val expected = 90.0 - greenwich.latitude

        assertTrue(abs(sun.altitudeDeg - expected) < 1.5, "hündürlük ${sun.altitudeDeg}")
    }

    @Test
    fun morningSunIsEastAndEveningSunIsWest() {
        // Eyni gün: 06:30 UT və 17:30 UT.
        val morning = SunCompass.at(greenwich, 1_773_988_200_000L)
        val evening = SunCompass.at(greenwich, 1_774_027_800_000L)

        assertTrue(morning.azimuthDeg in 45.0..135.0, "səhər ${morning.azimuthDeg}")
        assertTrue(evening.azimuthDeg in 225.0..315.0, "axşam ${evening.azimuthDeg}")
    }

    @Test
    fun azimuthStaysInsideTheFullCircle() {
        // Bütün gün boyu yarım saatlıq addımlarla — normalizasiya heç bir saatda sınmamalıdır.
        var millis = 1_773_964_800_000L

        repeat(48) {
            val sun = SunCompass.at(greenwich, millis)

            assertTrue(sun.azimuthDeg >= 0.0 && sun.azimuthDeg < 360.0, "azimut ${sun.azimuthDeg}")
            millis += 30 * 60 * 1000L
        }
    }

    @Test
    fun sunBelowTheHorizonIsNotUsable() {
        // Bakı, 2026-09-16 gecə saat 00:00 yerli (20:00 UT).
        val baku = GeoPoint(latitude = 40.4093, longitude = 49.8671)
        val sun = SunCompass.at(baku, 1_789_588_800_000L)

        assertTrue(sun.altitudeDeg < 0.0, "hündürlük ${sun.altitudeDeg}")
        assertFalse(sun.isUsable)
    }

    @Test
    fun sunHuggingTheHorizonIsNotOffered() {
        // Üfüqə yaxın günəş binaların və dağların arxasında qalır — nişanla üst-üstə salmaq olmur,
        // ona görə hədd sıfır deyil.
        assertFalse(SunPosition(azimuthDeg = 95.0, altitudeDeg = 1.0).isUsable)
        assertTrue(SunPosition(azimuthDeg = 95.0, altitudeDeg = 5.0).isUsable)
    }
}
