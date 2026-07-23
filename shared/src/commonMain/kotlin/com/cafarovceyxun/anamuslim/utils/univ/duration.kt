package com.cafarovceyxun.anamuslim.utils.univ

/**
 * Playback duration as `mm:ss`, or `h:mm:ss` once it passes an hour. Non-positive values (unknown
 * or not-yet-loaded durations) render as `0:00`.
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')

    return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
}
