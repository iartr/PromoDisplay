package ru.offerfactory.promodisplay.syncer.impl.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ConfigDto(
    val configVersion: Int,
    val generatedAt: String,
    val serverTime: String,
    val pollIntervalSec: Int,
    val fallback: FallbackDto,
    val downloadPolicy: DownloadPolicyDto,
    val storagePolicy: StoragePolicyDto,
    val items: List<ConfigItemDto>
)

@Serializable
data class FallbackDto(
    val type: String,          //enum
    val imageKey: String
)

@Serializable
data class DownloadPolicyDto(
    val maxParallelDownloads: Int,
    val maxRetries: Int,
    val baseBackoffMs: Long,
    val maxBackoffMs: Long
)

@Serializable
data class StoragePolicyDto(
    val minFreeBytes: Long,
    val evictStrategy: String,      //enum
    val pinnedAssetIds: List<String>
)

@Serializable
data class ConfigItemDto(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val asset: AssetDto
)

@Serializable
data class AssetDto(
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val sha256: String,
    val updatedAt: String,
    val supportsRange: Boolean
)