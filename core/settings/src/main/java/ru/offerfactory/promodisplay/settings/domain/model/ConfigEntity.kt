package ru.offerfactory.promodisplay.settings.domain.model

import kotlinx.serialization.Serializable
import ru.offerfactory.promodisplay.settings.domain.model.util.EvictStrategy
import ru.offerfactory.promodisplay.settings.domain.model.util.FallbackType

@Serializable
data class ConfigEntity(
    val version: Int,
    val pollIntervalSec: Long,
    val fallback: FallbackInfo,
    val downloadPolicy: DownloadPolicyInfo,
    val storagePolicy: StoragePolicyInfo,
    val items: List<ConfigItemInfo>
)

@Serializable
data class FallbackInfo(
    val type: FallbackType,
    val imageKey: String
)

@Serializable
data class DownloadPolicyInfo(
    val maxParallelDownloads: Int,
    val maxRetries: Int,
    val baseBackoffMs: Long,
    val maxBackoffMs: Long
)

@Serializable
data class StoragePolicyInfo(
    val minFreeBytes: Long,
    val evictStrategy: EvictStrategy,
    val pinnedAssetIds: List<String>
)

@Serializable
data class ConfigItemInfo(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val asset: AssetInfo,
)

@Serializable
data class AssetInfo(
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val supportsRange: Boolean,
    val sha256: String
)