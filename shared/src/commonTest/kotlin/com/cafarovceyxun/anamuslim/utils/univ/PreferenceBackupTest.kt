package com.cafarovceyxun.anamuslim.utils.univ

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ehtiyat nüsxənin ayarlar hissəsi: tip etiketi olmadan `Int` ilə `Long`, `Float` ilə `Double` JSON-da
 * eyni görünür, yanlış tiplə geri yazılan açar isə **oxunanda** çökür — ona görə dövrə testi burada.
 */
class PreferenceBackupTest {

    private fun roundTrip(values: Map<String, Any>): Map<String, Any> {
        val encoded = PreferenceBackup.encode(values)
        // Faylın özündən keçirir: kodlaşdırma nəticəsi mətnə yazılıb geri oxunur.
        val reparsed = Json.parseToJsonElement(encoded.toString()) as JsonArray

        return PreferenceBackup.decode(reparsed).entries.associate { (key, value) ->
            key.name to value
        }
    }

    @Test
    fun `every datastore type survives a round trip`() {
        val values = mapOf<String, Any>(
            "reader.book_mode" to true,
            "app_text_scale_percent" to 115,
            "reader.auto_scroll_instructions_shown_count" to 3L,
            "key.recitation.speed" to 1.25f,
            "some.double" to 0.5,
            "v2.theme_mode" to "dark",
            "translations" to setOf("az_cafarov", "en_sahih"),
        )

        val restored = roundTrip(values)

        assertEquals(values.keys, restored.keys)
        assertEquals(true, restored["reader.book_mode"])
        assertEquals(115, restored["app_text_scale_percent"])
        assertEquals(3L, restored["reader.auto_scroll_instructions_shown_count"])
        assertEquals(1.25f, restored["key.recitation.speed"])
        assertEquals(0.5, restored["some.double"])
        assertEquals("dark", restored["v2.theme_mode"])
        assertEquals(setOf("az_cafarov", "en_sahih"), restored["translations"])
    }

    /** Tip etiketi qorunmasa `Preferences` oxunanda `ClassCastException` verərdi. */
    @Test
    fun `numeric types are not collapsed into one`() {
        val restored = roundTrip(mapOf("a" to 1, "b" to 1L, "c" to 1f, "d" to 1.0))

        assertTrue(restored["a"] is Int)
        assertTrue(restored["b"] is Long)
        assertTrue(restored["c"] is Float)
        assertTrue(restored["d"] is Double)
    }

    /** Cihaza bağlı açar nə fayla düşür, nə də faylda gəlsə tətbiq olunur. */
    @Test
    fun `device local keys never travel`() {
        val restored = roundTrip(
            mapOf(
                "onboarding_completed_version" to 3,
                "wbw_resource_version_wbw_az" to 7,
                "current_resource_version" to 12,
                "reader.book_mode" to true,
            )
        )

        assertEquals(setOf("reader.book_mode"), restored.keys)
    }

    /**
     * Namaz ayarlarının bölgüsü: **yer** cihaza bağlıdır, **hesablama** portativdir.
     *
     * Bölgü pozulsa səhv səssizdir — Bakıda alınmış ehtiyat nüsxə Berlində quraşdırılanda tətbiq
     * heç nə demədən yanlış cədvəl göstərər.
     */
    @Test
    fun `prayer location keys stay on the device while calculation settings travel`() {
        val restored = roundTrip(
            mapOf(
                // yer — qalmalıdır
                "prayer.lat" to 40.4093,
                "prayer.lng" to 49.8671,
                "prayer.elevation" to 1178.0,
                "prayer.location_set" to true,
                "prayer.place_name" to "Bakı",
                "prayer.location_mode" to "gps",
                "prayer.location_at" to 1L,
                "prayer.delivered" to "2026-09-01#FAJR",
                "prayer.saved_places" to "Bakı\u001F40.4\u001F49.9\u001F0.0",
                // hesablama — köçməlidir
                "prayer.enabled" to true,
                "prayer.angle.fajr" to 12.0,
                "prayer.angle.isha" to 12.0,
                "prayer.offsets" to "0,0,0,0,2,0",
                "prayer.notify" to "fajr,!sunrise,dhuhr,asr,maghrib,isha",
                "prayer.use_elevation" to true,
            )
        )

        assertEquals(
            setOf(
                "prayer.enabled",
                "prayer.angle.fajr",
                "prayer.angle.isha",
                "prayer.offsets",
                "prayer.notify",
                "prayer.use_elevation",
            ),
            restored.keys,
        )
        assertTrue(restored["prayer.angle.fajr"] is Double, "bucaq Double qalmalıdır")
    }

    /**
     * Fayl istifadəçinin yaddaşından gəlir: pozulmuş sətir yalnız özünü itirməli, qalan ayarları
     * aparmamalıdır.
     */
    @Test
    fun `malformed rows are skipped rather than failing the import`() {
        val json = """
            [
              {"k": "reader.book_mode", "t": "bool", "v": true},
              {"k": "missing.type", "v": 1},
              {"t": "int", "v": 1},
              {"k": "unknown.type", "t": "blob", "v": "x"},
              {"k": "wrong.value", "t": "int", "v": "not a number"},
              "not an object",
              {"k": "app_text_scale_percent", "t": "int", "v": 130}
            ]
        """.trimIndent()

        val decoded = PreferenceBackup.decode(Json.parseToJsonElement(json) as JsonArray)
            .entries.associate { (key, value) -> key.name to value }

        assertEquals(mapOf("reader.book_mode" to true, "app_text_scale_percent" to 130), decoded)
    }

    /** Boş dəst dəyərdir: istifadəçi bütün tərcümələri söndürə bilər, bu, «açar yoxdur» demək deyil. */
    @Test
    fun `an empty string set is kept`() {
        val restored = roundTrip(mapOf("translations" to emptySet<String>()))

        assertEquals(emptySet<String>(), restored["translations"])
    }
}
