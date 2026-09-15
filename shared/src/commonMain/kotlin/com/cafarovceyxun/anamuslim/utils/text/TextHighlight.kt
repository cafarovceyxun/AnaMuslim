package com.cafarovceyxun.anamuslim.utils.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Sarı vurğu fonu — həm axtarış nəticələrində uyğun gələn hissə, həm də moderasiya panelindəki
 * fərq (diff) üçün. Şəffaflığı qəsdən aşağıdır ki, qaranlıq mövzuda da mətn oxunaqlı qalsın.
 */
val TextHighlightYellow = Color(0x66FFD858)

/**
 * **Cari** uyğunluğun fonu — oxların hazırda dayandığı söz.
 *
 * Sarıdan fərqli (narıncı) olmalıdır: bir neçə uyğunluq eyni ekranda görünəndə «neçənci»
 * sayğacı tək başına hansının olduğunu demir — istifadəçi oxa basır, ekran sürüşür, amma hansı sözə
 * gəldiyi bilinmir.
 */
val TextHighlightCurrent = Color(0xAAFF9800)

/** [TextHighlightYellow] fonu — uyğunluqlara tətbiq olunan üslub. */
val SearchHighlightStyle = SpanStyle(background = TextHighlightYellow)

/** Cari uyğunluğun üslubu — bax [TextHighlightCurrent]. */
val SearchCurrentHighlightStyle = SpanStyle(background = TextHighlightCurrent)

/**
 * Axtarış üçün «yastılanmış» mətn və hər simvolun **orijinaldakı** mövqeyi.
 *
 * Yastılama diakritikləri atır, ona görə yastı mətndə *i* indeksində tapılan uyğunluq orijinalda
 * tamam başqa (və əvvəlcədən bilinməyən) yerdədir — cədvəl məhz bunun üçündür. Boşluqlar
 * qəsdən toxunulmadan qalır: alt-sətir axtarışına lazım deyil, cədvəlin asılı olduğu bir-bir
 * simvol uyğunluğunu isə pozardı.
 *
 * ⚠️ Registr **simvol-simvol** açılır (`lowercaseChar`), sətir səviyyəsində `lowercase()` ilə yox:
 * azərbaycanca «İ» sətir kimi kiçildiləndə iki simvola («i» + birləşən nöqtə) açılır və ondan
 * sonrakı bütün ofsetlər sürüşür — vurğu səhv hərflərin üstünə düşərdi.
 */
fun foldSearchTextWithOffsets(text: String): Pair<String, IntArray> {
    val builder = StringBuilder(text.length)
    val offsets = IntArray(text.length)

    text.forEachIndexed { index, char ->
        val lower = char.lowercaseChar()
        val folded = when {
            lower in 'ً'..'ٟ' || lower == 'ٰ' || lower == 'ـ' -> null
            // Quranic annotation marks (U+06D6–U+06ED): waqf signs, the small letters, the
            // end-of-ayah sign. The muṣḥaf text is full of them — `بِسۡمِ` carries U+06E1 — while
            // the search index has none, so a fold that kept them found the row and then
            // highlighted nothing in the preview.
            lower in '\u06D6'..'\u06ED' -> null
            lower == 'أ' || lower == 'إ' || lower == 'آ' || lower == 'ٱ' -> 'ا'
            lower == 'ى' -> 'ي'
            else -> lower
        } ?: return@forEachIndexed

        offsets[builder.length] = index
        builder.append(folded)
    }

    return builder.toString() to offsets
}

/**
 * Sorğunun sözləri [text] içində harada keçir — orijinal mətnin indeksləri ilə, bitişik/üst-üstə
 * düşən uyğunluqlar birləşdirilmiş halda.
 *
 * İki hərfdən qısa sözlər atılır (axtarışın özündəki qayda ilə eyni): «və», «bu» kimi köməkçi
 * hecalar səhifənin yarısını sarıya boyayardı.
 */
fun searchMatchRanges(text: String, rawQuery: String): List<IntRange> {
    val tokens = rawQuery
        .trim()
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.length >= 2 }
        .distinctBy { it.lowercase() }

    if (tokens.isEmpty() || text.isEmpty()) return emptyList()

    val (folded, offsets) = foldSearchTextWithOffsets(text)
    if (folded.isEmpty()) return emptyList()

    val spans = mutableListOf<IntRange>()
    // Uzun sözdən qısaya: qısa söz uzununun içində də tapılır, birləşdirmə onsuz da eyni nəticəni
    // verir — sıra yalnız axtarışın öz davranışı ilə eyni qalsın deyə saxlanılır.
    for (token in tokens.sortedByDescending { it.length }) {
        val needle = foldSearchTextWithOffsets(token).first
        if (needle.isEmpty()) continue
        var idx = 0

        while (idx < folded.length) {
            val at = folded.indexOf(needle, idx)
            if (at < 0) break
            spans += offsets[at] until (offsets[at + needle.length - 1] + 1)
            idx = at + needle.length
        }
    }

    if (spans.isEmpty()) return emptyList()

    return spans
        .sortedBy { it.first }
        .fold(mutableListOf()) { acc: MutableList<IntRange>, range ->
            val last = acc.lastOrNull()
            if (last == null || range.first > last.last + 1) {
                acc.add(range)
            } else {
                acc[acc.lastIndex] = last.first..maxOf(last.last, range.last)
            }
            acc
        }
}

/**
 * Sorğunun uyğunluqlarını sarı fonla işarələyir — mətnin öz üslublarına (məsələn mötərizə rəngi)
 * toxunmadan, onların **üstündən**.
 *
 * `null`/boş sorğuda mətn olduğu kimi qaytarılır, ona görə çağıran yerdə şərt yazmağa ehtiyac
 * yoxdur: axtarışdan gəlməyən oxucu eyni kodla işləyir.
 */
/**
 * @param currentMatch oxların dayandığı uyğunluğun sıra nömrəsi (0-dan) — həmin söz narıncı,
 *   qalanları sarı olur. `null` = hamısı sarı (adi vurğu).
 */
fun AnnotatedString.withSearchHighlight(
    query: String?,
    currentMatch: Int? = null,
): AnnotatedString {
    val raw = query?.trim().orEmpty()
    if (raw.isEmpty()) return this

    val ranges = searchMatchRanges(this.text, raw)
    if (ranges.isEmpty()) return this

    return buildAnnotatedString {
        append(this@withSearchHighlight)
        ranges.forEachIndexed { index, range ->
            val style = if (index == currentMatch) SearchCurrentHighlightStyle
            else SearchHighlightStyle
            addStyle(style, range.first, range.last + 1)
        }
    }
}

/** [withSearchHighlight]-in düz sətir üçün qarşılığı. */
fun String.withSearchHighlight(
    query: String?,
    currentMatch: Int? = null,
): AnnotatedString {
    val raw = query?.trim().orEmpty()
    if (raw.isEmpty()) return AnnotatedString(this)

    val ranges = searchMatchRanges(this, raw)
    if (ranges.isEmpty()) return AnnotatedString(this)

    return buildAnnotatedString {
        append(this@withSearchHighlight)
        ranges.forEachIndexed { index, range ->
            val style = if (index == currentMatch) SearchCurrentHighlightStyle
            else SearchHighlightStyle
            addStyle(style, range.first, range.last + 1)
        }
    }
}
