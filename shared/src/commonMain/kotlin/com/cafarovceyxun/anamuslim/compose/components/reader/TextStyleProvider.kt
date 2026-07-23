package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafarovceyxun.anamuslim.compose.theme.appFontFamily
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences


data class QuranTextStyle(
    val quran: (Int) -> TextStyle?,
    val wbwTrltStyle: TextStyle?,
    val wbwTrStyle: TextStyle?,
    val wbwMaxWith: Dp,
)

val LocalQuranTextStyle = staticCompositionLocalOf<QuranTextStyle> {
    error("QuranTextStyle not provided")
}

@Composable
fun TextStyleProvider(
    pageTextStyles: Map<Int, TextStyle>,
    content: @Composable () -> Unit
) {
    val wbwMult = ReaderPreferences.observeWbwTextSizeMultiplier()

    val transliterationBase = typography.bodySmall.copy(
        color = colorScheme.onSurface.alpha(0.65f)
    )

    val translationBase = typography.bodySmall.copy(
        color = colorScheme.onSurface
    )

    val fontFamily = appFontFamily

    val transliterationStyle = remember(translationBase, wbwMult, fontFamily) {
        val fs = (transliterationBase.fontSize.value * wbwMult).sp

        transliterationBase.copy(
            fontSize = fs,
            lineHeight = fs * 1.35f,
            fontFamily = fontFamily
        )
    }

    val translationStyle = remember(translationBase, wbwMult, fontFamily) {
        val fs = (translationBase.fontSize.value * wbwMult).sp

        translationBase.copy(
            fontSize = fs,
            lineHeight = fs * 1.35f,
            fontFamily = fontFamily
        )
    }


    CompositionLocalProvider(
        LocalQuranTextStyle provides QuranTextStyle(
            quran = { pageTextStyles[it] },
            wbwTrltStyle = transliterationStyle,
            wbwTrStyle = translationStyle,
            wbwMaxWith = (90 * wbwMult / 100).coerceAtLeast(90F).dp
        )
    ) {
        content()
    }
}
