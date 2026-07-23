package com.cafarovceyxun.anamuslim.compose.components.player

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cafarovceyxun.anamuslim.compose.utils.ThemeUtils

/**
 * The recitation player keeps its own surface — a near-black stage in dark theme — but it follows the
 * app theme rather than staying black under a light theme. Every player surface reads these two, so
 * swapping them here re-themes the whole player.
 */

@Composable
fun playerBgColor(): Color =
    if (ThemeUtils.observeDarkTheme()) Color.Black else colorScheme.background

@Composable
fun playerContentColor(): Color =
    if (ThemeUtils.observeDarkTheme()) Color.White else colorScheme.onSurface
