package com.cafarovceyxun.anamuslim.utils.mediaplayer

import com.cafarovceyxun.anamuslim.utils.AppLogger
import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.reader.recitation.RecitationUtils
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import okio.Path

/**
 * Where word-by-word audio lives and where it comes from — the portable half of Android's
 * `WbwAudioRepository`. That object keeps the platform half (media3 playback and the `Uri`-shaped
 * API its callers use) and delegates here, so both platforms agree on file names and URLs.
 */
object WbwAudioFiles {
    private const val DIR_NAME = "wbw_audio"

    /** The one published WBW audio dataset; timings are keyed by it. */
    const val AUDIO_ID = "wbw_a1"

    const val TIMING_URL =
        "ghraw://AlfaazPlus/QuranAppInventory/master/wbw_timings/wbw_a1.json.gz"

    private const val AUDIO_URL_TEMPLATE =
        "https://github.com/dabatase/wbw_a1/releases/download/v1/{chapNo:%03d}.webm"

    private const val ONE_OFF_AUDIO_URL_BASE = "https://audio.qurancdn.com/wbw/"

    private fun rootDir(): Path = AppFileSystem.makeAndGetAppResourceDir(
        AppFileSystem.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, DIR_NAME)
    )

    fun chapterAudioPath(chapterNo: Int): Path =
        rootDir() / StringUtils.formatInvariant("%03d.webm", chapterNo)

    fun isChapterAudioDownloaded(chapterNo: Int): Boolean =
        (AppFileSystem.size(chapterAudioPath(chapterNo)) ?: 0L) > 0L

    fun deleteChapterAudio(chapterNo: Int) {
        AppFileSystem.delete(chapterAudioPath(chapterNo))
    }

    fun prepareChapterAudioUrl(chapterNo: Int): String? = try {
        RecitationUtils.URL_CHAPTER_PATTERN.replace(AUDIO_URL_TEMPLATE) { match ->
            StringUtils.formatInvariant(match.groupValues[1], chapterNo)
        }
    } catch (e: Exception) {
        AppLogger.saveError(e, "WbwAudioFiles.prepareChapterAudioUrl")
        null
    }

    /**
     * Single-word clip URL, used when the chapter file is not downloaded. The CDN numbers words
     * from 1 while the app numbers them from 0.
     */
    fun prepareOneOffWordAudioUrl(chapterNo: Int, verseNo: Int, appWordIndex: Int): String? {
        if (chapterNo <= 0 || verseNo <= 0 || appWordIndex < 0) return null

        val fileName = StringUtils.formatInvariant(
            "%03d_%03d_%03d.mp3",
            chapterNo,
            verseNo,
            appWordIndex + 1,
        )
        return ONE_OFF_AUDIO_URL_BASE + fileName
    }
}
