package ru.offerfactory.promodisplay.ad.source.api.domain.models

data class AdClip(
    val id: String,
    val videoUri: String?,
    val isReady: Boolean
)