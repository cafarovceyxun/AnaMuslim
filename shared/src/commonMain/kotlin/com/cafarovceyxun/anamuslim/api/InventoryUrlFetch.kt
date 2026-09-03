package com.cafarovceyxun.anamuslim.api

import com.cafarovceyxun.anamuslim.utils.app.DownloadSourceUtils

/**
 * Resolves inventory URLs based on current download source preference.
 *
 * - `ghraw://owner/repo/ref/path` - built for the active mirror by
 *   [DownloadSourceUtils.buildInventoryUrl]; each mirror needs a different URL shape.
 * - Any other string - returned as-is.
 */
fun resolveInventoryUrl(url: String): String {
    return if (url.startsWith("ghraw://")) {
        DownloadSourceUtils.buildInventoryUrl(url.removePrefix("ghraw://").trimStart('/'))
    } else {
        url
    }
}

/**
 * Streams an inventory URL, resolving the `ghraw://` scheme against the active mirror root.
 * The response body is only readable inside [block] (see [downloadStream]).
 */
suspend fun <T> streamInventory(url: String, block: suspend (HttpDownloadScope) -> T): T {
    return downloadStream(resolveInventoryUrl(url), block)
}
