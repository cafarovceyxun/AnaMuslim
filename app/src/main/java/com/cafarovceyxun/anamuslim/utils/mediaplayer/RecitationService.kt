package com.cafarovceyxun.anamuslim.utils.mediaplayer

import android.app.PendingIntent
import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.cafarovceyxun.anamuslim.R
import com.cafarovceyxun.anamuslim.activities.MainActivity
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ChapterTimingMetadata
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioTrack
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ResolvedAudioResult
import com.cafarovceyxun.anamuslim.components.reader.ChapterVersePair
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioEndBehaviour
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.compose.utils.appLocale
import com.cafarovceyxun.anamuslim.compose.utils.formatString
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences
import com.cafarovceyxun.anamuslim.compose.utils.preferences.RecitationPreferences.RECITATION_MIN_REPEAT_COUNT
import com.cafarovceyxun.anamuslim.db.DatabaseProvider
import com.cafarovceyxun.anamuslim.utils.IntentUtils.INTENT_ACTION_OPEN_READER
import com.cafarovceyxun.anamuslim.utils.Log
import com.cafarovceyxun.anamuslim.utils.quran.QuranMeta
import com.cafarovceyxun.anamuslim.utils.reader.factory.ReaderFactory
import com.cafarovceyxun.anamuslim.utils.univ.ErrorEvent
import com.cafarovceyxun.anamuslim.utils.univ.EventBus
import com.cafarovceyxun.anamuslim.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(UnstableApi::class)
class RecitationService : MediaLibraryService() {

    companion object {
        const val MILLIS_MULTIPLIER = 100L
        const val ACTION_SEEK_LEFT = -2L
        const val ACTION_SEEK_RIGHT = -1L

        const val EXTRA_CHAPTER = "chapter"
        const val EXTRA_VERSE = "verse"
        const val EXTRA_FROM_VERSE = "from_verse"
        const val EXTRA_TO_VERSE = "to_verse"
        const val EXTRA_JUZ = "juz"
        const val EXTRA_AUDIO_OPTION = "audio_option"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_REPEAT = "repeat"
        const val EXTRA_CONTINUE = "continue"
        const val EXTRA_SYNC = "sync"
        const val EXTRA_SEEK_AMOUNT = "seek_amount"
        const val EXTRA_FROM_USER = "from_user"

        /**
         * How far before a clip's end the single-verse stop is armed, covering the thread hop and
         * the audio sink's buffer so the pause lands on the boundary rather than past it.
         */
        private const val CLIP_STOP_LEAD_MS = 100L

        /** Max bytes for HTTP chapter audio in [SimpleCache] (LRU evicted when full). */
        private const val RECITATION_CACHE_MAX_BYTES = 512L * 1024 * 1024

        @Volatile
        private var recitationSimpleCache: SimpleCache? = null

        private val recitationCacheLock = Any()

        /**
         * Process-wide cache for streamed `https` chapter audio. Persists across [RecitationService]
         * instances; not cleared on service destroy.
         */
        private fun recitationCache(context: Context): SimpleCache {
            synchronized(recitationCacheLock) {
                recitationSimpleCache?.let { return it }

                val appCtx = context.applicationContext
                val dir = File(appCtx.cacheDir, "exo_recitation_cache")

                if (!dir.exists()) {
                    dir.mkdirs()
                }

                val evictor = LeastRecentlyUsedCacheEvictor(RECITATION_CACHE_MAX_BYTES)

                val dbProvider = StandaloneDatabaseProvider(appCtx)

                return SimpleCache(dir, evictor, dbProvider).also { recitationSimpleCache = it }
            }
        }

        val sharedState = MutableStateFlow(RecitationServiceState.EMPTY)
    }

    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var player: SessionPlayer

    private lateinit var audioRepository: RecitationAudioRepository
    private lateinit var fileUtils: FileUtils

    private val headsetReceiver = RecitationHeadsetReceiver(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var forceManifestFetch = false
    var forceTranslationManifestFetch = false

    @Volatile
    private var _repository = serviceScope.async {
        DatabaseProvider.getQuranRepository(applicationContext)
    }

    private var _singleTrackTimingMetadata = MutableStateFlow<ChapterTimingMetadata?>(null)
    internal var _verseClipPlan = MutableStateFlow<VerseClipPlan?>(null)

    private val singleTrackTimingMetadata get() = _singleTrackTimingMetadata.value

    private var verseTrackingJob: Job? = null
    private var latestPlaybackRequestId: Long = 0L
    private val chapterResolutionRequests =
        mutableMapOf<AudioResolutionRequest, Deferred<ResolvedAudioResult>>()

    private var repeatMessage: PlayerMessage? = null
    private var repeatRemainingPlaysForCurrentItem: Int = 0
    private var repeatScheduleGeneration: Long = 0L

    /**
     * Set while "recite only this verse" is armed: playback pauses at that verse's end instead of
     * continuing. Cleared by every other playback command, and once the stop has fired.
     */
    private var singleVerseStopAt: ChapterVersePair? = null

    /**
     * Whether the loaded playback was started as a single-verse burst, published so the UI can tell
     * it apart from a normal recitation and keep the mini player out of the way.
     *
     * Deliberately outlives [singleVerseStopAt]: the arm clears the instant the stop fires, a beat
     * before playback actually reports itself paused, and flipping this there would let the burst's
     * own ending pop the player open. Only a new, ordinary playback command clears it.
     */
    private var isSingleVersePlayback: Boolean = false
        set(value) {
            field = value
            updateState { copy(isSingleVersePlayback = value) }
        }

    val _state: MutableStateFlow<RecitationServiceState> get() = sharedState
    val state: StateFlow<RecitationServiceState> get() = sharedState

    // ==================== Lifecycle ====================

    override fun onCreate() {
        super.onCreate()
        fileUtils = FileUtils.newInstance(this)
        audioRepository = RecitationAudioRepository(this)

        initializePlayer()
        initializeMediaSession()
        registerHeadsetReceiver()

        scoped {
            initializeStateFromPreferences()

            launch {
                combine(
                    _verseClipPlan,
                    _singleTrackTimingMetadata,
                ) { plan, timing ->
                    plan != null || (timing != null && timing.hasVerseTiming)
                }.collectLatest { isAvailable ->
                    if (state.value.isVerseSyncAvailable == isAvailable) return@collectLatest
                    updateState { copy(isVerseSyncAvailable = isAvailable) }
                }
            }

            launch {
                state.map { it.currentVerse }
                    .distinctUntilChanged()
                    .collectLatest { verse ->
                        if (verse.isValid) {
                            updateSessionActivity(verse)
                            RecitationPreferences.setLastPlayedVerse(
                                verse.chapterNo,
                                verse.verseNo,
                            )
                        }
                    }
            }

            launch {
                state.collectLatest { newState ->
                    mediaLibrarySession?.setSessionExtras(newState.toBundle())
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession

    override fun onDestroy() {
        unregisterReceiver(headsetReceiver)
        audioRepository.cancelAll()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        serviceScope.cancel()
        sharedState.value = RecitationServiceState.EMPTY
        super.onDestroy()
    }

    private fun initializePlayer() {
        val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(
            15_000,
            30_000,
            1_500,
            3_000,
        ).build()

        val cache = recitationCache(this)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val dataSourceFactory = DefaultDataSource.Factory(this, cacheDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val basePlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                    true,
                )
                addListener(playerListener)
            }

        player = SessionPlayer(this, basePlayer)
    }

    private fun initializeMediaSession() {
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, mediaSessionCallback)
            .setId("RecitationService")
            .setSessionActivity(buildReaderPendingIntent(state.value.currentVerse))
            .build()
    }

    private fun updateSessionActivity(verse: ChapterVersePair) {
        mediaLibrarySession?.setSessionActivity(buildReaderPendingIntent(verse))
    }

    private fun buildReaderPendingIntent(verse: ChapterVersePair): PendingIntent {
        val launchIntent = if (verse.isValid) {
            ReaderFactory.prepareSingleVerseIntent(verse.chapterNo, verse.verseNo)
        } else {
            ReaderFactory.prepareChapterIntent(1)
        }.apply {
            setClass(this@RecitationService, MainActivity::class.java)
            action = INTENT_ACTION_OPEN_READER
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(this, 0, launchIntent, flags)
    }

    private fun registerHeadsetReceiver() {
        ContextCompat.registerReceiver(
            this,
            headsetReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG).apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private suspend fun repository() = _repository.await()

    private suspend fun initializeStateFromPreferences() {
        val initialSettings = PlayerSettings(
            speed = RecitationPreferences.getSpeed(),
            repeatCount = RecitationPreferences.getRepeatCount(),
            audioEndBehaviour = RecitationPreferences.getAudioEndBehaviour(),
            audioOption = AudioOption.ONLY_QURAN,
            reciter = RecitationPreferences.getReciterId(),
            translationReciter = RecitationPreferences.getTranslationReciterId(),
        )

        val lastVerse = RecitationPreferences.getLastPlayedVerse() ?: ChapterVersePair(1, 1)

        updateState { copy(settings = initialSettings, currentVerse = lastVerse) }
        player.setPlaybackSpeed(initialSettings.speed)
        invalidateRepeatSchedule()
    }

    // ==================== State helpers ====================

    private fun setResolving(chapterNo: Int?) {
        _state.value = state.value.copy(
            resolvingChapterNo = chapterNo,
            downloadProgress = null,
        )
    }

    private fun emitEvent(event: Any) {
        scoped { EventBus.send(event) }
    }

    private fun updateState(block: RecitationServiceState.() -> RecitationServiceState) {
        _state.value = state.value.block()
    }

    private fun updatePlaybackSnapshot(
        isPlaying: Boolean = player.isPlaying,
        isBuffering: Boolean = state.value.isBuffering,
    ) {
        updateState {
            copy(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
            )
        }
    }

    // ==================== Playback controls ====================

    fun playMedia() {
        player.play()
        updateState { copy(pausedByHeadset = false) }
    }

    fun pauseMedia(source: PlayerInterationSource = PlayerInterationSource.USER) {
        player.pause()
        updateState { copy(pausedByHeadset = source == PlayerInterationSource.HEADSET) }
    }

    fun playControl() {
        val duration = player.duration
        val position = player.currentPosition

        if (duration > 0 && position < duration) {
            if (player.isPlaying) pauseMedia() else playMedia()
        } else {
            restartVerse()
        }
    }

    fun stopMedia() {
        player.pause()
        player.stop()
        player.clearMediaItems()

        stopTrackings()

        _singleTrackTimingMetadata.value = null
        _verseClipPlan.value = null

        repeatRemainingPlaysForCurrentItem = 0
        singleVerseStopAt = null
        isSingleVersePlayback = false
        chapterResolutionRequests.values.forEach { it.cancel() }
        chapterResolutionRequests.clear()

        updatePlaybackSnapshot(isPlaying = false, isBuffering = false)
    }

    private fun seek(amountOrDirection: Long) {
        player.seekTo(amountOrDirection)
    }

    // Not a user seek: its only caller is [trySeekToVerseInLoadedChapter], i.e. the fast path of
    // starting a verse, so it must not clear an armed single-verse stop.
    fun seekToVerse(verseNo: Int) {
        invalidateRepeatSchedule()

        val plan = _verseClipPlan.value

        if (plan != null && !plan.isEmpty) {
            player.seekTo(plan.firstIndexForVerse(verseNo), 0L)
        } else {
            val timing = singleTrackTimingMetadata?.getVerseTiming(verseNo) ?: return
            
            // Safe seek: Ensure we don't seek exactly to or past the duration 
            // to avoid immediate STATE_ENDED triggers.
            val duration = player.duration
            val target = if (duration > 0) timing.startMs.coerceAtMost(duration - 200L) else timing.startMs
            player.seekTo(target.coerceAtLeast(0L))
        }

        updateState {
            copy(currentVerse = ChapterVersePair(currentVerse.chapterNo, verseNo))
        }

        updateNotificationMetadata(state.value.currentVerse.chapterNo, verseNo)
    }

    fun restartVerse() {
        val cv = state.value.currentVerse
        if (!cv.isValid) return
        playVerse(cv.chapterNo, cv.verseNo)
    }

    fun recitePreviousVerse() {
        singleVerseStopAt = null
        isSingleVersePlayback = false
        scoped {
            val prev = state.value.getPreviousVerse(repository()) ?: return@scoped
            playChapter(prev.chapterNo, prev.verseNo)
        }
    }

    fun reciteNextVerse() {
        singleVerseStopAt = null
        isSingleVersePlayback = false
        scoped {
            val next = state.value.getNextVerse(repository()) ?: return@scoped
            playChapter(next.chapterNo, next.verseNo)
        }
    }

    /**
     * Cancels WorkManager chapter downloads
     */
    fun cancelLoading() {
        audioRepository.cancelAll()
        setResolving(null)
    }

    // ==================== Chapter playback ====================

    fun playVerse(chapterNo: Int, verseNo: Int) {
        scoped {
            playChapter(chapterNo, verseNo)
        }
    }

    /**
     * Loads and plays [fromVerse] in [chapterNo], or seeks within the current chapter media
     * when the same chapter is already loaded and verse-level seeking is available.
     */
    private suspend fun playChapter(chapterNo: Int, fromVerse: Int) {
        val repository = repository()

        if (!repository.isVerseValid4Chapter(chapterNo, fromVerse)) {
            return
        }

        val requestId = ++latestPlaybackRequestId

        if (trySeekToVerseInLoadedChapter(chapterNo, fromVerse)) {
            if (requestId == latestPlaybackRequestId) {
                setResolving(null)
            }
            return
        }

        awaitChapterResolution(requestId, chapterNo) {
            startChapterPlayback(it, chapterNo, startVerse = fromVerse)
        }
    }

    private suspend fun awaitChapterResolution(
        requestId: Long, chapterNo: Int, action: suspend (ResolvedAudioResult.Resoved) -> Unit
    ) {
        setResolving(chapterNo)

        try {
            when (val result = resolveChapterAudio(chapterNo)) {
                is ResolvedAudioResult.Downloading -> {
                    // Terminal resolver output is always Error or Resolved.
                }

                is ResolvedAudioResult.Error -> {
                    emitEvent(ErrorEvent(result.error.message))

                    if (requestId != latestPlaybackRequestId) return
                    setResolving(null)
                }

                is ResolvedAudioResult.Resoved -> {
                    if (requestId != latestPlaybackRequestId) return
                    setResolving(null)

                    action(result)
                }
            }
        } catch (e: Exception) {
            if (requestId != latestPlaybackRequestId) return
            setResolving(null)
            Log.saveError(e, "RecitationService.loadChapterVerse")
            emitEvent(ErrorEvent(e.message))
        }
    }

    /**
     * Reuses an in-flight chapter resolution when available; otherwise starts a new one.
     * This keeps same-chapter requests efficient while allowing different chapters to
     * continue downloading in parallel.
     */
    private suspend fun resolveChapterAudio(chapterNo: Int): ResolvedAudioResult {
        val settings = state.value.settings
        val req = AudioResolutionRequest(chapterNo, settings)

        val inFlight = chapterResolutionRequests[req]
        if (inFlight != null && inFlight.isActive) {
            return inFlight.await()
        }

        val deferred = serviceScope.async {
            try {
                var terminal: ResolvedAudioResult? = null

                audioRepository.resolveAudioUris(
                    chapterNo = chapterNo,
                    settings = settings
                ).collect { result ->
                    when (result) {
                        is ResolvedAudioResult.Downloading -> {
                            // Playback resolution no longer emits this; bulk/explicit downloads use WorkManager UI.
                        }

                        is ResolvedAudioResult.Error -> terminal = result
                        is ResolvedAudioResult.Resoved -> terminal = result
                    }
                }

                terminal ?: ResolvedAudioResult.Error(
                    IllegalStateException("Audio resolution ended without terminal result")
                )
            } catch (e: Exception) {
                ResolvedAudioResult.Error(e)
            }
        }

        chapterResolutionRequests[req] = deferred

        return try {
            deferred.await()
        } finally {
            if (chapterResolutionRequests[req] == deferred && deferred.isCompleted) {
                chapterResolutionRequests.remove(req)
            }
        }
    }

    /**
     * Returns true if playback was adjusted in place (seek + play) without re-resolving audio.
     */
    private fun trySeekToVerseInLoadedChapter(
        chapterNo: Int, verseNo: Int
    ): Boolean {
        if (
            state.value.resolvingChapterNo != null ||
            state.value.currentVerse.chapterNo != chapterNo ||
            player.mediaItemCount == 0 ||
            this@RecitationService.player.playbackState == Player.STATE_IDLE
        ) return false

        val timingMeta = singleTrackTimingMetadata ?: return false
        if (timingMeta.chapterNo != chapterNo) return false

        val plan = _verseClipPlan.value
        val canSeekWithPlan = plan != null && !plan.isEmpty
        val canSeekWithTiming = timingMeta.getVerseTiming(verseNo) != null
        if (!canSeekWithPlan && !canSeekWithTiming) return false

        seekToVerse(verseNo)
        playMedia()

        // When playback was already running there is no isPlaying transition to (re)start verse
        // tracking, so the verse we just jumped to would get no boundary message of its own.
        rescheduleRepeatForCurrentPosition()

        return true
    }

    private suspend fun startChapterPlayback(
        result: ResolvedAudioResult.Resoved,
        chapterNo: Int,
        startVerse: Int,
    ) {
        val settings = state.value.settings
        val plan = buildMultiTrackVerseClipPlan(
            chapterNo = chapterNo,
            result = result,
            settings = settings,
        )

        _verseClipPlan.value = plan

        val updatedSettings = settings.copy(
            reciter = result.quran?.reciterId,
            translationReciter = result.translation?.reciterId,
        )

        player.stop()
        player.clearMediaItems()

        if (plan != null) {
            // Multi-track clipped playlist: verse boundaries are encoded in the clips themselves,
            // so per-track timing metadata is not needed at the service level.
            _singleTrackTimingMetadata.value = null
            player.setMediaItems(plan.items)
            
            val initialIndex = plan.firstIndexForVerse(startVerse)
            if (initialIndex >= 0) {
                player.seekTo(initialIndex, 0L)
            }
        } else {
            // Single file fallback — store the timing of the track we actually load,
            // so verse seeking / tracking works against its positions.
            val primary = result.quran ?: result.translation ?: run {
                emitEvent(ErrorEvent("No audio for this chapter"))
                return
            }

            _singleTrackTimingMetadata.value = primary.timingMetadata

            player.setMediaItem(
                buildFullChapterMediaItem(primary, chapterNo, startVerse),
            )

            // Re-read from value to be absolutely sure we have the latest
            val metadata = _singleTrackTimingMetadata.value
            val timing = metadata?.getVerseTiming(startVerse)
            
            if (timing != null && (timing.startMs > 0 || startVerse > 1)) {
                 val duration = player.duration
                 val target = if (duration > 0) timing.startMs.coerceAtMost(duration - 200L) else timing.startMs
                 player.seekTo(target.coerceAtLeast(0L))
            } else {
                 player.seekTo(0L)
            }
        }

        player.repeatMode = Player.REPEAT_MODE_OFF

        invalidateRepeatSchedule()

        player.prepare()
        player.play()

        updateState {
            copy(
                settings = updatedSettings,
                currentVerse = ChapterVersePair(chapterNo, startVerse),
                pausedByHeadset = false,
            )
        }
    }

    // ==================== Playlist building ====================
    /**
     * Determines which audio sources to include as separate tracks in the clipped playlist.
     * Result already contains resolved URIs and timing metadata based on current settings,
     * so this just maps to internal TrackSource objects.
     */
    private suspend fun resolveTrackSources(
        result: ResolvedAudioResult.Resoved,
    ): List<RecitationAudioTrack>? {
        val sources = mutableListOf<RecitationAudioTrack>()

        // Local vals: the tracks are `commonMain` properties, so they do not smart-cast here.
        val quranTrack = result.quran
        val translationTrack = result.translation

        if (quranTrack != null) {
            if (quranTrack.hasVerseTiming) {
                sources.add(quranTrack)
            } else {
                emitEvent(
                    ErrorEvent(
                        getString(R.string.missingTimingData, quranTrack.getReciterName())
                    )
                )
            }
        }

        if (translationTrack != null) {
            if (translationTrack.hasVerseTiming) {
                sources.add(translationTrack)
            } else {
                emitEvent(
                    ErrorEvent(
                        getString(R.string.missingTimingData, translationTrack.getReciterName())
                    )
                )
            }
        }

        return if (sources.isEmpty()) {
            null
        } else {
            sources.sortedBy { track ->
                when (track.kind) {
                    RecitationAudioKind.QURAN -> 0
                    RecitationAudioKind.TRANSLATION -> 1
                }
            }
        }
    }

    /**
     * Builds a clipped playlist for verse-level playback for multiple audio tracks (Quran + translation).
     * Returns null when timing is unavailable; caller falls back to single unclipped file.
     */
    private suspend fun buildMultiTrackVerseClipPlan(
        chapterNo: Int,
        result: ResolvedAudioResult.Resoved,
        settings: PlayerSettings,
    ): VerseClipPlan? {
        val tracks = resolveTrackSources(result) ?: return null

        val meta = repository()
        val verseCount = meta.getChapterVerseCount(chapterNo)
        val trackCount = tracks.size
        val items = ArrayList<MediaItem>(verseCount * trackCount)
        val metadataBuilder = buildMediaMetadata(chapterNo, 1)
        val chunkSize = RecitationPreferences.getVerseGroupSize()
        var startVerseNo = 1

        while (startVerseNo <= verseCount) {
            val endVerseNo = minOf(startVerseNo + chunkSize - 1, verseCount)
            for (track in tracks) {
                for (verseNo in startVerseNo..endVerseNo) {
                    val vt = track.timingMetadata!!.getVerseTiming(verseNo) ?: continue
                    
                    // quran.com style logic: For the last verse of the surah, 
                    // we play until the end of source to ensure perfect completion.
                    val isLastVerseOfSurah = verseNo == verseCount
                    val endMs = if (isLastVerseOfSurah) C.TIME_END_OF_SOURCE else vt.endMs

                    if (!isLastVerseOfSurah && !isValidTimingWindow(vt.startMs, endMs)) continue

                    items.add(
                        MediaItem.Builder()
                            .setMediaId("$chapterNo:$verseNo")
                            .setUri(track.audioUri)
                            .setMediaMetadata(
                                metadataBuilder
                                    .setTitle(buildTitle(chapterNo, verseNo))
                                    .build()
                            )
                            .setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(vt.startMs)
                                    .setEndPositionMs(endMs)
                                    .build(),
                            )
                            .build(),
                    )
                }
            }
            startVerseNo += chunkSize
        }

        return if (items.isEmpty()) null else VerseClipPlan.from(items)
    }

    private fun isValidTimingWindow(startMs: Long, endMs: Long): Boolean {
        if (
            startMs == C.TIME_UNSET ||
            endMs == C.TIME_UNSET ||
            startMs < 0L ||
            endMs < 0L ||
            endMs <= startMs
        ) return false

        return try {
            Math.subtractExact(endMs, startMs) > 0L
        } catch (_: ArithmeticException) {
            false
        }
    }

    /**
     * Single unclipped MediaItem used when no verse timing is available.
     */
    private suspend fun buildFullChapterMediaItem(
        audio: RecitationAudioTrack,
        chapterNo: Int,
        startVerse: Int
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("chapter_$chapterNo")
            .setUri(audio.audioUri)
            .setMediaMetadata(buildMediaMetadata(chapterNo, startVerse).build())
            .build()
    }

    // ==================== Metadata ====================

    private suspend fun buildTitle(chapterNo: Int, verseNo: Int?): String {
        val repository = repository()
        val chapterName = repository.getChapterName(chapterNo)
        return if (verseNo != null) getString(
            R.string.strLabelVerseWithChapNameAndNo,
            chapterName,
            chapterNo,
            verseNo
        ) else chapterName
    }

    private suspend fun buildArtist(): String? {
        val manager = RecitationModelManager
        return manager.getCurrentReciterNameForAudioOption()
    }

    private suspend fun buildMediaMetadata(
        chapterNo: Int,
        verseNo: Int
    ): MediaMetadata.Builder {

        return MediaMetadata.Builder()
            .setAlbumTitle(getString(R.string.strTitleHolyQuran))
            .setTitle(buildTitle(chapterNo, verseNo))
            .setArtist(buildArtist())
            .setArtworkUri(
                RecitationChapterArtwork.getChapterArtworkUri(
                    this,
                    chapterNo
                )
            )
    }

    // ==================== Verse tracking ====================

    /**
     * Starts position-based verse tracking for single-track mode.
     * Clip-plan mode does not need polling — verse changes are detected
     * via [checkVerseChanged] on media item transitions, and each clip
     * already has per-verse MediaMetadata baked in.
     */
    private fun startVerseTracking() {
        verseTrackingJob?.cancel()

        if (_verseClipPlan.value != null) {
            // Clip mode needs no polling, but a single-verse play still wants a sample-accurate
            // stop rather than the item transition, which only lands once the next clip has begun.
            scheduleSingleVerseClipStop()
            return
        }

        val timing = singleTrackTimingMetadata
        if (timing == null || !timing.hasVerseTiming) {
            return
        }

        verseTrackingJob = serviceScope.launch {
            // The verse playback started on gets no boundary from the loop below (it never
            // "changes"), so arm it here — that is the one a single-verse play has to stop at.
            rescheduleRepeatForCurrentPosition()

            while (isActive && player.isPlaying) {
                val vt = timing.getVerseAtPosition(player.currentPosition)
                val current = state.value.currentVerse

                if (vt != null) {
                    if (vt.verseNo != current.verseNo) {
                        val newVerse = ChapterVersePair(timing.chapterNo, vt.verseNo)
                        updateState { copy(currentVerse = newVerse) }
                        updateNotificationMetadata(timing.chapterNo, vt.verseNo)
                        invalidateRepeatSchedule()
                        scheduleVerseBoundary(vt.startMs, vt.endMs)
                    }
                }

                delay(200)
            }
        }
    }

    private fun stopTrackings() {
        verseTrackingJob?.cancel()
        verseTrackingJob = null

        // only cancel the job without resetting state of repeat schedule
        repeatMessage?.cancel()
        repeatMessage = null
    }


    /**
     * Rebuilds the current chapter queue after settings changes that impact resolved sources
     * or clip construction, restoring the user's virtual position afterwards.
     */
    private suspend fun invalidateChapterPlayback() {
        if (state.value.resolvingChapterNo != null) return
        if (player.mediaItemCount == 0 || this@RecitationService.player.playbackState == Player.STATE_IDLE) return

        val current = state.value.currentVerse

        val chapterNo = current.chapterNo
        val verseNo = current.verseNo

        if (player.isPlaying) {
            player.pause()
        }

        val shouldResumePlaying = player.isPlaying

        val requestId = ++latestPlaybackRequestId

        awaitChapterResolution(requestId, chapterNo) {
            startChapterPlayback(it, chapterNo, startVerse = verseNo)

            if (!shouldResumePlaying) {
                pauseMedia()
            }
        }
    }

    fun invalidateRepeatSchedule() {
        repeatScheduleGeneration++
        repeatMessage?.cancel()
        repeatMessage = null
        repeatRemainingPlaysForCurrentItem =
            state.value.settings.repeatCount.coerceAtLeast(RECITATION_MIN_REPEAT_COUNT)
    }

    /**
     * Arms a player message at the current verse's end, which either replays the verse (repeat
     * budget left) or pauses playback ("recite only this verse"). Sample-accurate, unlike the
     * 200 ms tracking poll, so neither case bleeds into the next verse.
     */
    private fun scheduleVerseBoundary(startMs: Long, endMs: Long) {
        repeatMessage?.cancel()

        if (!isSingleTrackRepeatEligible() && !isSingleVerseStopEligible()) return

        val myGeneration = repeatScheduleGeneration

        val message = player.exoPlayer.createMessage { _, _ ->
            if (myGeneration != repeatScheduleGeneration) return@createMessage

            if (isSingleTrackRepeatEligible()) {
                repeatRemainingPlaysForCurrentItem--

                this@RecitationService.player.seekTo(startMs)

                // reschedule for next repeat
                scheduleVerseBoundary(startMs, endMs)
                return@createMessage
            }

            if (isSingleVerseStopEligible()) {
                singleVerseStopAt = null
                pauseMedia()
            }
        }

        // Offset by 100ms to account for thread-hopping and audio-sink buffer
        // which prevents bleeding of the next verse.
        message
            .setPosition((endMs - 100).coerceAtLeast(0))
            .setLooper(Looper.getMainLooper())
            .setDeleteAfterDelivery(true)
            .send()

        repeatMessage = message
    }

    /**
     * Arms the "recite only this verse" stop against the end of the current clip. Clip mode has no
     * chapter-level timing to schedule against, but each item *is* the verse, so its own duration is
     * the boundary — and stopping there keeps the next verse from bleeding through, which waiting
     * for the media item transition does not.
     */
    private fun scheduleSingleVerseClipStop() {
        repeatMessage?.cancel()
        repeatMessage = null

        val armed = singleVerseStopAt ?: return
        val current = state.value.currentVerse
        if (!armed.doesEqual(current.chapterNo, current.verseNo)) return

        // The clip's own length, straight from the player: SessionPlayer.getDuration() deliberately
        // reports the whole chapter's virtual duration for the notification's progress bar, which
        // would put this message hours past the boundary it is meant to guard.
        // Unset right after a seek; the media item transition remains the fallback stop.
        val duration = player.exoPlayer.duration
        if (duration <= 0L) return

        val myGeneration = repeatScheduleGeneration
        val index = player.exoPlayer.currentMediaItemIndex

        val message = player.exoPlayer.createMessage { _, _ ->
            if (myGeneration != repeatScheduleGeneration) return@createMessage
            if (singleVerseStopAt == null) return@createMessage

            singleVerseStopAt = null
            pauseMedia()
        }

        message
            .setPosition(index, (duration - CLIP_STOP_LEAD_MS).coerceAtLeast(0L))
            .setLooper(Looper.getMainLooper())
            .setDeleteAfterDelivery(true)
            .send()

        repeatMessage = message
    }

    private fun rescheduleRepeatForCurrentPosition() {
        val timing = singleTrackTimingMetadata ?: return
        if (!timing.hasVerseTiming) return

        val vt = timing.getVerseAtPosition(player.currentPosition) ?: return

        scheduleVerseBoundary(vt.startMs, vt.endMs)
    }

    private fun isSingleTrackRepeatEligible(): Boolean {
        return state.value.settings.audioOption == AudioOption.ONLY_QURAN &&
                repeatRemainingPlaysForCurrentItem > 0 &&
                singleTrackTimingMetadata != null
    }

    private fun isSingleVerseStopEligible(): Boolean {
        val armed = singleVerseStopAt ?: return false
        val current = state.value.currentVerse

        return singleTrackTimingMetadata != null &&
                armed.doesEqual(current.chapterNo, current.verseNo)
    }

    /**
     * Updates the current media item's metadata so the notification reflects the current verse.
     * Only needed for single-track mode where one media item spans the whole chapter.
     */
    private fun updateNotificationMetadata(chapterNo: Int, verseNo: Int) {
        scoped {
            val meta = buildMediaMetadata(chapterNo, verseNo).build()

            val realPlayer = player.exoPlayer
            val updated = realPlayer.currentMediaItem?.buildUpon()
                ?.setMediaMetadata(meta)
                ?.build()
                ?: return@scoped

            realPlayer.replaceMediaItem(realPlayer.currentMediaItemIndex, updated)
        }
    }

    /**
     * Called on media item transitions and seek discontinuities.
     * Parses the mediaId ("chapterNo:verseNo") to update the current verse.
     * Only meaningful for clip-plan playlists where each item is a verse.
     *
     * [isAutoAdvance] separates playback rolling off the end of a clip on its own from the player
     * being moved — a seek, or the jump to index 0 that setting a fresh playlist reports. Only the
     * former ends a "recite only this verse" play; the latter two are how such a play *starts*.
     */
    private fun checkVerseChanged(isAutoAdvance: Boolean) {
        val mediaId = player.exoPlayer.currentMediaItem?.mediaId ?: return
        val parts = mediaId.split(':')
        if (parts.size != 2) return

        val chapter = parts[0].toIntOrNull() ?: return
        val verse = parts[1].toIntOrNull() ?: return

        val resolved = ChapterVersePair(chapter, verse)
        if (resolved != state.value.currentVerse) {
            val armed = singleVerseStopAt

            updateState { copy(currentVerse = resolved) }
            updateNotificationMetadata(chapter, verse)

            // Clip-plan mode has no single-track timing to arm a boundary message against, so the
            // "recite only this verse" stop lands on the transition off that verse's clip instead.
            // Single-track mediaIds name the chapter's load-time verse, not the playing one, so
            // this must never run there — the boundary message covers that mode.
            if (isAutoAdvance && _verseClipPlan.value != null && armed != null && armed != resolved) {
                singleVerseStopAt = null
                pauseMedia()
            }
        }
    }

    private fun handlePlaybackEnded() {
        // A "recite only this verse" play landing on a chapter's last verse ends the media itself,
        // so there is no transition to stop on — the end *is* the stop, and it must not roll on
        // into the next chapter.
        if (singleVerseStopAt != null) {
            singleVerseStopAt = null
            return
        }

        when (state.value.settings.audioEndBehaviour) {
            AudioEndBehaviour.STOP_PLAYBACK -> {
                // do nothing - playback has already ended, so just leave it at that.
            }

            AudioEndBehaviour.NEXT_CHAPTER -> scoped {
                // next verse logic will handle chapter transitions as well, so just try to go to the next verse.
                reciteNextVerse()
            }

            AudioEndBehaviour.REPEAT_CHAPTER -> scoped {
                val currentChapterNo = state.value.currentVerse.chapterNo

                if (repository().isVerseValid4Chapter(currentChapterNo, 1)) {
                    playChapter(currentChapterNo, 1)
                }
            }
        }
    }

    fun refreshLibrary() {
        mediaLibrarySession?.notifyChildrenChanged("root", Int.MAX_VALUE, null)
    }

    // ==================== Session callback & command dispatch ====================
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            checkVerseChanged(
                isAutoAdvance = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            )
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION || reason == Player.DISCONTINUITY_REASON_SEEK) {
                checkVerseChanged(
                    isAutoAdvance = reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackSnapshot(isBuffering = playbackState == Player.STATE_BUFFERING)

            // Duration is unset until the item is ready, which is typically after playback (and so
            // startVerseTracking) has already begun — this is the arm that actually catches.
            if (playbackState == Player.STATE_READY && _verseClipPlan.value != null) {
                scheduleSingleVerseClipStop()
            }

            if (playbackState == Player.STATE_ENDED) {
                // Ensure current verse is updated to the absolute last one before handling end.
                // This prevents the "restart from beginning" if the system tries to resume.
                scoped {
                    val count = repository().getChapterVerseCount(state.value.currentVerse.chapterNo)
                    if (state.value.currentVerse.verseNo < count) {
                        val finalVerse = state.value.currentVerse.copy(verseNo = count)
                        updateState { copy(currentVerse = finalVerse) }
                        RecitationPreferences.setLastPlayedVerse(finalVerse.chapterNo, finalVerse.verseNo)
                    }
                    handlePlaybackEnded()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState {
                copy(
                    isPlaying = isPlaying,
                    pausedByHeadset = if (isPlaying) false else pausedByHeadset,
                )
            }

            if (isPlaying) {
                startVerseTracking()
            } else {
                stopTrackings()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.saveError(error, "RecitationService.ExoPlayer")
            error.printStackTrace()
        }
    }

    private val mediaSessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .apply {
                    ALL_PLAYER_ACTIONS.forEach { add(SessionCommand(it, Bundle.EMPTY)) }
                }.build()

            val resultBuilder = MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(connectionResult.availablePlayerCommands)

            return resultBuilder.build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            dispatchCommand(command.customAction, args)

            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .build()
                )
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                try {
                    val children = mutableListOf<MediaItem>()
                    when (parentId) {
                        "root" -> {
                            children.add(
                                MediaItem.Builder()
                                    .setMediaId("surahs_root")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(getString(R.string.strTitleReaderChapters))
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                            .setIsPlayable(false)
                                            .setIsBrowsable(true)
                                            .build()
                                    )
                                    .build()
                            )
                            children.add(
                                MediaItem.Builder()
                                    .setMediaId("reciters_root")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(getString(R.string.strTitleSelectReciter))
                                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                                            .setIsPlayable(false)
                                            .setIsBrowsable(true)
                                            .build()
                                    )
                                    .build()
                            )
                        }

                        "surahs_root" -> {
                            buildItems(children, true) {
                                "chapter_$it"
                            }
                        }

                        "reciters_root" -> {
                            val reciters = RecitationModelManager
                                .getAllQuranModel()?.reciters.orEmpty()

                            reciters.forEach { reciter ->
                                children.add(
                                    MediaItem.Builder()
                                        .setMediaId("auto_reciter_${reciter.id}")
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(reciter.getReciterName())
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                                .setIsPlayable(false)
                                                .setIsBrowsable(true)
                                                .build()
                                        )
                                        .build()
                                )
                            }
                        }

                        else -> {
                            if (parentId.startsWith("auto_reciter_")) {
                                val reciterId = parentId.removePrefix("auto_reciter_")

                                buildItems(children, false) {
                                    "chapter_${it}_reciter_${reciterId}"
                                }
                            }
                        }
                    }

                    future.set(LibraryResult.ofItemList(children, null))
                } catch (e: Exception) {
                    future.setException(e)
                }
            }
            return future
        }

        suspend fun buildItems(
            children: MutableList<MediaItem>,
            withArtist: Boolean,
            idBuilder: (Int) -> String
        ) =
            withContext(Dispatchers.IO) {
                val appLocale = appLocale()
                val artist = if (withArtist) buildArtist() else null

                for (i in QuranMeta.chapterRange) {
                    val name = repository().getChapterName(i)

                    children.add(
                        MediaItem.Builder()
                            .setMediaId(idBuilder(i))
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle("$i. $name")
                                    .setTitle(
                                        formatString(
                                            this@RecitationService,
                                            appLocale,
                                            $$"%1$d. %2$s",
                                            i,
                                            getString(R.string.strLabelSurah, name)
                                        )
                                    )
                                    .apply {
                                        if (artist != null) {
                                            setArtist(artist)
                                        }
                                    }
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                    .setIsPlayable(true)
                                    .setIsBrowsable(false)
                                    .setArtworkUri(
                                        RecitationChapterArtwork.getChapterArtworkUri(
                                            this@RecitationService,
                                            i
                                        )
                                    )
                                    .build()
                            )
                            .build()
                    )
                }
            }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_NOT_SUPPORTED))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val autoItems = mediaItems.filter { it.mediaId.startsWith("auto_chapter_") }

            if (autoItems.isNotEmpty()) {
                val item = autoItems.first()
                val id = item.mediaId

                serviceScope.launch {
                    val chapterStr = if (id.contains("_reciter_")) {
                        val parts = id.removePrefix("auto_chapter_").split("_reciter_")
                        val chapterNo = parts[0].toIntOrNull()
                        val reciterId = parts.getOrNull(1)

                        if (reciterId != null) {
                            RecitationPreferences.setReciterId(reciterId)
                            RecitationPreferences.setAudioOption(AudioOption.ONLY_QURAN)

                            updateState {
                                copy(
                                    settings = settings.copy(
                                        reciter = reciterId,
                                        audioOption = AudioOption.ONLY_QURAN
                                    )
                                )
                            }
                        }

                        chapterNo
                    } else {
                        id.removePrefix("auto_chapter_").toIntOrNull()
                    }

                    if (chapterStr != null) {
                        playVerse(chapterStr, 1)
                    }
                }

                return Futures.immediateFuture(emptyList())
            }

            return super.onAddMediaItems(mediaSession, controller, mediaItems)
        }
    }

    private fun dispatchCommand(action: String, args: Bundle) {
        when (action) {
            StartCommand.ACTION -> StartCommand.fromBundle(args)?.let { reduce(it) }

            StartSingleVerseCommand.ACTION -> StartSingleVerseCommand.fromBundle(args)
                ?.let { reduce(it) }

            SetAudioOptionCommand.ACTION -> SetAudioOptionCommand(AudioOption.ONLY_QURAN)
                ?.let { reduce(it) }

            SetVerseGroupSizeCommand.ACTION -> SetVerseGroupSizeCommand.fromBundle(args)
                ?.let { reduce(it) }

            SetPlaybackSpeedCommand.ACTION -> SetPlaybackSpeedCommand.fromBundle(args)
                ?.let { reduce(it) }

            SetRepeatCommand.ACTION -> SetRepeatCommand.fromBundle(args)?.let { reduce(it) }

            SetAudioEndBehaviourCommand.ACTION -> SetAudioEndBehaviourCommand.fromBundle(args)
                ?.let { reduce(it) }

            SetReciterCommand.ACTION -> SetReciterCommand.fromBundle(args)?.let { reduce(it) }

            SeekToPositionCommand.ACTION -> SeekToPositionCommand.fromBundle(args)
                ?.let { reduce(it) }

            StopCommand.ACTION -> reduce(StopCommand)
            PreviousVerseCommand.ACTION -> reduce(PreviousVerseCommand)
            NextVerseCommand.ACTION -> reduce(NextVerseCommand)
        }
    }

    private fun <T : BasePlayerCommand> reduce(cmd: T) = scoped {
        when (cmd) {
            is StartCommand -> {
                singleVerseStopAt = null
                isSingleVersePlayback = false
                val verse = cmd.verse ?: state.value.currentVerse
                playVerse(verse.chapterNo, verse.verseNo)
            }

            is StartSingleVerseCommand -> {
                singleVerseStopAt = cmd.verse
                isSingleVersePlayback = true
                playVerse(cmd.verse.chapterNo, cmd.verse.verseNo)
            }

            is SetAudioOptionCommand -> {
                updateState { copy(settings = settings.copy(audioOption = AudioOption.ONLY_QURAN)) }
                invalidateChapterPlayback()
                refreshLibrary()
            }

            is SetVerseGroupSizeCommand -> {
                invalidateChapterPlayback()
            }

            is SetPlaybackSpeedCommand -> {
                updateState { copy(settings = settings.copy(speed = cmd.speed)) }
                player.setPlaybackSpeed(cmd.speed)

                invalidateRepeatSchedule()
                rescheduleRepeatForCurrentPosition()
            }

            is SetRepeatCommand -> {
                updateState {
                    copy(settings = settings.copy(repeatCount = cmd.repeatCount))
                }

                player.repeatMode = Player.REPEAT_MODE_OFF
                invalidateRepeatSchedule()
                rescheduleRepeatForCurrentPosition()
            }

            is SetAudioEndBehaviourCommand -> {
                updateState { copy(settings = settings.copy(audioEndBehaviour = cmd.behaviour)) }
            }

            is SeekToPositionCommand -> {
                singleVerseStopAt = null
                isSingleVersePlayback = false
                seek(cmd.positionMs)
            }

            is StopCommand -> stopMedia()
            is PreviousVerseCommand -> recitePreviousVerse()
            is NextVerseCommand -> reciteNextVerse()

            is SetReciterCommand -> {
                updateState {
                    copy(
                        settings = settings.copy(
                            reciter = if (cmd.kind == RecitationAudioKind.QURAN) cmd.reciter else settings.reciter,
                            translationReciter = if (cmd.kind == RecitationAudioKind.TRANSLATION) cmd.reciter else settings.translationReciter,
                        )
                    )
                }

                invalidateChapterPlayback()
                refreshLibrary()
            }
        }
    }

    private fun scoped(action: suspend CoroutineScope.() -> Unit) {
        serviceScope.launch { action() }
    }
}
