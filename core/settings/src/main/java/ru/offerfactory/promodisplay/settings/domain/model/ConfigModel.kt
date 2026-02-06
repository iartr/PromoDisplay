package ru.offerfactory.promodisplay.settings.domain.model

import java.time.Instant

data class ConfigModel(
    val version: Int,
    val generatedAt: Instant,
    val serverTime: Instant,
    val pollInterval: Long,
    val items: List<AdItem>
)
