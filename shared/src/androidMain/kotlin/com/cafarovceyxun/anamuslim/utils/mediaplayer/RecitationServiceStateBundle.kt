package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.os.Bundle
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationServiceStateKeys as Keys

/**
 * `Bundle` codec for [RecitationServiceState], carrying it across the Android media-session IPC
 * boundary (`setSessionExtras` -> `onExtrasChanged`).
 *
 * Android-only, so it lives beside the shared state rather than inside it. Declared as extensions
 * on the class and its companion, which keeps the original `state.toBundle()` /
 * `RecitationServiceState.fromBundle(extras)` call sites unchanged.
 */
fun RecitationServiceState.toBundle(): Bundle = Bundle().apply {
    putInt(Keys.CURRENT_CHAPTER, currentVerse.chapterNo)
    putInt(Keys.CURRENT_VERSE, currentVerse.verseNo)
    putInt(Keys.RESOLVING_CHAPTER_NO, resolvingChapterNo ?: -1)
    putInt(Keys.AUDIO_DOWNLOAD_PROGRESS, downloadProgress ?: -1)
    putBoolean(Keys.IS_PLAYING, isPlaying)
    putBoolean(Keys.IS_BUFFERING, isBuffering)
    putBoolean(Keys.IS_VERSE_SYNC_AVAILABLE, isVerseSyncAvailable)
    putBoolean(Keys.PAUSED_BY_HEADSET, pausedByHeadset)
    putString(Keys.CURRENT_RECITER, settings.reciter)
    putString(Keys.CURRENT_TRANSLATION_RECITER, settings.translationReciter)
    putFloat(Keys.PLAYBACK_SPEED, settings.speed)
    putInt(Keys.REPEAT_COUNT, settings.repeatCount)
    putString(Keys.AUDIO_END_BEHAVIOUR, settings.audioEndBehaviour.value)
    putString(Keys.AUDIO_OPTION, settings.audioOption.value)
}

fun RecitationServiceState.Companion.fromBundle(bundle: Bundle): RecitationServiceState {
    return RecitationServiceState(
        currentVerse = ChapterVersePair(
            chapterNo = bundle.getInt(Keys.CURRENT_CHAPTER, -1),
            verseNo = bundle.getInt(Keys.CURRENT_VERSE, -1),
        ),
        resolvingChapterNo = bundle.getInt(Keys.RESOLVING_CHAPTER_NO, -1)
            .takeIf { it != -1 },
        downloadProgress = bundle.getInt(Keys.AUDIO_DOWNLOAD_PROGRESS, -1)
            .takeIf { it >= 0 },
        isPlaying = bundle.getBoolean(Keys.IS_PLAYING, false),
        isBuffering = bundle.getBoolean(Keys.IS_BUFFERING, false),
        isVerseSyncAvailable = bundle.getBoolean(Keys.IS_VERSE_SYNC_AVAILABLE, true),
        pausedByHeadset = bundle.getBoolean(Keys.PAUSED_BY_HEADSET, false),
        settings = PlayerSettings(
            speed = bundle.getFloat(Keys.PLAYBACK_SPEED, 1.0f),
            repeatCount = bundle.getInt(Keys.REPEAT_COUNT, 1).coerceAtLeast(1),
            audioEndBehaviour = bundle.getString(
                Keys.AUDIO_END_BEHAVIOUR,
            )?.let { AudioEndBehaviour.fromValue(it) } ?: AudioEndBehaviour.DEFAULT,
            audioOption = bundle.getString(
                Keys.AUDIO_OPTION
            )?.let { AudioOption.fromValue(it) } ?: AudioOption.DEFAULT,
            reciter = bundle.getString(Keys.CURRENT_RECITER),
            translationReciter = bundle.getString(Keys.CURRENT_TRANSLATION_RECITER)
        ),
    )
}
