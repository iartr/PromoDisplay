package ru.offerfactory.promodisplay.ad.source.impl


import ru.offerfactory.promodisplay.ad.source.api.AdSourceApi
import ru.offerfactory.promodisplay.ad.source.api.models.AdAsset
import ru.offerfactory.promodisplay.ad.source.impl.data.db.AdAssetDao
import ru.offerfactory.promodisplay.ad.source.impl.data.db.AdAssetEntity
import java.io.File
import java.io.InputStream
import java.io.FileNotFoundException

class AdSourceImpl(
    private val adAssetDao: AdAssetDao,
    private val filesDir: File
): AdSourceApi {
    override suspend fun hasActualADAsset(adId: String, assetSHA256: ByteArray): Boolean {
        val entity = adAssetDao.getAssetById(adId) ?: return false
        if (!entity.sha256.contentEquals(assetSHA256)) return false

        val file = File(entity.localPath)
        return file.exists() && file.length() > 0L
    }

    override suspend fun loadADAsset(asset: AdAsset): Boolean {
        if (hasActualADAsset(asset.adId, asset.sha256)){
            return true
        }
        val adsDir = File(filesDir, "ads").apply { mkdirs() }
        val filePath = File(adsDir, asset.adId).absolutePath

        val entity = AdAssetEntity(
            adId = asset.adId,
            remoteUrl = asset.remoteUrl,
            sha256 = asset.sha256,
            localPath = filePath,
            isReady = false
        )

        adAssetDao.insertAsset(entity)

        return false
    }

    override suspend fun streamADAsset(asset: AdAsset): InputStream {
        val entity = adAssetDao.getAssetById(asset.adId)
        if (entity == null){
            throw IllegalStateException("Данные о ролике ${asset.adId} не найдены")
        }

        if (!entity.isReady) {
            throw IllegalStateException("Ролик ${asset.adId} еще не готов")
        }

        val path = entity.localPath
        val file = File(path)

        if (!file.exists()) {
            throw FileNotFoundException("Файл не найден: ${entity.localPath}")
        }

        return file.inputStream()
    }

}