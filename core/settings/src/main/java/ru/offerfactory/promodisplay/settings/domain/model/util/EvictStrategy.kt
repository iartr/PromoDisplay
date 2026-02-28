package ru.offerfactory.promodisplay.settings.domain.model.util

import kotlinx.serialization.Serializable

@Serializable
enum class EvictStrategy {
    LOW_PRIORITY_FIRST,
    OLDEST_FIRST
}