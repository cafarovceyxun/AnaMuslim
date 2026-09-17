package com.cafarovceyxun.anamuslim.compose.screens.dua

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Rejim zolağı ilə gizlətmə açarlarının **kəsişməsi**.
 *
 * Bu, gözlə tutulmayan sinifdəndir: səhv qayda ekranda «bir mətn əskikdir» kimi görünür, kompilyator
 * və ekran görüntüsü isə susur. Ona görə hər üç rejim hər üç açarla ayrıca yoxlanılır.
 */
class DuaVisibilityTest {

    @Test
    fun mixedModeShowsEverythingWhenAllSwitchesAreOn() {
        val v = duaBlockVisibility(DUA_MODE_MIXED, true, true, true)

        assertTrue(v.arabic)
        assertTrue(v.transliteration)
        assertTrue(v.translation)
        assertFalse(v.isEmpty)
    }

    @Test
    fun arabicModeHidesTransliterationAndTranslation() {
        val v = duaBlockVisibility(DUA_MODE_ARABIC, true, true, true)

        assertTrue(v.arabic)
        assertFalse(v.transliteration)
        assertFalse(v.translation)
    }

    /** Oxunuş «Tərcümə» rejimində qalır — istifadəçinin qərarı (bax [DUA_MODE_TRANSLATION]). */
    @Test
    fun translationModeKeepsTransliterationButDropsArabic() {
        val v = duaBlockVisibility(DUA_MODE_TRANSLATION, true, true, true)

        assertFalse(v.arabic)
        assertTrue(v.transliteration)
        assertTrue(v.translation)
    }

    /** Açar rejimin içində işləyir: rejim bloku açsa da, söndürülmüş açar onu gizli saxlayır. */
    @Test
    fun switchesNarrowTheModeRatherThanBeingOverriddenByIt() {
        val arabicOff = duaBlockVisibility(DUA_MODE_ARABIC, false, true, true)
        assertFalse(arabicOff.arabic)

        val translationOff = duaBlockVisibility(DUA_MODE_TRANSLATION, true, true, false)
        assertTrue(translationOff.transliteration)
        assertFalse(translationOff.translation)
    }

    /** Rejim və açarlar birlikdə hər şeyi gizlədə bilir — UI bunu izahlı mesajla qarşılamalıdır. */
    @Test
    fun arabicModeWithArabicSwitchOffLeavesNothingToShow() {
        val v = duaBlockVisibility(DUA_MODE_ARABIC, false, true, true)

        assertTrue(v.isEmpty)
    }

    @Test
    fun allSwitchesOffLeavesNothingInAnyMode() {
        listOf(DUA_MODE_MIXED, DUA_MODE_ARABIC, DUA_MODE_TRANSLATION).forEach { mode ->
            assertTrue(
                duaBlockVisibility(mode, false, false, false).isEmpty,
                "rejim $mode üçün boş olmalıydı",
            )
        }
    }

    /** DataStore-dan gələn tanınmayan dəyər ekranı boş qoymamalıdır. */
    @Test
    fun unknownModeFallsBackToMixed() {
        val expected = duaBlockVisibility(DUA_MODE_MIXED, true, true, true)

        assertEquals(expected, duaBlockVisibility(42, true, true, true))
        assertEquals(expected, duaBlockVisibility(-1, true, true, true))
    }
}
