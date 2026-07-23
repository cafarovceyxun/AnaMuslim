package com.cafarovceyxun.anamuslim.utils.chapterInfo

import com.cafarovceyxun.anamuslim.utils.app.AppUtils
import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import com.cafarovceyxun.anamuslim.utils.univ.StringUtils
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

object ChapterInfoUtils {
    @JvmField
    val DIR_NAME: String = AppFileSystem.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "chapters_info",
    )

    private const val CHAPTER_INFO_FILE_NAME_FORMAT = "chapter_info_%d-%s.json"

    @JvmStatic
    fun prepareChapterInfoFilePath(lang: String, chapterNo: Int): String {
        val fileName = StringUtils.formatInvariant(CHAPTER_INFO_FILE_NAME_FORMAT, chapterNo, lang)
        return AppFileSystem.createPath(chapterNo.toString(), fileName)
    }
}
