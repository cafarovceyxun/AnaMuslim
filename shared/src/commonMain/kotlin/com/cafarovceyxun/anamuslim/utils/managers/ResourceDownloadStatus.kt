package com.cafarovceyxun.anamuslim.utils.managers

sealed class ResourceDownloadStatus {
    object Idle : ResourceDownloadStatus()
    object Started : ResourceDownloadStatus()
    data class InProgress(val progress: Int) : ResourceDownloadStatus()
    object Completed : ResourceDownloadStatus()
    data class Failed(val error: String?) : ResourceDownloadStatus()
    object Cancelled : ResourceDownloadStatus()
}
