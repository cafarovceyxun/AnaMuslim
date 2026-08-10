package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.keepScreenOnDisabledMsg
import com.cafarovceyxun.anamuslim.resources.keepScreenOnEnabledMsg
import com.cafarovceyxun.anamuslim.resources.volumeKeyNavDisabledMsg
import com.cafarovceyxun.anamuslim.resources.volumeKeyNavEnabledMsg
import org.jetbrains.compose.resources.stringResource

/** Which app-bar switch a [ReaderToggleFeedback] message belongs to. */
enum class ReaderToggleKind {
    VolumeKeyNavigation,
    KeepScreenOn,
}

/** A "you just turned X on/off" message, shown briefly over the reader. */
data class ReaderToggleFeedback(
    val kind: ReaderToggleKind,
    val enabled: Boolean,
)

/**
 * The transient toast the reader and hadith screens show when an app-bar switch is flipped — both
 * switches change something invisible, so the message is the only confirmation the reader gets.
 *
 * Overlay both screens with this at the top level; it fills its parent and pins itself to the
 * bottom, so it must be the last child of the screen's root box.
 */
@Composable
fun ReaderToggleFeedbackOverlay(feedback: ReaderToggleFeedback?) {
    // `feedback` is already null while the exit animation plays, so the text is read from the last
    // non-null value — otherwise the toast flips to the opposite message as it fades out.
    val lastShown = remember { mutableStateOf<ReaderToggleFeedback?>(null) }
    SideEffect {
        if (feedback != null) lastShown.value = feedback
    }
    val shown = feedback ?: lastShown.value

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.padding(bottom = 120.dp)
        ) {
            val message = when (shown?.kind) {
                ReaderToggleKind.KeepScreenOn ->
                    if (shown.enabled) Res.string.keepScreenOnEnabledMsg
                    else Res.string.keepScreenOnDisabledMsg

                else ->
                    if (shown?.enabled != false) Res.string.volumeKeyNavEnabledMsg
                    else Res.string.volumeKeyNavDisabledMsg
            }

            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = stringResource(message),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
