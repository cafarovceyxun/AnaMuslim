package com.cafarovceyxun.anamuslim.utils.chapterInfo

import com.cafarovceyxun.anamuslim.db.entities.quran.RevelationType
import com.cafarovceyxun.anamuslim.resources.Res
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Makes the chapter-info page self-contained: the stylesheet, the script, the surah-icon font and
 * the revelation image are inlined into the HTML.
 *
 * Android used to serve these by intercepting `https://assets-file|font|image/...` inside its
 * `WebViewClient`, which is a WebView-specific mechanism with no `WKWebView` equivalent worth
 * reproducing. Inlining removes the interception entirely, so the same HTML string renders in a
 * WebView and a WKWebView alike — the page becomes data, not a mini web server.
 *
 * The assets total roughly 200 KB base64-encoded, all from Compose Resources, and the page is built
 * once per chapter view.
 */
@OptIn(ExperimentalEncodingApi::class)
internal object ChapterInfoAssets {

    private const val CSS_LINK =
        """<link type="text/css" href="https://assets-file/chapter_info/chapter_info_page.css" rel="stylesheet">"""

    private const val SCRIPT_TAG =
        """<script src="https://assets-file/chapter_info/chapter_info_script.js"></script>"""

    private const val FONT_URL = "https://assets-font/surah-icon"
    private const val IMAGE_URL = "https://assets-image/revelation-image"

    /** Replaces the external references in [template] with inline content. */
    suspend fun inline(template: String, revelationType: RevelationType): String {
        val css = Res.readBytes("files/chapter_info/chapter_info_page.css").decodeToString()
            .replace(FONT_URL, dataUri("font/ttf", Res.readBytes("font/suracon.ttf")))

        val js = Res.readBytes("files/chapter_info/chapter_info_script.js").decodeToString()

        val imageBytes = if (revelationType == RevelationType.meccan) {
            Res.readBytes("drawable/dr_makkah_old.webp")
        } else {
            Res.readBytes("drawable/dr_madina_old.webp")
        }

        return template
            .replace(CSS_LINK, "<style>$css</style>")
            // The script is inlined as-is; `</script>` cannot appear inside it, and it does not.
            .replace(SCRIPT_TAG, "<script>$js</script>")
            .replace(IMAGE_URL, dataUri("image/webp", imageBytes))
    }

    private fun dataUri(mimeType: String, bytes: ByteArray): String =
        "data:$mimeType;base64," + Base64.encode(bytes)
}
