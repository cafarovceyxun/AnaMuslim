package com.cafarovceyxun.anamuslim.compose.components.player.dialogs

/** What the player does when the current chapter's audio finishes. */
enum class AudioEndBehaviour(val value: String) {
    STOP_PLAYBACK("stop_playback"),
    NEXT_CHAPTER("next_chapter"),
    REPEAT_CHAPTER("repeat_chapter");

    companion object {
        val DEFAULT = STOP_PLAYBACK

        fun fromValue(value: String): AudioEndBehaviour {
            return entries.firstOrNull { it.value == value } ?: STOP_PLAYBACK
        }
    }
}
