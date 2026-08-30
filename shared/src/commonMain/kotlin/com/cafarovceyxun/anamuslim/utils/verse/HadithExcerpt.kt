package com.cafarovceyxun.anamuslim.utils.verse

/**
 * Hədis mətnini cümlələrə bölür ki, admin paneldə **hansı hissənin** günün hədisi kimi
 * göstəriləcəyini toxunuşla seçə bilsin.
 *
 * Səbəb: bu topludakı hədislərin böyük hissəsi isnad zənciri ilə başlayır («Bizə Musəddəd danışdı,
 * bizə Yəhya danışdı…»), mətn isə sonda gəlir. Kartda və bildirişdə tam sətri göstərmək mənası
 * itirir; ona görə admin cümlələri seçir, seçim isə `excerpt_az`/`excerpt_ar` sahələrində **mətn
 * kimi** saxlanılır. Yeri (offset) saxlamırıq: hədis sonradan redaktə olunanda offset səssizcə
 * başqa yerə düşərdi, mətn isə ya uyğun gəlir, ya da göz qabağında yanlışdır.
 *
 * Bölgü iki səviyyəlidir: əvvəlcə **rəvayətlər** («Digər bir rəvayətdə…» / «وفي رواية…»), rəvayət
 * sərhədi yoxdursa cümlələr. Qısaltmalara görə səhv bölünən cümlə problem deyil: admin qonşu
 * parçaları da seçir.
 */
object HadithExcerpt {

    private const val TERMINATORS = ".!?…؟۔"

    /**
     * Rəvayət sərhədləri — bu topluda hər hədisin ardınca gələn variantlar məhz belə başlayır.
     * Ərəbcə mətn eyni yerdə öz qarşılığı ilə bölünür, ona görə iki dil eyni sayda blok verir.
     */
    private val NARRATION_MARKERS = listOf("Digər bir rəvayətdə", "وفي رواية")

    /**
     * [text]-i seçilə bilən parçalara bölür.
     *
     * **Əvvəlcə rəvayətlərə**: bu topluda bir hədis çox vaxt bir neçə rəvayətdən ibarətdir
     * («… (Buxari, 3035). Digər bir rəvayətdə: …») və admin adətən **bir rəvayəti** seçmək istəyir.
     * Rəvayət sərhədi tapılmasa parça cümlələrə bölünür — qısa hədisdə seçim bu dəqiqlikdə lazımdır.
     *
     * Ayırıcı işarə parçanın **özündə** qalır, ona görə seçilmişləri birləşdirmək mətni verir.
     */
    fun sentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val narrations = narrations(text)
        if (narrations.size > 1) return narrations

        return splitOnTerminators(text)
    }

    /** Mətni rəvayət başlanğıclarından bölür; işarə yoxdursa tək parça qaytarır. */
    private fun narrations(text: String): List<String> {
        val starts = NARRATION_MARKERS
            .flatMap { marker -> marker.toRegex().findAll(text).map { it.range.first } }
            .sorted()
            .filter { it > 0 }

        if (starts.isEmpty()) return listOf(text.trim())

        val bounds = (listOf(0) + starts + listOf(text.length)).distinct()

        return bounds.zipWithNext { from, to -> text.substring(from, to).trim() }
            .filter { it.isNotEmpty() }
    }

    private fun splitOnTerminators(text: String): List<String> {

        val result = mutableListOf<String>()
        val current = StringBuilder()

        for (char in text) {
            current.append(char)

            if (char in TERMINATORS) {
                val piece = current.toString().trim()
                if (piece.isNotEmpty()) result += piece
                current.clear()
            }
        }

        val tail = current.toString().trim()
        if (tail.isNotEmpty()) result += tail

        return result
    }

    /** Seçilmiş indekslərdən çıxarış mətni — sıra həmişə orijinal mətnin sırasıdır. */
    fun join(sentences: List<String>, selected: Set<Int>): String =
        sentences.filterIndexed { index, _ -> index in selected }.joinToString(" ")

    /**
     * Saxlanmış çıxarışdan hansı cümlələrin seçildiyini bərpa edir — paneli təkrar açanda
     * əvvəlki seçim işarəli görünsün deyə. Çıxarış əl ilə redaktə olunubsa uyğunluq tapılmır və
     * boş dəst qayıdır; bu halda panel mətni sərbəst mətn kimi göstərir.
     */
    fun selectionOf(sentences: List<String>, excerpt: String?): Set<Int> {
        if (excerpt.isNullOrBlank()) return emptySet()

        val selected = sentences.indices.filter { sentences[it] in excerpt }.toSet()

        return if (join(sentences, selected) == excerpt.trim()) selected else emptySet()
    }
}
