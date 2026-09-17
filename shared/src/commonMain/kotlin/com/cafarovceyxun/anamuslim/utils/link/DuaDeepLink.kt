package com.cafarovceyxun.anamuslim.utils.link

/** Dərin linkin göstərdiyi yer. */
sealed interface DuaDeepLinkTarget {
    /** Konkret dua — `anamuslim://dua/7`. */
    data class Dua(val duaId: Long) : DuaDeepLinkTarget

    /** Əsmaül Hüsnədə ad — `anamuslim://asma/2` (nömrə 1–99). */
    data class Asma(val nameNo: Int) : DuaDeepLinkTarget
}

/**
 * «Dua və zikr» / «Əsmaül Hüsnə» dərin linkləri.
 *
 * Sxem **tətbiqə xasdır** (`anamuslim://`), `https://` deyil: veb domen App Links/Universal Links
 * təsdiqi tələb edir (`.well-known/assetlinks.json` və `apple-app-site-association`), o isə bizim
 * domenimiz olmadan heç vaxt keçmir — bax manifestdəki `quran.com` qeydi. Tətbiqə xas sxem hər iki
 * platformada təsdiqsiz işləyir.
 *
 * Analiz **əl ilədir**, URL kitabxanası ilə yox: `Uri` Android-ə, `NSURL` iOS-a aiddir, forma isə
 * bu qədər sadə olanda ortaq kod hər iki platformada eyni nəticəni verir — və testlənir.
 */
object DuaDeepLink {

    const val SCHEME = "anamuslim"

    private const val HOST_DUA = "dua"
    private const val HOST_ASMA = "asma"

    /** Əsmaül Hüsnədəki adların sayı — bundan kənar nömrə linki yanlışdır. */
    private const val ASMA_MAX = 99

    fun duaUrl(duaId: Long): String = "$SCHEME://$HOST_DUA/$duaId"

    fun asmaUrl(nameNo: Int): String = "$SCHEME://$HOST_ASMA/$nameNo"

    /**
     * Linki hədəfə çevirir; tanınmayan, yanlış və ya boş link üçün `null`.
     *
     * `null` qaytarmaq **səssiz uğursuzluq deyil**: çağıran tərəf onu «bu link bizim deyil» kimi
     * oxuyur və sistemə geri verir (Android-də başqa tətbiq, iOS-da heç nə). Yanlış linkdə ekran
     * açmaq daha pis olardı — istifadəçi hansı duanı gözlədiyini bilir, biz isə bilmirik.
     */
    fun parse(url: String?): DuaDeepLinkTarget? {
        val raw = url?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val prefix = "$SCHEME://"
        if (!raw.startsWith(prefix, ignoreCase = true)) return null

        // ⚠️ `removePrefix` **hərf həssasdır**: `ANAMUSLIM://…` linkində o, heç nə kəsmir və analiz
        // səssizcə `null` qaytarırdı (yuxarıdakı `startsWith` isə `ignoreCase` ilə keçirdi).
        // Uzunluqla kəsmək bu uyğunsuzluğu aradan qaldırır.
        //
        // Sorğu və fraqment atılır: `anamuslim://dua/7?utm=...` da işləməlidir.
        val body = raw.drop(prefix.length).substringBefore('?').substringBefore('#')
        val segments = body.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null

        val host = segments[0].lowercase()
        val value = segments[1]

        return when (host) {
            HOST_DUA -> value.toLongOrNull()?.takeIf { it > 0 }?.let(DuaDeepLinkTarget::Dua)

            HOST_ASMA -> value.toIntOrNull()
                ?.takeIf { it in 1..ASMA_MAX }
                ?.let(DuaDeepLinkTarget::Asma)

            else -> null
        }
    }
}
