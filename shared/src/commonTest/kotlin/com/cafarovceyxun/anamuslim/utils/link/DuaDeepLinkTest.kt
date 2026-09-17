package com.cafarovceyxun.anamuslim.utils.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Dərin linkin analizi.
 *
 * Səhv nəticənin iki üzü var və hər ikisi səssizdir: tanınmalı link tanınmır (toxunuş heç nə etmir,
 * ya da brauzer açılır) və ya yanlış link qəbul olunur (istifadəçi gözlədiyi duanın **yerinə**
 * başqasını görür). Ona görə hər iki tərəf burada bağlanır.
 */
class DuaDeepLinkTest {

    @Test
    fun parsesDuaAndAsmaLinks() {
        assertEquals(DuaDeepLinkTarget.Dua(7), DuaDeepLink.parse("anamuslim://dua/7"))
        assertEquals(DuaDeepLinkTarget.Asma(2), DuaDeepLink.parse("anamuslim://asma/2"))
        // Sxem və host böyük hərflə də gələ bilər — sistem onları normallaşdırmır.
        assertEquals(DuaDeepLinkTarget.Dua(7), DuaDeepLink.parse("ANAMUSLIM://DUA/7"))
        // Sorğu və fraqment atılır: paylaşılan linkə izləmə parametri qoşula bilər.
        assertEquals(DuaDeepLinkTarget.Dua(7), DuaDeepLink.parse("anamuslim://dua/7?from=widget"))
        assertEquals(DuaDeepLinkTarget.Dua(7), DuaDeepLink.parse("anamuslim://dua/7#top"))
        // Sonda əlavə seqment linki pozmur — gələcəkdə yol uzana bilər.
        assertEquals(DuaDeepLinkTarget.Dua(7), DuaDeepLink.parse("anamuslim://dua/7/"))
    }

    @Test
    fun buildsLinksItCanParseBack() {
        assertEquals(DuaDeepLinkTarget.Dua(12), DuaDeepLink.parse(DuaDeepLink.duaUrl(12)))
        assertEquals(DuaDeepLinkTarget.Asma(99), DuaDeepLink.parse(DuaDeepLink.asmaUrl(99)))
    }

    /** Yad sxem **bizim deyil**: `null` qaytarmaq onu sistemə geri verir. */
    @Test
    fun rejectsForeignAndMalformedLinks() {
        assertNull(DuaDeepLink.parse(null))
        assertNull(DuaDeepLink.parse(""))
        assertNull(DuaDeepLink.parse("https://quran.com/2/255"))
        assertNull(DuaDeepLink.parse("anamuslim://dua"))
        assertNull(DuaDeepLink.parse("anamuslim://reader/7"))
        assertNull(DuaDeepLink.parse("anamuslim://dua/abc"))
        // Sıfır və mənfi id baza açarı ola bilməz.
        assertNull(DuaDeepLink.parse("anamuslim://dua/0"))
        assertNull(DuaDeepLink.parse("anamuslim://dua/-3"))
    }

    /** Əsmada 99 ad var: aralıqdan kənar nömrə linki açmamalıdır. */
    @Test
    fun asmaNumbersOutsideTheNameRangeAreRejected() {
        assertEquals(DuaDeepLinkTarget.Asma(1), DuaDeepLink.parse("anamuslim://asma/1"))
        assertNull(DuaDeepLink.parse("anamuslim://asma/0"))
        assertNull(DuaDeepLink.parse("anamuslim://asma/100"))
    }

    @Test
    fun theRouterHoldsOneTargetUntilItIsConsumed() {
        DuaDeepLinkRouter.consume()

        assertEquals(false, DuaDeepLinkRouter.open("https://example.com"))
        assertNull(DuaDeepLinkRouter.pending.value)

        assertEquals(true, DuaDeepLinkRouter.open("anamuslim://dua/5"))
        assertEquals(DuaDeepLinkTarget.Dua(5), DuaDeepLinkRouter.pending.value)

        // Yeni link köhnəsini əvəz edir — istifadəçi sonuncunu gözləyir.
        DuaDeepLinkRouter.open("anamuslim://asma/3")
        assertEquals(DuaDeepLinkTarget.Asma(3), DuaDeepLinkRouter.pending.value)

        DuaDeepLinkRouter.consume()
        assertNull(DuaDeepLinkRouter.pending.value)
    }
}
