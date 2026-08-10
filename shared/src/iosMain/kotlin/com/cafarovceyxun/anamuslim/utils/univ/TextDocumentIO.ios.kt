package com.cafarovceyxun.anamuslim.utils.univ

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cafarovceyxun.anamuslim.utils.AppLogger
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject

/**
 * iOS counterpart of the Storage Access Framework: `UIDocumentPickerViewController` in its two
 * modes — exporting an existing file ("Save to Files") and opening one.
 *
 * Unlike Android there is no result launcher to register, so nothing here actually needs
 * composition; the composable shape is kept because the seam is shared and Android does need it.
 */

/**
 * The picker holds its delegate **weakly**, so a delegate that only the presenting call frame
 * referenced would be collected before the user finishes picking and the callback would never
 * arrive. Holding it here keeps it alive; the entry is dropped when the picker reports back.
 */
private val liveDelegates = mutableSetOf<NSObject>()

private class DocumentPickerDelegate(
    private val onResult: (NSURL?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    private fun finish(url: NSURL?) {
        liveDelegates.remove(this)
        onResult(url)
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentAtURL: NSURL,
    ) {
        finish(didPickDocumentAtURL)
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        finish(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        finish(null)
    }
}

private fun present(
    controller: UIDocumentPickerViewController,
    delegate: DocumentPickerDelegate,
): Boolean {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
    liveDelegates.add(delegate)
    controller.delegate = delegate
    root.presentViewController(controller, animated = true, completion = null)
    return true
}

@Composable
actual fun rememberTextDocumentSaver(onSaved: (Boolean) -> Unit): TextDocumentSaver {
    val currentOnSaved by rememberUpdatedState(onSaved)

    return remember {
        object : TextDocumentSaver {
            override fun save(suggestedFileName: String, content: String) {
                // "Export" moves a file the app already owns, so the document has to exist before
                // the picker opens; the copy in the temp directory is what the user then places.
                val tempPath = (NSTemporaryDirectory() + suggestedFileName).toPath()
                try {
                    FileSystem.SYSTEM.sink(tempPath).buffer().use { it.writeUtf8(content) }
                } catch (e: Exception) {
                    AppLogger.saveError(e, "TextDocumentSaver.writeTemp")
                    currentOnSaved(false)
                    return
                }

                val fileUrl = NSURL.fileURLWithPath(tempPath.toString())
                val delegate = DocumentPickerDelegate { url -> currentOnSaved(url != null) }
                val controller = UIDocumentPickerViewController(
                    forExportingURLs = listOf(fileUrl),
                    asCopy = true,
                )
                if (!present(controller, delegate)) currentOnSaved(false)
            }
        }
    }
}

@Composable
actual fun rememberTextDocumentOpener(onOpened: (String?) -> Unit): TextDocumentOpener {
    val currentOnOpened by rememberUpdatedState(onOpened)

    return remember {
        object : TextDocumentOpener {
            override fun open() {
                val delegate = DocumentPickerDelegate { url ->
                    currentOnOpened(url?.let { readText(it) })
                }
                // Plain text alongside JSON: some providers type a hand-edited export as text.
                val controller = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText),
                )
                if (!present(controller, delegate)) currentOnOpened(null)
            }
        }
    }
}

private fun readText(url: NSURL): String? {
    // A file picked outside the app sandbox is only readable inside a security-scoped block.
    val scoped = url.startAccessingSecurityScopedResource()
    return try {
        val path = url.path ?: return null
        FileSystem.SYSTEM.source(path.toPath()).buffer().use { it.readUtf8() }
    } catch (e: Exception) {
        AppLogger.saveError(e, "TextDocumentOpener.read")
        null
    } finally {
        if (scoped) url.stopAccessingSecurityScopedResource()
    }
}
