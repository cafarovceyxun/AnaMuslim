package com.cafarovceyxun.anamuslim.utils.reader.atlas.tajweed

import androidx.compose.ui.graphics.Color

/**
 * Canonical tajweed class -> colour palette for the Uthmani atlas render path.
 *
 * This object — not the palette embedded in `tajweed.bin` — is the single colour authority for
 * tajweed rendering. Tweaking a colour is a one-line edit here; it never requires touching
 * `tajweed.bin` or regenerating any data file. Index is the tajweed class id as decoded by
 * [TajweedBinDecoder] (0..10); index 0 is unused since a class of 0 means "no tajweed colour" and
 * glyphs with that class are never tinted.
 *
 * Colours are tuned to stay legible on the app's AMOLED/near-black backgrounds.
 */
object TajweedPalette {

    val colors: List<Color> = listOf(
        Color(0xFF000000), // 0 - unused/default, class 0 means "no tajweed colour"
        Color(0xFF90A4AE), // 1 - silent / ğunnəsiz idğam (slate grey)
        Color(0xFFF9A825), // 2 - normal madd (dark yellow / amber)
        Color(0xFFFB8C00), // 3 - separated madd (orange)
        Color(0xFFEC407A), // 4 - connected madd (pink)
        Color(0xFFD81B60), // 5 - necessary madd (deep rose)
        Color(0xFFB71C1C), // 6 - ghunnah (dark red)
        Color(0xFF43A047), // 7 - qalqalah (green)
        Color(0xFFEF5350), // 8 - ikhfa (light red)
        Color(0xFF8E24AA), // 9 - idgham (purple)
        Color(0xFF1E88E5), // 10 - iqlab (blue)
    )
}
