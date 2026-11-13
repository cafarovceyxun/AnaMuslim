package com.cafarovceyxun.anamuslim.utils.speech

/**
 * Cihazda mətndən nitq — platformanın sintez mühərriki (sherpa-onnx + öz Piper modelimiz).
 *
 * Android-də `app/`-dakı `SherpaOfflineSpeechEngine` (AAR yalnız tətbiq modulunda qoşula bilir),
 * iOS-da isə Swift-dəki `SherpaSpeechEngine` (`SherpaOnnxC.xcframework`) — o, [IosSherpaTts] kimi
 * qeydiyyatdan keçir. Paylaşılan kod yalnız bu interfeysi görür.
 *
 * Hər iki metod **bloklayır** (model yükləmə ~1 s, bir cümlənin sintezi telefonda ~0.2–0.5 s):
 * onları yalnız [SpeechPlayer]-in tək axınlı dispetçerindən çağır — mühərrik eyni anda iki
 * çağırışa hazır deyil.
 */
interface OfflineSpeechEngine {
    /**
     * Modeli [modelDir]-dən yükləyir və nümunə tezliyini qaytarır; alınmasa 0. Eyni qovluqla təkrar
     * çağırış ucuzdur — model artıq yaddaşdadırsa yenidən oxunmur.
     */
    fun load(modelDir: String): Int

    /** Bir parçanı sintez edir: mono, `[-1, 1]` aralığında float PCM. Alınmasa null. */
    fun synthesize(text: String, speed: Float): FloatArray?

    /** Modeli yaddaşdan buraxır — fayllar silinəndən əvvəl çağırılır. */
    fun release()
}

/** Sintez olunmuş PCM-i çalan platforma çıxışı (Android `AudioTrack`, iOS `AVAudioEngine`). */
interface PcmSpeechOutput {
    /** Audio sessiyanı/fokusu alır və çalmağa hazırlaşır. */
    fun open(sampleRate: Int)

    /**
     * [samples]-i çalır və onlar **eşidilib bitəndə** qayıdır. Korutin ləğv olunanda dərhal susur.
     * Pauza zamanı gözləyir.
     */
    suspend fun play(samples: FloatArray)

    fun pause()

    fun resume()

    /** Susur, fokusu/sessiyanı buraxır. Təkrar çağırış zərərsizdir. */
    fun close()
}

/**
 * Seam. Hər iki platforma qeydiyyat edir, amma mühərrik hansısa səbəbdən (iOS-da xcframework
 * qoşulmayıb, Android-də native kitabxana yüklənmir) olmaya bilər — ona görə inert default və
 * [isAvailable]: UI «Dinlə» düyməsini yalnız bu `true` olanda göstərir (CLAUDE.md, «İnert default
 * UI-ni azad etmir»).
 */
object OfflineSpeechProvider {
    private var engine: OfflineSpeechEngine? = null
    private var outputFactory: (() -> PcmSpeechOutput)? = null

    fun register(engine: OfflineSpeechEngine, outputFactory: () -> PcmSpeechOutput) {
        this.engine = engine
        this.outputFactory = outputFactory
    }

    val isAvailable: Boolean get() = engine != null && outputFactory != null

    internal fun engine(): OfflineSpeechEngine? = engine

    internal fun newOutput(): PcmSpeechOutput? = outputFactory?.invoke()
}
