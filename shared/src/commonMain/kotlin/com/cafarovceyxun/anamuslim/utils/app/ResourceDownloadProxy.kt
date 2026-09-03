package com.cafarovceyxun.anamuslim.utils.app

/**
 * Resurs yükləmə mirror-u.
 *
 * ⚠️ `ALFAAZ_PLUS` (`gh-proxy.alfaazplus.com`) **çıxarıldı və defolt idi**. Həmin proxy yalnız
 * AlfaazPlus-ın öz repolarını verir: `cafarovceyxun/AnaMuslim`-dəki hər şeyə 404 qaytarır. Yəni
 * defolt ayarlı hər istifadəçidə bu repodan gələn fayllar **səssizcə gəlmirdi** — tərcümə
 * səsləndirmə manifesti (`ApiConfig.OWN_TRANSLATION_RECITATIONS_URL`) daxil. Saxlanmış
 * `"alfaazplus"` dəyəri [fromValue] ilə [DEFAULT]-a düşür, yəni miqrasiya lazım deyil.
 */
enum class ResourceDownloadProxy(val value: String) {
    GITHUB("github"),
    JSDELIVR("jsdelivr");

    companion object {
        val DEFAULT = GITHUB

        fun fromValue(value: String): ResourceDownloadProxy {
            return entries.find { it.value == value } ?: DEFAULT
        }
    }
}
