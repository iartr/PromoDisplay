package ru.offerfactory.promodisplay.syncer.impl.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class StoragePolicyDto(
    val minFreeBytes: Long,
    val evictStrategy: String,      //enum
    val pinnedAssetIds: List<String>
)