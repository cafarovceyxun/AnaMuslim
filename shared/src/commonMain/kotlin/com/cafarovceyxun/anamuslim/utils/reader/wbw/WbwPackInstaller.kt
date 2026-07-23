package com.cafarovceyxun.anamuslim.utils.reader.wbw

import com.cafarovceyxun.anamuslim.api.JsonHelper
import com.cafarovceyxun.anamuslim.api.resolveInventoryUrl
import com.cafarovceyxun.anamuslim.api.models.wbw.WbwLanguageInfo
import com.cafarovceyxun.anamuslim.api.models.wbw.WbwPayloadModel
import com.cafarovceyxun.anamuslim.compose.utils.preferences.ReaderPreferences
import com.cafarovceyxun.anamuslim.db.entities.wbw.WbwWordEntity
import com.cafarovceyxun.anamuslim.repository.RepositoryProvider
import com.cafarovceyxun.anamuslim.utils.univ.BackgroundFileTransfer
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.GzipSource
import okio.Path
import okio.buffer
import okio.use

/**
 * Downloads a word-by-word language pack and installs it into the external Quran database.
 *
 * The *work* — fetch, gunzip, parse, replace rows, bump the content epoch — is platform-neutral, so
 * both platforms run this. Android wraps it in `WbwDownloadWorker` (WorkManager: background
 * survival + notification); iOS drives it from [SharedWbwResourceSource]. Same split as the
 * translation, hadith and recitation waves.
 */
object WbwPackInstaller {

    /**
     * Installs [info]'s pack and returns the payload version, for the caller to record.
     * [onProgress] receives 0..100, or null while the total size is unknown.
     */
    suspend fun install(
        info: WbwLanguageInfo,
        onProgress: suspend (Int?) -> Unit = {},
    ): Int = withContext(Dispatchers.IO) {
        val tempFile = WbwManifest.tempDownloadPath(info.id)

        try {
            // Pack URLs may carry the `ghraw://` scheme, which resolves against the mirror the user
            // picked — the same resolution Android's worker got from `streamInventory`.
            BackgroundFileTransfer.download(resolveInventoryUrl(info.url), tempFile) { consumed, total ->
                onProgress(if (total > 0L) ((consumed * 100L) / total).toInt() else null)
            }

            val payload = decodePayload(tempFile)

            RepositoryProvider.externalQuranDatabase.wbwDao()
                .replaceByWbwId(info.id, toEntities(payload, info.id))
            // Reader caches word data per epoch; without this bump the new pack stays invisible.
            ReaderPreferences.bumpWbwContentEpoch()

            payload.version
        } finally {
            AppFileSystem.delete(tempFile)
        }
    }

    /** Packs are served both gzipped and plain, so the magic number decides — as with timing files. */
    private fun decodePayload(source: Path): WbwPayloadModel {
        val bytes = AppFileSystem.readBytes(source)
        val isGzip = bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

        val text = if (isGzip) {
            GzipSource(Buffer().write(bytes)).buffer().use { it.readUtf8() }
        } else {
            bytes.decodeToString()
        }

        return JsonHelper.json.decodeFromString(text)
    }

    private fun toEntities(payload: WbwPayloadModel, wbwId: String): List<WbwWordEntity> {
        if (payload.verses.isEmpty()) return emptyList()

        val out = ArrayList<WbwWordEntity>()
        for ((ayahId, words) in payload.verses) {
            if (ayahId <= 0) continue

            for ((wordIndex, pair) in words) {
                out.add(
                    WbwWordEntity(
                        ayahId = ayahId,
                        wordIndex = wordIndex,
                        wbwId = wbwId,
                        translation = pair.getOrNull(0)?.takeIf { it.isNotBlank() },
                        transliteration = pair.getOrNull(1)?.takeIf { it.isNotBlank() },
                    )
                )
            }
        }

        return out
    }
}
