package com.cafarovceyxun.anamuslim.compose.screens.chapterInfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSNumber
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled
import platform.darwin.NSObject

/**
 * iOS implementation: a `WKWebView` loading the self-contained page.
 *
 * The page speaks the Android bridge's vocabulary (`window.ChapterInfoJSInterface.openReference`),
 * so instead of editing the shared JavaScript a tiny shim is injected that forwards those calls to
 * a `WKScriptMessageHandler`. That keeps one script file for both platforms.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ChapterInfoHtmlView(
    html: String,
    onOpenReference: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit,
    modifier: Modifier,
) {
    // The handler is registered once with the controller, so it must not capture a stale callback.
    val handler = remember { ReferenceMessageHandler() }
    handler.onOpenReference = onOpenReference

    // Reloading on every recomposition would throw away the reader's scroll position, so the last
    // loaded page is tracked — the same guard the Android implementation applies.
    val lastLoaded = remember { arrayOfNulls<String>(1) }

    UIKitView(
        factory = {
            val controller = WKUserContentController()
            controller.addScriptMessageHandler(handler, name = MESSAGE_NAME)

            val configuration = WKWebViewConfiguration().apply {
                userContentController = controller
                preferences.javaScriptEnabled = true
            }

            WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
                opaque = false
                scrollView.bounces = false
                lastLoaded[0] = html
                loadHTMLString(BRIDGE_SHIM + html, baseURL = null)
            }
        },
        update = { webView ->
            if (lastLoaded[0] != html) {
                lastLoaded[0] = html
                webView.loadHTMLString(BRIDGE_SHIM + html, baseURL = null)
            }
        },
        modifier = modifier,
    )
}

private const val MESSAGE_NAME = "chapterInfoOpenReference"

/** Maps the Android-shaped bridge call onto WebKit's message handler. */
private val BRIDGE_SHIM = """
    <script>
      window.ChapterInfoJSInterface = {
        openReference: function (chapterNo, fromVerse, toVerse) {
          window.webkit.messageHandlers.$MESSAGE_NAME.postMessage([chapterNo, fromVerse, toVerse]);
        }
      };
    </script>
""".trimIndent()

private class ReferenceMessageHandler : NSObject(), WKScriptMessageHandlerProtocol {
    var onOpenReference: (Int, Int, Int) -> Unit = { _, _, _ -> }

    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        // JS numbers arrive as NSNumber; anything else is a page bug, not a user-visible case.
        val body = didReceiveScriptMessage.body as? List<*> ?: return
        if (body.size < 3) return

        val values = body.map { (it as? NSNumber)?.intValue ?: return }

        onOpenReference(values[0], values[1], values[2])
    }
}
