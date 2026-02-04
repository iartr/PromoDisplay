package ru.offerfactory.promodisplay.ad.source.api.models

data class AdClip(
    val id: String,
    val videoUri: String?,
    val isReady: Boolean
)