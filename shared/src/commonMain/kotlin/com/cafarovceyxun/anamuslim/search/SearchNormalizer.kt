package com.cafarovceyxun.anamuslim.search

import com.cafarovceyxun.anamuslim.utils.nfkcNormalize

enum class ScriptType {
    LATIN,
    ARABIC,
    OTHER,
}


object SearchNormalizer {
    fun scriptForLang(langCode: String): ScriptType {
        val code = langCode.lowercase()

        return when (code) {
            "en", "fr", "es", "id", "tr", "de", "nl", "sv", "no", "da", "it", "pt", "ms" -> ScriptType.LATIN
            "ar" -> ScriptType.ARABIC
            else -> ScriptType.OTHER
        }
    }

    fun normalize(input: String): String {
        return nfkcNormalize(input)
            .lowercase()
            .trim()
    }

    /**
     * Strips what a reader types but an index does not store: harakat, tatweel, and the alif/ya
     * spelling variants.
     *
     * The Quran's `arabic_search` FTS table ships already reduced this way — `بسم الله الرحمن الرحيم`,
     * bare alif, no vowel marks. So a query carrying harakat (which is what an Arabic keyboard with
     * tashkeel, or any copy-paste out of a muṣḥaf, produces) could never match a single row: the user
     * was searching diacritised text against an undiacritised index. Running the query through the
     * same reduction is what makes `ٱلرَّحِيم` and `الرحيم` the same search.
     *
     * Safe to run over any script — every rule is confined to Arabic code points.
     */
    fun arabicNormalize(s: String): String {
        return s
            .replace("[\\u064B-\\u065F\\u0670]".toRegex(), "")
            .replace("ـ", "")
            .replace("[أإآٱ]".toRegex(), "ا")
            .replace("ى", "ي")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * [arabicNormalize] plus the Quranic annotation marks — waqf signs, the small letters, the
     * end-of-ayah sign (U+06D6–U+06ED).
     *
     * Only the Quran side may go this far. `arabic_search` is built without those marks, so a query
     * pasted out of the muṣḥaf never matched: `بِسۡمِ` keeps its U+06E1 through [arabicNormalize],
     * and `بسۡم` is not a token in the index. The hadith side stays on [arabicNormalize], whose
     * reduction is mirrored character for character by a `replace()` chain inside SQLite (see
     * `HadithDao.searchHadiths`) — stripping more here than there would make the two disagree.
     */
    fun quranNormalize(s: String): String {
        return arabicNormalize(s.replace("[\\u06D6-\\u06ED]".toRegex(), ""))
    }

    /** Whether [s] contains any Arabic letter, i.e. whether [arabicNormalize] has anything to do. */
    fun containsArabic(s: String): Boolean = s.any { it in '؀'..'ۿ' }
}
