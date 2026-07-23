package com.cafarovceyxun.anamuslim.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.compose.components.dialogs.BottomSheet
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.compose.components.reader.ReaderMode
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.autoScroll
import com.cafarovceyxun.anamuslim.resources.autoScrollSpeed
import com.cafarovceyxun.anamuslim.resources.startAutoScroll
import com.cafarovceyxun.anamuslim.resources.stopAutoScroll
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.MutableIntState
import kotlinx.coroutines.launch

@Composable
fun AutoScrollSheet(
    autoScrollSpeedProvider: MutableState<Float?>,
    isAutoScrollGestureMode: MutableState<Boolean>? = null,
    autoScrollStep: MutableIntState? = null,
    readerMode: ReaderMode? = null,
    isOpen: Boolean,
    onClose: () -> Unit
) {
    val savedAutoScrollSpeed = ReaderPreferences.observeAutoScrollSpeed()
    val scope = rememberCoroutineScope()

    var autoScrollSpeed by autoScrollSpeedProvider
    val isScrolling = autoScrollSpeed != null

    BottomSheet(
        isOpen,
        onClose,
        skipPartiallyExpanded = false,
        title = stringResource(Res.string.autoScroll)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(Res.string.autoScrollSpeed),
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Slider(
                    enabled = !isScrolling,
                    value = savedAutoScrollSpeed,
                    onValueChange = {
                        scope.launch {
                            ReaderPreferences.setAutoScrollSpeed(it)
                        }
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${savedAutoScrollSpeed.toInt()}x",
                    modifier = Modifier.width(50.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }

            Button(
                onClick = {
                    if (!isScrolling) {
                        if (readerMode == ReaderMode.VerseByVerse && isAutoScrollGestureMode != null && autoScrollStep != null) {
                            isAutoScrollGestureMode.value = true
                            autoScrollStep.intValue = 1
                            autoScrollSpeed = 0.5f // Start at 1x
                        } else {
                            autoScrollSpeed = (savedAutoScrollSpeed / 2f)
                        }
                        onClose()
                    } else {
                        autoScrollSpeed = null
                        isAutoScrollGestureMode?.value = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScrolling) colorScheme.errorContainer
                    else colorScheme.primary,
                    contentColor = if (isScrolling) colorScheme.onErrorContainer
                    else colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isScrolling) stringResource(Res.string.stopAutoScroll)
                    else stringResource(Res.string.startAutoScroll),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
