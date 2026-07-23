package com.cafarovceyxun.anamuslim.viewModels

import android.view.KeyEvent

/**
 * Android keycode table for the shared [HadithViewModel.handleScrollKey] seam. The scroll logic
 * (amount, volume-key opt-in, event emission) lives in the shared ViewModel; only this mapping is
 * Android-specific, so it stays in `:app`.
 */
fun HadithViewModel.handleKeyEvent(keyCode: Int): Boolean {
    val key = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_BUTTON_1,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_PAGE_DOWN -> HadithScrollKey.FORWARD

        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_BUTTON_2,
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_PAGE_UP -> HadithScrollKey.BACKWARD

        else -> return false
    }

    val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
    return handleScrollKey(key, isVolumeKey)
}
