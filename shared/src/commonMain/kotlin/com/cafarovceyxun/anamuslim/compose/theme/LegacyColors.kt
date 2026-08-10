package com.cafarovceyxun.anamuslim.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils

/**
 * Colors carried over from Android `res/values/colors.xml` for components migrated to
 * commonMain. These are fixed values, deliberately independent of the active theme palette.
 */
object LegacyColors {

    /** `R.color.colorPrimary` — static brand green, same in light and dark. */
    val brandPrimary = Color(0xFF008B5B)

    /** `R.color.colorText2` — secondary text; had a `values-night` override. */
    @Composable
    fun text2(): Color =
        if (ThemeUtils.observeDarkTheme()) Color(0xFFB0B0B0) else Color(0xFF333333)
}
