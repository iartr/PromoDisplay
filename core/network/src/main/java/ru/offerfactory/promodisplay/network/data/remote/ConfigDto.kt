package ru.offerfactory.promodisplay.network.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class FallbackDto(
    val type: String,          //enum
    val imageKey: String
)

@JsonClass(generateAdapter = true)
data class DownloadPolicyDto(
    val maxParallelDownloads: Int,
    val maxRetries: Int,
    val baseBackoffMs: Long,
    val maxBackoffMs: Long
)

@JsonClass(generateAdapter = true)
data class StoragePolicyDto(
    val minFreeBytes: Long,
    val evictStrategy: String,      //enum
    val pinnedAssetIds: List<String>
)

@JsonClass(generateAdapter = true)
data class ConfigItemDto(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val asset: AssetDto
)

@JsonClass(generateAdapter = true)
data class AssetDto(
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val sha256: String,
    val updatedAt: String,
    val supportsRange: Boolean
)