package ru.offerfactory.promodisplay.ad.source.api.models

data class AdSyncResult(
    val toDownload: List<AdAsset>,
    val toDelete: List<String>,
    val isUpToDate: Boolean
)