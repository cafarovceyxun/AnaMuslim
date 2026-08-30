package com.cafarovceyxun.anamuslim.utils.verse

import com.cafarovceyxun.anamuslim.utils.supabase.DailyContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** «Hədisin müəyyən bir qismini seç» tələbinin məntiqi. */
class HadithExcerptTest {

    private val hadith =
        "Bizə Musəddəd danışdı. Bizə Yəhya danışdı. Əməllər niyyətlərə görədir. Hər kəsə niyyət etdiyi vardır."

    @Test
    fun `splits on sentence terminators keeping them`() {
        val sentences = HadithExcerpt.sentences(hadith)

        assertEquals(4, sentences.size)
        assertEquals("Bizə Musəddəd danışdı.", sentences[0])
        assertEquals("Əməllər niyyətlərə görədir.", sentences[2])
    }

    /** Bu topluda bir hədis çox vaxt bir neçə rəvayətdən ibarətdir — seçim vahidi elə rəvayətdir. */
    @Test
    fun `splits at narration markers instead of sentences`() {
        val text = "Birinci mətn. (Buxari, 1). Digər bir rəvayətdə: İkinci mətn. (Muslim, 2). " +
            "Digər bir rəvayətdə: Üçüncü mətn."

        val parts = HadithExcerpt.sentences(text)

        assertEquals(3, parts.size)
        assertEquals("Birinci mətn. (Buxari, 1).", parts[0])
        assertTrue(parts[1].startsWith("Digər bir rəvayətdə: İkinci mətn."))
        assertEquals("Digər bir rəvayətdə: Üçüncü mətn.", parts[2])
    }

    @Test
    fun `arabic splits at its own narration marker`() {
        val text = "النص الأول. (البخاري-١). وفي رواية: النص الثاني. وفي رواية: النص الثالث."

        val parts = HadithExcerpt.sentences(text)

        assertEquals(3, parts.size)
        assertTrue(parts[1].startsWith("وفي رواية:"))
        assertTrue(parts[2].startsWith("وفي رواية:"))
    }

    @Test
    fun `narration selection round trips`() {
        val text = "Birinci. Digər bir rəvayətdə: İkinci. Digər bir rəvayətdə: Üçüncü."
        val parts = HadithExcerpt.sentences(text)

        val excerpt = HadithExcerpt.join(parts, setOf(1))

        assertEquals("Digər bir rəvayətdə: İkinci.", excerpt)
        assertEquals(setOf(1), HadithExcerpt.selectionOf(parts, excerpt))
    }

    @Test
    fun `text without a terminator is a single sentence`() {
        assertEquals(listOf("Tək cümlə"), HadithExcerpt.sentences("Tək cümlə"))
        assertTrue(HadithExcerpt.sentences("   ").isEmpty())
        assertTrue(HadithExcerpt.sentences("").isEmpty())
    }

    @Test
    fun `join keeps the original order regardless of selection order`() {
        val sentences = HadithExcerpt.sentences(hadith)

        assertEquals(
            "Əməllər niyyətlərə görədir. Hər kəsə niyyət etdiyi vardır.",
            HadithExcerpt.join(sentences, setOf(3, 2)),
        )
    }

    @Test
    fun `selection round trips through join`() {
        val sentences = HadithExcerpt.sentences(hadith)
        val selected = setOf(2, 3)
        val excerpt = HadithExcerpt.join(sentences, selected)

        assertEquals(selected, HadithExcerpt.selectionOf(sentences, excerpt))
    }

    @Test
    fun `hand edited excerpt reports no selection`() {
        val sentences = HadithExcerpt.sentences(hadith)

        // Panel belə halda nişanları boş göstərir və mətni sərbəst mətn kimi saxlayır.
        assertTrue(HadithExcerpt.selectionOf(sentences, "Əl ilə yazılmış başqa mətn").isEmpty())
        assertTrue(HadithExcerpt.selectionOf(sentences, null).isEmpty())
        assertTrue(HadithExcerpt.selectionOf(sentences, "").isEmpty())
    }

    @Test
    fun `display text prefers the excerpt over the full text`() {
        val content = DailyContent(
            content_type = DailyContent.CONTENT_TYPE_HADITH,
            hadith_id = 1L,
            text_ar = "النص الكامل",
            text_az = hadith,
            excerpt_az = "Əməllər niyyətlərə görədir.",
        )

        assertEquals("Əməllər niyyətlərə görədir.", content.displayTextAz)
        // Ərəbcə çıxarış verilməyib — tam mətn qalır.
        assertEquals("النص الكامل", content.displayTextAr)
    }

    @Test
    fun `blank excerpt falls back to the full text`() {
        val content = DailyContent(
            content_type = DailyContent.CONTENT_TYPE_HADITH,
            hadith_id = 1L,
            text_ar = "",
            text_az = hadith,
            excerpt_az = "   ",
        )

        assertEquals(hadith, content.displayTextAz)
    }
}
