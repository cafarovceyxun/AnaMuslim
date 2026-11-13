package com.cafarovceyxun.anamuslim.utils.speech

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpeechPlayerTest {

    private class FakeEngine(val sampleRate: Int = 1000) : OfflineSpeechEngine {
        val spoken = mutableListOf<String>()
        var loads = 0
        var released = false
        override fun load(modelDir: String): Int { loads++; return sampleRate }
        override fun synthesize(text: String, speed: Float): FloatArray {
            spoken += text
            return FloatArray(10) { 0.5f }
        }
        override fun release() { released = true }
    }

    private class FakeOutput : PcmSpeechOutput {
        val played = mutableListOf<Int>()
        var opened = 0
        var closed = 0
        var paused = false
        /** Doldurulanda `play` onun tamamlanmasını gözləyir — «hələ çalınır» vəziyyəti üçün. */
        var gate: CompletableDeferred<Unit>? = null
        override fun open(sampleRate: Int) { opened++ }
        override suspend fun play(samples: FloatArray) {
            played += samples.size
            gate?.await()
        }
        override fun pause() { paused = true }
        override fun resume() { paused = false }
        override fun close() { closed++ }
    }

    /**
     * Pleyerin scope-u testin `backgroundScope`-u **deyil**: `advanceUntilIdle` fon işlərini gözləmir,
     * yəni növbə ümumiyyətlə başlamazdı. Ayrıca scope test bitəndə ləğv olunur.
     */
    private val scopes = mutableListOf<CoroutineScope>()

    private fun TestScope.player(engine: OfflineSpeechEngine?, output: FakeOutput): SpeechPlayer {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob())
        scopes += scope
        return SpeechPlayer(
            engine = { engine },
            newOutput = { output },
            modelDir = { "/model" },
            scope = scope,
            synthDispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    @AfterTest
    fun cancelScopes() {
        scopes.forEach { it.cancel() }
        scopes.clear()
    }

    private val items = listOf(
        SpeechItem(1, 1, "1. Birinci hədisin mətni buradadır. İkinci cümləsi də buradadır, bir az uzundur."),
        SpeechItem(2, 2, "2. İkinci hədis qısa deyil, amma bir cümlədir."),
    )

    @Test
    fun playsWholeQueueThenGoesIdle() = runTest {
        val engine = FakeEngine()
        val output = FakeOutput()
        val player = player(engine, output)

        player.play(items)
        advanceUntilIdle()

        assertEquals(
            listOf("Birinci hədisin mətni buradadır.", "İkinci cümləsi də buradadır, bir az uzundur.", "İkinci hədis qısa deyil, amma bir cümlədir."),
            engine.spoken,
        )
        assertEquals(1, engine.loads)
        assertEquals(1, output.opened)
        assertEquals(1, output.closed)
        // Hər parçanın sonuna cümlə fasiləsi qoşulur: 10 nümunə + 320 ms × 1000 Hz.
        assertTrue(output.played.all { it == 10 + 320 })
        assertEquals(SpeechState.Idle, player.state.value)
    }

    @Test
    fun reportsCurrentItemWhileSpeaking() = runTest {
        val output = FakeOutput().apply { gate = CompletableDeferred() }
        val player = player(FakeEngine(), output)

        player.play(items, startIndex = 1)
        advanceUntilIdle()

        val state = assertIs<SpeechState.Active>(player.state.value)
        assertEquals(2L, state.item.id)
        assertEquals(1, state.queueIndex)
        assertEquals(false, state.preparing)
        assertEquals(false, state.hasNext)
    }

    @Test
    fun stopClosesOutputAndOldQueueCannotOverwriteNewOne() = runTest {
        val output = FakeOutput().apply { gate = CompletableDeferred() }
        val player = player(FakeEngine(), output)

        player.play(items)
        advanceUntilIdle()
        player.play(items, startIndex = 1)
        advanceUntilIdle()

        assertEquals(2L, assertIs<SpeechState.Active>(player.state.value).item.id)
        assertEquals(1, output.closed)

        player.stop()
        advanceUntilIdle()
        assertEquals(SpeechState.Idle, player.state.value)
        assertEquals(2, output.closed)
    }

    @Test
    fun pauseIsForwardedAndReflectedInState() = runTest {
        val output = FakeOutput().apply { gate = CompletableDeferred() }
        val player = player(FakeEngine(), output)

        player.play(items)
        advanceUntilIdle()
        player.pause()
        assertTrue(output.paused)
        assertTrue(assertIs<SpeechState.Active>(player.state.value).paused)

        player.resume()
        assertEquals(false, output.paused)
    }

    @Test
    fun missingEngineFailsInsteadOfHanging() = runTest {
        val player = player(engine = null, output = FakeOutput())
        player.play(items)
        advanceUntilIdle()
        assertEquals(SpeechState.Failed(SpeechFailure.EngineUnavailable), player.state.value)
    }

    @Test
    fun failedModelLoadIsReported() = runTest {
        val player = player(FakeEngine(sampleRate = 0), FakeOutput())
        player.play(items)
        advanceUntilIdle()
        assertEquals(SpeechState.Failed(SpeechFailure.ModelLoadFailed), player.state.value)
    }

    @Test
    fun outputErrorFailsInsteadOfCrashingTheApp() = runTest {
        // Telefonda AudioTrack «Invalid audio buffer size» atdı və proses-ömürlü scope bütün tətbiqi yıxdı.
        val output = object : PcmSpeechOutput by FakeOutput() {
            override fun open(sampleRate: Int) = throw UnsupportedOperationException("Invalid audio buffer size.")
        }
        val player = SpeechPlayer(
            engine = { FakeEngine() },
            newOutput = { output },
            modelDir = { "/model" },
            scope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()).also { scopes += it },
            synthDispatcher = StandardTestDispatcher(testScheduler),
        )
        player.play(items)
        advanceUntilIdle()
        assertEquals(SpeechState.Failed(SpeechFailure.PlaybackFailed), player.state.value)
    }

    @Test
    fun shutdownReleasesEngine() = runTest {
        val engine = FakeEngine()
        val player = player(engine, FakeOutput().apply { gate = CompletableDeferred() })
        player.play(items)
        advanceUntilIdle()
        player.shutdownEngine()
        assertTrue(engine.released)
        assertEquals(SpeechState.Idle, player.state.value)
    }
}
