package com.cafarovceyxun.anamuslim.compose.components.reader

enum class ReaderMode(val value: String) {
    VerseByVerse("mode_vbv"),
    Reading("mode_reading"),
    Translation("mode_translation"),
    TranslationVertical("mode_translation_vertical");

    /**
     * Rejim səhifə-səhifə oxuyurmu.
     *
     * Konkret səhifəyə açılış ([com.cafarovceyxun.anamuslim.utils.reader.ReaderIntentData.MushafPage])
     * yalnız belə rejimdə mənalıdır — [VerseByVerse] siyahıdır, səhifə nömrəsini göstərəcək yeri yoxdur.
     */
    val isPageMode: Boolean
        get() = this == Reading || this == Translation || this == TranslationVertical

    companion object {
        fun fromValue(value: String): ReaderMode {
            return when (value) {
                "mode_translation_continuous" -> TranslationVertical
                else -> entries.find { it.value == value } ?: VerseByVerse
            }
        }

        fun fromLegacyStyleInt(style: Int): ReaderMode = when (style) {
            0x2 -> Reading
            0x3 -> Translation
            else -> VerseByVerse
        }
    }
}
