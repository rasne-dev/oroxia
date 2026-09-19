package com.oroxia.launcher.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.oroxia.launcher.data.local.dao.AppDao
import com.oroxia.launcher.data.local.dao.FolderDao
import com.oroxia.launcher.data.local.entity.AppEntity
import com.oroxia.launcher.data.local.entity.FolderEntity

@Database(
    entities = [AppEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OroxiaDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: OroxiaDatabase? = null

        fun getInstance(context: Context): OroxiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OroxiaDatabase::class.java,
                    "oroxia_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
