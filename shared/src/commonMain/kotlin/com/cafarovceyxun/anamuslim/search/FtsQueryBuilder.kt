package com.cafarovceyxun.anamuslim.search

object FtsQueryBuilder {

    fun toTranslationTextQuery(rawQuery: String): String? =
        toMatchQuery(translationTerms(rawQuery))

    /** Tərcümə indeksinə gedən sorğunun sözləri — bax [searchTerms]. */
    fun translationTerms(rawQuery: String): List<String> =
        searchTerms(SearchNormalizer.normalize(rawQuery))

    fun toPrefixAndQuery(
        normalizedQuery: String,
        matchColumn: String? = null,
    ): String? = toMatchQuery(searchTerms(normalizedQuery), matchColumn)

    /**
     * Sorğunun FTS sözləri — normallaşdırılmış sorğudan, indeksin özü ilə eyni qaydada.
     *
     * Əvvəl söz xam halda sorğuya yazılırdı (`kitabdır,*`, `(c.c)*`, `2:255*`): FTS4 durğu işarəsini
     * operator kimi oxuyur — mötərizə qrup, `:` sütun süzgəci, öndəki `-` isə NOT olur. Yapışdırılmış
     * uzun mətnin hər cümləsində belə işarə var, ona görə «uzun mətni tapmır»dı. İndi kənar durğu
     * atılır, söz isə [toMatchQuery]-də dırnağa alınır: dırnaq içində FTS heç nəyi operator saymır,
     * qalan durğunu (`şəkk-şübhə`, `ne’mət`) isə indeksdəki kimi söz ayırıcısı kimi işlədir.
     *
     * Qaydalar:
     * - `-11:` ayə başlığı atılır — oxucudan kopyalanan ayə onunla başlayır, indeksdə isə yoxdur.
     * - Sözə yapışıq 1–2 rəqəm (`möminlər1`, üst-indeksdən kopyalanan haşiyə) atılır — indeks onları
     *   saxlamır ([indexableTranslationText]).
     * - Rəqəm **tək** də ola bilər («7»); hərf sözü isə ən azı iki simvol olmalıdır — «o», «a»
     *   bütün bazanı qaytarar, AND sorğusunda isə onsuz da heç nə daraltmır.
     */
    fun searchTerms(normalizedQuery: String): List<String> =
        normalizedQuery
            .replace(VERSE_LABEL_PREFIX, "")
            .split(WHITESPACE)
            .map { stripGluedFootnote(it.replace("\"", "").trim { c -> !c.isLetterOrDigit() }) }
            .filter { token -> isNumber(token) || token.count { it.isLetterOrDigit() } >= 2 }
            .distinct()

    /**
     * [terms]-in AND sorğusu. Söz prefikslə (`"iman*"`), **ədəd isə dəqiq** (`"40"`) axtarılır:
     * `40*` «400»-ü, «4»-ün prefiksi isə bütün rəqəmləri tutardı.
     *
     * ⚠️ Prefiks ulduzu dırnağın **içində** olmalıdır. FTS4-də `"iman"*` prefiks deyil — ulduz səssizcə
     * atılır və sorğu dəqiq sözə çevrilir (FTS5-də isə işləyir, ona görə sənədlərdə ikisi qarışır).
     */
    fun toMatchQuery(terms: List<String>, matchColumn: String? = null): String? {
        if (terms.isEmpty()) return null

        return terms.joinToString(" ") { token ->
            val phrase = if (isNumber(token)) "\"$token\"" else "\"$token*\""
            if (matchColumn == null) phrase else "$matchColumn:$phrase"
        }
    }

    /**
     * [toMatchQuery], uzun sorğuda isə **tapılmayan sözlər atılmış** halda.
     *
     * Yapışdırılan və ya yaddaşdan yazılan uzun mətndə bir hərf səhvi, fərqli yazılış («nemət» /
     * «ne’mət») və ya başqa tərcümədən gələn bir söz AND sorğusunu boş qaytarırdı — yəni nə qədər çox
     * yazsan, bir o qədər az tapırdın. Sərt sorğu nəticə verirsə dəyişmir; vermirsə, indeksdə
     * **heç yerdə** olmayan sözlər atılır. Bu qərar yalnız indeksin məzmunundan asılıdır, ona görə hər
     * səhifə yüklənəndə eyni sorğu çıxır və səhifələmə sürüşmür.
     *
     * Qalan sözlər yarıdan az olsa sərt sorğu saxlanılır: o halda «təxmini» nəticə artıq sorğunun
     * özü deyil.
     */
    suspend fun toMatchQueryWithFallback(
        terms: List<String>,
        matchColumn: String? = null,
        hasMatches: suspend (ftsQuery: String) -> Boolean,
    ): String? {
        val strict = toMatchQuery(terms, matchColumn) ?: return null
        if (terms.size < RELAX_MIN_TERMS || hasMatches(strict)) return strict

        val present = terms.filter { term -> hasMatches(toMatchQuery(listOf(term), matchColumn)!!) }
        if (present.size == terms.size || present.size < 2 || present.size * 2 < terms.size) return strict

        return toMatchQuery(present, matchColumn)
    }

    /**
     * Bir neçə yazılış variantının **dəqiq** OR-u — `الرحمن OR رحمن`.
     *
     * ⚠️ Prefiks (`*`) qəsdən yoxdur. [toPrefixAndQuery] hər tokeni `*` ilə bitirir, bu isə Əsmaül
     * Hüsnə uyğunlaşdırması üçün yararsızdır: «الحي*» «الحياة»-ni və «الحيوان»-ı da tutardı, yəni
     * onlarla ad üçün siyahı yalan nəticə ilə dolardı. Burada uyğunluq **tam token** olmalıdır.
     *
     * Çoxsözlü variant («مالك الملك») dırnaq içində **fraza** kimi gedir: sözlərin ayrı-ayrı OR-u
     * adı iki müstəqil sözə parçalayıb mənasını itirərdi.
     *
     * @return `null` — işlənəsi variant qalmayanda (boş siyahı, yalnız qısa/etibarsız tokenlər).
     */
    fun toExactOrQuery(variants: List<String>): String? {
        val terms = variants
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { variant ->
                val tokens = variant.split(' ')
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.length >= 2 && isValidToken(it) }

                when {
                    tokens.isEmpty() -> null
                    tokens.size == 1 -> escapeFtsToken(tokens.single())
                    else -> tokens.joinToString(" ", prefix = "\"", postfix = "\"") {
                        it.replace("\"", "\"\"")
                    }
                }
            }
            .distinct()

        if (terms.isEmpty()) return null

        return terms.joinToString(" OR ")
    }

    private fun isNumber(token: String): Boolean = token.isNotEmpty() && token.all { it.isDigit() }

    /** `möminlər1` → `möminlər`: hərfə yapışıq 1–2 rəqəm haşiyə nömrəsidir, sözün hissəsi deyil. */
    private fun stripGluedFootnote(token: String): String {
        val digits = token.takeLastWhile { it.isDigit() }.length
        if (digits !in 1..2 || digits == token.length) return token

        val stem = token.dropLast(digits)
        return if (stem.last().isLetter()) stem else token
    }

    private fun isValidToken(token: String): Boolean {
        return token.any { it.isLetterOrDigit() }
    }

    private fun escapeFtsToken(token: String): String {
        val escaped = token.replace("\"", "\"\"")

        val isOperator = escaped.equals("OR", true)
                || token.equals("AND", true)
                || token.equals("NOT", true)

        return if (isOperator) "\"$escaped\"" else escaped
    }

    private val WHITESPACE = Regex("\\s+")

    /** Oxucudakı ayə başlığı: `-11: Onlara…`. */
    private val VERSE_LABEL_PREFIX = Regex("^\\s*-\\d+\\s*:\\s*")

    /** Bundan az sözlü sorğu sərt qalır — bax [toMatchQueryWithFallback]. */
    private const val RELAX_MIN_TERMS = 3
}
