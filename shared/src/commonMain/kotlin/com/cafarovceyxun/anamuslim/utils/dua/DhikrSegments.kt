package com.cafarovceyxun.anamuslim.utils.dua

/**
 * Hədisin içindəki **zikrləri** tapır ki, dua seçim ekranı onları bir toxunuşla təklif etsin.
 *
 * Bu topluda zikr həmişə eyni formada yazılır: ərəbcə mətndə və rəvayətdə mötərizə arasında
 * (rəvayətdə bu, **oxunuşdur**), mənası isə qeyddə «…tərcüməsi belədir: "…"» qəlibi ilə. Mötərizə
 * həm `[…]`, həm də `{…}` ola bilər ([BRACKET_PAIRS]): köhnə məzmun `{…}` ilə yazılıb, yenisi `[…]`
 * ilə yazılır, ikisi də tanınır. Bir hədisdə bir neçə
 * zikr ola bilər — Əhməd 803-də beşi var (açılış, rüku, rükudan qalxma, səcdə, salamdan sonra) və
 * hər biri başqa mövzuya gedir. Əvvəl hər birini əl ilə tutacaqlarla seçmək lazım idi, «Mötərizədə»
 * düyməsi isə yalnız **ilk** `{…}`-nu tapırdı.
 *
 * Cütləşdirmə indekslə gedir və **yalnız say eyni olanda**: rəvayətdə ərəbcədən az mötərizə varsa,
 * üçüncü zikrin altına dördüncünün oxunuşu düşərdi. Belə halda həmin sahə `null` qalır və boş sahə
 * əl ilə doldurulur — yanlış dolmuş sahədən yaxşıdır
 * ([com.cafarovceyxun.anamuslim.utils.verse.HadithExcerpt.pairedNarrations] ilə eyni ehtiyat).
 *
 * Mötərizələr və dırnaqlar nəticədən **atılır**: saxlanan dua ekranda `[…]` göstərməməlidir. Qaynaq
 * vurğusu yenə işləyir, çünki daxili mətn mənbənin alt sətridir (`excerptMatchRange`).
 */
data class DhikrSegment(
    val arabic: String,
    /** Rəvayətdəki `[…]`/`{…}` — yalnız mötərizə sayı ərəbcə ilə eyni olanda. */
    val transliteration: String?,
    /** Qeyddəki «…belədir: "…"» — yalnız tərcümə sayı ərəbcə ilə eyni olanda. */
    val translation: String?,
)

/** Açılan dırnaq → bağlanan dırnaq. `“` həm `„…“`-nun sonu, həm də `“…”`-nun əvvəlidir. */
private val QUOTE_PAIRS = mapOf(
    '"' to '"',
    '“' to '”',
    '«' to '»',
    '„' to '“',
)

/** Zikr mötərizəsi → bağlanan cütü. `[…]` yeni yazılış, `{…}` köhnə məzmundakı. */
private val BRACKET_PAIRS = mapOf(
    '[' to ']',
    '{' to '}',
)

/**
 * Mətndəki bütün zikr mötərizəsi aralıqları (`[…]` və `{…}`), soldan sağa — mötərizələrin **özü**
 * daxil.
 *
 * Mötərizə öz cütü ilə bağlanır: `[` yalnız `]` ilə, `{` yalnız `}` ilə. Bağlanmayan mötərizə
 * dayanma nöqtəsidir: ondan sonrakı mətn heç bir aralığa düşmür, əvvəlkilər isə qalır.
 *
 * ⚠️ Ərəbcə mətndə `[…]` bəzən redaktor əlavəsi üçün də işlənir. Belə hədisdə mötərizə sayı rəvayətlə
 * uyğun gəlməyə bilər — onda [dhikrSegments] oxunuşu/tərcüməni `null` qoyur və sahə əl ilə dolur.
 */
fun braceRanges(text: String): List<IntRange> = buildList {
    var from = 0
    while (from < text.length) {
        val open = text.indexOfAny(BRACKET_PAIRS.keys.toCharArray(), startIndex = from)
        if (open < 0) break

        val close = text.indexOf(BRACKET_PAIRS.getValue(text[open]), startIndex = open + 1)
        if (close < 0) break

        add(open..close)
        from = close + 1
    }
}

/**
 * Qeyddəki tərcümələr — dırnaq içindəki parçalar, amma **yalnız** «…belədir: "…"» qəlibində, yəni
 * açılan dırnaqdan əvvəlki ilk boşluq olmayan simvol `:` olanda.
 *
 * Qəlib şərti qəsdəndir: eyni qeydin sonunda «Buxari "Raf'ul-yədeyn"-də rəvayət etmişdir» kimi
 * kitab adları da dırnaqdadır. Onlar sayılsaydı tərcümə sayı ərəbcə zikrlərin sayından çox çıxar
 * və heç bir zikrə tərcümə cütlənməzdi.
 */
fun noteTranslations(note: String?): List<String> {
    if (note.isNullOrBlank()) return emptyList()

    val result = mutableListOf<String>()
    var index = 0

    while (index < note.length) {
        val closing = QUOTE_PAIRS[note[index]]
        if (closing == null) {
            index++
            continue
        }

        val end = note.indexOf(closing, startIndex = index + 1)
        if (end < 0) break

        var before = index - 1
        while (before >= 0 && note[before].isWhitespace()) before--

        if (before >= 0 && note[before] == ':') {
            note.substring(index + 1, end).trim()
                .takeIf { it.isNotEmpty() }
                ?.let(result::add)
        }

        // Qəlibə uymayan dırnaq da **bütöv** keçilir — yoxsa onun bağlanan dırnağı növbəti
        // parçanın açılanı sayılardı.
        index = end + 1
    }

    return result
}

/**
 * Hədisin zikrləri — ərəbcə `{…}` sayı qədər; ərəbcədə mötərizə yoxdursa boş siyahı.
 *
 * @param narration hədisin tərcümə mətni (rəvayət) — oxunuş buradakı `{…}`-dan götürülür.
 * @param note hədisin qeydi — tərcümə buradakı «…belədir: "…"» parçalarından götürülür.
 */
fun dhikrSegments(arabic: String, narration: String?, note: String?): List<DhikrSegment> {
    val arabicParts = braceContents(arabic)
    if (arabicParts.isEmpty()) return emptyList()

    val transliterations = braceContents(narration.orEmpty())
        .takeIf { it.size == arabicParts.size }
    val translations = noteTranslations(note)
        .takeIf { it.size == arabicParts.size }

    return arabicParts.mapIndexed { index, part ->
        DhikrSegment(
            arabic = part,
            transliteration = transliterations?.get(index)?.takeIf { it.isNotEmpty() },
            translation = translations?.get(index),
        )
    }
}

/** `{…}` parçalarının mötərizəsiz, kənar boşluqsuz içi. */
private fun braceContents(text: String): List<String> =
    braceRanges(text).map { range -> text.substring(range.first + 1, range.last).trim() }
