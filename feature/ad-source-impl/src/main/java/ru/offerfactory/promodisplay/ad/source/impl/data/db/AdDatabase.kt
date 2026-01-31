package ru.offerfactory.promodisplay.ad.source.impl.data.db

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [AdAssetEntity::class], version = 1)
abstract class AdDatabase: RoomDatabase() {
    abstract fun adAssetDao(): AdAssetDao
}