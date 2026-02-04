package ru.offerfactory.promodisplay.ad.source.api

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.ad.source.api.models.AdClip

interface AdvertisementApi {

  suspend fun getClips(): Flow<List<AdClip>>

}