import Foundation
import SherpaOnnxC
import shared

/// sherpa-onnx (1.13.8, `SherpaOnnxC.xcframework`) üzərində Kotlin-in `IosSherpaTts` protokolu —
/// hədisləri cihazda öz Piper modelimizlə səsləndirir. Paylaşılan pleyer (`SpeechPlayer`) bunu tək
/// axınlı dispetçerdən çağırır; kilid yenə də qoyulub ki, model silinərkən (`releaseModel`) yarımçıq
/// sintezin altından yaddaş çəkilməsin.
///
/// Parametrlər Android-dəki `SherpaOfflineSpeechEngine` ilə eynidir (Piper modelinin `inference`
/// bölməsi); `silence_scale = 1` vacibdir — sherpa-nın defoltu (0.2) fasilələri sıxır.
final class SherpaSpeechEngine: NSObject, IosSherpaTts {

    private var tts: OpaquePointer?
    private var loadedDir: String?
    private let lock = NSLock()

    func loadModel(modelDir: String) -> Int32 {
        lock.lock()
        defer { lock.unlock() }

        if let tts, loadedDir == modelDir {
            return SherpaOnnxOfflineTtsSampleRate(tts)
        }
        destroy()

        // C sətirləri `SherpaOnnxCreateOfflineTts` qayıdana qədər yaşamalıdır — model yaradılanda
        // kopyalanır, ona görə bloklardan çıxandan sonra lazım deyil.
        let created: OpaquePointer? = "\(modelDir)/model.onnx".withCString { model in
            "\(modelDir)/tokens.txt".withCString { tokens in
                "\(modelDir)/espeak-ng-data".withCString { dataDir in
                    "cpu".withCString { provider in
                        // Sıfırla başlanır (C API nümunəsindəki `memset` kimi): null sətir sahələri
                        // kitabxanada boş sətir sayılır.
                        var config = SherpaOnnxOfflineTtsConfig()
                        config.model.vits.model = model
                        config.model.vits.tokens = tokens
                        config.model.vits.data_dir = dataDir
                        config.model.vits.noise_scale = 0.667
                        config.model.vits.noise_scale_w = 0.8
                        config.model.vits.length_scale = 1.0
                        config.model.num_threads = 2
                        config.model.debug = 0
                        config.model.provider = provider
                        config.max_num_sentences = 1
                        config.silence_scale = 1.0
                        return SherpaOnnxCreateOfflineTts(&config)
                    }
                }
            }
        }

        guard let created else {
            NSLog("SherpaSpeech: model açılmadı (\(modelDir))")
            return 0
        }
        tts = created
        loadedDir = modelDir
        return SherpaOnnxOfflineTtsSampleRate(created)
    }

    func synthesize(text: String, speed: Float) -> Data? {
        lock.lock()
        defer { lock.unlock() }

        guard let tts else { return nil }
        var generation = SherpaOnnxGenerationConfig()
        generation.silence_scale = 1.0
        generation.speed = speed
        generation.sid = 0

        let audio = text.withCString { SherpaOnnxOfflineTtsGenerateWithConfig(tts, $0, &generation, nil, nil) }
        guard let audio else { return nil }
        defer { SherpaOnnxDestroyOfflineTtsGeneratedAudio(audio) }

        let count = Int(audio.pointee.n)
        guard count > 0, let samples = audio.pointee.samples else { return Data() }
        return Data(bytes: samples, count: count * MemoryLayout<Float>.size)
    }

    func releaseModel() {
        lock.lock()
        defer { lock.unlock() }
        destroy()
    }

    private func destroy() {
        if let tts { SherpaOnnxDestroyOfflineTts(tts) }
        tts = nil
        loadedDir = nil
    }
}
