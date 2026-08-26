package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ChapterTimingMetadata
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ResolvedAudioResult
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences.RECITATION_MIN_REPEAT_COUNT
import com.cafarovceyxun.anamuslim.repository.QuranVerseStructure
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The platform-neutral half of recitation playback: what to play next, where a verse starts and
 * ends, how many times to repeat it, and what to do when a chapter finishes. The actual audio
 * transport is delegated to a [RecitationAudioOutput].
 *
 * This mirrors the behaviour of Android's `RecitationService`, which keeps the same policy next to
 * its media3 player. Android is deliberately left on that implementation — it also owns the
 * notification, media session and Android Auto browsing, none of which this covers. iOS drives this
 * class with an AVFoundation output.
 *
 * Two playback shapes, picked per chapter:
 * - **Single track** (`ONLY_QURAN`, `ONLY_TRANSLATION`): one continuous file. Verse-level seeking,
 *   highlighting and the repeat budget come from the chapter's timing metadata, polled here.
 * - **Clipped sequence** (`BOTH`): the verse/track order from [VerseClipPlanner] — the same list
 *   Android turns into clipped media3 items — handed to the output as a clip queue. There the clip
 *   *is* the verse, so highlighting follows [RecitationAudioOutputListener.onClipChanged] and
 *   nothing is polled. Repeat is not offered in this mode, matching the Android service and the
 *   repeat sheet's own rule.
 */
class SharedRecitationPlayer(
    private val output: RecitationAudioOutput,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    /**
     * Narrow test seams. Production leaves both null / default, so the code path is byte-for-byte the
     * one that ships: [verseStructure] still reads the global [RepositoryProvider.quranRepository]
     * lazily at call time (never eagerly at construction), and [resolveAudio] is still
     * `RecitationAudioResolver.resolveAudioUris(...).first()`. They exist only so chapter resolution
     * and the verse-tracking loop can be exercised without a Room database, network or audio files —
     * the two things `SharedRecitationPlayerTest` could not previously reach.
     */
    verseStructureSeam: QuranVerseStructure? = null,
    private val resolveAudio: suspend (chapterNo: Int, settings: PlayerSettings) -> ResolvedAudioResult =
        { chapterNo, settings -> RecitationAudioResolver.resolveAudioUris(chapterNo, settings).first() },
) : RecitationPlayer {

    private val verseStructureOverride = verseStructureSeam
    private val verseStructure: QuranVerseStructure
        get() = verseStructureOverride ?: RepositoryProvider.quranRepository

    private val _state = MutableStateFlow(RecitationServiceState.EMPTY)
    override val state: StateFlow<RecitationServiceState> = _state.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnectedState: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlayingState: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    override val isBufferingState: StateFlow<Boolean> = _isBuffering.asStateFlow()

    override val isPlaying: Boolean get() = _isPlaying.value
    override val isLoading: Boolean
        get() = _state.value.resolvingChapterNo != null || _isBuffering.value

    override val currentPositionMs: Long
        get() {
            val timeline = clipTimeline ?: return output.positionMs
            val index = output.currentClipIndex
            val clip = timeline.clips.getOrNull(index) ?: return 0L

            return timeline.virtualPositionAt(index, output.positionMs - clip.startMs)
        }

    override val durationMs: Long
        get() = clipTimeline?.totalDurationMs ?: output.durationMs

    /** Timing of the track currently loaded; null when the chapter has no verse timing. */
    private var timing: ChapterTimingMetadata? = null

    /** Set only in clipped (Quran+translation) mode; null while a single file is loaded. */
    private var clipTimeline: ClipTimeline? = null

    /** Chapter whose audio is on the output right now — null means nothing to reload. */
    private var loadedChapterNo: Int? = null

    private var verseTrackingJob: Job? = null
    private var bufferingDelayJob: Job? = null

    /** Guards against a slow resolution overwriting a newer play request. */
    private var latestPlaybackRequestId = 0L

    /** Remaining extra plays of the current verse (`repeatCount` is "how many times again"). */
    private var repeatRemainingForCurrentVerse = 0

    /**
     * Set while "recite only this verse" is armed; playback pauses at that verse's end.
     *
     * Mirrored into the published state so the UI can tell this short in-place burst apart from a
     * normal recitation — the mini player stays out of the way for it.
     */
    private var singleVerseStopAt: ChapterVersePair? = null
        set(value) {
            field = value
            updateState { copy(isSingleVersePlayback = value != null) }
        }

    private var activeConnectionOwners = 0
    private var settingsLoaded = false

    private val outputListener = object : RecitationAudioOutputListener {
        override fun onPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            updateState { copy(isPlaying = isPlaying, pausedByHeadset = if (isPlaying) false else pausedByHeadset) }

            if (isPlaying) startVerseTracking() else stopVerseTracking()
        }

        override fun onBufferingChanged(isBuffering: Boolean) {
            bufferingDelayJob?.cancel()

            if (!isBuffering) {
                _isBuffering.value = false
                updateState { copy(isBuffering = false) }
                return
            }

            // Only surface buffering once it lasts long enough to be worth showing, so short
            // stalls do not flicker the UI (same 500 ms rule as RecitationController).
            bufferingDelayJob = scope.launch {
                delay(BUFFERING_VISIBLE_AFTER_MS)
                _isBuffering.value = true
                updateState { copy(isBuffering = true) }
            }
        }

        override fun onEnded() {
            handlePlaybackEnded()
        }

        override fun onClipChanged(index: Int) {
            handleClipChanged(index)
        }

        override fun onError(error: Throwable) {
            AppLogger.saveError(error, "SharedRecitationPlayer.output")
            setResolving(null)
        }
    }

    init {
        output.setListener(outputListener)
    }

    // ==================== Connection ====================

    override fun connect() {
        activeConnectionOwners += 1
        _isConnected.value = true

        if (!settingsLoaded) {
            settingsLoaded = true
            scope.launch { loadSettingsFromPreferences() }
        }
    }

    override fun disconnect() {
        activeConnectionOwners = (activeConnectionOwners - 1).coerceAtLeast(0)

        // Another screen is still attached; playback (and any notification) keeps running.
        if (activeConnectionOwners > 0) return

        _isConnected.value = false
    }

    private suspend fun loadSettingsFromPreferences() {
        val settings = PlayerSettings(
            speed = RecitationPreferences.getSpeed(),
            repeatCount = RecitationPreferences.getRepeatCount(),
            audioEndBehaviour = RecitationPreferences.getAudioEndBehaviour(),
            audioOption = RecitationPreferences.getAudioOption(),
            reciter = RecitationPreferences.getReciterId(),
            translationReciter = RecitationPreferences.getTranslationReciterId(),
        )

        val lastVerse = RecitationPreferences.getLastPlayedVerse() ?: ChapterVersePair(1, 1)
        val hasSession = RecitationPreferences.hasRecitationSession()

        updateState { copy(settings = settings, currentVerse = lastVerse, hasSession = hasSession) }
        output.setSpeed(settings.speed)
        resetRepeatBudget()
    }

    // ==================== Playback ====================

    override fun playControl(verse: ChapterVersePair) {
        singleVerseStopAt = null

        val sameVerse = _state.value.currentVerse.doesEqual(verse.chapterNo, verse.verseNo)

        if (!sameVerse || output.durationMs <= 0L) {
            startInternal(verse)
            return
        }

        playPause()
    }

    override fun playSingleVerse(verse: ChapterVersePair) {
        val isPlayingThisVerse = output.isPlaying &&
                _state.value.currentVerse.doesEqual(verse.chapterNo, verse.verseNo)

        if (isPlayingThisVerse) {
            singleVerseStopAt = null
            pause()
            return
        }

        // Always (re)start from the verse's own beginning: after an automatic stop the playhead
        // sits at the verse end, where resuming would immediately spill into the next one.
        singleVerseStopAt = verse
        startInternal(verse)
    }

    override fun start(verse: ChapterVersePair?) {
        singleVerseStopAt = null
        startInternal(verse)
    }

    private fun startInternal(verse: ChapterVersePair?) {
        val target = verse ?: _state.value.currentVerse
        scope.launch { playChapter(target.chapterNo, target.verseNo) }
    }

    override fun playPause(suggestedVerse: ChapterVersePair?) {
        if (output.durationMs <= 0L) {
            start(suggestedVerse)
            return
        }

        if (output.isPlaying) output.pause() else output.play()
    }

    override fun pause() {
        output.pause()
    }

    override fun resume() {
        if (output.durationMs <= 0L) {
            start(null)
            return
        }

        output.play()
    }

    override fun stop() {
        stopVerseTracking()
        output.stop()
        timing = null
        clipTimeline = null
        loadedChapterNo = null
        repeatRemainingForCurrentVerse = 0
        singleVerseStopAt = null
        _isPlaying.value = false
        _isBuffering.value = false
        updateState { copy(isPlaying = false, isBuffering = false, resolvingChapterNo = null) }
    }

    override fun seekTo(positionMs: Long) {
        singleVerseStopAt = null

        val timeline = clipTimeline

        if (timeline != null) {
            val (index, offset) = timeline.locate(positionMs.coerceIn(0L, timeline.totalDurationMs))
            output.seekToClip(index, offset)
            return
        }

        val duration = output.durationMs
        val upper = if (duration > 0L) duration else Long.MAX_VALUE

        output.seekTo(positionMs.coerceIn(0L, upper))
        resetRepeatBudget()
    }

    override fun seekLeft() = seekTo(output.positionMs - SEEK_STEP_MS)

    override fun seekRight() = seekTo(output.positionMs + SEEK_STEP_MS)

    override fun previousVerse() {
        singleVerseStopAt = null
        scope.launch {
            val previous = _state.value.getPreviousVerse(verseStructure) ?: return@launch
            playChapter(previous.chapterNo, previous.verseNo)
        }
    }

    override fun nextVerse() {
        singleVerseStopAt = null
        scope.launch {
            val next = _state.value.getNextVerse(verseStructure) ?: return@launch
            playChapter(next.chapterNo, next.verseNo)
        }
    }

    /**
     * Loads and plays [fromVerse] of [chapterNo]. When that chapter's audio is already loaded and
     * has verse timing, this is just a seek — no re-resolution, no re-buffering.
     */
    private suspend fun playChapter(chapterNo: Int, fromVerse: Int) {
        if (!verseStructure.isVerseValid4Chapter(chapterNo, fromVerse)) return

        // The one place every play request funnels through, so it is where "this install has a
        // recitation session" becomes true — see RecitationPreferences.markRecitationSession.
        // The store write is launched, not awaited: everything below is what actually starts the
        // audio, and it must not wait on a file write to publish `resolvingChapterNo`.
        if (!_state.value.hasSession) {
            updateState { copy(hasSession = true) }
            scope.launch { RecitationPreferences.markRecitationSession() }
        }

        val requestId = ++latestPlaybackRequestId

        if (trySeekToVerseInLoadedChapter(chapterNo, fromVerse)) {
            if (requestId == latestPlaybackRequestId) setResolving(null)
            return
        }

        setResolving(chapterNo)

        val result = try {
            resolveAudio(chapterNo, _state.value.settings)
        } catch (e: Exception) {
            AppLogger.saveError(e, "SharedRecitationPlayer.playChapter")
            ResolvedAudioResult.Error(e)
        }

        // A newer request overtook this one while it was resolving.
        if (requestId != latestPlaybackRequestId) return

        setResolving(null)

        when (result) {
            is ResolvedAudioResult.Error -> AppLogger.saveError(result.error, "SharedRecitationPlayer.resolve")
            is ResolvedAudioResult.Downloading -> Unit // Resolution only ends in Error or Resolved.
            is ResolvedAudioResult.Resoved -> startChapterPlayback(result, chapterNo, fromVerse)
        }
    }

    private suspend fun startChapterPlayback(
        result: ResolvedAudioResult.Resoved,
        chapterNo: Int,
        startVerse: Int,
    ) {
        if (startClippedPlayback(result, chapterNo, startVerse)) return

        // Single-track playback: the Quran track when present, otherwise the translation one.
        val track = result.quran ?: result.translation ?: run {
            AppLogger.d("SharedRecitationPlayer", "No audio for chapter $chapterNo")
            return
        }

        clipTimeline = null
        timing = track.timingMetadata

        val startMs = timing?.getVerseTiming(startVerse)?.startMs?.coerceAtLeast(0L) ?: 0L

        loadedChapterNo = chapterNo
        output.load(track.audioUri, startMs, _state.value.settings.speed)
        resetRepeatBudget()

        updateState {
            copy(
                settings = settings.copy(
                    reciter = if (track.kind == RecitationAudioKind.QURAN) track.reciterId else settings.reciter,
                    translationReciter = if (track.kind == RecitationAudioKind.TRANSLATION) track.reciterId else settings.translationReciter,
                ),
                currentVerse = ChapterVersePair(chapterNo, startVerse),
                isVerseSyncAvailable = timing?.hasVerseTiming == true,
                pausedByHeadset = false,
            )
        }

        persistLastPlayedVerse(chapterNo, startVerse)
    }

    /**
     * Quran **and** translation together: hands the output the clip sequence instead of a file.
     *
     * Only this combination needs it — a single track plays continuously and keeps the simpler,
     * well-worn path above (including the repeat budget, which the clipped mode does not offer).
     * Returns false when the chapter cannot be clipped, so the caller falls back to one file.
     */
    private suspend fun startClippedPlayback(
        result: ResolvedAudioResult.Resoved,
        chapterNo: Int,
        startVerse: Int,
    ): Boolean {
        val tracks = VerseClipPlanner.clippableTracks(result.quran, result.translation)

        if (tracks.size < 2) return false

        val clips = VerseClipPlanner.build(
            chapterNo = chapterNo,
            verseCount = verseStructure.getChapterVerseCount(chapterNo),
            tracks = tracks,
            groupSize = RecitationPreferences.getVerseGroupSize(),
        )

        if (clips.isEmpty()) return false

        val timeline = ClipTimeline(clips)
        clipTimeline = timeline

        // Verse tracking is driven by clip boundaries here, so the polling loop must stay off.
        timing = null

        val startIndex = timeline.firstIndexForVerse(startVerse)
        loadedChapterNo = chapterNo
        output.loadClips(clips, startIndex, _state.value.settings.speed)

        updateState {
            copy(
                settings = settings.copy(
                    reciter = result.quran?.reciterId ?: settings.reciter,
                    translationReciter = result.translation?.reciterId ?: settings.translationReciter,
                ),
                currentVerse = ChapterVersePair(chapterNo, startVerse),
                isVerseSyncAvailable = true,
                pausedByHeadset = false,
            )
        }

        persistLastPlayedVerse(chapterNo, startVerse)

        return true
    }

    /**
     * A clip boundary was crossed. The clip carries its verse, so the highlight moves without any
     * polling; an armed "recite only this verse" stops here rather than running into the next one.
     */
    private fun handleClipChanged(index: Int) {
        val timeline = clipTimeline ?: return
        val clip = timeline.clips.getOrNull(index) ?: return

        // The last clip of a file has no measured end; fold in the real duration once it is known
        // so the progress bar stops guessing.
        if (clip.openEnded && clip.durationMs == 0L) {
            timeline.withMeasuredDuration(index, output.durationMs - clip.startMs)
        }

        val armed = singleVerseStopAt

        if (armed != null && !armed.doesEqual(clip.chapterNo, clip.verseNo)) {
            singleVerseStopAt = null
            output.pause()
            return
        }

        val current = _state.value.currentVerse

        if (current.doesEqual(clip.chapterNo, clip.verseNo)) return

        updateState { copy(currentVerse = ChapterVersePair(clip.chapterNo, clip.verseNo)) }
        persistLastPlayedVerse(clip.chapterNo, clip.verseNo)
    }

    /** True when the request was satisfied by seeking inside the already loaded chapter. */
    private fun trySeekToVerseInLoadedChapter(chapterNo: Int, verseNo: Int): Boolean {
        if (_state.value.resolvingChapterNo != null) return false

        clipTimeline?.let { timeline ->
            if (timeline.clips.firstOrNull()?.chapterNo != chapterNo) return false

            output.seekToClip(timeline.firstIndexForVerse(verseNo), 0L)
            updateState { copy(currentVerse = ChapterVersePair(chapterNo, verseNo)) }
            persistLastPlayedVerse(chapterNo, verseNo)

            return true
        }

        if (output.durationMs <= 0L) return false

        val loaded = timing ?: return false
        if (loaded.chapterNo != chapterNo) return false

        val verseTiming = loaded.getVerseTiming(verseNo) ?: return false

        seekToVerseStart(verseTiming.startMs)
        updateState { copy(currentVerse = ChapterVersePair(chapterNo, verseNo)) }
        persistLastPlayedVerse(chapterNo, verseNo)
        output.play()

        return true
    }

    /** Seeks just inside the verse, never onto the duration boundary (which would end playback). */
    private fun seekToVerseStart(startMs: Long) {
        val duration = output.durationMs
        val target = if (duration > 0L) startMs.coerceAtMost(duration - END_GUARD_MS) else startMs

        output.seekTo(target.coerceAtLeast(0L))
        resetRepeatBudget()
    }

    // ==================== Verse tracking & repeat ====================

    /**
     * Follows the playhead through the chapter's verse timings: moves the highlighted verse and
     * applies the repeat budget at each verse boundary. Only single-track mode needs this; the
     * loop stops as soon as playback does.
     */
    private fun startVerseTracking() {
        verseTrackingJob?.cancel()

        val loaded = timing ?: return
        if (!loaded.hasVerseTiming) return

        verseTrackingJob = scope.launch {
            while (isActive && output.isPlaying) {
                val position = output.positionMs
                val current = _state.value.currentVerse
                val currentTiming = loaded.getVerseTiming(current.verseNo)

                val atVerseEnd = currentTiming != null &&
                        position >= currentTiming.endMs - REPEAT_GUARD_MS

                // Verse is about to end and still has repeats left: jump back to its start.
                if (atVerseEnd && repeatRemainingForCurrentVerse > 0) {
                    repeatRemainingForCurrentVerse -= 1
                    output.seekTo(currentTiming!!.startMs.coerceAtLeast(0L))
                } else if (atVerseEnd && singleVerseStopAt?.doesEqual(current.chapterNo, current.verseNo) == true) {
                    // "Recite only this verse": stop here instead of running into the next one.
                    singleVerseStopAt = null
                    output.pause()
                    break
                } else {
                    val playing = loaded.getVerseAtPosition(position)

                    if (playing != null && playing.verseNo != current.verseNo) {
                        updateState { copy(currentVerse = ChapterVersePair(loaded.chapterNo, playing.verseNo)) }
                        persistLastPlayedVerse(loaded.chapterNo, playing.verseNo)
                        resetRepeatBudget()
                    }
                }

                delay(VERSE_TRACKING_INTERVAL_MS)
            }
        }
    }

    private fun stopVerseTracking() {
        verseTrackingJob?.cancel()
        verseTrackingJob = null
    }

    private fun resetRepeatBudget() {
        repeatRemainingForCurrentVerse =
            _state.value.settings.repeatCount.coerceAtLeast(RECITATION_MIN_REPEAT_COUNT)
    }

    private fun handlePlaybackEnded() {
        stopVerseTracking()

        // A "recite only this verse" play landing on a chapter's last verse ends the track itself,
        // so the tracking loop never reaches its boundary — the end *is* the stop, and it must not
        // roll on into the next chapter.
        if (singleVerseStopAt != null) {
            singleVerseStopAt = null
            return
        }

        when (_state.value.settings.audioEndBehaviour) {
            AudioEndBehaviour.STOP_PLAYBACK -> Unit

            // Verse advancing already handles chapter transitions.
            AudioEndBehaviour.NEXT_CHAPTER -> nextVerse()

            AudioEndBehaviour.REPEAT_CHAPTER -> scope.launch {
                playChapter(_state.value.currentVerse.chapterNo, 1)
            }
        }
    }

    // ==================== Settings ====================

    override fun setSpeed(speed: Float) {
        updateState { copy(settings = settings.copy(speed = speed)) }
        output.setSpeed(speed)
    }

    override fun setRepeatCount(repeatCount: Int) {
        updateState {
            copy(settings = settings.copy(repeatCount = repeatCount.coerceAtLeast(RECITATION_MIN_REPEAT_COUNT)))
        }
        resetRepeatBudget()
    }

    override fun setAudioEndBehaviour(behaviour: AudioEndBehaviour) {
        updateState { copy(settings = settings.copy(audioEndBehaviour = behaviour)) }
    }

    override fun setAudioOption(option: AudioOption) {
        if (_state.value.settings.audioOption == option) return

        updateState { copy(settings = settings.copy(audioOption = option)) }
        reloadCurrentChapter()
    }

    override fun setVerseGroupSize(size: Int) {
        // Only changes the clipped sequence; a single continuous file has no groups to regroup.
        if (clipTimeline == null) return

        reloadCurrentChapter()
    }

    /**
     * Rebuilds the current chapter after a setting that changes *what* is played (audio option,
     * group size, reciter). Keeps the listener's verse, and only resumes if it was playing.
     */
    private fun reloadCurrentChapter() {
        // Nothing is loaded yet: the new setting simply applies to the next chapter that starts.
        if (loadedChapterNo == null) return

        val current = _state.value.currentVerse
        val wasPlaying = output.isPlaying

        timing = null
        clipTimeline = null
        loadedChapterNo = null
        output.stop()

        if (wasPlaying) {
            scope.launch { playChapter(current.chapterNo, current.verseNo) }
        }
    }

    override fun setReciter(id: String, kind: RecitationAudioKind) {
        updateState {
            copy(
                settings = settings.copy(
                    reciter = if (kind == RecitationAudioKind.QURAN) id else settings.reciter,
                    translationReciter = if (kind == RecitationAudioKind.TRANSLATION) id else settings.translationReciter,
                )
            )
        }

        // Reload the current chapter from the new reciter, keeping the listener's place.
        reloadCurrentChapter()
    }

    // ==================== State helpers ====================

    private fun setResolving(chapterNo: Int?) {
        updateState { copy(resolvingChapterNo = chapterNo, downloadProgress = null) }
    }

    private fun updateState(block: RecitationServiceState.() -> RecitationServiceState) {
        _state.value = _state.value.block()
    }

    private fun persistLastPlayedVerse(chapterNo: Int, verseNo: Int) {
        scope.launch { RecitationPreferences.setLastPlayedVerse(chapterNo, verseNo) }
    }

    private companion object {
        const val SEEK_STEP_MS = 5_000L
        const val VERSE_TRACKING_INTERVAL_MS = 200L
        const val BUFFERING_VISIBLE_AFTER_MS = 500L

        /** Keeps a verse seek off the exact duration boundary, which would end playback instead. */
        const val END_GUARD_MS = 200L

        /** Repeats fire slightly before the verse ends so the next one does not bleed through. */
        const val REPEAT_GUARD_MS = 100L
    }
}
