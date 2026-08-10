package com.cafarovceyxun.anamuslim.compose.components.player.dialogs

enum class AudioOption(val value: String) {
    ONLY_QURAN("only_quran"),
    ONLY_TRANSLATION("only_translation"),
    BOTH("both");

    companion object {
        val DEFAULT = ONLY_QURAN

        fun fromValue(value: String): AudioOption {
            return entries.find { it.value == value } ?: ONLY_QURAN
        }
    }
}
