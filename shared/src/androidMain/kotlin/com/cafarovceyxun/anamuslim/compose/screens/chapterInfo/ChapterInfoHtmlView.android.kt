package com.cafarovceyxun.anamuslim.compose.screens.chapterInfo

import android.annotation.SuppressLint
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.cafarovceyxun.anamuslim.utils.AppLogger

/**
 * Android implementation: a `WebView` loading the self-contained page.
 *
 * The former `ChapterInfoWebViewClient`, which served the CSS/JS/font/image by intercepting
 * `https://assets-*` requests, is gone — those are inlined into the HTML now, so the client only
 * has to keep navigation inside the page.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun ChapterInfoHtmlView(
    html: String,
    onOpenReference: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit,
    modifier: Modifier,
) {
    // The bridge object is registered once, so it must not close over a changing callback.
    val bridge = remember { JsBridge() }
    bridge.onOpenReference = onOpenReference

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0x00000000)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = true
                overScrollMode = View.OVER_SCROLL_NEVER

                addJavascriptInterface(bridge, "ChapterInfoJSInterface")

                webViewClient = object : WebViewClient() {
                    /** The page is local; links must never navigate the view away from it. */
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = true
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                        AppLogger.d("[${message.lineNumber()}] ${message.message()}")
                        return true
                    }
                }
            }
        },
        update = { webView ->
            if (webView.getTag(TAG_KEY) != html) {
                webView.setTag(TAG_KEY, html)
                webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "utf-8", null)
            }
        },
        modifier = modifier,
    )
}

/** Avoids reloading (and so re-scrolling) the page when recomposition brings the same HTML. */
private val TAG_KEY = "chapter_info_html".hashCode()

private class JsBridge {
    var onOpenReference: (Int, Int, Int) -> Unit = { _, _, _ -> }

    @JavascriptInterface
    fun openReference(chapterNo: Int, fromVerse: Int, toVerse: Int) {
        onOpenReference(chapterNo, fromVerse, toVerse)
    }
}
