package ru.offerfactory.promodisplay.ad.source.impl.data.local

import android.content.Context
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import ru.offerfactory.promodisplay.ad.source.impl.util.AdConstants
import ru.offerfactory.promodisplay.logger.AppLogger

@Singleton
class AdStorageManager @Inject constructor(
    private val appLogger: AppLogger,
    context: Context
) {
    private val adsDir = File(context.filesDir, AdConstants.ADS_DIRECTORY_NAME).apply { mkdirs() }

    fun getFileForId(id: String): File = File(adsDir, "$id.${AdConstants.AD_FILE_EXTENSION}")

    fun getTempFileForId(id: String): File =
        File(adsDir, "$id.${AdConstants.AD_FILE_EXTENSION}${AdConstants.TEMP_FILE_SUFFIX}")

    fun getBadFileForId(id: String): File =
        File(adsDir, "$id${AdConstants.BAD_FILE_INFIX}.${AdConstants.AD_FILE_EXTENSION}")

    fun cleanupOldFiles(activeIds: Set<String>) {
        adsDir.listFiles()?.forEach { file ->
            val assetId = resolveAssetId(file)

            if (assetId == null) {
                file.delete()
                return@forEach
            }

            if (assetId !in activeIds) {
                file.delete()
            }
        } ?: appLogger.logError(Throwable("Failed to list files in ads directory"))
    }

    private fun resolveAssetId(file: File): String? {
        val name = file.name
        val extensionSuffix = ".${AdConstants.AD_FILE_EXTENSION}"
        val tempSuffix = "$extensionSuffix${AdConstants.TEMP_FILE_SUFFIX}"
        val badSuffix = "${AdConstants.BAD_FILE_INFIX}$extensionSuffix"

        return when {
            name.endsWith(tempSuffix) -> name.removeSuffix(tempSuffix)
            name.endsWith(badSuffix) -> name.removeSuffix(badSuffix)
            name.endsWith(extensionSuffix) -> name.removeSuffix(extensionSuffix)
            else -> null
        }
    }
}