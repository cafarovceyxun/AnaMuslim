package com.cafarovceyxun.anamuslim.utils.app

import com.cafarovceyxun.anamuslim.utils.univ.AppFileSystem
import kotlin.jvm.JvmField

object AppUtils {
    @JvmField
    val BASE_APP_DOWNLOADED_SAVED_DATA_DIR: String = AppFileSystem.createPath(
        "downloaded",
        "saved_data"
    )

    val BASE_APP_LOG_DATA_DIR = "logs"

    @JvmField
    val APP_OTHER_DIR: String = AppFileSystem.createPath(BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "other")
}
