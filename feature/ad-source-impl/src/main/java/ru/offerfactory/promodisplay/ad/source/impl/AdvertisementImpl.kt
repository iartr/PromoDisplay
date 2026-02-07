package ru.offerfactory.promodisplay.ad.source.impl

import kotlinx.coroutines.flow.Flow
import ru.offerfactory.promodisplay.ad.source.api.AdvertisementApi
import ru.offerfactory.promodisplay.ad.source.api.domain.models.AdClip
import ru.offerfactory.promodisplay.settings.ConfigManager
import javax.inject.Inject

class AdvertisementImpl @Inject constructor(
    private val configManager: ConfigManager
): AdvertisementApi {
    override suspend fun getClips(): Flow<List<AdClip>> {
        TODO("Not yet implemented")
    }
}

