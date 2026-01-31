package ru.offerfactory.promodisplay.ad.source.impl.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ad_asset")
data class AdAssetEntity(
    @PrimaryKey val adId: String,
    val remoteUrl: String,
    val sha256: ByteArray,
    val localPath: String,
    val isReady: Boolean


)
