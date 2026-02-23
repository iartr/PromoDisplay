package ru.offerfactory.promodisplay.ad.source.impl.domain.models

data class AdItem(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val asset: AdAsset,
    val downloadState: DownloadState
)

