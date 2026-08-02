package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.SimpleTooltip
import com.cafarovceyxun.anamuslim.compose.theme.AppIcons
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.compose.utils.preferences.AppPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.strTitleKeepScreenOn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * App-bar switch for holding the screen awake while reading. Reads and writes the same preference
 * as the settings switch, so `KeepScreenOnIfEnabled` in the reader and hadith screens follows
 * either one. Unlike [VolumeKeyToggle] there is no platform guard — both platforms can hold the
 * screen awake.
 */
@Composable
fun KeepScreenOnToggle(
    onToggle: (Boolean) -> Unit = {}
) {
    val keepScreenOnEnabled = AppPreferences.observeKeepScreenOnEnabled()
    val scope = rememberCoroutineScope()

    val activeColor = colorScheme.primary
    val inactiveColor = colorScheme.onSurface.alpha(0.6f)

    val tint by animateColorAsState(
        targetValue = if (keepScreenOnEnabled) activeColor else inactiveColor,
        label = "KeepScreenOnToggleColor"
    )

    SimpleTooltip(text = stringResource(Res.string.strTitleKeepScreenOn)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    scope.launch {
                        val newState = !keepScreenOnEnabled
                        AppPreferences.setKeepScreenOnEnabled(newState)
                        onToggle(newState)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (keepScreenOnEnabled) AppIcons.ScreenAwake
                else AppIcons.ScreenAwakeOff,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
