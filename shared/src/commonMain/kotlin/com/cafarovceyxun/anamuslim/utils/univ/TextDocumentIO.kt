package com.cafarovceyxun.anamuslim.utils.univ

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Presents the platform's "save a file" UI and writes a text document to wherever the user picks.
 *
 * A composable-scoped handle rather than a plain object: Android's Storage Access Framework only
 * hands back a `Uri` through an `ActivityResultLauncher`, which must be registered during
 * composition (`rememberNotificationPermission` follows the same shape for the same reason).
 */
@Stable
interface TextDocumentSaver {
    /**
     * Opens the picker with [suggestedFileName] pre-filled and writes [content] to the chosen
     * location. The result arrives through the `onSaved` callback given to
     * [rememberTextDocumentSaver] — never synchronously.
     */
    fun save(suggestedFileName: String, content: String)
}

/** Counterpart of [TextDocumentSaver] for reading a user-picked text document. */
@Stable
interface TextDocumentOpener {
    /**
     * Opens the picker. The file's text arrives through the `onOpened` callback given to
     * [rememberTextDocumentOpener], or `null` if the user cancelled or the file was unreadable.
     */
    fun open()
}

/** MIME type of the export file; iOS maps it to `UTType.json`. */
const val MIME_TYPE_JSON = "application/json"

/**
 * A saver for JSON documents. [onSaved] reports whether the file was actually written — `false`
 * covers both cancellation and a failed write, so callers should not claim success on their own.
 */
@Composable
expect fun rememberTextDocumentSaver(onSaved: (Boolean) -> Unit): TextDocumentSaver

/**
 * An opener for JSON documents. [onOpened] receives the file's decoded text, or `null` when the
 * user cancelled or the file could not be read.
 */
@Composable
expect fun rememberTextDocumentOpener(onOpened: (String?) -> Unit): TextDocumentOpener

/** Word sənədi (`.docx`) — iOS-da `org.openxmlformats.wordprocessingml.document`. */
const val MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

/**
 * İdxal sənədi üçün açıcı: düz mətn **və** Word (`.docx`) faylı. [onOpened] faylın xam baytlarını
 * alır (ləğvdə/oxunmayanda `null`) — mətnə çevirmə formatdan asılıdır və ortaq koddadır
 * (`readImportDocument`). JSON açıcısından ayrıdır, çünki o, baytı UTF-8 kimi açıb docx-i pozur.
 */
@Composable
expect fun rememberImportDocumentOpener(onOpened: (ByteArray?) -> Unit): TextDocumentOpener
