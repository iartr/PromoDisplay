package ru.offerfactory.promodisplay.player.impl

import android.net.Uri
import java.io.File

private const val SCHEME_PATTERN_1 = "://"
private const val SCHEME_PATTERN_2 = "content:"
private const val SCHEME_PATTERN_3 = "file:"

internal object ClipUri {
    fun toUri(value: String): Uri {
        val trimmedValue = value.trim()
        val hasScheme = trimmedValue.contains(SCHEME_PATTERN_1) ||
                trimmedValue.startsWith(SCHEME_PATTERN_2) ||
                trimmedValue.startsWith(SCHEME_PATTERN_3)

        return if (hasScheme) {
            Uri.parse(trimmedValue)
        } else {
            Uri.fromFile(File(trimmedValue))
        }
    }
}