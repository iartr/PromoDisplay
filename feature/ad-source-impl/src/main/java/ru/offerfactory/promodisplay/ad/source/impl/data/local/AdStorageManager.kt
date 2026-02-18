package ru.offerfactory.promodisplay.ad.source.impl.data.local

import android.content.Context
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdStorageManager @Inject constructor(
    context: Context
) {
    private val adsDir = File(context.filesDir, "ads").apply { mkdirs() }

    fun getFileForId(id: String): File = File(adsDir, "$id.mp4")

    fun cleanupOldFiles(activeIds: Set<String>) {
        adsDir.listFiles()?.forEach { file ->
            val id = file.nameWithoutExtension
            if (id !in activeIds) file.delete()
        }
    }
}