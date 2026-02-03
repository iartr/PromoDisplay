package ru.offerfactory.promodisplay.network.data.mappers

import ru.offerfactory.promodisplay.network.data.remote.ConfigDto
import ru.offerfactory.promodisplay.network.data.remote.ConfigItemDto
import ru.offerfactory.promodisplay.network.domain.model.*
import ru.offerfactory.promodisplay.network.domain.model.util.EvictStrategy
import ru.offerfactory.promodisplay.network.domain.model.util.FallbackType

class ConfigMappers {
    fun String.toFallbackType(): FallbackType =
        FallbackType.entries.firstOrNull { it.name == this } ?: FallbackType.BUILTIN_IMAGE

    fun String.toEvictStrategy(): EvictStrategy =
        EvictStrategy.entries.firstOrNull { it.name == this } ?: EvictStrategy.LOW_PRIORITY_FIRST

    fun ConfigDto.toDomain(): ConfigInfo =
        ConfigInfo(
            version = configVersion,
            pollIntervalSec = pollIntervalSec,
            fallback = FallbackInfo(
                type = fallback.type.toFallbackType(),
                imageKey = fallback.imageKey
            ),
            downloadPolicy = DownloadPolicyInfo(
                maxParallelDownloads = downloadPolicy.maxParallelDownloads,
                maxRetries = downloadPolicy.maxRetries,
                baseBackoffMs = downloadPolicy.baseBackoffMs,
                maxBackoffMs = downloadPolicy.maxBackoffMs
            ),
            storagePolicy = StoragePolicyInfo(
                minFreeBytes = storagePolicy.minFreeBytes,
                evictStrategy = storagePolicy.evictStrategy.toEvictStrategy(),
                pinnedAssetIds = storagePolicy.pinnedAssetIds
            ),
            items = items.map { it.toDomainItem() }
        )

    private fun ConfigItemDto.toDomainItem(): ConfigItemInfo =
        ConfigItemInfo(
            id = id,
            priority = priority,
            repeatInCycle = repeatInCycle,
            asset = AssetInfo(
                mimeType = asset.mimeType,
                sizeBytes = asset.sizeBytes,
                durationMs = asset.durationMs,
                supportsRange = asset.supportsRange
            )
        )
}