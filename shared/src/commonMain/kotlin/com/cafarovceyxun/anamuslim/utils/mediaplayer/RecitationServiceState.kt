package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.repository.QuranVerseStructure
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta

enum class PlayerInterationSource {
    HEADSET,
    USER
}

data class PlayerSettings(
    val speed: Float = 1.0f,
    val repeatCount: Int = 1,
    val audioEndBehaviour: AudioEndBehaviour = AudioEndBehaviour.DEFAULT,
    val audioOption: AudioOption = AudioOption.DEFAULT,
    val reciter: String? = null,
    val translationReciter: String? = null
)


data class AudioResolutionRequest(val chapterNo: Int, val settings: PlayerSettings)

data class RecitationServiceState(
    val currentVerse: ChapterVersePair = ChapterVersePair(1, 1),
    /** Helps in indiacating if the player is resolving and also for which chapter no */
    val resolvingChapterNo: Int? = null,
    /** 0–100 while chapter audio is downloading from the network; null otherwise. */
    val downloadProgress: Int? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val pausedByHeadset: Boolean = false,
    /** False when single-file chapter audio has no timing and no verse clip playlist is in use. */
    val isVerseSyncAvailable: Boolean = true,
    /**
     * True while a "recite only this verse" play is armed. Playback is a short one-verse burst the
     * reader triggers in place, so the mini player must not pop itself open for it.
     */
    val isSingleVersePlayback: Boolean = false,
    /**
     * True once a recitation has actually been started on this install — including a session
     * restored from preferences after the process died.
     *
     * [currentVerse] cannot answer this: it is seeded with Fatiha 1:1, so it is valid from the very
     * first launch. Without a separate flag the player UI has no way to tell "paused on a chapter
     * the user was listening to yesterday" apart from "nothing has ever played", which is why the
     * mini player and its reopen button both vanished on restart.
     */
    val hasSession: Boolean = false,
    val settings: PlayerSettings = PlayerSettings(),
) {
    suspend fun getPreviousVerse(repository: QuranVerseStructure): ChapterVersePair? {
        if (!currentVerse.isValid) return null

        val currentChapterNo = currentVerse.chapterNo
        val currentVerseNo = currentVerse.verseNo

        if (
            !QuranMeta.isChapterValid(currentChapterNo) ||
            !repository.isVerseValid4Chapter(
                currentChapterNo,
                currentVerseNo
            )
        ) {
            return null
        }

        var previousChapterNo = currentChapterNo
        var previousVerseNo = currentVerseNo - 1

        if (previousVerseNo < 1) {
            if (QuranMeta.isChapterValid(previousChapterNo - 1)) {
                previousChapterNo--
                previousVerseNo = repository.getChapterVerseCount(previousChapterNo)
            } else {
                previousVerseNo = -1
            }
        }

        if (previousChapterNo == -1 || previousVerseNo == -1) {
            return null
        }

        return ChapterVersePair(previousChapterNo, previousVerseNo)
    }

    suspend fun getNextVerse(repository: QuranVerseStructure): ChapterVersePair? {
        if (!currentVerse.isValid) return null

        val currentChapterNo = currentVerse.chapterNo
        val currentVerseNo = currentVerse.verseNo

        if (
            !QuranMeta.isChapterValid(currentChapterNo) ||
            !repository.isVerseValid4Chapter(
                currentChapterNo,
                currentVerseNo
            )
        ) {
            return null
        }

        var nextChapterNo = currentChapterNo
        var nextVerseNo = currentVerseNo + 1

        if (nextVerseNo > repository.getChapterVerseCount(nextChapterNo)) {
            if (QuranMeta.isChapterValid(nextChapterNo + 1)) {
                nextChapterNo++
                nextVerseNo = 1
            } else {
                nextVerseNo = -1
            }
        }

        if (nextChapterNo == -1 || nextVerseNo == -1) {
            return null
        }

        return ChapterVersePair(nextChapterNo, nextVerseNo)
    }

    companion object {
        val EMPTY = RecitationServiceState()
    }
}

/**
 * Keys for the Android `Bundle` codec (`toBundle`/`fromBundle` in androidMain), which carries this
 * state across the media-session IPC boundary. They live here so the codec stays a thin platform
 * shell over the shared state shape.
 */
internal object RecitationServiceStateKeys {
    const val CURRENT_CHAPTER = "state_current_chapter"
    const val CURRENT_VERSE = "state_current_verse"
    const val CURRENT_RECITER = "state_current_reciter"
    const val CURRENT_TRANSLATION_RECITER = "state_current_translation_reciter"
    const val RESOLVING_CHAPTER_NO = "state_resolving_chapter_no"
    const val AUDIO_DOWNLOAD_PROGRESS = "state_audio_download_progress"
    const val IS_PLAYING = "state_is_playing"
    const val IS_BUFFERING = "state_is_buffering"
    const val IS_VERSE_SYNC_AVAILABLE = "state_is_verse_sync_available"
    const val IS_SINGLE_VERSE_PLAYBACK = "state_is_single_verse_playback"
    const val HAS_SESSION = "state_has_session"
    const val PAUSED_BY_HEADSET = "state_paused_by_headset"
    const val PLAYBACK_SPEED = "state_playback_speed"
    const val REPEAT_COUNT = "state_repeat_count"
    const val AUDIO_END_BEHAVIOUR = "state_audio_end_behaviour"
    const val AUDIO_OPTION = "state_audio_option"
}
