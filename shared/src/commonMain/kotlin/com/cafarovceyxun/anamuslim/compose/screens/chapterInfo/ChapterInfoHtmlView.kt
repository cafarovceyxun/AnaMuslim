package com.cafarovceyxun.anamuslim.compose.screens.chapterInfo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders the chapter-info page and reports verse references tapped inside it.
 *
 * [html] is self-contained (see `ChapterInfoAssets`), so an implementation only needs to load a
 * string and expose one callback to JavaScript: Android uses a `WebView`, iOS a `WKWebView`.
 * The page calls `window.ChapterInfoJSInterface.openReference(chapterNo, fromVerse, toVerse)`;
 * `-1, -1, -1` means "the whole chapter", which the caller resolves.
 */
@Composable
expect fun ChapterInfoHtmlView(
    html: String,
    onOpenReference: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit,
    modifier: Modifier,
)
