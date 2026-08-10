package com.cafarovceyxun.anamuslim.compose.utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform-agnostic utility functions for common UI actions.
 */
expect object PlatformUtils {
    /** Copies the given [text] to the system clipboard. */
    fun copyToClipboard(text: String)

    /** Opens the given [url] in the system's default browser or handler. */
    fun browseLink(url: String)

    /**
     * Hands [text] to the system share sheet. [chooserTitle] labels Android's chooser;
     * iOS has no equivalent and ignores it.
     */
    fun shareText(text: String, chooserTitle: String? = null)

    /**
     * Encodes [image] as PNG and hands it to the system share sheet. Returns false if the platform
     * could not produce or present it (the caller then leaves the UI untouched).
     *
     * The rendering side stays in common code: `rememberGraphicsLayer()` / `record {}` /
     * `toImageBitmap()` are Compose Multiplatform APIs — only encoding and the share intent are
     * platform-bound.
     */
    fun shareImage(image: ImageBitmap, chooserTitle: String? = null): Boolean

    /** Shows a short-duration message (Toast) to the user. */
    fun showToast(text: String)

    /** Shows a longer-duration message, for text the user needs more time to read. */
    fun showLongToast(text: String)

    /**
     * Confirms to the user that [text] was copied, *only where the OS does not say so itself*
     * (Android 13+ shows its own clipboard confirmation, so this is a no-op there).
     */
    fun showClipboardMessage(text: String)
}
