package com.cafarovceyxun.anamuslim.compose.screens.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the reader is currently in a chrome-free mode — immersive fullscreen or the tajweed
 * legend — and therefore wants the recitation mini player off screen.
 *
 * The reader already states that intent locally (`MiniPlayerVisibility.HIDDEN`), but the player is
 * drawn by the app host *above* the nav graph, which cannot reach reader-internal state. This is
 * that seam: the reader publishes, the host observes. Without it the mini player keeps floating
 * over the fullscreen mushaf.
 */
object ReaderChromeState {

    private val _hidePlayer = MutableStateFlow(false)

    /** `true` while the reader is fullscreen (or showing the tajweed legend). */
    val hidePlayer: StateFlow<Boolean> = _hidePlayer.asStateFlow()

    fun setHidePlayer(value: Boolean) {
        _hidePlayer.value = value
    }

    private val _verseSync = MutableStateFlow<ReaderVerseSync?>(null)

    /**
     * The reader's verse-follow control while a reader is on screen, `null` otherwise.
     *
     * Same seam as [hidePlayer], for the player's lock button: the reader owns the toggle (its
     * `ReaderViewModel` lives in the nav back-stack entry's store, out of the host's reach), so it
     * publishes the binding here and the host wires the button to it. Hosts that reached for their
     * own `ReaderViewModel` instead were toggling a *different* instance, which is why the button
     * did nothing.
     */
    val verseSync: StateFlow<ReaderVerseSync?> = _verseSync.asStateFlow()

    fun setVerseSync(value: ReaderVerseSync?) {
        _verseSync.value = value
    }

    private val _readingChapterNo = MutableStateFlow<Int?>(null)

    /**
     * The chapter the on-screen reader is currently showing, `null` when no reader is on screen.
     *
     * Same seam as [verseSync]: the reader owns its scroll position (in a `ReaderViewModel` the host
     * cannot reach), so it publishes the chapter here. The mini player's "go to reciting surah"
     * button reads this to hide itself while that surah is already open.
     */
    val readingChapterNo: StateFlow<Int?> = _readingChapterNo.asStateFlow()

    fun setReadingChapterNo(value: Int?) {
        _readingChapterNo.value = value
    }
}

/** The reader's verse-follow state plus the toggle that owns it. */
data class ReaderVerseSync(
    val isEnabled: Boolean,
    val onToggle: () -> Unit,
)
