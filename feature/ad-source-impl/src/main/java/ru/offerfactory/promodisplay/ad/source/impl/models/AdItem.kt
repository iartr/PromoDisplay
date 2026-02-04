package ru.offerfactory.promodisplay.ad.source.impl.models

data class AdItem(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val asset: AdAsset
)

