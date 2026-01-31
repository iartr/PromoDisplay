package ru.offerfactory.promodisplay.ad.source.api

import ru.offerfactory.promodisplay.ad.source.api.models.AdAsset
import java.io.InputStream

interface AdSourceApi {

    suspend fun hasActualADAsset(adId: String, assetSHA256: ByteArray): Boolean

    suspend fun loadADAsset(asset: AdAsset): Boolean

    suspend fun streamADAsset(asset: AdAsset): InputStream
}