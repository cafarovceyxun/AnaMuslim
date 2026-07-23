package com.cafarovceyxun.anamuslim.utils.reader.recitation

import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlin.jvm.JvmField

object RecitationUtils {
    @JvmField
    val DIR_NAME: String = AppFileSystem.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "recitations"
    )

    /** Matches `{chapNo:<format>}` tokens in a recitation URL template (the group is a format like `%03d`). */
    val URL_CHAPTER_PATTERN: Regex = Regex("\\{chapNo:(.*?)\\}", RegexOption.IGNORE_CASE)

    const val AVAILABLE_RECITATIONS_FILENAME: String = "available_recitations.json"
    const val AVAILABLE_RECITATION_TRANSLATIONS_FILENAME: String =
        "available_recitation_translations.json"
    const val RECITATION_AUDIO_NAME_FORMAT_LOCAL: String = "%03d-%03d.mp3"
}
