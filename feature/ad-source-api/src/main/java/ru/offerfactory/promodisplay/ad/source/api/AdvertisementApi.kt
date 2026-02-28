package ru.offerfactory.promodisplay.ad.source.api

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.ad.source.api.domain.models.AdClip

interface AdvertisementApi {
    fun getClips(): Flow<List<AdClip>>
}