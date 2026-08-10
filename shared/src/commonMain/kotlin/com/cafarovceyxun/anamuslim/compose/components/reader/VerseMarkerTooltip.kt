package com.cafarovceyxun.anamuslim.compose.components.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.theme.alpha
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.dr_icon_open
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.strLabelOpen
import com.cafarovceyxun.anamuslim.resources.strTitleVerseRecitation
import com.cafarovceyxun.anamuslim.utils.reader.LocalVerseActions
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Müshəf səhifəsində ayə nömrəsinə vurulanda çıxan iki düyməli balon: yalnız həmin ayəni
 * səsləndirmək və ayənin ərəbcəsi ilə tərcüməsini alt vərəqdə açmaq üçün.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseMarkerTooltip(
    verse: ChapterVersePair,
    onDismiss: () -> Unit,
    anchor: @Composable () -> Unit,
) {
    val recitation = LocalRecitation.current
    val verseActions = LocalVerseActions.current
    val isVersePlaying = recitation.isAnyPlaying && recitation.playingVerse == verse

    val state = rememberTooltipState(isPersistent = true)

    LaunchedEffect(verse) {
        state.show()
    }

    // Single-verse playback deliberately leaves the mini player alone, so this balloon is its only
    // transport: it stays up while the verse recites (the button reads as pause), and closes itself
    // once the verse is done — or the moment the user pauses it.
    var hasStartedPlaying by remember(verse) { mutableStateOf(false) }

    LaunchedEffect(isVersePlaying) {
        if (isVersePlaying) hasStartedPlaying = true
        else if (hasStartedPlaying) onDismiss()
    }

    // Balonu kənara vuraraq bağlamaq onu görünməz edir, lakin lövbər sözü seçili qalır; onu da
    // təmizləyirik ki, eyni nömrəyə təkrar vuranda balon yenidən açılsın.
    LaunchedEffect(state) {
        snapshotFlow { state.isVisible }
            .dropWhile { !it }
            .filter { !it }
            .collect { onDismiss() }
    }

    val playLabel = stringResource(Res.string.strTitleVerseRecitation)
    val openLabel = stringResource(Res.string.strLabelOpen)

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                shape = shapes.small,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                containerColor = colorScheme.surface,
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VerseMarkerAction(
                            painter = painterResource(
                                if (isVersePlaying) Res.drawable.ic_pause else Res.drawable.ic_play
                            ),
                            contentDescription = playLabel,
                            onClick = { recitation.controller.playSingleVerse(verse) },
                        )

                        VerseMarkerAction(
                            painter = painterResource(Res.drawable.dr_icon_open),
                            contentDescription = openLabel,
                            onClick = {
                                verseActions.onReferenceClick(
                                    emptySet(),
                                    verse.chapterNo,
                                    verse.verseNo.toString(),
                                )
                                onDismiss()
                            },
                        )
                    }
                }
            }
        },
        state = state,
        content = anchor,
    )
}

@Composable
private fun VerseMarkerAction(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .padding(4.dp),
            tint = colorScheme.onSurface.alpha(0.8f),
        )
    }
}
