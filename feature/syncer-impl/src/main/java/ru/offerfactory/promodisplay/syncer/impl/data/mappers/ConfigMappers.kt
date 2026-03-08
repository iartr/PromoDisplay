package ru.offerfactory.promodisplay.syncer.impl.data.mappers

import ru.offerfactory.promodisplay.settings.domain.model.AssetInfo
import ru.offerfactory.promodisplay.settings.domain.model.ConfigEntity
import ru.offerfactory.promodisplay.settings.domain.model.ConfigItemInfo
import ru.offerfactory.promodisplay.settings.domain.model.DownloadPolicyInfo
import ru.offerfactory.promodisplay.settings.domain.model.FallbackInfo
import ru.offerfactory.promodisplay.settings.domain.model.StoragePolicyInfo
import ru.offerfactory.promodisplay.settings.domain.model.util.EvictStrategy
import ru.offerfactory.promodisplay.settings.domain.model.util.FallbackType
import ru.offerfactory.promodisplay.syncer.impl.data.remote.ConfigDto
import ru.offerfactory.promodisplay.syncer.impl.data.remote.ConfigItemDto


class ConfigMappers {
    fun String.toFallbackType(): FallbackType =
        FallbackType.entries.firstOrNull { it.name == this } ?: FallbackType.BUILTIN_IMAGE

    fun String.toEvictStrategy(): EvictStrategy =
        EvictStrategy.entries.firstOrNull { it.name == this } ?: EvictStrategy.LOW_PRIORITY_FIRST

    fun ConfigDto.toDomain(): ConfigEntity =
        ConfigEntity(
            version = configVersion,
            pollIntervalSec = pollIntervalSec.toLong(),
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
                supportsRange = asset.supportsRange,
                sha256 = asset.sha256,
            )
        )
}