package ru.offerfactory.promodisplay.syncer.impl.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class DownloadPolicyDto(
    val maxParallelDownloads: Int,
    val maxRetries: Int,
    val baseBackoffMs: Long,
    val maxBackoffMs: Long
)