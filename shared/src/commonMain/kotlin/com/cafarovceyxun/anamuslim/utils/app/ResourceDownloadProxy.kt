package com.cafarovceyxun.anamuslim.utils.app

enum class ResourceDownloadProxy(val value: String) {
    ALFAAZ_PLUS("alfaazplus"),
    GITHUB("github"),
    JSDELIVR("jsdelivr");

    companion object {
        val DEFAULT = ALFAAZ_PLUS

        fun fromValue(value: String): ResourceDownloadProxy {
            return entries.find { it.value == value } ?: DEFAULT
        }
    }
}
