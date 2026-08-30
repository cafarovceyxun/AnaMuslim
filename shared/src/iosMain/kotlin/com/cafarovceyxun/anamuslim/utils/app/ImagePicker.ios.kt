package com.cafarovceyxun.anamuslim.utils.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/**
 * `PHPickerViewController` — proses xaricində işləyən sistem seçicisi, ona görə **foto icazəsi
 * istəmir** və `Info.plist`-ə `NSPhotoLibraryUsageDescription` əlavə etmək lazım deyil.
 *
 * Fayl `public.jpeg` kimi istənilir: iPhone şəkilləri HEIC saxlaya bilər, item provider isə bu UTI
 * üçün özü çevirir — bucket yalnız png/jpeg/webp qəbul edir.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePicker(onPicked: (PickedImage) -> Unit): (() -> Unit)? {
    val currentOnPicked by rememberUpdatedState(onPicked)

    // PHPickerViewController.delegate zəif referansdır: delegate-i burada saxlamasaq seçici
    // açılan kimi toplanır və nəticə heç vaxt gəlmir.
    val delegate = remember {
        PhotoPickerDelegate { picked -> picked?.let { currentOnPicked(it) } }
    }

    DisposableEffect(delegate) {
        onDispose { delegate.detach() }
    }

    return remember(delegate) {
        {
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (root == null) {
                AppLogger.d("ImagePicker", "No root view controller to present from")
            } else {
                val configuration = PHPickerConfiguration().apply {
                    filter = PHPickerFilter.imagesFilter()
                    selectionLimit = 1
                }

                val controller = PHPickerViewController(configuration)
                controller.delegate = delegate
                root.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PhotoPickerDelegate(
    private val onResult: (PickedImage?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    private var active = true

    fun detach() {
        active = false
    }

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)

        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            onResult(null)
            return
        }

        result.itemProvider.loadDataRepresentationForTypeIdentifier(JPEG_UTI) { data, error ->
            if (error != null) {
                AppLogger.d("ImagePicker", "Load failed: ${error.localizedDescription}")
            }

            val bytes = data?.toByteArray()

            // Nəticə arxa fon növbəsində gəlir; UI-yə yalnız əsas axından toxunulur.
            dispatch_async(dispatch_get_main_queue()) {
                if (active) {
                    onResult(bytes?.takeIf { it.isNotEmpty() }?.let { PickedImage(it, "image/jpeg") })
                }
            }
        }
    }
}

/** HEIC şəkillər də bu UTI ilə istənəndə item provider tərəfindən JPEG-ə çevrilir. */
private const val JPEG_UTI = "public.jpeg"

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)

    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}
