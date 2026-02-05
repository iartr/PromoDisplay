package ru.offerfactory.promodisplay.ad.source.impl.domain.models

sealed class DownloadState {
    object Pending : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}