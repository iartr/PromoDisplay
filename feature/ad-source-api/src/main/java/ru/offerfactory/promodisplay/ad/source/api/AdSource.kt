package ru.offerfactory.promodisplay.ad.source.api

import ru.offerfactory.promodisplay.ad.source.api.models.AdAsset
import java.io.InputStream

interface AdSourceApi {

    fun hasActualADAsset(adId: String, assetSha256: ByteArray): Boolean

    fun loadADAsset(asset: AdAsset): Boolean

    fun streamADAsset(asset: AdAsset): InputStream
}