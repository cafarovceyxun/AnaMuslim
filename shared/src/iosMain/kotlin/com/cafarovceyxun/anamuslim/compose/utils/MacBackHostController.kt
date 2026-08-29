package com.cafarovceyxun.anamuslim.compose.utils

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo
import platform.Foundation.iOSAppOnMac
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIKeyCommand
import platform.UIKit.UIKeyInputLeftArrow
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UIScrollTypeMaskAll
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.addKeyCommand
import platform.UIKit.didMoveToParentViewController
import platform.darwin.NSObject
import kotlin.math.abs

/**
 * Wraps the Compose controller so the Mac build gets back navigation — a two-finger swipe to the
 * right and the ⌘ shortcuts — and returns [content] untouched everywhere else.
 *
 * Only Mac is wrapped, deliberately: on iPhone and iPad the system's own edge-swipe already reaches
 * Compose, and hosting the Compose controller as a child would put a view controller of ours between
 * it and the window (status bar, safe area, first responder) for no gain. `isiOSAppOnMac` is the
 * runtime check for the "Designed for iPhone" Mac build — the same binary, so there is no build flag
 * to key this off.
 */
@OptIn(ExperimentalForeignApi::class)
fun wrapForMacBack(content: UIViewController): UIViewController =
    if (NSProcessInfo.processInfo.iOSAppOnMac) MacBackHostController(content) else content

/** How far into the window a swipe may start while the reader is paging horizontally. */
private const val EDGE_ZONE = 80.0

/** Travel a swipe needs before it counts as back, and how much it must beat its vertical drift. */
private const val SWIPE_DISTANCE = 90.0
private const val HORIZONTAL_RATIO = 2.0

/**
 * Host for the Compose controller on Mac: it owns the inputs macOS gives an iOS app in place of the
 * back gesture, and forwards both to [MacBack].
 *
 * The swipe is a [UIPanGestureRecognizer] with `allowedScrollTypesMask` — the UIKit opt-in that
 * makes a pan recognizer also see trackpad scroll, which is how macOS delivers a two-finger swipe.
 * It recognizes simultaneously with everything else ([SimultaneousGestureDelegate]) so Compose keeps
 * scrolling normally; the swipe only counts when it is decidedly horizontal and long enough, and it
 * fires once per gesture.
 *
 * The ⌘ shortcuts are `UIKeyCommand`s, which travel the responder chain — that is the reason this is
 * a real view controller above Compose rather than a bare gesture recognizer: the chain needs an
 * object that implements the action.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@ExportObjCClass
private class MacBackHostController(
    private val content: UIViewController,
) : UIViewController(nibName = null, bundle = null) {

    /** Held here because a recognizer keeps only a weak reference to its delegate. */
    private val gestureDelegate = SimultaneousGestureDelegate()

    private var swipeStartX = 0.0
    private var swipeHandled = false

    override fun viewDidLoad() {
        super.viewDidLoad()

        addChildViewController(content)
        content.view.setFrame(view.bounds)
        content.view.setAutoresizingMask(
            UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight,
        )
        view.addSubview(content.view)
        content.didMoveToParentViewController(this)

        val swipe = UIPanGestureRecognizer(
            target = this,
            action = NSSelectorFromString("handleBackSwipe:"),
        )
        swipe.allowedScrollTypesMask = UIScrollTypeMaskAll
        swipe.delegate = gestureDelegate
        view.addGestureRecognizer(swipe)

        // ⌘[ and ⌘← are both "back" on macOS; binding both keeps either habit working.
        addKeyCommand(
            UIKeyCommand.keyCommandWithInput(
                input = "[",
                modifierFlags = UIKeyModifierCommand,
                action = NSSelectorFromString("handleBackCommand"),
            ),
        )
        addKeyCommand(
            UIKeyCommand.keyCommandWithInput(
                input = UIKeyInputLeftArrow,
                modifierFlags = UIKeyModifierCommand,
                action = NSSelectorFromString("handleBackCommand"),
            ),
        )
    }

    @ObjCAction
    fun handleBackSwipe(recognizer: UIPanGestureRecognizer) {
        when (recognizer.state) {
            UIGestureRecognizerStateBegan -> {
                swipeHandled = false
                swipeStartX = recognizer.locationInView(view).useContents { x }
            }

            UIGestureRecognizerStateChanged -> {
                if (swipeHandled) return
                if (MacBack.edgeOnly && swipeStartX > EDGE_ZONE) return

                val translation = recognizer.translationInView(view)
                val dx = translation.useContents { x }
                val dy = translation.useContents { y }
                // Rightwards, and clearly horizontal: a diagonal flick down a list is not a back.
                if (dx < SWIPE_DISTANCE || abs(dx) < HORIZONTAL_RATIO * abs(dy)) return

                swipeHandled = true
                MacBack.perform()
            }

            else -> swipeHandled = false
        }
    }

    @ObjCAction
    fun handleBackCommand() {
        MacBack.perform()
    }
}

/**
 * Lets the back swipe run alongside Compose's own gesture handling. Without it UIKit would make the
 * two recognizers exclusive and one of them — usually scrolling — would stop working.
 */
@OptIn(BetaInteropApi::class)
private class SimultaneousGestureDelegate : NSObject(), UIGestureRecognizerDelegateProtocol {
    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer,
    ): Boolean = true
}
