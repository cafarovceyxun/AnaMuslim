package com.cafarovceyxun.anamuslim.utils.univ

import kotlin.jvm.JvmStatic

object StringUtils {
    const val DASH: String = "–"
    const val VERTICAL_BAR: String = "│"
    const val HYPHEN: String = "—"
    const val RTL_MARK: String = "‏"

    private val HTML_TAG_PAIR = Regex("<.*?>(.*?)<.*?>")
    private val RTL_LANGS = arrayOf("ar")

    /**
     * Remove all (paired) HTML tags from a string, optionally preserving inner content.
     */
    @JvmStatic
    fun removeHTML(string: String, preserveContent: Boolean): String {
        return string.replace(HTML_TAG_PAIR, if (preserveContent) "$1" else "")
    }

    @JvmStatic
    fun escapeRegex(string: String): String {
        return Regex.escape(string)
    }

    private val HTML_LINE_BREAK = Regex("(?i)<br\\s*/?>")
    private val HTML_PARAGRAPH_END = Regex("(?i)</p>")
    private val HTML_ANY_TAG = Regex("<[^>]*>")

    /**
     * Common-code stand-in for Android's `HtmlCompat.fromHtml(...).toString()` when building
     * plain-text output (share/copy): breaks and paragraph ends become newlines, remaining
     * tags are stripped and the entities occurring in translation texts are decoded.
     */
    fun htmlToPlainText(html: String): String {
        return html
            .replace(HTML_LINE_BREAK, "\n")
            .replace(HTML_PARAGRAPH_END, "\n\n")
            .replace(HTML_ANY_TAG, "")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .trim()
    }

    @JvmStatic
    fun formatInvariant(format: String, vararg args: Any?): String {
        return stringFormatInvariant(format, *args)
    }

    @JvmStatic
    fun isRtlLanguage(langCode: String?): Boolean {
        if (langCode.isNullOrEmpty()) return false

        val parts = langCode.split('_', '-')
        if (parts.isEmpty()) return false

        val code = parts[0]
        return RTL_LANGS.any { it.equals(code, ignoreCase = true) }
    }
}
