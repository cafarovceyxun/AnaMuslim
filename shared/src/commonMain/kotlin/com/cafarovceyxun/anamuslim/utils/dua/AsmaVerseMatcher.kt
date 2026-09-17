package com.cafarovceyxun.anamuslim.utils.dua

import com.cafarovceyxun.anamuslim.search.SearchNormalizer

/**
 * Əsmaül Hüsnə adlarını Quran ayələri ilə **hərəkəsiz** uyğunlaşdırmaq üçün təmiz funksiyalar.
 *
 * Bazaya toxunmur: burada yalnız adın hansı yazılış variantlarının axtarılacağı həll olunur, axtarış
 * özü `quranapp.db`-dəki `arabic_search` FTS4 cədvəlindədir (mətn orada onsuz da hərəkəsizdir).
 */
object AsmaVerseMatcher {

    /**
     * «ال» artiklı atılandan sonra qalığın **ən az** uzunluğu.
     *
     * Bu hədd yalan pozitivlərə qarşıdır: «الله» → «له» (ona) və «الحي» → «حي» (diri) kimi iki hərfli
     * qalıqlar Quranda yüzlərlə yerdə adi söz və ya əvəzlik kimi keçir, yəni ad axtarışını istifadəsiz
     * səs-küyə çevirərdi. Üç hərfdən başlayan qalıqlar («ملك», «سلام», «عزيز») isə artıq ad kökünün
     * özüdür.
     */
    private const val MIN_STEM_LENGTH = 3

    private const val DEFINITE_ARTICLE = "ال"

    /**
     * Adın axtarılacaq yazılış variantları — normallaşdırılmış, təkrarsız.
     *
     * İki variant qurulur: ad olduğu kimi və baş «ال» atılmış hal. Səbəb: Quran hər iki formanı
     * işlədir — «الْغَفُورُ الرَّحِيمُ» (artikllı) ↔ «رَءُوفٌ رَحِيمٌ» (artiklsız). Yalnız artikllı
     * formanı axtarmaq ikinci qrupu tamamilə itirərdi.
     *
     * ⚠️ Proklitiklər («بالرحمن», «والرحمن», «فالرحمن», «للرحمن») **daxil edilmir**: FTS-də onlar ayrı
     * tokendir və dəqiq axtarışa düşmür. Bu, bilinən bir əskikdir — genişləndirmədən əvvəl mövcud əl
     * ilə əlavə edilmiş dəlillərlə ölçmək lazımdır, çünki hər yeni variant yalan pozitiv riskini də
     * artırır.
     *
     * @return boş siyahı — ad ərəbcə deyilsə və ya normallaşdırmadan sonra heç nə qalmırsa.
     */
    fun arabicNameVariants(nameAr: String): List<String> {
        val normalized = SearchNormalizer.quranNormalize(SearchNormalizer.normalize(nameAr))
        if (normalized.isBlank() || !SearchNormalizer.containsArabic(normalized)) return emptyList()

        val variants = mutableListOf(normalized)

        // Artikl yalnız **ilk sözdən** atılır: çoxsözlü adlarda («مالك الملك») ikinci sözün artiklı
        // ifadənin bir hissəsidir, onu atmaq adı tanınmaz hala salardı.
        val stem = normalized.removePrefix(DEFINITE_ARTICLE)
        if (stem !== normalized && stem.length >= MIN_STEM_LENGTH) {
            variants += stem
        }

        return variants.distinct()
    }

    /**
     * Ayənin normallaşdırılmış mətnində [variant] **tam söz kimi** keçirmi.
     *
     * FTS onsuz da tam token qaytarır (prefiks axtarışı qəsdən işlədilmir), ona görə bu funksiya
     * axtarışın süzgəci deyil — **göstərmə** tərəfi üçündür: ayə mətnində adın harada olduğunu
     * işarələmək və şübhəli nəticəni gözlə yoxlamaq lazım olanda işlənir.
     *
     * Sərhəd ərəbcə hərfin olub-olmaması ilə təyin olunur: ərəbcədə söz sərhədi boşluq və durğu
     * işarəsidir, `isLetter()` isə ərəbcə hərfləri də hərf sayır, yəni «رحمن»-in «الرحمن» içindəki
     * təsadüfi uyğunluğu belə kəsilir.
     */
    fun containsAsWord(normalizedAyah: String, variant: String): Boolean {
        if (variant.isBlank()) return false

        var from = 0
        while (true) {
            val at = normalizedAyah.indexOf(variant, from)
            if (at < 0) return false

            val before = normalizedAyah.getOrNull(at - 1)
            val after = normalizedAyah.getOrNull(at + variant.length)

            if (!before.isWordChar() && !after.isWordChar()) return true

            from = at + 1
        }
    }

    private fun Char?.isWordChar(): Boolean = this != null && (isLetter() || isDigit())
}
