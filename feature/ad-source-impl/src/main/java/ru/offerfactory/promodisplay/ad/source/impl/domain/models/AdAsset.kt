package ru.offerfactory.promodisplay.ad.source.impl.domain.models

import java.time.Instant

data class AdAsset(
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val sha256: ByteArray,
    val updatedAt: Instant,
    val supportsRange: Boolean,
    val localPath: String?
)