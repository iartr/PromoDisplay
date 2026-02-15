package ru.offerfactory.promodisplay.ad.source.api

import ru.offerfactory.promodisplay.ad.source.api.domain.models.AdClip
sealed class AdSourceState {
    object ShowFallback : AdSourceState()
    data class PlaylistReady(val clips: List<AdClip>): AdSourceState()
}