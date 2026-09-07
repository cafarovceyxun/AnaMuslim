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
     * İşarə ilə mətnin arasındakı ayırıcılar — [withoutNarrationMarker] onları da atır.
     *
     * Tire siyahıda **yoxdur**: bu topluda rəvayət mətnləri onsuz da «-» ilə başlayır (birinci
     * rəvayət də daxil), ona görə onu atmaq seçilmiş parçanı qalanlardan fərqli göstərərdi.
     */
    private val MARKER_SEPARATORS =
        charArrayOf(':', '،', ',', ' ', '\u00A0', '\n', '\r', '\t')

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

    /**
     * **Yalnız** rəvayət sərhədləri üzrə bölür — [sentences]-dən fərqli olaraq cümlə fallback-ı
     * yoxdur, yəni bir rəvayətli hədis tək parça qaytarır.
     *
     * Paylaşma axını buna görə ayrıdır: orada sual «hansı rəvayət», «hansı cümlə» deyil. Cümlələrə
     * də bölsəydik tək rəvayətli hədis onlarla parçaya dağılır və istifadəçiyə mənasız bir seçim
     * pəncərəsi açılardı — bir parça qayıdanda isə çağıran tərəf dialoqu heç açmır.
     */
    fun narrationParts(text: String): List<String> =
        if (text.isBlank()) emptyList() else narrations(text)

    /**
     * Ərəbcə və tərcümə rəvayətlərini cüt-cüt qarşılaşdırır — oxucudakı «rəvayətləri qarşılaşdır»
     * ayarı ([com.cafarovceyxun.anamuslim.compose.utils.preferences.HadithPreferences.PAIR_NARRATIONS])
     * mətni məhz bu cütlərlə düzür: hər rəvayətin tərcüməsi öz ərəbcəsinin altında.
     *
     * `null` qayıdışı «qarşılaşdırma mümkün deyil» deməkdir və çağıran tərəf onda köhnə düzülüşə
     * (bütöv ərəbcə, sonra bütöv tərcümə) qayıdır. İki hal belədir:
     *  - **tək rəvayət** — qarşılaşdırılacaq bir şey yoxdur, bölgü yalnız artıq boşluq yaradardı;
     *  - **fərqli parça sayı** — tərcümədə rəvayət sərhədi var, ərəbcədə yox (və ya əksi). İndeks
     *    uyğunluğu onda **saxta** olardı: ikinci ərəbcə rəvayətin altına üçüncünün tərcüməsi düşərdi.
     *    Eyni ehtiyat paylaşma axınındakı
     *    [com.cafarovceyxun.anamuslim.compose.screens.hadith.HadithShareSheet]-dədir.
     */
    fun pairedNarrations(arabic: String, translation: String): List<Pair<String, String>>? {
        val arabicParts = narrationParts(arabic)
        val translationParts = narrationParts(translation)

        if (arabicParts.size < 2 || arabicParts.size != translationParts.size) return null

        return arabicParts.zip(translationParts)
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

    /**
     * Parça rəvayət işarəsi ilə başlayırmı — yəni [withoutNarrationMarker] onda bir şey dəyişirmi.
     *
     * Paylaşma vərəqi «işarəni çıxar» keçidini məhz buna görə göstərir: hədisin ilk (və ya yeganə)
     * rəvayəti seçiləndə atılacaq söz yoxdur, keçid isə orada olsa nə etdiyi bilinməzdi.
     */
    fun hasNarrationMarker(part: String): Boolean {
        val trimmed = part.trimStart()

        return NARRATION_MARKERS.any { trimmed.startsWith(it) }
    }

    /**
     * Rəvayət parçasının başındakı işarəni («Digər bir rəvayətdə:», «وفي رواية:») atır.
     *
     * Paylaşmada lazımdır: bir neçə rəvayətdən yalnız biri seçiləndə parça hələ də «Digər bir
     * rəvayətdə» ilə başlayır — tək başına paylaşılan mətn onda görünməyən bir mətnə istinad edir.
     * İşarədən sonrakı ayırıcı durğu işarələri də düşür, mətnin özünə toxunulmur.
     */
    fun withoutNarrationMarker(part: String): String {
        val trimmed = part.trim()
        val marker = NARRATION_MARKERS.firstOrNull { trimmed.startsWith(it) } ?: return trimmed

        return trimmed.drop(marker.length).trimStart(*MARKER_SEPARATORS)
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
