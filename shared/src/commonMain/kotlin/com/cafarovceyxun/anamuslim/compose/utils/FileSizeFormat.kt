package com.cafarovceyxun.anamuslim.compose.utils

/**
 * Human-readable byte count in the user's locale (e.g. `1.5 MB`) — the commonMain seam replacing
 * the Android-only `android.text.format.Formatter.formatFileSize(context, bytes)`.
 *
 * Android delegates to that same `Formatter`; iOS uses `NSByteCountFormatter`. Both render SI
 * units (kB/MB/GB), matching what the download UIs showed before.
 */
expect fun formatFileSize(bytes: Long): String
