package com.cafarovceyxun.anamuslim.utils.reader

import com.cafarovceyxun.anamuslim.db.entities.quran.AyahWordEntity
import com.cafarovceyxun.anamuslim.search.SearchNormalizer
import com.cafarovceyxun.anamuslim.utils.dua.AsmaVerseMatcher
import com.cafarovceyxun.anamuslim.utils.text.excerptMatchRange
import com.cafarovceyxun.anamuslim.utils.text.searchMatchRanges

/**
 * Ayənin ərəbcə mətnində **nəyin** sarı ilə işarələnəcəyi — sürətli baxış vərəqi üçün.
 *
 * Üç mənbə üç cür axtarır, ona görə ayrı növlərdir: axtarış sorğusu söz-söz uyğunlaşır, Əsma adı
 * kartdakı qayda ilə (tam söz, xəncər əlif — [AsmaVerseMatcher.nameRangeIn]), dəlilin çıxarışı isə
 * **bitişik bir parçadır** ([excerptMatchRange]). Data sinifləridir: vərəqin elementləri dəyər
 * bərabərliyi ilə tapılır.
 */
sealed interface VerseHighlight {
    /** Axtarış sorğusu — ərəbcə yazılıbsa (latın sorğusu tərcümədə işarələnir). */
    data class Query(val text: String) : VerseHighlight

    /** Əsmaül Hüsnə adı — avtomatik tapılan ayədə adın özü. */
    data class Name(val nameAr: String) : VerseHighlight

    /** Dəlilin ərəbcə çıxarışı — ayənin dəlil olan hissəsi. */
    data class Excerpt(val textAr: String) : VerseHighlight
}

/**
 * Ayənin [words]-ündən [highlight]-a düşənlərin `wordIndex`-ləri.
 *
 * Sözlər birləşdirilib bir mətn qurulur (axtarış kartı və Əsma kartı ayəni eyni cür — sözlər
 * boşluqla — göstərir), uyğunluq o mətndə simvol aralığı kimi tapılır, sonra aralığa toxunan
 * sözlərə köçürülür. Ərəbcə mətn ekranda sözbəsöz xanalarla çəkildiyi üçün vurğu da söz
 * dəqiqliyindədir.
 *
 * [words] **uthmani** yazısında olmalıdır (hərflər), nömrə işarəsi olan son söz daxil ola bilər —
 * rəqəm heç bir ərəbcə sorğuya uyğun gəlmir.
 */
fun VerseHighlight.wordIndexesIn(words: List<AyahWordEntity>): Set<Int> {
    if (words.isEmpty()) return emptySet()

    val starts = IntArray(words.size)
    val text = buildString {
        words.forEachIndexed { i, word ->
            if (i > 0) append(' ')
            starts[i] = length
            append(word.text)
        }
    }

    val ranges: List<IntRange> = when (this) {
        is VerseHighlight.Query -> {
            if (!SearchNormalizer.containsArabic(this.text)) return emptySet()
            // Axtarışın özü kimi normallaşdırılır — kartda sarı olan söz vərəqdə də sarı olsun.
            searchMatchRanges(text, SearchNormalizer.quranNormalize(SearchNormalizer.normalize(this.text)))
        }

        is VerseHighlight.Name -> listOfNotNull(AsmaVerseMatcher.nameRangeIn(text, nameAr))
        is VerseHighlight.Excerpt -> listOfNotNull(excerptMatchRange(text, textAr))
    }
    if (ranges.isEmpty()) return emptySet()

    return words.indices.filter { i ->
        val start = starts[i]
        val end = start + words[i].text.length - 1
        ranges.any { it.first <= end && it.last >= start }
    }.mapTo(HashSet()) { words[it].wordIndex }
}
