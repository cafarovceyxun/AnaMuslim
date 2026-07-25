package com.cafarovceyxun.anamuslim.compose.components.player

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.common.IconButton
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.resources.ic_pause
import com.cafarovceyxun.anamuslim.resources.ic_play
import com.cafarovceyxun.anamuslim.resources.strLabelPause
import com.cafarovceyxun.anamuslim.resources.strLabelPlay
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayerProvider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Al-'Alaq — its opening verses are the reciter voice sample. */
const val RECITER_PREVIEW_CHAPTER_NO = 96

/** The sample is bounded to the first verses of [RECITER_PREVIEW_CHAPTER_NO]. */
const val RECITER_PREVIEW_LAST_VERSE_NO = 10

/**
 * Lets a reciter be sampled before selecting or downloading them, using the app's real
 * [com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationPlayer] rather than a throwaway audio
 * object: resolving a reciter's URL template, falling back between sources and reusing
 * already-downloaded files all live there, and duplicating it for a sample would drift (the same
 * reasoning onboarding's reciter sheet follows).
 *
 * Because that session is shared and single, previewing takes it over — which pauses whatever the
 * main player was doing — and [rememberReciterPreview] restores the user's chosen reciter when the
 * host leaves the screen. Obtain one with [rememberReciterPreview] and draw a control per row with
 * [ReciterPreviewButton].
 */
@Stable
class ReciterPreviewState internal constructor(
    private val previewingReciterId: String?,
    private val isPreviewPlaying: Boolean,
    private val resolvingChapterNo: Int?,
    private val onToggle: (reciterId: String) -> Unit,
) {
    fun isActive(reciterId: String): Boolean = previewingReciterId == reciterId

    fun isPlaying(reciterId: String): Boolean = isActive(reciterId) && isPreviewPlaying

    fun isLoading(reciterId: String): Boolean =
        isActive(reciterId) && resolvingChapterNo == RECITER_PREVIEW_CHAPTER_NO

    /** Starts the sample for [reciterId], or toggles play/pause when it is already the active one. */
    fun toggle(reciterId: String) = onToggle(reciterId)
}

@Composable
fun rememberReciterPreview(): ReciterPreviewState {
    val player = remember { RecitationPlayerProvider.player }
    val playerState by player.state.collectAsState()
    val isPlaying by player.isPlayingState.collectAsState()
    val selectedId = RecitationPreferences.observeReciterId()

    var previewReciterId by rememberSaveable { mutableStateOf<String?>(null) }
    // Read inside effects that run after this composable's own snapshot reads are gone.
    val latestSelectedId by rememberUpdatedState(selectedId)
    val latestPreviewId by rememberUpdatedState(previewReciterId)

    fun restoreChosenReciter() {
        latestSelectedId?.let { player.setReciter(it, RecitationAudioKind.QURAN) }
    }

    DisposableEffect(Unit) {
        player.connect()
        onDispose {
            // A preview left running would keep playing under the main app (and, on Android, keep
            // its notification up) after the screen is gone.
            if (latestPreviewId != null) {
                player.stop()
                restoreChosenReciter()
            }
            player.disconnect()
        }
    }

    // The player has no "play verses 1..N" mode — it runs to the end of the chapter — so the sample
    // is bounded here, by watching the verse it reports.
    LaunchedEffect(playerState.currentVerse, previewReciterId) {
        if (previewReciterId == null) return@LaunchedEffect

        val verse = playerState.currentVerse
        if (verse.chapterNo != RECITER_PREVIEW_CHAPTER_NO || verse.verseNo > RECITER_PREVIEW_LAST_VERSE_NO) {
            previewReciterId = null
            player.stop()
            restoreChosenReciter()
        }
    }

    return ReciterPreviewState(
        previewingReciterId = previewReciterId,
        isPreviewPlaying = isPlaying,
        resolvingChapterNo = playerState.resolvingChapterNo,
        onToggle = { reciterId ->
            when {
                previewReciterId != reciterId -> {
                    previewReciterId = reciterId
                    player.setReciter(reciterId, RecitationAudioKind.QURAN)
                    player.start(ChapterVersePair(RECITER_PREVIEW_CHAPTER_NO, 1))
                }

                isPlaying -> player.pause()
                else -> player.resume()
            }
        },
    )
}

/** Play/pause control for a reciter's sample recitation. */
@Composable
fun ReciterPreviewButton(
    preview: ReciterPreviewState,
    reciterId: String,
    modifier: Modifier = Modifier,
    tint: Color = colorScheme.primary,
) {
    if (preview.isLoading(reciterId)) {
        CircularProgressIndicator(
            modifier = modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = tint,
        )
        return
    }

    val playing = preview.isPlaying(reciterId)

    IconButton(
        painter = painterResource(if (playing) Res.drawable.ic_pause else Res.drawable.ic_play),
        contentDescription = stringResource(
            if (playing) Res.string.strLabelPause else Res.string.strLabelPlay,
        ),
        onClick = { preview.toggle(reciterId) },
        tint = tint,
        small = true,
    )
}
