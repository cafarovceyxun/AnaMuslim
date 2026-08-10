package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.theme.AppIcons
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.app.supportsVolumeKeyNavigation
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import kotlinx.coroutines.launch

/**
 * App-bar switch for paging the reader with the hardware volume keys. Renders nothing where the
 * platform has no such keys to bind ([supportsVolumeKeyNavigation]) — the guard lives here rather
 * than at the four call sites (reader app bar ×3, hadith app bar) so they stay identical.
 */
@Composable
fun VolumeKeyToggle(
    onToggle: (Boolean) -> Unit = {}
) {
    if (!supportsVolumeKeyNavigation) return

    val volumeKeyNavEnabled = AppPreferences.observeVolumeKeyNavigationEnabled()
    val scope = rememberCoroutineScope()
    
    val activeColor = colorScheme.primary
    val inactiveColor = colorScheme.onSurface.alpha(0.6f)
    
    val tint by animateColorAsState(
        targetValue = if (volumeKeyNavEnabled) activeColor else inactiveColor,
        label = "VolumeToggleColor"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                scope.launch {
                    val newState = !volumeKeyNavEnabled
                    AppPreferences.setVolumeKeyNavigationEnabled(newState)
                    onToggle(newState)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (volumeKeyNavEnabled) AppIcons.VolumeUp
            else AppIcons.VolumeOff,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
