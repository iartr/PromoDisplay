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

    fun cleanupOldFiles(activeIds: Set<String>) {
        adsDir.listFiles()?.forEach { file ->
            val id = file.nameWithoutExtension
            if (id !in activeIds) file.delete()
        } ?: appLogger.logError(Throwable("Failed to list files in ads directory"))
    }
}