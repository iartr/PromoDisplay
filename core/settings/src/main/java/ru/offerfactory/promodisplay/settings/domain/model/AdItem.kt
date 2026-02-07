package ru.offerfactory.promodisplay.settings.domain.model

import java.time.Instant

data class AdItem(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val sizeBytes: Long,
    val sha256: String,
    val mimeType: String,
    val durationMs: Long,
    val updatedAt: Instant,
    val supportsRange: Boolean,
)
