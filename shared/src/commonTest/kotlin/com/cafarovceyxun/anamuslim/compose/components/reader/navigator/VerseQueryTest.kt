package com.cafarovceyxun.anamuslim.compose.components.reader.navigator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Süzgəc qutusuna yazılan istinadın oxunuşu.
 *
 * Bu qaydanı ekran görüntüsü ilə tutmaq olmur: səhv parse ediləndə sətir sadəcə **çıxmır** və
 * istifadəçi «işləmir» deyir. Ona görə hər yazılış forması burada kilidlənir — xüsusən `-`-in iki
 * mənası: «1-5» tək ayədir, «1:1-5» isə aralıqdır.
 */
class VerseQueryTest {

    @Test
    fun readsSingleVerseInEverySeparator() {
        for (query in listOf("1:7", "1.7", "1/7", "1-7", " 1 : 7 ")) {
            assertEquals(VerseReference(1, 7, 7), parseVerseReference(query), query)
        }
    }

    @Test
    fun readsRangeOnlyAfterAColonLikeSeparator() {
        assertEquals(VerseReference(1, 1, 5), parseVerseReference("1:1-5"))
        assertEquals(VerseReference(1, 1, 5), parseVerseReference("1.1-5"))

        // «1-5» aralıq deyil: qutuda surə axtaran istifadəçi bununla 1-ci surənin 5-ci ayəsini
        // nəzərdə tutur.
        assertEquals(VerseReference(1, 5, 5), parseVerseReference("1-5"))
    }

    @Test
    fun rejectsNonReferences() {
        for (query in listOf("", "salam", "7", "115:1", "1:0", "1:5-2", "1:2:3")) {
            assertNull(parseVerseReference(query), query)
        }
    }

    @Test
    fun singleVerseHelperTakesTheStartOfARange() {
        assertEquals(1 to 1, parseChapterVerseQuery("1:1-5"))
        assertEquals(2 to 255, parseChapterVerseQuery("2:255"))
    }
}
