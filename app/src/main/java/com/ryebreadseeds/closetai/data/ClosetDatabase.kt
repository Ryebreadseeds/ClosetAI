package com.ryebreadseeds.closetai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ryebreadseeds.closetai.data.dao.ClosetDao
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.data.entity.DislikedComboEntity
import com.ryebreadseeds.closetai.data.entity.OutfitEntity

@Database(
    entities = [ClosetItemEntity::class, OutfitEntity::class, DislikedComboEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ClosetDatabase : RoomDatabase() {
    abstract fun closetDao(): ClosetDao

    companion object {
        @Volatile private var instance: ClosetDatabase? = null

        fun get(context: Context): ClosetDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClosetDatabase::class.java,
                    "closetai.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
