package ru.offerfactory.promodisplay.ad.source.impl.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface AdAssetDao {
    @Query("SELECT * FROM ad_asset WHERE adId = :id")
    suspend fun getAssetById(id: String): AdAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AdAssetEntity)

    @Query("DELETE FROM ad_asset WHERE adId = :id")
    suspend fun deleteById(id: String)
}