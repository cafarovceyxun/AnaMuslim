package com.cafarovceyxun.anamuslim.utils.univ

/**
 * Search/reference patterns shared by the quick-link parser and the reference dialog.
 *
 * These were `java.util.regex.Pattern`s; Kotlin's [Regex] is the multiplatform equivalent and
 * compiles the same expressions, so the matching behaviour is unchanged.
 */
object RegexPattern {
    val VERSE_RANGE_PATTERN = Regex("(\\d+)-(\\d+)")

    val CHAPTER_OR_JUZ_PATTERN = Regex("(\\d+)")

    val VERSE_JUMP_PATTERN = Regex("(\\d+)[\\s+]?:[\\s+]?(\\d+)")

    val VERSE_RANGE_JUMP_PATTERN = Regex("(\\d+)[\\s+]?:[\\s+]?(\\d+)[\\-](\\d+)")
}
