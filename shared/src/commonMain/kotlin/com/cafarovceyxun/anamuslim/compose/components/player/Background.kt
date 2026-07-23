package com.cafarovceyxun.anamuslim.compose.components.player


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils

@Composable
fun Background(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = playerBgColor()
    val isDark = ThemeUtils.observeDarkTheme()

    // The stage tint and the colored glows are tuned for a black stage; on a light one they are
    // pulled back so the surface stays light instead of turning into a muddy wash.
    val bandColor = if (isDark) Color(0xFF101020) else colorScheme.surfaceVariant
    val glowAlphaMult = if (isDark) 1f else 0.45f
    val focusLight = if (isDark) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .then(modifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Deep base gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bgColor,
                        bandColor,
                        bgColor
                    )
                )
            )

            // Glow 1 - purple
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C4DFF).copy(alpha = 0.28f * glowAlphaMult),
                        Color.Transparent
                    )
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.18f, h * 0.22f)
            )

            // Glow 2 - pink
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF4FC3).copy(alpha = 0.22f * glowAlphaMult),
                        Color.Transparent
                    )
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.82f, h * 0.28f)
            )

            // Glow 3 - cyan
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00D1FF).copy(alpha = 0.18f * glowAlphaMult),
                        Color.Transparent
                    )
                ),
                radius = w * 0.50f,
                center = Offset(w * 0.52f, h * 0.78f)
            )

            // Focus light behind album art area
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        focusLight.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                ),
                radius = w * 0.30f,
                center = Offset(w * 0.5f, h * 0.38f)
            )

            // Subtle vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        bgColor.copy(alpha = 0.38f)
                    ),
                    radius = w * 0.9f,
                    center = Offset(w / 2f, h / 2f)
                )
            )

            // Bottom darkness for controls readability
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        bgColor.copy(alpha = 0.18f),
                        bgColor.copy(alpha = 0.45f),
                        bgColor.copy(alpha = 0.72f)
                    ),
                    startY = h * 0.45f,
                    endY = h
                )
            )
        }

        content()
    }
}
