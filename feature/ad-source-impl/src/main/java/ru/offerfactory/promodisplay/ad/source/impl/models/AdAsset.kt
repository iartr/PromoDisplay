package ru.offerfactory.promodisplay.ad.source.impl.models

import java.time.Instant

data class AdAsset(
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val sha256: ByteArray,
    val updatedAt: Instant,
    val supportsRange: Boolean,
    val localPath: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdAsset

        if (sizeBytes != other.sizeBytes) return false
        if (durationMs != other.durationMs) return false
        if (supportsRange != other.supportsRange) return false
        if (mimeType != other.mimeType) return false
        if (!sha256.contentEquals(other.sha256)) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sizeBytes.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + supportsRange.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + sha256.contentHashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
