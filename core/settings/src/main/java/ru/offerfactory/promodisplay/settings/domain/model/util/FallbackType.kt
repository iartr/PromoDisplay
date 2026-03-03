package ru.offerfactory.promodisplay.settings.domain.model.util

import kotlinx.serialization.Serializable

@Serializable
enum class FallbackType {
    BUILTIN_IMAGE,
    BUILTIN_VIDEO,
    REMOTE_ASSET
}