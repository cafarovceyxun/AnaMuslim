package com.cafarovceyxun.anamuslim.utils.univ

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

/**
 * Seçicinin delegate-i **zəif** saxlanır — burada saxlamasaq istifadəçi seçməmiş yığılar və
 * callback gəlməz (bax `TextDocumentIO.ios.kt`).
 */
private val liveDelegates = mutableSetOf<NSObject>()

private const val TEMP_DIR = "apk-upload"

private class LargeFileDelegate(
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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberLargeFilePicker(onPicked: (PickedLargeFile?) -> Unit): LargeFilePicker {
    val currentOnPicked by rememberUpdatedState(onPicked)

    return remember {
        object : LargeFilePicker {
            override fun pick() {
                val delegate = LargeFileDelegate { url ->
                    currentOnPicked(url?.let { copyToTemp(it) })
                }
                // `asCopy = false`: sistem 130 MB-ı ikinci dəfə kopyalamasın, biz özümüz bir dəfə köçürürük.
                val controller = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeItem),
                    asCopy = false,
                )
                val root = UIApplication.sharedApplication.keyWindow?.rootViewController
                if (root == null) {
                    currentOnPicked(null)
                    return
                }
                liveDelegates.add(delegate)
                controller.delegate = delegate
                root.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun copyToTemp(url: NSURL): PickedLargeFile? {
    // Sandbox-dan kənar fayl yalnız security-scoped blokda oxunur.
    val scoped = url.startAccessingSecurityScopedResource()
    return try {
        val fm = NSFileManager.defaultManager
        val name = url.lastPathComponent?.takeIf { it.isNotBlank() } ?: "upload.apk"
        val dir = NSTemporaryDirectory() + TEMP_DIR
        // Əvvəlki yükləmələrin qalıqları yığılmasın.
        fm.removeItemAtPath(dir, error = null)
        fm.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)

        val targetPath = "$dir/$name"
        val target = NSURL.fileURLWithPath(targetPath)

        val copied = memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            val ok = fm.copyItemAtURL(url, toURL = target, error = err.ptr)
            if (!ok) AppLogger.d("LargeFilePicker", "copy failed: ${err.value?.localizedDescription}")
            ok
        }
        if (!copied) return null

        val size = (fm.attributesOfItemAtPath(targetPath, error = null)?.get(NSFileSize) as? NSNumber)
            ?.longLongValue ?: 0L
        PickedLargeFile(path = targetPath, name = name, size = size)
    } catch (e: Exception) {
        AppLogger.saveError(e, "LargeFilePicker.copy")
        null
    } finally {
        if (scoped) url.stopAccessingSecurityScopedResource()
    }
}
