package com.cafarovceyxun.anamuslim.utils.mediaplayer

import java.io.File

/**
 * `java.io.File` views of the shared reciter storage layout, for the Android download workers and
 * the media3 service, which speak `File`/`Uri` rather than okio paths.
 */
fun RecitationModelManager.getRecitationAudioFile(reciterId: String, chapterNo: Int): File =
    File(getRecitationAudioPath(reciterId, chapterNo).toString())

fun RecitationModelManager.getRecitationTimingFile(reciterId: String): File =
    File(getRecitationTimingPath(reciterId).toString())
