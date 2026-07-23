package com.cafarovceyxun.anamuslim.compose.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import androidx.compose.ui.graphics.ImageBitmap
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.popoverPresentationController

actual object PlatformUtils {
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun browseLink(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
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

    actual fun showToast(text: String) = IosToast.show(text, longDuration = false)

    actual fun showLongToast(text: String) = IosToast.show(text, longDuration = true)

    actual fun showClipboardMessage(text: String) = showToast(text)
}
