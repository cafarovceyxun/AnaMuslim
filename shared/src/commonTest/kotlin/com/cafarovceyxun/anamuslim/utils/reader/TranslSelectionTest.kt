package com.cafarovceyxun.anamuslim.utils.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs on both Android and iOS. [TranslUtils.resolveSelectionChange] is the pure half of the former
 * Android-only `TranslUtilsAndroid.resolveSelectionChange`, which also popped the limit dialog;
 * telling the user is now the ViewModel's job, so the decision itself is pinned here.
 */
class TranslSelectionTest {

    @Test
    fun selectingAddsSlug() {
        val slugs = mutableSetOf("az")
        assertTrue(TranslUtils.resolveSelectionChange(slugs, "en", isSelected = true))
        assertEquals(setOf("az", "en"), slugs)
    }

    @Test
    fun deselectingRemovesSlug() {
        val slugs = mutableSetOf("az", "en")
        assertTrue(TranslUtils.resolveSelectionChange(slugs, "en", isSelected = false))
        assertEquals(setOf("az"), slugs)
    }

    @Test
    fun selectingAtLimitFailsAndLeavesSetUntouched() {
        val slugs = (1..TranslUtils.TRANSL_MAX_SELECTION_LIMIT).map { "t$it" }.toMutableSet()
        val before = slugs.toSet()

        assertFalse(TranslUtils.resolveSelectionChange(slugs, "extra", isSelected = true))
        assertEquals(before, slugs)
    }

    @Test
    fun deselectingAtLimitStillWorks() {
        val slugs = (1..TranslUtils.TRANSL_MAX_SELECTION_LIMIT).map { "t$it" }.toMutableSet()
        assertTrue(TranslUtils.resolveSelectionChange(slugs, "t1", isSelected = false))
        assertEquals(TranslUtils.TRANSL_MAX_SELECTION_LIMIT - 1, slugs.size)
    }

    @Test
    fun reselectingAnAlreadySelectedSlugAtLimitIsRejected() {
        // The set is full and re-adding is a no-op, but the limit check runs first — the old
        // Android version behaved the same way, so the dialog appears here too.
        val slugs = (1..TranslUtils.TRANSL_MAX_SELECTION_LIMIT).map { "t$it" }.toMutableSet()
        assertFalse(TranslUtils.resolveSelectionChange(slugs, "t1", isSelected = true))
        assertEquals(TranslUtils.TRANSL_MAX_SELECTION_LIMIT, slugs.size)
    }
}
