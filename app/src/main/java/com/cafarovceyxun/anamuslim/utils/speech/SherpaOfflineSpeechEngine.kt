package com.cafarovceyxun.anamuslim.utils.speech

import android.util.Log
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig

/**
 * sherpa-onnx (1.13.8, AAR) üzərində [OfflineSpeechEngine]. AAR yalnız tətbiq modulunda qoşula bilir
 * (kitabxana modulu yerli `.aar` asılılığı qəbul etmir), ona görə bu sinif `shared/`-də deyil.
 *
 * Parametrlər Piper modelinin öz `inference` bölməsindəndir (`az-v1.onnx.json`): noise 0.667,
 * noise_w 0.8. `silenceScale = 1` vacibdir — sherpa-nın defoltu (0.2) cümlə daxilindəki fasilələri
 * sıxır və nitq Mac-da dinlənilən nümunədən tələsik çıxır (`tools/tts/voice/export_sherpa.py`).
 */
class SherpaOfflineSpeechEngine : OfflineSpeechEngine {

    private var tts: OfflineTts? = null
    private var loadedDir: String? = null

    override fun load(modelDir: String): Int {
        tts?.takeIf { loadedDir == modelDir }?.let { return it.sampleRate() }
        release()
        return try {
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = "$modelDir/model.onnx",
                        tokens = "$modelDir/tokens.txt",
                        dataDir = "$modelDir/espeak-ng-data",
                        noiseScale = 0.667f,
                        noiseScaleW = 0.8f,
                        lengthScale = 1f,
                    ),
                    numThreads = THREADS,
                    debug = false,
                    provider = "cpu",
                ),
                maxNumSentences = 1,
                silenceScale = 1f,
            )
            // assetManager = null → model fayl sistemindən oxunur (`newFromFile`).
            val engine = OfflineTts(assetManager = null, config = config)
            tts = engine
            loadedDir = modelDir
            engine.sampleRate()
        } catch (t: Throwable) {
            Log.e(TAG, "model açılmadı", t)
            release()
            0
        }
    }

    override fun synthesize(text: String, speed: Float): FloatArray? {
        val engine = tts ?: return null
        return try {
            engine.generateWithConfig(
                text,
                GenerationConfig(silenceScale = 1f, speed = speed, sid = 0),
            ).samples
        } catch (t: Throwable) {
            Log.e(TAG, "sintez alınmadı", t)
            null
        }
    }

    override fun release() {
        tts?.release()
        tts = null
        loadedDir = null
    }

    private companion object {
        const val TAG = "SherpaSpeech"

        /**
         * Telefonda 2 axın Piper medium üçün kifayətdir (Mac-da RTF 0.056 idi, orta telefonda ~0.2–0.3
         * gözlənir) və səs çalınarkən UI-yə nüvə saxlayır.
         */
        const val THREADS = 2
    }
}
