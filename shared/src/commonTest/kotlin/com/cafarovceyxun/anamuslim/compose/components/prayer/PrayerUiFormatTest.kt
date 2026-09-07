package com.cafarovceyxun.anamuslim.compose.components.prayer

import com.cafarovceyxun.anamuslim.utils.prayer.Prayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Cümə günü zöhr bildirişi «Cümə namazı» adını almalıdır.
 *
 * Bu yalnız **bildiriş** yolundadır ([com.cafarovceyxun.anamuslim.utils.prayer.PrayerNotificationContent]),
 * ona görə nə ekran görüntüsü, nə də kompilyator onu tuta bilir — bildirişin mətni yalnız vaxt
 * çatanda görünür.
 *
 * ⚠️ `Res.string.*` burada işlənmir: generasiya olunan resurs aksessorları `commonTest`-in
 * kompilyasiya yoluna **düşmür** (`Unresolved reference`). Ona görə müqayisə açar üzərindədir —
 * cümə zöhrü adi zöhrdən **fərqli** resursa getməlidir, qalan hər şey isə eynisinə.
 */
class PrayerUiFormatTest {

    private fun keyOf(prayer: Prayer, dateIso: String) =
        PrayerUiFormat.labelOf(prayer, dateIso).key

    @Test
    fun `friday dhuhr becomes jumuah`() {
        // 2026-09-11 cümədir.
        assertNotEquals(
            PrayerUiFormat.labelOf(Prayer.DHUHR).key,
            keyOf(Prayer.DHUHR, "2026-09-11"),
        )
    }

    @Test
    fun `other days keep dhuhr`() {
        // 2026-09-10 cümə axşamı, 2026-09-12 şənbədir — hər ikisi adi zöhr.
        val plain = PrayerUiFormat.labelOf(Prayer.DHUHR).key

        assertEquals(plain, keyOf(Prayer.DHUHR, "2026-09-10"))
        assertEquals(plain, keyOf(Prayer.DHUHR, "2026-09-12"))
    }

    @Test
    fun `other prayers are untouched on friday`() {
        Prayer.entries.filter { it != Prayer.DHUHR }.forEach { prayer ->
            assertEquals(
                PrayerUiFormat.labelOf(prayer).key,
                keyOf(prayer, "2026-09-11"),
                "${prayer.name} cümə günü dəyişməməlidir",
            )
        }
    }

    @Test
    fun `malformed date falls back to the plain name`() {
        assertEquals(PrayerUiFormat.labelOf(Prayer.DHUHR).key, keyOf(Prayer.DHUHR, "pozuq"))
    }

    @Test
    fun `notification uses the long form rather than the screen label`() {
        val screen = keyOf(Prayer.DHUHR, "2026-09-11")
        val notification = PrayerUiFormat.notificationLabelOf(Prayer.DHUHR, "2026-09-11").key

        // «Cümə» ≠ «Cümə namazı»: birincisi sütun başlığıdır, ikincisi cümlənin içinə düşür.
        assertNotEquals(screen, notification)
        assertNotEquals(PrayerUiFormat.labelOf(Prayer.DHUHR).key, notification)
    }

    @Test
    fun `notification keeps the plain name on other days and prayers`() {
        assertEquals(
            PrayerUiFormat.labelOf(Prayer.DHUHR).key,
            PrayerUiFormat.notificationLabelOf(Prayer.DHUHR, "2026-09-10").key,
        )
        assertEquals(
            PrayerUiFormat.labelOf(Prayer.ASR).key,
            PrayerUiFormat.notificationLabelOf(Prayer.ASR, "2026-09-11").key,
        )
    }
}
