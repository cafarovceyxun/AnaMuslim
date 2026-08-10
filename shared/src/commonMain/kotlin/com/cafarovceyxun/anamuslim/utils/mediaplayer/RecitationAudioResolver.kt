package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ChapterTimingMetadata
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioTrack
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.ResolvedAudioResult
import com.cafarovceyxun.anamuslim.api.models.recitation2.RecitationModelBase
import com.cafarovceyxun.anamuslim.api.streamInventory
import com.cafarovceyxun.anamuslim.compose.components.player.dialogs.AudioOption
import com.cafarovceyxun.anamuslim.resources.Res
import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.exceptions.HttpNotFoundException
import com.cafarovceyxun.anamuslim.utils.exceptions.NoInternetException
import com.cafarovceyxun.anamuslim.utils.network.isNetworkConnected
import com.cafarovceyxun.anamuslim.utils.reader.recitation.RecitationUtils.URL_CHAPTER_PATTERN
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Buffer
import okio.GzipSource
import okio.Path
import okio.buffer

/**
 * Turns "play chapter N with these settings" into concrete audio URIs plus verse timing, for both
 * platforms.
 *
 * Audio URIs
 * - If the chapter file under the reciter directory exists and is non-empty, [RecitationAudioTrack.audioUri]
 *   is a `file://` URI (explicit / bulk download). This always wins.
 * - Otherwise, when the device is online, `audioUri` is the prepared HTTPS URL for that chapter.
 *   The platform player buffers it; bytes are not written to the reciter folder by this call.
 * - Offline with an empty placeholder file fails with [NoInternetException].
 *
 * Timing: cache if valid; otherwise fetch when online ([ChapterTimingMetadata] may be null).
 * Missing audio for a required track fails; missing timing does not fail the flow.
 */
object RecitationAudioResolver {

    /** Bundled reciters point their `timingUrl` here instead of at a mirror. */
    private const val ASSET_SCHEME = "asset://"

    /** Compose Resources directory holding the timing files shipped with the app. */
    private const val BUNDLED_TIMING_DIR = "files/"

    internal sealed class TimingParseResult {
        data class Found(val metadata: ChapterTimingMetadata) : TimingParseResult()
        data object ChapterMissing : TimingParseResult()
        data object ParseFailed : TimingParseResult()
    }

    fun prepareAudioUrl(urlTemplate: String, chapterNo: Int): String? {
        return try {
            URL_CHAPTER_PATTERN.replace(urlTemplate) { match ->
                StringUtils.formatInvariant(match.groupValues[1], chapterNo)
            }
        } catch (e: Exception) {
            AppLogger.saveError(e, "RecitationAudioResolver.prepareAudioUrl")
            null
        }
    }

    fun resolveAudioUris(
        chapterNo: Int,
        settings: PlayerSettings,
    ): Flow<ResolvedAudioResult> = flow {
        val (quranModel, translationModel) = RecitationModelManager.resolveModels(settings)

        val audioOption = settings.audioOption

        val shouldPlayArabic = audioOption != AudioOption.ONLY_TRANSLATION
        val shouldPlayTranslation = audioOption != AudioOption.ONLY_QURAN

        val failed = when {
            shouldPlayArabic && quranModel == null -> true
            shouldPlayTranslation && translationModel == null -> true
            else -> false
        }

        if (failed) {
            emit(ResolvedAudioResult.Error(IllegalStateException("Failed to obtain recitation models")))
            return@flow
        }

        val quranPath = quranModel?.let {
            RecitationModelManager.getRecitationAudioPath(it.id, chapterNo)
        }
        val translationPath = translationModel?.let {
            RecitationModelManager.getRecitationAudioPath(it.id, chapterNo)
        }

        val quranNeedsStream = shouldPlayArabic && quranPath != null && quranPath.isEmptyFile()
        val translationNeedsStream =
            shouldPlayTranslation && translationPath != null && translationPath.isEmptyFile()

        try {
            coroutineScope {
                val quranTimingDeferred = async {
                    quranModel?.let { resolveChapterTimingMetadata(it, chapterNo) }
                }

                val translationTimingDeferred = async {
                    translationModel?.let { resolveChapterTimingMetadata(it, chapterNo) }
                }

                if (quranNeedsStream || translationNeedsStream) {
                    if (!isNetworkConnected()) {
                        throw NoInternetException()
                    }
                }

                val quranTiming = quranTimingDeferred.await()
                val translationTiming = translationTimingDeferred.await()

                AppLogger.d(
                    "ChapterTiming",
                    "Resolved timings for chapter $chapterNo - quran: $quranTiming, translation: $translationTiming"
                )

                fun resolveTrackUri(
                    needsStream: Boolean,
                    path: Path?,
                    urlTemplate: String,
                ): String {
                    if (path == null) {
                        throw IllegalStateException("Missing audio file path")
                    }

                    if (!needsStream) {
                        return "file://$path"
                    }

                    return prepareAudioUrl(urlTemplate, chapterNo)
                        ?: throw IllegalStateException("Failed to prepare audio URL")
                }

                emit(
                    ResolvedAudioResult.Resoved(
                        chapter = chapterNo,
                        quran = if (shouldPlayArabic && quranModel != null && quranPath != null) {
                            RecitationAudioTrack(
                                kind = RecitationAudioKind.QURAN,
                                chapterNo = chapterNo,
                                reciterId = quranModel.id,
                                audioUri = resolveTrackUri(
                                    needsStream = quranNeedsStream,
                                    path = quranPath,
                                    urlTemplate = quranModel.urlTemplate,
                                ),
                                timingMetadata = quranTiming,
                            )
                        } else {
                            null
                        },
                        translation = if (shouldPlayTranslation && translationModel != null && translationPath != null) {
                            RecitationAudioTrack(
                                kind = RecitationAudioKind.TRANSLATION,
                                chapterNo = chapterNo,
                                reciterId = translationModel.id,
                                audioUri = resolveTrackUri(
                                    needsStream = translationNeedsStream,
                                    path = translationPath,
                                    urlTemplate = translationModel.urlTemplate,
                                ),
                                timingMetadata = translationTiming,
                            )
                        } else {
                            null
                        },
                    ),
                )
            }
        } catch (e: Exception) {
            AppLogger.saveError(e, "RecitationAudioResolver.resolveAudioUris")
            emit(ResolvedAudioResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun resolveChapterTimingMetadata(
        model: RecitationModelBase,
        chapterNo: Int,
    ): ChapterTimingMetadata? = withContext(Dispatchers.IO) {
        val timingUrl = model.timingUrl?.trim().orEmpty()

        if (timingUrl.isEmpty()) return@withContext null

        val cacheFile = RecitationModelManager.getRecitationTimingPath(model.id)
        val upstreamVersion = model.timingVersion ?: 0

        if (!cacheFile.isEmptyFile()) {
            try {
                when (val parsed = parseTimingJson(AppFileSystem.readText(cacheFile), chapterNo, upstreamVersion)) {
                    is TimingParseResult.Found -> return@withContext parsed.metadata
                    TimingParseResult.ChapterMissing -> return@withContext null
                    TimingParseResult.ParseFailed -> AppFileSystem.delete(cacheFile)
                }
            } catch (e: Exception) {
                AppLogger.saveError(e, "RecitationAudioResolver.resolveChapterTimingMetadata - read cache")
                AppFileSystem.delete(cacheFile)
            }
        }

        val bundled = timingUrl.startsWith(ASSET_SCHEME)

        if (!bundled && !isNetworkConnected()) {
            return@withContext null
        }

        try {
            val raw = downloadTimingMetadata(timingUrl) ?: return@withContext null
            val content = decodeUtf8MaybeGzipped(raw)

            when (val parsed = parseTimingJson(content, chapterNo, upstreamVersion)) {
                is TimingParseResult.Found -> {
                    AppFileSystem.writeText(cacheFile, content)
                    parsed.metadata
                }

                TimingParseResult.ChapterMissing -> {
                    AppLogger.saveError(
                        Exception("Timing JSON had no entry for chapter $chapterNo"),
                        "RecitationAudioResolver.resolveChapterTimingMetadata",
                    )
                    null
                }

                TimingParseResult.ParseFailed -> null
            }
        } catch (e: Exception) {
            AppLogger.saveError(e, "RecitationAudioResolver.resolveChapterTimingMetadata - download")
            null
        }
    }

    /**
     * Fetches the timing file bytes: from the app's own resources for bundled reciters
     * (`asset://…`), otherwise streamed from the active mirror.
     */
    private suspend fun downloadTimingMetadata(timingUrl: String): ByteArray? {
        if (timingUrl.startsWith(ASSET_SCHEME)) {
            val assetPath = timingUrl.removePrefix(ASSET_SCHEME).trimStart('/')
            return Res.readBytes(BUNDLED_TIMING_DIR + assetPath)
        }

        return streamInventory(timingUrl) { scope ->
            if (!scope.isSuccessful) {
                if (scope.statusCode == 404) throw HttpNotFoundException()
                throw okio.IOException("Timing metadata download failed: HTTP ${scope.statusCode}")
            }

            scope.channel.readRemaining().readByteArray()
        }.takeIf { it.isNotEmpty() }
    }

    /** Timing files are served both plain and gzipped; the magic number decides which. */
    internal fun decodeUtf8MaybeGzipped(bytes: ByteArray): String {
        val isGzip = bytes.size >= 2 &&
                bytes[0] == 0x1f.toByte() &&
                bytes[1] == 0x8b.toByte()

        if (!isGzip) return bytes.decodeToString()

        val source = GzipSource(Buffer().write(bytes)).buffer()

        return try {
            source.readUtf8()
        } finally {
            source.close()
        }
    }

    /**
     * Parses only the requested chapter from the timing JSON, avoiding full
     * deserialization of all 114 chapters and their verse/segment data.
     */
    internal fun parseTimingJson(
        content: String,
        chapterNo: Int,
        upstreamVersion: Int,
    ): TimingParseResult {
        try {
            val root = JsonHelper.json.parseToJsonElement(content).jsonObject

            val version = root["version"]?.jsonPrimitive?.intOrNull ?: 0

            if (version < upstreamVersion) {
                return TimingParseResult.ParseFailed
            }

            val chaptersArray = root["chapters"]?.jsonArray ?: return TimingParseResult.ParseFailed

            for (element in chaptersArray) {
                val chapterObj = element.jsonObject
                val chapter = chapterObj["chapter"]?.jsonPrimitive?.intOrNull ?: continue

                if (chapter == chapterNo) {
                    val metadata = JsonHelper.json.decodeFromJsonElement<ChapterTimingMetadata>(chapterObj)
                    return TimingParseResult.Found(metadata)
                }
            }

            return TimingParseResult.ChapterMissing
        } catch (e: Exception) {
            AppLogger.saveError(e, "RecitationAudioResolver.parseTimingJson")
            return TimingParseResult.ParseFailed
        }
    }

    /** True when the file is absent or a zero-byte placeholder — i.e. nothing usable on disk. */
    private fun Path.isEmptyFile(): Boolean = (AppFileSystem.size(this) ?: 0L) == 0L
}
