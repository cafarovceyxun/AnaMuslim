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

    private fun latinNormalize(s: String): String {
        return s
            .lowercase()
    }

    private fun arabicNormalize(s: String): String {
        return s
            .replace("[\\u064B-\\u065F\\u0670]".toRegex(), "")
            .replace("ـ", "")
            .replace("[أإآٱ]".toRegex(), "ا")
            .replace("ى", "ي")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
