package com.cafarovceyxun.anamuslim.utils

object IntentUtils {
    const val INTENT_ACTION_OPEN_READER = "com.cafarovceyxun.anamuslim.action.OPEN_READER"
    const val INTENT_ACTION_OPEN_REFERENCE = "com.cafarovceyxun.anamuslim.action.OPEN_REFERENCE"
    const val INTENT_ACTION_OPEN_CHAPTER_INFO = "com.cafarovceyxun.anamuslim.action.OPEN_CHAPTER_INFO"

    /**
     * İdarəetmə paneli qısayolu. Hədəf `MainActivity`-dir: `ActivitySettings` exported deyil,
     * ona görə launcher onu birbaşa aça bilmir — `MainActivity` intent-i tutub paneli açır.
     */
    const val INTENT_ACTION_OPEN_ADMIN = "com.cafarovceyxun.anamuslim.action.OPEN_ADMIN"
}
