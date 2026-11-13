package com.cafarovceyxun.anamuslim.utils.speech

import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.BackgroundFileTransfer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.Path

/**
 * Azərbaycanca səs modeli (öz Piper modelimiz, `az-v1`) — telefona bir dəfə endirilir.
 *
 * Model tətbiqə **qablaşdırılmır**: 64 MB-lıq faylı hədis dinləməyən hər kəsə yükləmək olmaz. Fayllar
 * Quran tərcüməsinin səsləndirməsi kimi GitHub Releases-dədir (`tts-az-piper-v1`), dəsti
 * `tools/tts/voice/export_sherpa.py` qurur və ölçüləri də o çap edir — modeli yeniləyəndə [FILES]-ı
 * oradan köçür və [ID]-ni dəyiş (köhnə qovluq [delete]-də yox, [refresh]-də təmizlənir).
 *
 * Quraşdırılma faktı ayrıca bayraqda **saxlanmır**: hər faylın gözlənilən ölçüdə olması özü sübutdur.
 * Səbəb iOS-dur — orada endirmə fon `NSURLSession`-ında davam edir və proses ölsə də fayl sonra
 * yerinə düşür; bayraq isə onu yazacaq korutin artıq olmadığı üçün heç vaxt yazılmazdı.
 */
object OfflineVoiceModel {

    const val ID = "az-v1"

    private const val RELEASE_BASE =
        "https://github.com/cafarovceyxun/AnaMuslim/releases/download/tts-az-piper-v1/"

    /** Release-dəki fayl adı → model qovluğundakı yol (sherpa-onnx bu düzülüşü gözləyir). */
    internal data class ModelFile(val asset: String, val path: String, val size: Long)

    internal val FILES = listOf(
        ModelFile("az-v1.onnx", "model.onnx", 63_516_183),
        ModelFile("tokens.txt", "tokens.txt", 968),
        ModelFile("espeak-phondata", "espeak-ng-data/phondata", 550_424),
        ModelFile("espeak-phonindex", "espeak-ng-data/phonindex", 39_074),
        ModelFile("espeak-phontab", "espeak-ng-data/phontab", 55_796),
        ModelFile("espeak-intonations", "espeak-ng-data/intonations", 2_040),
        ModelFile("espeak-az_dict", "espeak-ng-data/az_dict", 43_773),
        ModelFile("espeak-lang-trk-az", "espeak-ng-data/lang/trk/az", 45),
    )

    val totalBytes: Long = FILES.sumOf { it.size }

    sealed interface State {
        data object NotInstalled : State
        data class Downloading(val progress: Float) : State
        data object Installed : State
        data object Failed : State
    }

    private val _state = MutableStateFlow<State>(State.NotInstalled)
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null

    private fun rootDir(): Path = AppFileSystem.appFilesDir() / "tts"

    fun modelDir(): Path = rootDir() / ID

    fun isInstalled(): Boolean = FILES.all { AppFileSystem.size(modelDir() / it.path) == it.size }

    /** Diskə baxıb vəziyyəti yeniləyir. Davam edən endirməni pozmur. */
    fun refresh() {
        if (downloadJob?.isActive == true) return
        _state.value = if (isInstalled()) State.Installed else State.NotInstalled
        // Köhnə model versiyalarının qovluqları — yeni ID gələndə yer tutmasınlar.
        AppFileSystem.listDirectories(rootDir())
            .filter { it.name != ID }
            .forEach { AppFileSystem.deleteRecursively(it) }
    }

    /**
     * Endirməni başladır (artıq gedirsə heç nə etmir) və proses-ömürlü scope-da işlədir: vərəq
     * bağlansa da endirmə davam edir. [label] iOS-da tətbiq ölüb endirmə sonra bitəndə göstərilən
     * bildirişin mətnidir.
     */
    fun startDownload(label: String) {
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            _state.value = State.Downloading(0f)
            try {
                var done = 0L
                for (file in FILES) {
                    val target = modelDir() / file.path
                    if (AppFileSystem.size(target) != file.size) {
                        AppFileSystem.delete(target)
                        val base = done
                        BackgroundFileTransfer.download(RELEASE_BASE + file.asset, target, label) { consumed, _ ->
                            _state.value = State.Downloading(((base + consumed).toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                        if (AppFileSystem.size(target) != file.size) {
                            AppFileSystem.delete(target)
                            error("${file.asset}: gözlənilməz ölçü")
                        }
                    }
                    done += file.size
                    _state.value = State.Downloading(done.toFloat() / totalBytes)
                }
                _state.value = State.Installed
            } catch (e: CancellationException) {
                _state.value = if (isInstalled()) State.Installed else State.NotInstalled
                throw e
            } catch (e: Throwable) {
                _state.value = State.Failed
            }
        }
    }

    /**
     * Modeli silir. Əvvəl pleyer dayanır və mühərrik modeli yaddaşdan buraxır — bu, sintez axınında
     * gedir ki, yarımçıq cümlənin altından fayl çəkilməsin.
     */
    fun delete() {
        downloadJob?.cancel()
        _state.value = State.NotInstalled
        scope.launch {
            HadithSpeech.player.shutdownEngine()
            AppFileSystem.deleteRecursively(modelDir())
            _state.value = State.NotInstalled
        }
    }

    /** Diskdə tutduğu yer (endirmə yarımçıqdırsa yarımçıq hissə daxil). */
    fun bytesOnDisk(): Long = FILES.sumOf { AppFileSystem.size(modelDir() / it.path) ?: 0L }
}
