package ru.offerfactory.promodisplay.settings.model

data class AdItem(
    val id: String,
    val priority: Int,
    val repeatInCycle: Int,
    val sizeBytes: Long,
    val sha256: String
)
