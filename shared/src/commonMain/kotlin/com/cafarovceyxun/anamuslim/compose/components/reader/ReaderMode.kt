package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils

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
        /**
         * Oxucunun bu rejimdə səhifələrin **arxasına** çəkdiyi rəng — ekranın `Scaffold`
         * konteyner rəngi.
         *
         * Səhifə dönmə effektləri də bunu oxuyur ([pageTurnEffect]): effekt səhifələri bir-birinin
         * üstünə yığır, oxucu səhifəsinin isə öz fonu yoxdur (mətn birbaşa ekranın fonunda durur).
         * Fonsuz iki səhifə üst-üstə düşəndə **ikisinin də mətni birdən oxunur** — ona görə effekt
         * hər səhifəyə bu rəngi ayrıca çəkir ki, üstdəki səhifə altdakını həqiqətən örtsün.
         *
         * [mode] `null` ola bilər — oxucu rejimi hələ oxunmayıb; onda işıqlı mövzunun `surface`-i
         * qayıdır, yəni ekranın açılışdakı köhnə davranışı.
         */
        @Composable
        fun groundColor(mode: ReaderMode?): Color =
            if (ThemeUtils.observeDarkTheme() || mode == Translation) colorScheme.background
            else colorScheme.surface

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
