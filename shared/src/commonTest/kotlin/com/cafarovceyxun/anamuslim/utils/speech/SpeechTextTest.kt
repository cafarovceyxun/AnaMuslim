package com.cafarovceyxun.anamuslim.utils.speech

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Gözlənilən nəticələr `tools/tts/normalize.py`-nin `normalize_hadith` çıxışıdır (2026-11-13) — model
 * bu formada mətnlə öyrədilib, Kotlin köçürməsi ondan ayrılmamalıdır. Yeganə qəsdən fərq: durğu
 * işarəsinə bitişik tire (`.-Xeyr`) Python-da `., Xeyr`, burada `. Xeyr` olur.
 *
 * Testlər həm JVM-də, həm iOS-da qaçır: azərbaycanca hərflər (`ə`, `ş`, `ı`) JVM regex-inin `\w`-ni
 * keçmir, ona görə hərf yoxlamasının platformadan asılı olmadığını məhz bunlar göstərir.
 */
class SpeechTextTest {

    @Test
    fun hadithPrefixBracesAndSourceReferenceAreDropped() {
        assertEquals(
            "Bizə Yəhya ibn Bükeyr rəvayət etdi: Əməllər niyyətlərə görədir.",
            SpeechText.hadith("221. Bizə Yəhya ibn Bükeyr rəvayət etdi: {Əməllər niyyətlərə görədir.} (Buxari-1)"),
        )
    }

    @Test
    fun parenthesesAndGluedFootnotesFollowThePythonRules() {
        assertEquals(
            "mələklər həmd, sənalar, təriflər görməyi və \"Rainə\" dedi. 3 il ilə 9 il.",
            SpeechText.hadith("mələk(lər) həmd(-sənalar, təriflər) görməyi1 və ”Rainə”1 dedi. 3 il ilə 9 il (1)"),
        )
        assertEquals("Allah təala buyurdu.", SpeechText.hadith("Allah (təala) buyurdu"))
    }

    @Test
    fun azerbaijaniLettersCountAsWordCharacters() {
        // `ş`/`ə` ASCII `\w` deyil — regex-lə yazılsaydı JVM-də haşiyə rəqəmi qalardı.
        assertEquals("Peyğəmbər, rəhmət.", SpeechText.hadith("Peyğəmbər2, rəhmət12."))
    }

    @Test
    fun dialogueDashBecomesAPause() {
        assertEquals("\", Bəli, dedi. Xeyr.", SpeechText.hadith("\"-Bəli, dedi.-Xeyr"))
    }

    @Test
    fun arabicScriptIsDropped() {
        assertEquals("O dedi: və getdi.", SpeechText.hadith("O dedi: اللهُ أَكْبَرُ və getdi."))
    }

    @Test
    fun emptyTextStaysEmpty() {
        assertEquals("", SpeechText.hadith("  12. {} "))
        assertTrue(SpeechText.chunks("").isEmpty())
    }

    @Test
    fun chunksFollowSentencesAndMergeShortOnes() {
        val chunks = SpeechText.chunks("Bəli. O, dedi ki, namaz dinin dirəyidir! Sonra nə oldu? O getdi və bir daha qayıtmadı.")
        assertEquals(
            listOf("Bəli. O, dedi ki, namaz dinin dirəyidir!", "Sonra nə oldu? O getdi və bir daha qayıtmadı."),
            chunks.map { it.text },
        )
        // Sonda qalan qısa cümlə əvvəlkinə qoşulur.
        assertEquals(listOf("O, dedi ki, namaz dinin dirəyidir! Getdi."), SpeechText.chunks("O, dedi ki, namaz dinin dirəyidir! Getdi.").map { it.text })
        assertTrue(chunks.all { it.pause == SpeechPause.Sentence })
    }

    @Test
    fun longIsnadIsSplitAtClausesWithinTheLimit() {
        val isnad = (1..12).joinToString(", ") { "bizə filankəs ibn filankəs $it rəvayət etdi" } +
            ": Allahın Elçisi buyurdu."
        val chunks = SpeechText.chunks(isnad, maxChars = 120)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.text.length <= 120 }, chunks.joinToString("\n") { it.text })
        assertEquals(SpeechPause.Sentence, chunks.last().pause)
        assertEquals(SpeechPause.Clause, chunks.first().pause)
        // Heç bir söz itmir.
        assertEquals(isnad.split(' ').size, chunks.sumOf { it.text.split(' ').size })
    }

    @Test
    fun linesAreSeparateChunks() {
        val chunks = SpeechText.chunks("Birinci sətir burada bitir\nİkinci sətir də burada bitir")
        assertEquals(2, chunks.size)
    }
}
