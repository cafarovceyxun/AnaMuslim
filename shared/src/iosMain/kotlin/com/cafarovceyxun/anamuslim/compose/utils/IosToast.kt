package com.cafarovceyxun.anamuslim.compose.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSString
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSStringDrawingUsesFontLeading
import platform.UIKit.NSStringDrawingUsesLineFragmentOrigin
import platform.UIKit.boundingRectWithSize
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.math.ceil
import kotlin.math.min

/**
 * A transient bottom overlay standing in for Android's `Toast`, which iOS has no native equivalent
 * of. Without this every shared `PlatformUtils.showToast` call — "added to favourites", "copied",
 * download errors — only reached the console, so the user never saw it.
 *
 * A dark rounded pill fades in over the key window, holds, then fades out and removes itself. A new
 * toast replaces any still on screen, matching Android's single-toast behaviour. The pill is dark
 * with white text in both themes (like the platform toast), so it needs no theme wiring.
 */
@OptIn(ExperimentalForeignApi::class)
object IosToast {

    private const val SHORT_SECONDS = 2.0
    private const val LONG_SECONDS = 3.5
    private const val FADE_IN = 0.25
    private const val FADE_OUT = 0.4

    private const val H_PADDING = 16.0
    private const val V_PADDING = 10.0
    private const val SIDE_MARGIN = 24.0
    private const val BOTTOM_INSET = 100.0

    /** Retained so a follow-up toast can drop the previous one instead of stacking. */
    private var current: UIView? = null

    fun show(text: String, longDuration: Boolean) {
        dispatch_async(dispatch_get_main_queue()) {
            present(text, if (longDuration) LONG_SECONDS else SHORT_SECONDS)
        }
    }

    private fun present(text: String, holdSeconds: Double) {
        val window = UIApplication.sharedApplication.keyWindow ?: run {
            println("Toast: $text") // No window yet (very early startup); keep the console breadcrumb.
            return
        }

        current?.removeFromSuperview()

        val font = UIFont.systemFontOfSize(14.0)
        val screenWidth = window.bounds.useContents { size.width }
        val screenHeight = window.bounds.useContents { size.height }
        val maxTextWidth = screenWidth - 2 * SIDE_MARGIN - 2 * H_PADDING

        val measured = (text as NSString).boundingRectWithSize(
            size = CGSizeMake(maxTextWidth, 10_000.0),
            options = NSStringDrawingUsesLineFragmentOrigin or NSStringDrawingUsesFontLeading,
            attributes = mapOf<Any?, Any?>(NSFontAttributeName to font),
            context = null,
        )
        val textWidth = measured.useContents { ceil(size.width) }
        val textHeight = measured.useContents { ceil(size.height) }

        val containerWidth = min(textWidth + 2 * H_PADDING, screenWidth - 2 * SIDE_MARGIN)
        val containerHeight = textHeight + 2 * V_PADDING
        val x = (screenWidth - containerWidth) / 2
        val y = screenHeight - containerHeight - BOTTOM_INSET

        val container = UIView(frame = CGRectMake(x, y, containerWidth, containerHeight)).apply {
            backgroundColor = UIColor.colorWithWhite(white = 0.0, alpha = 0.85)
            layer.cornerRadius = 12.0
            clipsToBounds = true
            alpha = 0.0
            userInteractionEnabled = false
        }

        val label = UILabel(
            frame = CGRectMake(H_PADDING, V_PADDING, containerWidth - 2 * H_PADDING, textHeight),
        ).apply {
            setText(text)
            setFont(font)
            textColor = UIColor.whiteColor
            numberOfLines = 0
            textAlignment = NSTextAlignmentCenter
        }

        container.addSubview(label)
        window.addSubview(container)
        current = container

        UIView.animateWithDuration(FADE_IN) { container.alpha = 1.0 }

        val delayNs = ((FADE_IN + holdSeconds) * NSEC_PER_SEC.toDouble()).toLong()
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delayNs), dispatch_get_main_queue()) {
            UIView.animateWithDuration(
                duration = FADE_OUT,
                animations = { container.alpha = 0.0 },
                completion = {
                    container.removeFromSuperview()
                    if (current == container) current = null
                },
            )
        }
    }
}
