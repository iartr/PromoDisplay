package ru.offerfactory.promodisplay.ad.source.api.models

data class AdAsset(
    val adId: String,
    val remoteUrl: String,
    val sha256: ByteArray
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AdAsset
        if (adId != other.adId) return false
        if (remoteUrl != other.remoteUrl) return false
        if (!sha256.contentEquals(other.sha256)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = adId.hashCode()
        result = 31 * result + remoteUrl.hashCode()
        result = 31 * result + sha256.contentHashCode()
        return result
    }
}

