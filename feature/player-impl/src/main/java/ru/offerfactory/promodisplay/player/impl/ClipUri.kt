package ru.offerfactory.promodisplay.player.impl

import android.net.Uri
import java.io.File

internal object ClipUri {
    fun toUri(value: String): Uri {
        val v = value.trim()
        val hasScheme = v.contains("://") || v.startsWith("content:") || v.startsWith("file:")
        return if (hasScheme) Uri.parse(v) else Uri.fromFile(File(v))
    }
}