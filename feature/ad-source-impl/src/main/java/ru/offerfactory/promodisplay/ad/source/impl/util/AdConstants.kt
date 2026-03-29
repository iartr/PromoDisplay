package ru.offerfactory.promodisplay.ad.source.impl.util

object AdConstants {
    const val MIME_TYPE_VIDEO = "video/mp4"
    const val AD_FILE_EXTENSION = "mp4"
    const val ADS_DIRECTORY_NAME = "ads"
    const val TEMP_FILE_SUFFIX = ".part"
    const val BAD_FILE_INFIX = ".bad"

    const val BUFFER_SIZE_BYTES = 64 * 1024
    const val RANGE_CHUNK_SIZE_BYTES = 1024 * 1024L

    const val HTTP_OK = 200
    const val HTTP_PARTIAL_CONTENT = 206
    const val HTTP_RANGE_NOT_SATISFIABLE = 416

    const val SHA256_ALGORITHM = "SHA-256"

    const val RESOLVE_MAX_ATTEMPTS = 3
    const val RANGE_REQUEST_MAX_ATTEMPTS = 3
    const val RETRY_BASE_DELAY_MS = 1000L
}