package com.cafarovceyxun.anamuslim.compose.screens.hadith

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_edit
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Minimalist edit affordance shared across the hadith screens.
 *
 * Deliberately quieter than a filled primary [androidx.compose.material3.FloatingActionButton]: a
 * flat, borderless-feeling circle in the surface tone with a primary-tinted glyph and no shadow, so
 * it reads as a subtle control over the content rather than a heavy call to action.
 */
@Composable
fun HadithEditFab(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    icon: DrawableResource = Res.drawable.dr_icon_edit,
    size: Dp = 48.dp,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = colorScheme.surfaceContainerHigh.alpha(0.9f),
        contentColor = colorScheme.primary,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.6f)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(size * 0.4f),
                tint = colorScheme.primary,
            )
        }
    }
}
