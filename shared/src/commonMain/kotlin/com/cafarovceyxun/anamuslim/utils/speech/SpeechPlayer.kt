package com.cafarovceyxun.anamuslim.utils.speech

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Növbədə bir hədis: [text] xam `text_az`-dır, normallaşdırma pleyerin işidir. */
data class SpeechItem(val id: Long, val number: Int, val text: String)

sealed interface SpeechState {
    data object Idle : SpeechState

    data class Active(
        val item: SpeechItem,
        val queueIndex: Int,
        val queueSize: Int,
        /** Hazırda səslənən parça; [preparing] ikən hələ heç biri səslənmir. */
        val chunkIndex: Int,
        val chunkCount: Int,
        val preparing: Boolean,
        val paused: Boolean,
    ) : SpeechState {
        val hasNext: Boolean get() = queueIndex < queueSize - 1
    }

    data class Failed(val reason: SpeechFailure) : SpeechState
}

enum class SpeechFailure {
    /** Platforma mühərrik qeydiyyat etməyib. */
    EngineUnavailable,

    /** Model faylları var, amma mühərrik onları aça bilmədi (pozulmuş fayl, yaddaş). */
    ModelLoadFailed,
}

/**
 * Hədis növbəsini cihazda səsləndirir: mətni parçalara bölür, növbəti parçanı əvvəlki çalınarkən
 * sintez edir və növbə bitənə qədər hədisdən hədisə keçir.
 *
 * Sintez [synthDispatcher]-də gedir — tək axınlıdır, çünki mühərrik paralel çağırışa hazır deyil.
 * Orkestr [scope]-dadır, kompozisiyada **yox**: ekran fırlansa və ya vərəq bağlansa da səs kəsilmir
 * (CLAUDE.md, «`rememberCoroutineScope()` çox addımlı iş üçün etibarlı deyil»).
 */
class SpeechPlayer(
    private val engine: () -> OfflineSpeechEngine?,
    private val newOutput: () -> PcmSpeechOutput?,
    private val modelDir: () -> String,
    private val scope: CoroutineScope,
    private val synthDispatcher: CoroutineDispatcher,
) {
    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    /** Danışıq sürəti; 1 = modelin öz tempi. Növbəti parçadan etibarən tətbiq olunur. */
    var speed: Float = 1f

    private var job: Job? = null
    private var queue: List<SpeechItem> = emptyList()
    private var output: PcmSpeechOutput? = null

    /**
     * Hər [play]/[stop] nəsli artırır. Ləğv kooperativdir: köhnə növbə növbəti dayanacağına qədər
     * bir-iki sətir də işləyə bilər — vəziyyəti yalnız cari nəsil yazır ki, köhnə hədis yenisinin
     * üstünə düşməsin.
     */
    private var generation = 0

    fun play(items: List<SpeechItem>, startIndex: Int = 0) {
        if (items.isEmpty() || startIndex !in items.indices) return
        val previous = job
        previous?.cancel()
        val gen = ++generation
        queue = items
        val paused = (_state.value as? SpeechState.Active)?.paused == true
        _state.value = active(items, startIndex, chunkIndex = 0, chunkCount = 0, preparing = true, paused = paused)
        job = scope.launch {
            previous?.join()
            runQueue(gen, items, startIndex)
        }
    }

    fun stop() {
        generation++
        job?.cancel()
        job = null
        _state.value = SpeechState.Idle
    }

    fun pause() {
        val current = _state.value as? SpeechState.Active ?: return
        output?.pause()
        _state.value = current.copy(paused = true)
    }

    fun resume() {
        val current = _state.value as? SpeechState.Active ?: return
        output?.resume()
        _state.value = current.copy(paused = false)
    }

    fun next() {
        val current = _state.value as? SpeechState.Active ?: return
        if (current.hasNext) play(queue, current.queueIndex + 1)
    }

    /** Parçanın ortasındadırsa hədisi başdan, əvvəlindədirsə əvvəlki hədisi oxuyur. */
    fun previous() {
        val current = _state.value as? SpeechState.Active ?: return
        val target = if (current.chunkIndex <= 0 && current.queueIndex > 0) current.queueIndex - 1 else current.queueIndex
        play(queue, target)
    }

    /** Pleyeri dayandırır və mühərriki sintez axınında buraxır (model silinməzdən əvvəl). */
    suspend fun shutdownEngine() {
        generation++
        job?.cancelAndJoin()
        job = null
        _state.value = SpeechState.Idle
        withContext(synthDispatcher) { engine()?.release() }
    }

    private fun setState(gen: Int, transform: (SpeechState) -> SpeechState) {
        if (gen == generation) _state.update(transform)
    }

    private suspend fun runQueue(gen: Int, items: List<SpeechItem>, startIndex: Int) {
        val engine = engine() ?: return setState(gen) { SpeechState.Failed(SpeechFailure.EngineUnavailable) }
        val sampleRate = withContext(synthDispatcher) { engine.load(modelDir()) }
        if (sampleRate <= 0) return setState(gen) { SpeechState.Failed(SpeechFailure.ModelLoadFailed) }
        val output = newOutput() ?: return setState(gen) { SpeechState.Failed(SpeechFailure.EngineUnavailable) }

        this.output = output
        try {
            output.open(sampleRate)
            // Hazırlıq zamanı pauzaya basılıbsa çıxış da pauzada başlasın.
            if ((_state.value as? SpeechState.Active)?.paused == true) output.pause()
            for (index in startIndex until items.size) {
                speakItem(gen, engine, output, sampleRate, items, index)
            }
            setState(gen) { SpeechState.Idle }
        } finally {
            output.close()
            if (this.output === output) this.output = null
        }
    }

    private suspend fun speakItem(
        gen: Int,
        engine: OfflineSpeechEngine,
        output: PcmSpeechOutput,
        sampleRate: Int,
        items: List<SpeechItem>,
        index: Int,
    ) = coroutineScope {
        val chunks = SpeechText.chunks(SpeechText.hadith(items[index].text))
        if (chunks.isEmpty()) return@coroutineScope
        setState(gen) { current ->
            val paused = (current as? SpeechState.Active)?.paused == true
            active(items, index, 0, chunks.size, preparing = true, paused = paused)
        }

        // Tutum 1: biri çalınarkən növbətisi hazır olur, amma sintez çox qabağa qaçıb yaddaş yığmır.
        val ready = Channel<FloatArray>(capacity = 1)
        launch(synthDispatcher) {
            try {
                for (chunk in chunks) {
                    val pcm = engine.synthesize(chunk.text, speed) ?: FloatArray(0)
                    ready.send(pcm.withSilence(chunk.pause.millis * sampleRate / 1000))
                }
            } finally {
                ready.close()
            }
        }

        var chunkIndex = 0
        for (pcm in ready) {
            setState(gen) { current ->
                (current as? SpeechState.Active)?.copy(
                    chunkIndex = chunkIndex, chunkCount = chunks.size, preparing = false,
                ) ?: current
            }
            output.play(pcm)
            chunkIndex++
        }
    }

    private fun active(
        items: List<SpeechItem>,
        index: Int,
        chunkIndex: Int,
        chunkCount: Int,
        preparing: Boolean,
        paused: Boolean,
    ) = SpeechState.Active(
        item = items[index],
        queueIndex = index,
        queueSize = items.size,
        chunkIndex = chunkIndex,
        chunkCount = chunkCount,
        preparing = preparing,
        paused = paused,
    )

    private fun FloatArray.withSilence(samples: Int): FloatArray =
        if (samples <= 0) this else copyOf(size + samples)
}

/** Tətbiqin tək hədis pleyeri — ekranlar arasında paylaşılır, proses ömrü boyu yaşayır. */
object HadithSpeech {
    @OptIn(ExperimentalCoroutinesApi::class)
    val player: SpeechPlayer by lazy {
        SpeechPlayer(
            engine = OfflineSpeechProvider::engine,
            newOutput = OfflineSpeechProvider::newOutput,
            modelDir = { OfflineVoiceModel.modelDir().toString() },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            synthDispatcher = Dispatchers.Default.limitedParallelism(1),
        )
    }
}
