package ru.offerfactory.promodisplay.ad.source.impl


import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.ad.source.api.models.AdClip
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdvertisementImpl @Inject constructor(

): AdvertisementApi {
    override suspend fun getClips(): Flow<List<AdClip>> {
        TODO("Not yet implemented")
    }


}