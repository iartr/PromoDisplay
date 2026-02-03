package ru.offerfactory.promodisplay.network.domain.model

import ru.offerfactory.promodisplay.network.domain.model.util.EvictStrategy
import ru.offerfactory.promodisplay.network.domain.model.util.FallbackType

data class ConfigInfo(
    val version: Int,
    val pollIntervalSec: Int,
    val fallback: FallbackInfo,
    val downloadPolicy: DownloadPolicyInfo,
    val storagePolicy: StoragePolicyInfo,
    val items: List<ConfigItemInfo>
)

data class FallbackInfo(
    val type: FallbackType,
    val imageKey: String
)

data class DownloadPolicyInfo(
    val maxParallelDownloads: Int,
    val maxRetries: Int,
    val baseBackoffMs: Long,
    val maxBackoffMs: Long
)

data class StoragePolicyInfo(
    val minFreeBytes: Long,
    val evictStrategy: EvictStrategy,
    val pinnedAssetIds: List<String>
)

data class ConfigItemInfo(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val asset: AssetInfo
)

data class AssetInfo(
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val supportsRange: Boolean
)