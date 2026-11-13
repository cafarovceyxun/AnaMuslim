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
import okio.use

/**
 * Azərbaycanca səs modeli (öz Piper modelimiz, `az-v1`) — telefona bir dəfə endirilir.
 *
 * Model tətbiqə **qablaşdırılmır**: 64 MB-lıq faylı hədis dinləməyən hər kəsə yükləmək olmaz. Fayllar
 * öz serverimizin Supabase Storage-ındadır (public `tts` bucket-i, `az-v1/`) — kənar CDN Azərbaycanda
 * boğulur, Oracle-dakı storage isə Supabase-in egress kvotasına düşmür. Dəsti
 * `tools/tts/voice/export_sherpa.py` qurur və yükləyir, ölçüləri də o çap edir — modeli yeniləyəndə
 * [FILES]-ı oradan köçür və [ID]-ni dəyiş (köhnə qovluq [refresh]-də təmizlənir).
 *
 * ⚠️ Serverin ümumi fayl limiti 50 MB-dır, ona görə `model.onnx` 32 MiB-lıq **hissələrlə** gəlir və
 * telefonda birləşdirilir ([Part]).
 *
 * Quraşdırılma faktı ayrıca bayraqda **saxlanmır**: hər faylın gözlənilən ölçüdə olması özü sübutdur.
 * Səbəb iOS-dur — orada endirmə fon `NSURLSession`-ında davam edir və proses ölsə də fayl sonra
 * yerinə düşür; bayraq isə onu yazacaq korutin artıq olmadığı üçün heç vaxt yazılmazdı.
 */
object OfflineVoiceModel {

    const val ID = "az-v1"

    private const val STORAGE_BASE =
        "https://anamuslim.cafarovceyxun.com/storage/v1/object/public/tts/$ID/"

    /** Storage-dakı bir fayl (və ya böyük faylın hissəsi). */
    internal data class Part(val asset: String, val size: Long)

    /** Model qovluğundakı yol (sherpa-onnx bu düzülüşü gözləyir) ← ardıcıl birləşdirilən hissələr. */
    internal data class ModelFile(val path: String, val parts: List<Part>) {
        val size: Long get() = parts.sumOf { it.size }
    }

    private fun single(asset: String, path: String, size: Long) = ModelFile(path, listOf(Part(asset, size)))

    internal val FILES = listOf(
        ModelFile(
            "model.onnx",
            listOf(Part("az-v1.onnx.000", 33_554_432), Part("az-v1.onnx.001", 29_961_751)),
        ),
        single("tokens.txt", "tokens.txt", 968),
        single("espeak-phondata", "espeak-ng-data/phondata", 550_424),
        single("espeak-phonindex", "espeak-ng-data/phonindex", 39_074),
        single("espeak-phontab", "espeak-ng-data/phontab", 55_796),
        single("espeak-intonations", "espeak-ng-data/intonations", 2_040),
        single("espeak-az_dict", "espeak-ng-data/az_dict", 43_773),
        single("espeak-lang-trk-az", "espeak-ng-data/lang/trk/az", 45),
        // espeak tanımadığı sözdə ingiliscəyə keçir və en_dict yoxdursa prosesi bağlayır.
        single("espeak-en_dict", "espeak-ng-data/en_dict", 166_944),
        single("espeak-lang-gmw-en", "espeak-ng-data/lang/gmw/en", 140),
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

    private fun partsDir(): Path = modelDir() / ".parts"

    /**
     * Hissələri [target]-ə birləşdirir: əvvəl müvəqqəti fayla yazır, sonra atomik köçürür — yarımçıq
     * birləşmə heç vaxt `model.onnx` adı ilə qalmır. Tək hissəli fayl sadəcə köçürülür.
     */
    private fun assemble(parts: List<Path>, target: Path) {
        if (parts.size == 1) {
            AppFileSystem.delete(target)
            check(AppFileSystem.atomicMove(parts.single(), target)) { "${target.name}: köçürülmədi" }
            return
        }
        val temp = target.parent!! / "${target.name}.part"
        AppFileSystem.write(temp) { sink ->
            for (part in parts) AppFileSystem.fileSystem.source(part).use { sink.writeAll(it) }
        }
        check(AppFileSystem.atomicMove(temp, target)) { "${target.name}: köçürülmədi" }
        parts.forEach { AppFileSystem.delete(it) }
    }

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
                        // Hissələr ayrıca qovluğa enir: iOS-da proses ölsə də fon endirməsi onları
                        // yerinə qoyur, növbəti cəhd isə hazır hissəni yenidən endirmir.
                        val partPaths = file.parts.map { part ->
                            val partPath = partsDir() / part.asset
                            if (AppFileSystem.size(partPath) != part.size) {
                                AppFileSystem.delete(partPath)
                                val base = done
                                BackgroundFileTransfer.download(STORAGE_BASE + part.asset, partPath, label) { consumed, _ ->
                                    _state.value = State.Downloading(((base + consumed).toFloat() / totalBytes).coerceIn(0f, 1f))
                                }
                                if (AppFileSystem.size(partPath) != part.size) {
                                    AppFileSystem.delete(partPath)
                                    error("${part.asset}: gözlənilməz ölçü")
                                }
                            }
                            done += part.size
                            _state.value = State.Downloading(done.toFloat() / totalBytes)
                            partPath
                        }
                        assemble(partPaths, target)
                        if (AppFileSystem.size(target) != file.size) {
                            AppFileSystem.delete(target)
                            error("${file.path}: birləşdirmədən sonra gözlənilməz ölçü")
                        }
                    } else {
                        done += file.size
                    }
                }
                AppFileSystem.deleteRecursively(partsDir())
                runCatching { excludeFromBackup(modelDir().toString()) }
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
}
