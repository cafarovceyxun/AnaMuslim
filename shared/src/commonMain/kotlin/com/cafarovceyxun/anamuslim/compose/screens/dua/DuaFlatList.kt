package com.cafarovceyxun.anamuslim.compose.screens.dua

import com.cafarovceyxun.anamuslim.utils.supabase.Dua
import com.cafarovceyxun.anamuslim.utils.supabase.DuaCategory
import com.cafarovceyxun.anamuslim.utils.supabase.DuaSubcategory

/**
 * Vərəqləyicidəki bir səhifə — dua və onun mövzu konteksti.
 *
 * Kontekst səhifənin özündə daşınır, çünki vərəqləyici artıq **bir mövzunun içində** deyil: sağa
 * sürüşdürmək mövzu sərhədini keçə bilir və ekran hansı mövzuda olduğunu hər səhifədə bilməlidir.
 */
internal data class DuaFlatEntry(
    val dua: Dua,
    val category: DuaCategory,
    val subcategory: DuaSubcategory?,
    /** Bu, öz qrupunun **ilk** duasıdır — səhifədə mövzu başlığı yalnız burada göstərilir. */
    val isGroupStart: Boolean,
) {
    /** Ekranda görünən mövzu adı: alt başlıq varsa o, yoxsa başlıq. */
    val groupTitle: String get() = subcategory?.name ?: category.name

    /** Qrupu birmənalı göstərən açar — sıralama və geri qayıtma bundan istifadə edir. */
    val groupKey: String get() = subcategory?.slug ?: category.slug
}

/**
 * Üç səviyyəli ağacı **tək siyahıya** yastılayır: başlıq → (birbaşa dualar) → alt başlıqlar.
 *
 * Səbəb: əvvəl vərəqləyici yalnız bir qrupun dualarını göstərirdi, yəni növbəti mövzuya keçmək üçün
 * geri qayıdıb siyahıdan seçmək lazım gəlirdi. Yastı siyahı ilə sürüşdürmə fasiləsiz olur —
 * `AsmaDetailPager`-in 99 ad üzərində işləməsi ilə eyni forma.
 *
 * Sıra **gələn sıradır**: repozitoriya kateqoriyaları, alt kateqoriyaları və duaları onsuz da
 * `sort_no` üzrə düzülmüş qaytarır, ona görə burada yenidən sıralama aparılmır — əks halda
 * istifadəçinin sürükləyib qurduğu sıra ekranda itərdi.
 *
 * Başlığın **birbaşa** duaları (alt başlığı olmayanlar) alt başlıqlardan **əvvəl** gəlir: siyahı
 * ekranı da onları yuxarıda göstərir (`DuaSubcategoryScreen`), iki yerdə iki fərqli sıra isə
 * «dua itdi» hissi yaradardı.
 *
 * Başlığı tanınmayan dualar (silinmiş başlıq, məlumat qüsuru) **kənarda qalır**: onların mövzu
 * konteksti yoxdur, ona görə səhifədə başlıq sətri boş çıxardı. Alt başlığı tanınmayanlar isə
 * kənarda qalmır — onlar başlığın birbaşa duası kimi göstərilir.
 */
internal fun flattenDuas(
    categories: List<DuaCategory>,
    subcategories: List<DuaSubcategory>,
    duas: List<Dua>,
): List<DuaFlatEntry> {
    if (categories.isEmpty() || duas.isEmpty()) return emptyList()

    val byCategory = duas.groupBy { it.category_slug }
    val subsByCategory = subcategories.groupBy { it.category_slug }

    return buildList {
        categories.forEach { category ->
            val categoryDuas = byCategory[category.slug].orEmpty()
            if (categoryDuas.isEmpty()) return@forEach

            val categorySubs = subsByCategory[category.slug].orEmpty()
            val knownSubSlugs = categorySubs.mapTo(mutableSetOf()) { it.slug }
            val bySubcategory = categoryDuas.groupBy { it.subcategory_slug }

            // Alt başlığı **tanınmayan** dualar da birbaşa başlığın altına düşür: alt başlıq başqa
            // cihazdan silinəndə sətir `dua`-da qalır (FK `on delete set null` deyil), belə dua isə
            // heç bir qrupa düşməyib səssizcə yoxa çıxardı.
            val directDuas = categoryDuas.filter {
                it.subcategory_slug == null || it.subcategory_slug !in knownSubSlugs
            }

            directDuas.forEachIndexed { index, dua ->
                add(
                    DuaFlatEntry(
                        dua = dua,
                        category = category,
                        subcategory = null,
                        isGroupStart = index == 0,
                    ),
                )
            }

            categorySubs.forEach { subcategory ->
                bySubcategory[subcategory.slug].orEmpty().forEachIndexed { index, dua ->
                    add(
                        DuaFlatEntry(
                            dua = dua,
                            category = category,
                            subcategory = subcategory,
                            isGroupStart = index == 0,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Yastı siyahıda verilmiş qrupun ilk səhifəsinin indeksi.
 *
 * Qrup tapılmasa `0` qaytarır: istifadəçinin açdığı alt başlıq elə bu anda silinmiş ola bilər
 * (başqa cihazdan redaktə), ekran isə boş yox, siyahının başından açılmalıdır.
 */
internal fun indexOfGroup(entries: List<DuaFlatEntry>, groupKey: String?): Int {
    if (groupKey == null) return 0
    return entries.indexOfFirst { it.groupKey == groupKey }.coerceAtLeast(0)
}

/** Verilmiş səhifə ilə **eyni qrupdakı** duaların id-ləri — sıralama ekranı bunları alır. */
internal fun groupDuaIds(entries: List<DuaFlatEntry>, groupKey: String): List<Long> =
    entries.filter { it.groupKey == groupKey }.mapNotNull { it.dua.id }

/**
 * Səhifənin **öz qrupundakı** mövqeyi: `(neçənci, neçədən)`, hər ikisi 1-dən başlayır.
 *
 * Vərəqləyici bütün dualar üzərində olsa da nişan qrup daxilində qalır: «3 / 12» istifadəçiyə
 * mövzunun harasında olduğunu deyir, «3 / 47» isə heç nə demir — bütün duaların sayı onun üçün
 * mənalı vahid deyil.
 *
 * Qrup elementləri siyahıda **bitişikdir** ([flattenDuas] onları ardıcıl yazır), ona görə sərhədlər
 * sadəcə sağa-sola gedərək tapılır — bütün siyahını süzməyə ehtiyac yoxdur.
 */
internal fun groupPositionOf(entries: List<DuaFlatEntry>, page: Int): Pair<Int, Int> {
    val entry = entries.getOrNull(page) ?: return 1 to 1
    val key = entry.groupKey

    var start = page
    while (start > 0 && entries[start - 1].groupKey == key) start--

    var end = page
    while (end < entries.lastIndex && entries[end + 1].groupKey == key) end++

    return (page - start + 1) to (end - start + 1)
}
