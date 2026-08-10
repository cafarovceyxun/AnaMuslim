package com.cafarovceyxun.anamuslim.api.models.mediaplayer

import com.cafarovceyxun.anamuslim.utils.mediaplayer.RecitationModelManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * "No value" marker for a timing position, mirroring media3's `C.TIME_UNSET` so manifests parsed
 * here and playback code on either platform agree on the sentinel.
 */
const val TIME_UNSET: Long = Long.MIN_VALUE + 1

data class VerseSegment(
    val index: Int,
    val startMs: Long,
    val endMs: Long
)

@Serializable
data class VerseTiming(
    @SerialName("verse")
    val verseNo: Int,

    @SerialName("start_ms")
    val startMs: Long,

    @SerialName("end_ms")
    val endMs: Long,

    @SerialName("segments")
    private val segments: List<List<Long>>? = null
) {
    val durationMs: Long
        get() {
            if (!isValidTimingWindow(startMs, endMs)) return 0L

            return endMs - startMs
        }

    val seg: List<VerseSegment> by lazy {
        segments?.map {
            VerseSegment(
                index = it[0].toInt(),
                startMs = it[1],
                endMs = it[2]
            )
        } ?: emptyList()
    }

    fun containsPosition(positionMs: Long): Boolean {
        return positionMs in startMs until endMs
    }

    fun getSegmentAtPosition(positionMs: Long): VerseSegment? {
        return seg.find { positionMs >= it.startMs && positionMs < it.endMs }
    }
}

/**
 * True when [startMs]–[endMs] is a usable, non-empty playback window. Both bounds must be real
 * positions (not [TIME_UNSET] or negative) and the end must come after the start.
 */
fun isValidTimingWindow(startMs: Long, endMs: Long): Boolean {
    return startMs != TIME_UNSET &&
            endMs != TIME_UNSET &&
            startMs >= 0L &&
            endMs >= 0L &&
            endMs > startMs
}

@Serializable
data class ChapterTimingMetadata(
    @SerialName("chapter")
    val chapterNo: Int,

    @SerialName("duration_ms")
    val durationMs: Long,

    @SerialName("verses")
    val verses: List<VerseTiming>? = null
) {
    val hasVerseTiming: Boolean get() = !verses.isNullOrEmpty()

    @Transient
    private val verseByNo: Map<Int, VerseTiming> =
        verses?.associateBy { it.verseNo } ?: emptyMap()

    fun getVerseAtPosition(positionMs: Long): VerseTiming? {
        val list = verses ?: return null
        var lo = 0
        var hi = list.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val v = list[mid]
            when {
                positionMs < v.startMs -> hi = mid - 1
                positionMs >= v.endMs -> lo = mid + 1
                else -> return v
            }
        }
        return null
    }

    fun getVerseTiming(verseNo: Int): VerseTiming? {
        val timing = verseByNo[verseNo]
        if (timing != null && (timing.startMs > 0 || timing.endMs > 0)) {
            return timing
        }

        // Robust Fallback: If timing is missing or zeroed (common for last ayah in some manifests)
        if (verses != null && verses.isNotEmpty()) {
            val lastValid = verses.filter { it.startMs > 0 || it.endMs > 0 }.maxByOrNull { it.verseNo }
            if (lastValid != null) {
                if (verseNo >= lastValid.verseNo) {
                    // This is either the last ayah or past the last valid timing.
                    // Assume it starts where the last valid one ends and goes to the file's end.
                    return VerseTiming(
                        verseNo = verseNo,
                        startMs = lastValid.endMs,
                        endMs = durationMs.coerceAtLeast(lastValid.endMs + 1000L)
                    )
                }
            }
        }

        // Final fallback: If everything is missing but surah is valid, just start at 0
        // (This should only happen for ayah 1 if timing is completely missing)
        return timing ?: if (verseNo == 1) VerseTiming(1, 0L, durationMs) else null
    }

    fun hasCompleteTimingFor(fromVerse: Int, toVerse: Int): Boolean {
        if (verseByNo.isEmpty()) return false
        return (fromVerse..toVerse).all { it in verseByNo }
    }
}

@Serializable
data class AudioTimingMetadata(
    @SerialName("reciter")
    val reciterId: String,
    @SerialName("chapters")
    val chapters: List<ChapterTimingMetadata> = emptyList()
)

/**
 * One resolved audio source for a chapter. [audioUri] is a plain URI string (a `file://` path for
 * downloaded audio, otherwise the streaming `https` URL) so both media3 and AVFoundation can take it
 * directly.
 */
class RecitationAudioTrack(
    val kind: RecitationAudioKind,
    val chapterNo: Int,
    val reciterId: String,
    val audioUri: String,
    val timingMetadata: ChapterTimingMetadata?,
) {
    val hasVerseTiming: Boolean
        get() =
            timingMetadata?.hasVerseTiming ?: false

    suspend fun getReciterName(): String {
        if (kind == RecitationAudioKind.QURAN) {
            return RecitationModelManager.getQuranModel(reciterId)?.getReciterName() ?: ""
        } else {
            return RecitationModelManager.getTranslationModel(reciterId)?.getReciterName() ?: ""
        }
    }
}


sealed class ResolvedAudioResult {
    data class Resoved(
        val chapter: Int,
        val quran: RecitationAudioTrack?,
        val translation: RecitationAudioTrack?,
    ) : ResolvedAudioResult()

    /** [progress] 0–100 from the download worker; negative values are internal signals (e.g. clear UI). */
    data class Downloading(val progress: Int) : ResolvedAudioResult()

    data class Error(val error: Throwable) : ResolvedAudioResult()
}
