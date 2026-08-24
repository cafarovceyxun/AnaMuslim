package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioKind
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.RecitationAudioTrack
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.TIME_UNSET
import com.cafarovceyxun.anamuslim.api.models.mediaplayer.isValidTimingWindow

/**
 * One verse of one track, clipped out of that track's full-chapter file.
 *
 * [openEnded] marks the very last clip of a sequence: it plays to the end of its file instead of
 * stopping at [endMs], so a chapter finishes cleanly even when the timing manifest ends a little
 * early. Android maps that to media3's `C.TIME_END_OF_SOURCE`, iOS to a far-future end time.
 *
 * ⚠️ [endMs] stays the **measured** boundary even then. Playback and measurement are deliberately
 * separate: when the open-ended clip also had an unknown length, the progress bar's total came out
 * short and the position ran past it, so the timer never stopped (seen on device 2026-08-24).
 */
data class AudioClip(
    val chapterNo: Int,
    val verseNo: Int,
    val kind: RecitationAudioKind,
    val uri: String,
    val startMs: Long,
    val endMs: Long,
    val openEnded: Boolean = false,
) {
    /** Clip length, or 0 when the manifest gave no usable end for it. */
    val durationMs: Long
        get() = if (endMs == TIME_UNSET) 0L else (endMs - startMs).coerceAtLeast(0L)
}

/**
 * Turns "play this chapter with Quran and/or translation audio" into the flat clip sequence both
 * platforms play: verses are emitted in groups, and inside a group every track is played in turn
 * (all Arabic verses of the group, then all translation verses of the same group).
 *
 * A group size of 1 gives the familiar verse → translation → next verse alternation; larger groups
 * let the listener hear several verses in Arabic before their translation.
 *
 * Shared on purpose: Android maps the result to clipped media3 `MediaItem`s and iOS to
 * `AVPlayerItem`s, but the *order and the windows* — the part with actual policy in it — are
 * decided once, here, and covered by `VerseClipPlannerTest`.
 */
object VerseClipPlanner {

    /**
     * Tracks that can be clipped: a track without verse timing cannot be cut into verses, so the
     * caller falls back to unclipped single-file playback (and tells the user why).
     */
    fun clippableTracks(
        quran: RecitationAudioTrack?,
        translation: RecitationAudioTrack?,
    ): List<RecitationAudioTrack> = listOfNotNull(quran, translation)
        .filter { it.hasVerseTiming }
        .sortedBy { if (it.kind == RecitationAudioKind.QURAN) 0 else 1 }

    fun build(
        chapterNo: Int,
        verseCount: Int,
        tracks: List<RecitationAudioTrack>,
        groupSize: Int,
    ): List<AudioClip> {
        if (tracks.isEmpty() || verseCount <= 0) return emptyList()

        val step = groupSize.coerceAtLeast(1)
        val clips = ArrayList<AudioClip>(verseCount * tracks.size)

        var groupStart = 1
        while (groupStart <= verseCount) {
            val groupEnd = minOf(groupStart + step - 1, verseCount)

            for (track in tracks) {
                for (verseNo in groupStart..groupEnd) {
                    val timing = track.timingMetadata?.getVerseTiming(verseNo) ?: continue

                    val isLastVerse = verseNo == verseCount
                    val usable = isValidTimingWindow(timing.startMs, timing.endMs)

                    // Ölçüsüz pəncərəni atırıq — amma son ayəni yox: onun sonu onsuz da
                    // faylın sonu ilə əvəzlənir, başlanğıcı düzgün olsa kifayətdir.
                    if (!usable && !(isLastVerse && timing.startMs >= 0L)) continue

                    clips.add(
                        AudioClip(
                            chapterNo = chapterNo,
                            verseNo = verseNo,
                            kind = track.kind,
                            uri = track.audioUri,
                            startMs = timing.startMs,
                            endMs = if (usable) timing.endMs else TIME_UNSET,
                        )
                    )
                }
            }

            groupStart += step
        }

        // Yalnız **ardıcıllığın sonuncu** klipi faylın sonunadək oxunur: published vaxt
        // cədvəlləri son ayəni bir neçə yüz ms erkən bitirir və ora klipləsək son ayə kəsilir.
        // ⚠️ Bunu hər trekin son ayəsinə tətbiq etmək olmaz — «ərəbcə + tərcümə» rejimində
        // ərəbcə fayl sonunadək oxunur və ardınca gələn tərcümə klipi ya gec başlayır,
        // ya da ümumiyyətlə oxunmur (2026-08-24-də Fatihədə məhz bu baş verdi).
        if (clips.isNotEmpty()) {
            val last = clips.last()
            if (last.verseNo == verseCount) {
                clips[clips.lastIndex] = last.copy(openEnded = true)
            }
        }

        return clips
    }
}

/**
 * Maps a clip list onto one continuous timeline, so a player that is really jumping between
 * clipped items can still report a single position and accept a single seek.
 *
 * Clips that run to the end of their file have no known length up front; [withMeasuredDuration]
 * folds in the real one once the player reports it.
 */
class ClipTimeline(val clips: List<AudioClip>) {

    private val durations = LongArray(clips.size) { clips[it].durationMs }
    private var starts = cumulative(durations)

    val isEmpty: Boolean get() = clips.isEmpty()

    var totalDurationMs: Long = durations.sum()
        private set

    fun clipStartMs(index: Int): Long = if (index in starts.indices) starts[index] else 0L

    fun clipDurationMs(index: Int): Long = if (index in durations.indices) durations[index] else 0L

    /** Records the measured length of a clip whose end was not known (the last verse of a file). */
    fun withMeasuredDuration(index: Int, durationMs: Long) {
        if (index !in durations.indices || durationMs <= 0L) return
        if (durations[index] == durationMs) return

        durations[index] = durationMs
        starts = cumulative(durations)
        totalDurationMs = durations.sum()
    }

    fun virtualPositionAt(index: Int, positionInClipMs: Long): Long {
        if (index !in starts.indices) return 0L

        val duration = durations[index]
        val clamped = if (duration > 0L) positionInClipMs.coerceIn(0L, duration)
        else positionInClipMs.coerceAtLeast(0L)

        // Never report a position past the end: an open-ended last clip can outrun its measured
        // window, and a progress bar that goes past 100% reads as "it never finished".
        return (starts[index] + clamped).coerceAtMost(totalDurationMs)
    }

    /** The clip containing [virtualMs], and how far into it that position sits. */
    fun locate(virtualMs: Long): Pair<Int, Long> {
        if (clips.isEmpty()) return 0 to 0L

        val target = virtualMs.coerceAtLeast(0L)

        for (i in clips.indices) {
            val end = starts[i] + durations[i]
            if (target < end || i == clips.lastIndex) {
                return i to (target - starts[i]).coerceAtLeast(0L)
            }
        }

        return clips.lastIndex to 0L
    }

    /** First clip of [verseNo]; falls back to the last clip for a verse past the end. */
    fun firstIndexForVerse(verseNo: Int): Int {
        val index = clips.indexOfFirst { it.verseNo == verseNo }
        if (index >= 0) return index
        if (clips.isEmpty()) return 0

        return if (verseNo >= clips.last().verseNo) clips.lastIndex else 0
    }

    private fun cumulative(values: LongArray): LongArray {
        val out = LongArray(values.size)
        var sum = 0L
        for (i in values.indices) {
            out[i] = sum
            sum += values[i].coerceAtLeast(0L)
        }
        return out
    }
}
