package ru.offerfactory.promodisplay.settings.model

data class ConfigModel(
    val version: Int,
    val pollInterval: Long,
    val items: List<AdItem>
)
