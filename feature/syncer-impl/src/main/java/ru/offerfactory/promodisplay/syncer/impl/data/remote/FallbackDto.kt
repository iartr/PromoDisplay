package ru.offerfactory.promodisplay.syncer.impl.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class FallbackDto(
    val type: String,          //enum
    val imageKey: String
)