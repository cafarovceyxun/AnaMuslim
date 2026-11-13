package com.cafarovceyxun.anamuslim.utils.speech

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeCompletionDataPlayedBack
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.posix.memcpy
import kotlin.coroutines.resume

/**
 * sherpa-onnx-un iOS tərəfi. Mühərrik **Swift**-dədir (`iosApp/iosApp/SherpaSpeechEngine.swift`):
 * `SherpaOnnxC.xcframework` dinamik framework-dür və yalnız tətbiq hədəfinə qoşulur. Kotlin onu
 * cinterop ilə birbaşa çağırsaydı, `iosSimulatorArm64Test` test binarı da həmin kitabxananı linklə
 * tələb edərdi — protokol bu asılılığı tətbiqin özündə saxlayır.
 *
 * Metod adları qəsdən `load`/`release` deyil: ikisi də ObjC-də `NSObject`-in öz seçiciləridir.
 */
interface IosSherpaTts {
    /** Nümunə tezliyi; alınmasa 0. */
    fun loadModel(modelDir: String): Int

    /** Float32 PCM (mono, `[-1, 1]`) xam baytlar kimi; alınmasa null. */
    fun synthesize(text: String, speed: Float): NSData?

    fun releaseModel()
}

/** Swift `AppDelegate`-dən çağırılır — bax `iosAppApp.swift`. */
object IosOfflineSpeechBridge {
    fun register(tts: IosSherpaTts) {
        OfflineSpeechProvider.register(IosOfflineSpeechEngine(tts)) { AvAudioEngineSpeechOutput() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class IosOfflineSpeechEngine(private val tts: IosSherpaTts) : OfflineSpeechEngine {
    override fun load(modelDir: String): Int = tts.loadModel(modelDir)

    override fun synthesize(text: String, speed: Float): FloatArray? {
        val data = tts.synthesize(text, speed) ?: return null
        val count = (data.length / 4u).toInt()
        val samples = FloatArray(count)
        if (count > 0) {
            samples.usePinned { memcpy(it.addressOf(0), data.bytes, count.toULong() * 4u) }
        }
        return samples
    }

    override fun release() = tts.releaseModel()
}

/**
 * `AVAudioEngine` + `AVAudioPlayerNode` ilə çalır: hər parça bir `AVAudioPCMBuffer`-dir və
 * [play] «eşidilib bitdi» (`DataPlayedBack`) geri çağırışını gözləyir — «planlaşdırıldı» yox, yoxsa
 * panel parçanı səs çatmamış dəyişərdi.
 *
 * Ləğv olunanda korutin dərhal qayıdır, səsi isə [close] kəsir (`SpeechPlayer` onu `finally`-də
 * çağırır) — `node.stop()` planlaşdırılmış buferləri atır.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class AvAudioEngineSpeechOutput : PcmSpeechOutput {
    private var engine: AVAudioEngine? = null
    private var node: AVAudioPlayerNode? = null
    private var format: AVAudioFormat? = null

    override fun open(sampleRate: Int) {
        close()
        runCatching {
            AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
            AVAudioSession.sharedInstance().setActive(true, null)
        }
        val format = AVAudioFormat(standardFormatWithSampleRate = sampleRate.toDouble(), channels = 1u)
        val engine = AVAudioEngine()
        val node = AVAudioPlayerNode()
        engine.attachNode(node)
        engine.connect(node, engine.mainMixerNode, format)
        engine.prepare()
        engine.startAndReturnError(null)
        node.play()
        this.engine = engine
        this.node = node
        this.format = format
    }

    override suspend fun play(samples: FloatArray) {
        val node = node ?: return
        val format = format ?: return
        if (samples.isEmpty()) return
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = samples.size.toUInt())
        buffer.frameLength = samples.size.toUInt()
        val channel = buffer.floatChannelData?.get(0) ?: return
        samples.usePinned { memcpy(channel, it.addressOf(0), samples.size.toULong() * 4u) }

        suspendCancellableCoroutine { continuation ->
            node.scheduleBuffer(
                buffer,
                completionCallbackType = AVAudioPlayerNodeCompletionDataPlayedBack,
            ) { _ ->
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    override fun pause() {
        node?.pause()
    }

    override fun resume() {
        node?.play()
    }

    override fun close() {
        node?.stop()
        engine?.stop()
        node = null
        engine = null
        format = null
    }
}
