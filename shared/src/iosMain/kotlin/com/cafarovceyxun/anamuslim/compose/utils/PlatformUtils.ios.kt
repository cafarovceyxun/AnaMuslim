package com.cafarovceyxun.anamuslim.compose.utils

import com.cafarovceyxun.anamuslim.utils.AppLogger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import androidx.compose.ui.graphics.ImageBitmap
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.popoverPresentationController
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary

actual object PlatformUtils {
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    /**
     * ⚠️ iOS 16+ hər proqramlı oxunuşda «Yapışdırmağa icazə verilsin?» sistem dialoqu göstərir —
     * bunu keçmək mümkün deyil (yeganə alternativ `UIPasteControl`-dur, o da Compose-da yoxdur).
     * Yəni düymə işləyir, sadəcə hər dəfə bir təsdiq soruşulur.
     */
    actual fun readFromClipboard(): String? =
        UIPasteboard.generalPasteboard.string?.takeIf { it.isNotBlank() }

    /**
     * ⚠️ Sinxron `openURL(url)` **işlətmə** — o, iOS 10-dan bəri köhnəlmişdir və müasir iOS-da
     * səssizcə heç nə etmir (Haqqımızda ekranındakı beş linkin hamısı ölü idi, 63-cü dalğa).
     * Yeganə düzgün variant `options`/`completionHandler` alan üç arqumentli yükləmədir.
     */
    actual fun browseLink(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>(), null)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun shareText(text: String, chooserTitle: String?) {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val controller = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        // An iPad presents the sheet as a popover and needs an anchor; the app window is the
        // only one available from here, so it points at the window's centre.
        controller.popoverPresentationController?.let { popover ->
            popover.sourceView = root.view
            popover.permittedArrowDirections = 0u
            popover.sourceRect = CGRectMake(
                root.view.bounds.useContents { size.width / 2 },
                root.view.bounds.useContents { size.height / 2 },
                0.0,
                0.0,
            )
        }
        root.presentViewController(controller, animated = true, completion = null)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun shareImage(image: ImageBitmap, chooserTitle: String?): Boolean {
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
        val uiImage = image.toUIImage() ?: return false

        val controller = UIActivityViewController(
            activityItems = listOf(uiImage),
            applicationActivities = null,
        )
        // Same iPad popover anchoring as shareText.
        controller.popoverPresentationController?.let { popover ->
            popover.sourceView = root.view
            popover.permittedArrowDirections = 0u
            popover.sourceRect = CGRectMake(
                root.view.bounds.useContents { size.width / 2 },
                root.view.bounds.useContents { size.height / 2 },
                0.0,
                0.0,
            )
        }
        root.presentViewController(controller, animated = true, completion = null)
        return true
    }

    /**
     * Foto kitabxanasına yazır.
     *
     * `UIImageWriteToSavedPhotosAlbum` deyil, `PHPhotoLibrary`: birincisi nəticəni bildirmir — icazə
     * verilməyəndə də səssizcə qayıdır və istifadəçiyə «yazıldı» deyərdik. Burada əvvəlcə **yalnız
     * əlavə etmə** səviyyəsində icazə istənilir, sonra nəticə gözlənilir.
     *
     * ⚠️ `Info.plist`-də `NSPhotoLibraryAddUsageDescription` **məcburidir** — açar olmadan icazə
     * sorğusu tətbiqi dərhal çökdürür.
     */
    actual suspend fun saveImageToGallery(image: ImageBitmap, fileName: String): Boolean {
        val uiImage = image.toUIImage() ?: return false

        val authorized = suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(
                PHAccessLevelAddOnly,
            ) { status ->
                continuation.resume(
                    status == PHAuthorizationStatusAuthorized ||
                        status == PHAuthorizationStatusLimited
                )
            }
        }
        if (!authorized) return false

        return suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                changeBlock = {
                    PHAssetChangeRequest.creationRequestForAssetFromImage(uiImage)
                    Unit
                },
                completionHandler = { success, error ->
                    if (error != null) {
                        AppLogger.d("PlatformUtils", "saveImageToGallery: ${error.localizedDescription}")
                    }
                    continuation.resume(success)
                },
            )
        }
    }

    actual fun showToast(text: String) = IosToast.show(text, longDuration = false)

    actual fun showLongToast(text: String) = IosToast.show(text, longDuration = true)

    actual fun showClipboardMessage(text: String) = showToast(text)
}
