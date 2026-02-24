package ru.offerfactory.promodisplay.syncer.impl.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class AssetDto(
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val sha256: String,
    val updatedAt: String,
    val supportsRange: Boolean
)