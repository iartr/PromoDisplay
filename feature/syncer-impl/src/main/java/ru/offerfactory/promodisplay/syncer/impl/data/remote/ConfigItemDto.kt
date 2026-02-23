package ru.offerfactory.promodisplay.syncer.impl.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ConfigItemDto(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val asset: AssetDto
)