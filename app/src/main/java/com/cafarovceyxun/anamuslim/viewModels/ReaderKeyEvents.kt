package com.cafarovceyxun.anamuslim.viewModels

import android.view.KeyEvent

/**
 * Android keycode table for the shared [ReaderViewModel.handleScrollKey] seam, mirroring
 * [HadithViewModel.handleKeyEvent]. The navigation logic (mode handling, volume-key opt-in,
 * page/verse stepping) lives in the ViewModel; only this mapping is Android-specific, so it
 * stays in `:app`.
 */
fun ReaderViewModel.handleKeyEvent(keyCode: Int): Boolean {
    val key = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_BUTTON_1, // S Pen tək basış (elan olunmayan cihazlarda ehtiyat)
        KeyEvent.KEYCODE_PAGE_DOWN, // S Pen tək basış (res/xml/spen_remote_action.xml)
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> ReaderScrollKey.FORWARD

        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_BUTTON_2, // S Pen ikiqat basış (ehtiyat)
        KeyEvent.KEYCODE_PAGE_UP, // S Pen ikiqat basış (res/xml/spen_remote_action.xml)
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> ReaderScrollKey.BACKWARD

        else -> return false
    }

    val isVolumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
    return handleScrollKey(key, isVolumeKey)
}
