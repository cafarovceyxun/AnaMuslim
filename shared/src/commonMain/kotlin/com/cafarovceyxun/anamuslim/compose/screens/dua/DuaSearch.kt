package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.text.searchMatchRanges

/**
 * Bir duanın axtarışa açıq **bütün** mətnləri.
 *
 * Mövzu adları da daxildir: istifadəçi «namazdan sonra» yazanda həmin mövzunun duasını gözləyir,
 * halbuki o söz duanın öz mətnində keçməyə bilər.
 *
 * ⚠️ Başlıq **və** alt başlıq, [groupTitle] deyil: o, alt başlıq varsa yalnız onu qaytarır, yəni
 * başlığın adını yazan adam onun alt bölmələrindəki duaları tapa bilmirdi — halbuki ağacda onlar
 * məhz həmin başlığın altındadır.
 */
private fun DuaFlatEntry.searchableTexts(): List<String> = listOf(
    dua.text_ar,
    dua.transliteration.orEmpty(),
    dua.text_az,
    dua.note.orEmpty(),
    dua.source.orEmpty(),
    category.name,
    subcategory?.name.orEmpty(),
).filter { it.isNotBlank() }

/**
 * Sorğuya uyğun gələn **səhifə indeksləri**, sıra ilə.
 *
 * Uyğunluq [searchMatchRanges] ilə yoxlanılır, yəni axtarış ekranı ilə **eyni** qaydalar işləyir:
 * ərəbcə hərəkələr və alif variantları atılır (hərəkəli mətni mushafdan kopyalayıb yapışdırmaq
 * işləyir), iki hərfdən qısa sözlər isə buraxılır — «və», «bu» kimi hecalar hər səhifəni tutardı.
 *
 * ⚠️ Nəticə **səhifə** indeksləridir, uyğunluq sayı yox: bir səhifədə bir neçə keçid ola bilər,
 * amma vərəqləyicidə gəzinti vahidi səhifədir. Zolaqdakı «3 / 17» də buna görə səhifələri sayır.
 */
internal fun duaSearchMatches(entries: List<DuaFlatEntry>, query: String): List<Int> {
    val raw = query.trim()
    if (raw.isEmpty()) return emptyList()

    return entries.indices.filter { index ->
        entries[index].searchableTexts().any { searchMatchRanges(it, raw).isNotEmpty() }
    }
}

/**
 * Cari səhifənin uyğunluqlar siyahısındakı **1-dən başlayan** nömrəsi; səhifə uyğun deyilsə `0`.
 *
 * Zolaq «hansı uyğunluqdayıq» sualına cavab verir, ona görə sürüşdürüb uyğun olmayan səhifəyə
 * düşəndə sayğac sıfırlanır — yanlış nömrə göstərməkdənsə heç nə göstərmək daha doğrudur.
 */
internal fun duaMatchPosition(matches: List<Int>, page: Int): Int =
    (matches.indexOf(page) + 1).coerceAtLeast(0)

/**
 * [page]-dən **sonrakı** ilk uyğunluq; yoxdursa başa dolanır.
 *
 * Dolanma qəsdənidir: 17 uyğunluğun sonuncusundan «növbəti»ni basan adam axtarışın bitdiyini yox,
 * siyahının başa qayıtdığını gözləyir (brauzerdəki «səhifədə tap» ilə eyni davranış).
 */
internal fun duaNextMatch(matches: List<Int>, page: Int): Int? =
    matches.firstOrNull { it > page } ?: matches.firstOrNull()

/** [page]-dən **əvvəlki** son uyğunluq; yoxdursa sona dolanır — bax [duaNextMatch]. */
internal fun duaPreviousMatch(matches: List<Int>, page: Int): Int? =
    matches.lastOrNull { it < page } ?: matches.lastOrNull()
