package com.cafarovceyxun.anamuslim.search

object FtsQueryBuilder {

    fun toTranslationTextQuery(rawQuery: String): String? {
        val base = SearchNormalizer.normalize(rawQuery)

        return toPrefixAndQuery(base, matchColumn = null)
    }

    fun toPrefixAndQuery(
        normalizedQuery: String,
        matchColumn: String? = null,
    ): String? {
        val tokens = normalizedQuery.split(' ')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { it.isNotBlank() }
            .filter { it.length >= 2 }
            .filter { isValidToken(it) }

        if (tokens.isEmpty()) return null

        return tokens.joinToString(" ") { token ->
            val term = "${escapeFtsToken(token)}*"
            if (matchColumn == null) term else "$matchColumn:$term"
        }
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
}
