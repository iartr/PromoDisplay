package ru.offerfactory.promodisplay.syncer.impl.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ConfigDto(
    val configVersion: Int,
    val generatedAt: String,
    val serverTime: String,
    val pollIntervalSec: Long,
    val fallback: FallbackDto,
    val downloadPolicy: DownloadPolicyDto,
    val storagePolicy: StoragePolicyDto,
    val items: List<ConfigItemDto>
)